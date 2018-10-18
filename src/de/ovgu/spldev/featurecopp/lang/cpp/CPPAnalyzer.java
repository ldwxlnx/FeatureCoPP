package de.ovgu.spldev.featurecopp.lang.cpp;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.regex.Pattern;

import de.ovgu.spldev.featurecopp.config.Configuration;
import de.ovgu.spldev.featurecopp.filesystem.Filesystem;
import de.ovgu.spldev.featurecopp.filesystem.Finder;
import de.ovgu.spldev.featurecopp.filesystem.Finder.Processable;
import de.ovgu.spldev.featurecopp.lang.cpp.ExpressionParser.ObjMacroHistogram;
import de.ovgu.spldev.featurecopp.log.Logger;
import de.ovgu.spldev.featurecopp.splmodel.ElifTree;
import de.ovgu.spldev.featurecopp.splmodel.ElseTree;
import de.ovgu.spldev.featurecopp.splmodel.IfTree;
import de.ovgu.spldev.featurecopp.splmodel.IfdefTree;
import de.ovgu.spldev.featurecopp.splmodel.IfndefTree;

/**
 * A specialized C-preprocessor analyzer. Compatible to a file-by-file
 * file-system traversal implementing Finder.Processable.
 * 
 * @author K. Ludwig
 * @see Finder.Processable
 */
public final class CPPAnalyzer implements Processable {
	/**
	 * Performs a CPP related scan on give file system object.
	 */
	@Override
	public void process(Path fso) {
		// needed for output file placement
		// this.currentFile = fso;

		String blacklistedFile = Configuration.isBlacklisted(fso);		
		if(blacklistedFile != null) {
			logger.writeFail("Refused processing " + fso + " due to existing blacklist entry!");
			return;
		}
		logger.writeInfo("Processing " + fso);
		try {
			scan(false, fso);
		} catch (Exception e) {
			// smth regarding input file went wrong
			logger.writeFail(e.getMessage());
			e.printStackTrace();
		}
	}

	/** Token classification, also provided to JFlex spec */
	public static enum TYPE {
		SRC, IF, IFDEF, IFNDEF, ELIF, ELSE, ENDIF, COMMENT, LINECOMMENT, DIRLT, LINETERM, UNDEF
	};

	public static class Token {
		public Token(TYPE type, int line, int column, String value) {
			this.type = type;
			this.line = line;
			this.column = column;
			this.value = value;
		}

		public Token(TYPE type, int line, int column) {
			this.type = type;
			this.line = line;
			this.column = column;
		}

		private TYPE type;
		private int line;
		private int column;
		private String value;
	}

	public CPPAnalyzer(Logger logger, final Path inputDir,
			final Path outputDir, final Path moduleDir,
			final Pattern requestExprPattern, final Pattern sdPattern) {
		this.logger = logger;
		this.inputDir = inputDir;
		this.outputDir = outputDir;
		this.featureScopeManager = new FeatureScopeManager(moduleDir, logger, sdPattern);
		this.cppScanner = new CPPScanner();
		this.cppScanner.debug(false);
		this.requestExprPattern = requestExprPattern;
	}

