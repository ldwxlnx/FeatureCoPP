package de.ovgu.spldev.featurecopp.lang.cpp;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

import de.ovgu.spldev.featurecopp.config.Configuration;
import de.ovgu.spldev.featurecopp.config.Configuration.UserConf;
import de.ovgu.spldev.featurecopp.filesystem.Filesystem;
import de.ovgu.spldev.featurecopp.filesystem.Finder;
import de.ovgu.spldev.featurecopp.filesystem.Finder.Processable;
import de.ovgu.spldev.featurecopp.lang.cpp.ExpressionParser.ObjMacroHistogram;
import de.ovgu.spldev.featurecopp.log.Logger;
import de.ovgu.spldev.featurecopp.splmodel.ElifTree;
import de.ovgu.spldev.featurecopp.splmodel.ElseTree;
import de.ovgu.spldev.featurecopp.splmodel.FeatureTable;
import de.ovgu.spldev.featurecopp.splmodel.FeatureTable.Quadruple;
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

		if(userConf.isBlackListed(fso)) {
			logger.writeFail("Refused processing " + fso + " due to existing blacklist entry!");
			return;
		}
		logger.writeInfo("Processing " + fso);
		try {
			scan(fso);
		} catch (Exception e) {
			// smth regarding input file went wrong
			logger.writeFail(e.getMessage());
			e.printStackTrace();
		}
	}	
	/** Token classification, also provided to JFlex spec */
	public static enum TYPE {
		SRC, IF, IFDEF, IFNDEF, ELIF, ELSE, ENDIF, COMMENT, LINECOMMENT, DIRLT, LINETERM, UNDEF, ERROR
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

	public CPPAnalyzer(Logger logger, UserConf userConf) {
		this.logger = logger;
		this.userConf = userConf;
		this.inputDir = userConf.getInputDirectory();
		this.outputDir = userConf.getOutputDirectory();
		this.featureScopeManager = new FeatureScopeManager(userConf.getModuleDirectory(), logger, userConf.getMacroPattern());
		this.cppScanner = new CPPScanner();
		this.cppScanner.debug(false);
		this.requestExprPattern = userConf.getMacroPattern();
	}

	public void scan(final Path currentFile)
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
//					System.err.println(symVal);
//					System.err.println(sym.line);
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
							//throw new Exception(String.format("Line %d: %s has no preceding #if*", sym.line, symVal));
							handleUnbalancedFile(dstFile, String.format("Line %d: %s has no preceding #if*", sym.line, symVal));
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
//					System.err.println(symVal);
//					System.err.println(sym.line);
					if(featureScopeManager.size() == 1 || featureScopeManager.getCurrentNumOfBranches() == 0) {
						//throw new Exception(String.format("Line %d: %s has no preceding #if*", sym.line, symVal));						
						handleUnbalancedFile(dstFile, String.format("Line %d: %s has no preceding #if*", sym.line, symVal));
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
//					System.err.println(symVal);
//					System.err.println(sym.line);
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
				case ERROR: { // ambiguous tokens may follow
					//System.err.println(symVal);
					//System.err.println(sym.line);
					String symbols = scanPastLineBreak();
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
				e.printStackTrace();
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
			logger.writeInfo("--");
			logger.writeInfo("Statistics:");
			// @formatter:off
			FeatureTable.DirectiveCount directiveCountTotal = FeatureTable.countRequestedDirectives(false);
			FeatureTable.DirectiveCount directiveLoFTotal = FeatureTable.countRequestedDirectives(true);
			FeatureTable.DirectiveCount directiveCountAbsence = FeatureTable.countRequestedSimpleAbsenceDirectives(false);
			FeatureTable.DirectiveCount directiveLoFAbsence = FeatureTable.countRequestedSimpleAbsenceDirectives(true);
			FeatureTable.DirectiveCount directiveCountPresence = FeatureTable.countRequestedSimplePresenceDirectives(false);
			FeatureTable.DirectiveCount directiveLoFPresence = FeatureTable.countRequestedSimplePresenceDirectives(true);
			FeatureTable.DirectiveCount directiveCountComplex = FeatureTable.countRequestedNonSimpleDirectives(false);
			FeatureTable.DirectiveCount directiveLoFComplex = FeatureTable.countRequestedNonSimpleDirectives(true);
			logger.writeInfo(String.format("VP:     [%6s||%13s|%13s|%13s|%13s||%13s]", "Total", 
					"Analyzed", "Absence", "Presence", "CC", "Other"));
			logger.writeInfo(String.format("        [%6s||%6s/%6s|%6s/%6s|%6s/%6s|%6s/%6s||%6s/%6s]",
					"N", "N", "LoF", "N", "LoF", "N", "LoF", "N", "LoF", "N", "LoF"));
			logger.writeInfo(String.format("#if     [%6d||%6d/%6d|%6d/%6d|%6d/%6d|%6d/%6d||%6d/%6d]",
					IfTree.count,
					directiveCountTotal.getIfCount(), directiveLoFTotal.getIfCount(),
					directiveCountAbsence.getIfCount(), directiveLoFAbsence.getIfCount(),
					directiveCountPresence.getIfCount(), directiveLoFPresence.getIfCount(),
					directiveCountAbsence.getIfCount(), directiveLoFAbsence.getIfCount(),
					directiveCountComplex.getIfCount(), directiveLoFComplex.getIfCount()
					));
			logger.writeInfo(String.format("#ifdef  [%6d||%6d/%6d|%6d/%6d|%6d/%6d|%6d/%6d||%6d/%6d]",
					IfdefTree.count,
					directiveCountTotal.getIfdefCount(), directiveLoFTotal.getIfdefCount(),
					directiveCountAbsence.getIfdefCount(), directiveLoFAbsence.getIfdefCount(),
					directiveCountPresence.getIfdefCount(), directiveLoFPresence.getIfdefCount(),
					0, 0,
					directiveCountComplex.getIfdefCount(), directiveLoFComplex.getIfdefCount()
					));
			logger.writeInfo(String.format("#ifndef [%6d||%6d/%6d|%6d/%6d|%6d/%6d|%6d/%6d||%6d/%6d]",
					IfndefTree.count,
					directiveCountTotal.getIfndefCount(), directiveLoFTotal.getIfndefCount(),
					directiveCountAbsence.getIfndefCount(), directiveLoFAbsence.getIfndefCount(),
					directiveCountPresence.getIfndefCount(), directiveLoFPresence.getIfndefCount(),
					directiveCountTotal.getIfndefCount(), directiveLoFTotal.getIfndefCount(),
					directiveCountComplex.getIfndefCount(), directiveLoFComplex.getIfndefCount()
					));
			logger.writeInfo(String.format("#elif   [%6d||%6d/%6d|%6d/%6d|%6d/%6d|%6d/%6d||%6d/%6d]",
					ElifTree.count,
					directiveCountTotal.getElifCount(), directiveLoFTotal.getElifCount(),
					directiveCountAbsence.getElifCount(), directiveLoFAbsence.getElifCount(),
					directiveCountPresence.getElifCount(), directiveLoFPresence.getElifCount(),
					directiveCountAbsence.getElifCount(), directiveLoFAbsence.getElifCount(),
					directiveCountComplex.getElifCount(), directiveLoFComplex.getElifCount()
					));
			logger.writeInfo(String.format("#else   [%6d||%6d/%6d|%6d/%6d|%6d/%6d|%6d/%6d||%6d/%6d]",
					ElseTree.count,
					directiveCountTotal.getElseCount(), directiveLoFTotal.getElseCount(),
					directiveCountAbsence.getElseCount(), directiveLoFAbsence.getElseCount(),
					directiveCountPresence.getElseCount(), directiveLoFPresence.getElseCount(),
					directiveCountTotal.getElseCount(), directiveLoFTotal.getElseCount(),
					directiveCountComplex.getElseCount(), directiveLoFComplex.getElseCount()
					));
			long all_count = directiveCountTotal.getIfCount() + directiveCountTotal.getIfdefCount() + directiveCountTotal.getIfndefCount() + directiveCountTotal.getElifCount() + directiveCountTotal.getElseCount();
			long all_lof = directiveLoFTotal.getIfCount() + directiveLoFTotal.getIfdefCount() + directiveLoFTotal.getIfndefCount() + directiveLoFTotal.getElifCount() + directiveLoFTotal.getElseCount();
			long cc_count = directiveCountAbsence.getIfCount() + directiveCountAbsence.getIfndefCount() + directiveCountAbsence.getElifCount() + directiveCountTotal.getElseCount();
			long cc_lof = directiveLoFAbsence.getIfCount() + directiveLoFAbsence.getIfndefCount() + directiveLoFAbsence.getElifCount() + directiveLoFTotal.getElseCount();
			logger.writeInfo(String.format(Locale.US, "#endif  [%6d||%6d/%6d=%12s%15s=%6d/%6d=>%%%2.2f/%2.2f",
					endifCount,
					// Total
					all_count,
					all_lof,
					"Analyzed_sum",
					"CC_sum",
					// CC
					cc_count,
					cc_lof,
					// ratio
					(cc_count * 1.0/all_count) * 100,
					(cc_lof * 1.0/all_lof) * 100));
			logger.writeInfo(String.format("Opening/Closing [%d/%d]", endifCount, (IfTree.count + IfdefTree.count + IfndefTree.count)));
			// @formatter:on
			// SCATTERING DEGREE
			logger.writeInfo("Scattering Degree (SD):");
			ObjMacroHistogram objMacroHistogram = ExpressionParser.getObjMacroHistogramProj();
			logger.writeInfo(String.format("SD top %d rank excl. #else=%s",
					Configuration.LOGFILE_SD_TOP_N, objMacroHistogram.topNtoString(Configuration.LOGFILE_SD_TOP_N)));
			logger.writeInfo("Most scattered feature expression: " + objMacroHistogram.getMostScatteredObjMacro());
			int sd_old_sum = objMacroHistogram.accumulateValues();
			
			ObjMacroHistogram objMacroHistogramInclElse = ExpressionParser.getObjMacroHistogramProjInclElse();
			logger.writeInfo(String.format("SD top %d rank incl. #else=%s",
					Configuration.LOGFILE_SD_TOP_N, objMacroHistogramInclElse.topNtoString(Configuration.LOGFILE_SD_TOP_N)));	
			logger.writeInfo("Most scattered feature expression: " + objMacroHistogramInclElse.getMostScatteredObjMacro());
			long sd_new_sum = objMacroHistogramInclElse.accumulateValues();
			
			logger.writeInfo(String.format(Locale.US, "SD sum excl. #else=[%6d]", sd_old_sum));
			logger.writeInfo(String.format(Locale.US, "SD sum incl. #else=[%6d]", sd_new_sum));
			logger.writeInfo(String.format(Locale.US, "SD missed:   delta=[%6d] (ratio=%.3f)", sd_new_sum - sd_old_sum, (1.0 - ((sd_old_sum * 1.0) / (sd_new_sum * 1.0)))));
			// TANGLING DEGREE
			long td_new_sum = FeatureTable.summarizeTanglingDegree(true);
			long td_old_sum = FeatureTable.summarizeTanglingDegree(false);
			logger.writeInfo("Tangling Degree (TD):");
			logger.writeInfo(String.format(Locale.US, "TD sum excl. #else=[%6d]", td_old_sum));
			Quadruple<String, String, String, Integer> tdMaxExcl = FeatureTable.getTDMax(false);
			Quadruple<String, String, String, Integer> tdMaxIncl = FeatureTable.getTDMax(true);
			logger.writeInfo(String.format("Most tangled feature expression=%s", tdMaxExcl.s));
			logger.writeInfo(String.format("Keyword=%s, TD=%d", tdMaxExcl.t, tdMaxExcl.v));
			logger.writeInfo(String.format("e.g., in file=%s", tdMaxExcl.u));
			logger.writeInfo(String.format(Locale.US, "TD sum incl. #else=[%6d]", td_new_sum));
			logger.writeInfo(String.format("Most tangled feature expression=%s", tdMaxIncl.s));
			logger.writeInfo(String.format("Keyword=%s, TD=%d", tdMaxIncl.t, tdMaxIncl.v));
			logger.writeInfo(String.format("e.g., in file=%s", tdMaxIncl.u));
			logger.writeInfo(String.format(Locale.US, "TD missed    delta=[%6d] (ratio=%.3f)", td_new_sum - td_old_sum, (1.0 - ((td_old_sum * 1.0) / (td_new_sum * 1.0)))));
			
			logger.writeInfo(String.format(
					"Processed text size (UTF-8): %d bytes (%03.3fMiB)",
					textSize, textSize * 1.0 / (1024 * 1024)));
			// clear static tables in Parser for further invocations
			objMacroHistogram.clear();
			objMacroHistogramInclElse.clear();
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
			throws Exception {
		// TODO read tokens until newline (EXPR, etc.)
		StringBuilder sb = new StringBuilder();
		while (true) {
			Token currToken = cppScanner.yylex();
			if(currToken == null) {
				throw new Exception(String.format("Unexpected null token in [%s]", sb.toString()));
			}
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
	private Configuration.UserConf userConf;
}