	public void scan(boolean showLexerOutput, final Path currentFile)
			throws Exception {
		cppScanner.setReader(currentReader = new InputStreamReader(new FileInputStream(currentFile.toString()), "UTF-8"));
//		cppScanner.setReader(currentReader = new FileReader(currentFile
//				.toFile()));
		Token sym = null;
		String condExpr = null;
		int openCount = 0; //if(n/def)
		int closeCount = 0;// endif
		int charCount = 0;
		// linenumber from token (needed in catch, when Token is potentially freed)
		int currLine = 0;
		// base feature file (uncontrolled code) is top-most
		Path dstFile = Filesystem.substitute(currentFile,
				inputDir, outputDir);
		featureScopeManager.addBasefile(dstFile);//foo/bar/baz.c -> foo_split/bar/baz.c
		featureScopeManager.setCurrentOriginalSourceFile(currentFile);
		try {
			while ((sym = cppScanner.yylex()) != null) {			
				currLine = sym.line;
				String symVal = sym.value; // Token data
				switch (sym.type) {
				case IF:
				case IFDEF:
				case IFNDEF: { 
					openCount++;
				}
				case ELIF: {
					//System.err.println(symVal);
					// store original directive to provide to hook file
					StringBuilder originalSrc = new StringBuilder(symVal);
					// retrieve complete feature expression -- all meanwhile
					// detected tokens are added to originalSrc-StringBuilder
					condExpr = symVal
							+ collectFeatureExpression(originalSrc);
					// write back original conditional to corresponding hook
					// file (base or feature)
					if (sym.type == TYPE.ELIF) {
						if(featureScopeManager.getCurrentNumOfBranches() == 0) {
							//throw new Exception(symVal + " has no previous #if*");
							handleUnbalancedFile(dstFile, symVal + " has no previous #if*");
							return;
						}
						// SIBLING ON TOP
						featureScopeManager.cacheInPrevMostRecent(originalSrc
								.toString());
					} else {
						// ENCLOSING ON TOP OR BASE
						featureScopeManager.cacheInMostRecent(originalSrc
								.toString());
					}
					// maintain a new scope
					featureScopeManager.addIfElif(requestExprPattern,
							condExpr, sym.line, sym.type);
					break;
				}
				case ELSE: { // SIBLING ON TOP
					//System.err.println(symVal);
					if(featureScopeManager.getCurrentNumOfBranches() == 0) {
						//throw new Exception(symVal + " has no previous #if*");
						handleUnbalancedFile(dstFile, symVal + " has no previous #if*");
						return;
					}
					// remove line terminators after directive token (otherwise
					// 'prepended' to controlled code)
					String symbols = scanPastLineBreak();
					// write collected symbols until line break to corresponding
					// hook file (base or feature)
					featureScopeManager.cacheInPrevMostRecent(symVal + symbols);
					// maintain a new scope
					featureScopeManager.addElse(requestExprPattern, sym.line);
					break;
				}
				case ENDIF: { // SIBLING ON TOP
					//System.err.println(symVal);
					endifCount++;
					closeCount++;
					// remove line terminators after directive token
					// (otherwise 'prepended' to controlled code)
					String symbols = scanPastLineBreak();
					// unmatched endif?
					if (featureScopeManager.hasConditionals()) {
						// pop previous scope
						featureScopeManager.handleEndif(sym.line);
					}
					else {	
						//featureScopeManager.dumpStack(System.out);
//						throw new Exception(String.format(
//								"Line %d: Unpaired #endif detected (o=%d<->c=%d)!", sym.line, openCount, closeCount));
						handleUnbalancedFile(dstFile, String.format(
								"Line %d: Unpaired #endif detected (o=%d<->c=%d)!", sym.line, openCount, closeCount));
						return;
					}				
					// write collected symbols until line break to
					// corresponding hook file (base or feature)
					// -> top most is now enclosing
					featureScopeManager.cacheInMostRecent(symVal + symbols);				
					break;
				}
				default: // CONTROLLED CODE (SINGLE SYMBOLS) OR ANYTHING ELSE
					//System.out.print(symVal);
					featureScopeManager.cacheInMostRecent(symVal);
					break;
				}
			} /* while ((sym = cppScanner.yylex()) != null) */
			if(openCount != closeCount) {				
				//throw new Exception("Asymmetric opening and closing of directives (o=" + openCount + "<->c=" +closeCount + ")!");
				handleUnbalancedFile(dstFile, "Asymmetric opening and closing of directives (o=" + openCount + "<->c=" +closeCount + ")!");
				return;
			}
			charCount += cppScanner.numOfChars();
			textSize += charCount;
			logger.writeInfo("Done (" + charCount + " bytes processed)!");
		} catch (Exception e) {
			if (logger != null) {
				logger.writeFail(String.format("Line %d: %s", currLine,
						e.getMessage()));
				//e.printStackTrace();
				// COMPLETENESS! these errors (e.g. asymmetric directives) result in unsafe output, hence purging and exit
				// NOT CRITICAL: errors related to C-Parsing and CSP -> handled in FeatureScopeManager.writeBackControlledCodeCache
				Configuration.purgeOutputDir(logger, outputDir.toString());
				System.exit(1);
//				logger.writeFail(String.format("%d conditional groups left behind due to errors!",
//						featureScopeManager.numOfConditionalInProcess()));
			}
			//e.printStackTrace();
		}
		// stack teared down and base file written back
		cleanUp();
	}

	// TODO maybe a statistics class?
	public void showStatistics() {
		if (logger != null) {
			logger.writeInfo("Statistics:");
			logger.writeInfo("#if=" + IfTree.count + ";#ifdef="
					+ IfdefTree.count + ";#ifndef=" + IfndefTree.count
					+ ";Total="
					+ (IfTree.count + IfdefTree.count + IfndefTree.count));
			logger.writeInfo("#elif=" + ElifTree.count);
			logger.writeInfo("#else=" + ElseTree.count);
			logger.writeInfo("#endif=" + endifCount);			
			ObjMacroHistogram objMacroHistogram = ExpressionParser.getObjMacroHistogramProj();
			logger.writeInfo("SD=" + objMacroHistogram.toString());
			logger.writeInfo("SD_max=" + objMacroHistogram.getMostScatteredObjMacro());
			logger.writeInfo("SD_total="+ objMacroHistogram.getTotalObjMacroCount());
			logger.writeInfo("SD_sum=" + objMacroHistogram.accumulateValues());
			logger.writeInfo(String.format(
					"Processed text size (UTF-8): %d bytes (%03.3fMiB)",
					textSize, textSize * 1.0 / (1024 * 1024)));
		}
	}
	
	private void handleUnbalancedFile(final Path dstFile, final String msg) {
		logger.writeFail(msg);
		dstFile.toFile().delete();
		logger.writeFail("Deleted " + dstFile + " due to unbalanced conditional directives!");
	}

	/**
	 * Clean up actions since in case CPPAnalyzer is reused.
	 * 
	 * @throws Exception
	 */
	private void cleanUp() throws Exception {
		cppScanner.yyreset(currentReader);
		currentReader.close();
		featureScopeManager.cleanUp();
	}
	// TODO: WON'T WORK FOR OLD APPLE '\r' line breaks
	private String collectFeatureExpression(StringBuilder originalSrc)
			throws IOException {
		// TODO read tokens until newline (EXPR, etc.)
		StringBuilder sb = new StringBuilder();
		while (true) {
			Token currToken = cppScanner.yylex();
			String currTokenVal = currToken.value;
			// add directive expression tokens to hook file
			originalSrc.append(currTokenVal);
			// line comment ? newline is consumed during scanning! OR end-of-directive?
			if (currToken.type == TYPE.LINECOMMENT || currToken.type == TYPE.LINETERM) {
				break;
			}
			sb.append(currTokenVal);
		}
		return sb.toString();
	}
	// TODO: WON'T WORK FOR OLD APPLE '\r' line breaks
	/**
	 * Reads tokens until line break is detected. This is useful for omitting
	 * collection of line breaks after e.g. #else-directives
	 */
	private String scanPastLineBreak() throws IOException {
		Token nextToken = null;
		// grab all scanned tokens to add into destination file
		StringBuilder sb = new StringBuilder();
		do {
			nextToken = cppScanner.yylex();
			// End-of-file?
			if (nextToken == null) {
				break;
			}
			sb.append(nextToken.value);
			// line comment? newline is already consumed by scanner!
			if (nextToken.type == TYPE.LINECOMMENT) {
				break;
			}
		} while (nextToken.type != TYPE.LINETERM);
		return sb.toString();
	}

	public static long endifCount;
	public static long textSize;
	/** current input stream for scanner (n per run) */
	private Reader currentReader;
	/** manages nested scope structure and FeatureModule information */
	// private Scope scope;
	private FeatureScopeManager featureScopeManager;
	/** cxx-file scanner */
	private de.ovgu.spldev.featurecopp.lang.cpp.CPPScanner cppScanner;
	/** log writer (1 per run) */
	private Logger logger;
	/**
	 * source path portion to be substituted for each input file at by outputDir
	 * portion
	 */
	private Path inputDir;
	/**
	 * destination path portion to be substituted for each input file at write
	 * back
	 */
	private Path outputDir;
	/** a regular expression describing feature expression to get honored */
	private Pattern requestExprPattern;
}
