package de.ovgu.spldev.featurecopp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.chocosolver.solver.Model;

import de.ovgu.spldev.featurecopp.config.Configuration;
import de.ovgu.spldev.featurecopp.filesystem.Filesystem;
import de.ovgu.spldev.featurecopp.filesystem.Finder;
import de.ovgu.spldev.featurecopp.filesystem.Finder.TYPE;
import de.ovgu.spldev.featurecopp.io.Merger;
import de.ovgu.spldev.featurecopp.lang.cpp.CPPAnalyzer;
import de.ovgu.spldev.featurecopp.lang.cpp.FeatureScopeManager;
import de.ovgu.spldev.featurecopp.log.Logger;
import de.ovgu.spldev.featurecopp.splmodel.ElifTree;
import de.ovgu.spldev.featurecopp.splmodel.ElseTree;
import de.ovgu.spldev.featurecopp.splmodel.FeatureModule;
import de.ovgu.spldev.featurecopp.splmodel.FeatureTable;
import de.ovgu.spldev.featurecopp.splmodel.IfTree;
import de.ovgu.spldev.featurecopp.splmodel.IfdefTree;
import de.ovgu.spldev.featurecopp.splmodel.IfndefTree;
import de.ovgu.spldev.featurecopp.time.Time;

/**
 * @author K. Ludwig
 */
public class Main {
	/**
	 * Show help screen
	 */
	public static void usage() {
		// TODO if more complex, use cli-arg-parser lib
		// @formatter:off
		System.out.println("usage: java -jar " + Configuration.APPLICATION_NAME + ".jar --(a)split[inputdir][expr-regex]|--merge[inputdir]|[--report]|[-areport]|[--help]");
		System.out.println("--help:  shows this screen");
		System.out.println("--split: extracts preprocessor controlled code of conditional directives from all files within");
		System.out.println("--asplit: behaves like --split but performs additional statistical syntax analysis of controlled code");
		System.out.println("\t'inputdir' and writes them back to 'inputdir" + Configuration.EXTRACT_DIR_SUFFIX + "'.");
		System.out.println("\tAdditionally a comprehensive analysis report is written as XML file:");
		System.out.println("\t'inputdir"
				+ Configuration.EXTRACT_DIR_SUFFIX + File.separator
				+ Configuration.MODULE_DIR + "'" + File.separator
				+ Configuration.XML_REPORT_FILE);
		System.out.println("\t'expr-regex': denotes a java regex which should match a class of feature expressions.");
		System.out.println("\t\tdefault: .* (all found feature expressions)");
		System.out.println("\t\texample: CONFIG_\\w+ finds all expression containing macro CONFIG succeeded by arbitrary alphanumeric characters.");
		System.out.println("\t\tNote! Quote '.*' to prevent certain shells from automatic directory expansion.");
		System.out.println("\t\texample: #else.* finds all expression containing #else-directives succeeded by arbitrary symbols.");
		System.out.println("--merge: performs vice versa, i.e. merges extracted controlled code into one code base.");
		System.out.println();
		System.out.println("If 'input_dir' is omitted current directory '.' is assumed.");
		System.out.println();
		System.out.println("For any found c-preprocessor conditionals the respective");
		System.out.println("controlled code is written to 'inputdir" + Configuration.EXTRACT_DIR_SUFFIX
				+ File.separator + Configuration.MODULE_DIR + "'");
		System.out.println("as follows: filename ::= unique_module_id \".fcp\"");
		System.out.println("For later inspection of the transformation process a log is");
		System.out.println("written to: " + Configuration.SPLIT_LOGFILE);
		System.out.println("--areport/report: performs read only run on source and creates the above mentioned XML report with or wothout analysis,");
		System.out.println("\t i.e. behaves like --asplit/split without physical separation");
		// @formatter:on
	}

	public static void split(final Path inputDir,
			final Pattern requestExprPattern) {
		try {
			PrintStream logfile = new PrintStream(Configuration.SPLIT_LOGFILE);
			Logger logger = new Logger();
			logger.setInfoStrms(System.out, logfile);
			logger.setFailStrms(System.err, logfile);
			// Logger is ready

			FeatureModule.initCSPLogger(Configuration.CSP_LOGFILE);
			// CSP logger is ready

			FeatureScopeManager.initASTStrm(Configuration.AST_LOGFILE);
			
			Configuration.readBlacklist(logger);

			// build absolute path to module/output directory
			Path outputDir = Filesystem.genPath(inputDir
					+ Configuration.EXTRACT_DIR_SUFFIX);
			// likewise with module directory
			Path moduleDir = Filesystem.genPath(outputDir.toString(),
					Configuration.MODULE_DIR);

			// remove output of previous runs -- set logger to null to
			// circumvent logging blowout
			// - since we write appending to feature files *.{h,c,fcp} a
			// previous
			// cleanup is necessary
			// - error message if dir non-existent can be ignored
			logger.writeInfo("Deleting " + outputDir.toString() + " ...");
			Filesystem.deleteDirRecursive(outputDir.toString(), null);

			logger.writeInfo("Starting split with"
					+ (Configuration.SKIP_ANALYSIS ? "out" : "") + " analysis");
			logger.writeInfo("Directory=" + inputDir.toString());
			logger.writeInfo("File pattern=" + Configuration.FIND_GLOB_PATTERN);
			logger.writeInfo("Feature/SD pattern=" + requestExprPattern);
			CPPAnalyzer cppAnalyzer = new CPPAnalyzer(logger, inputDir,
					outputDir, moduleDir, requestExprPattern);
			long start = Time.getCurrentNanoSecs();
			// ...and re-create (potentially empty, if nothing processed)
			try {
				Filesystem.makePath(outputDir);
				Filesystem.makePath(moduleDir);

				Finder.FindParameter fparam = new Finder.FindParameter(
						inputDir.toString(), TYPE.FILE, 0,
						Configuration.FIND_GLOB_PATTERN, false, false,
						cppAnalyzer);
				logger.writeInfo(fparam.toString());
				logger.writeInfo("Processed files: " + Finder.find(fparam));
			} catch (java.nio.file.AccessDeniedException perm_e) {
				logger.writeFail("Error accessing " + perm_e.getMessage());
				Configuration.purgeOutputDir(logger, outputDir.toString());
				logfile.close();
				return;
			}
			logger.writeInfo(String.format("Duration: %.3f secs",
					Time.nano2Sec(Time.elapsedNanoSecs(start))));
			// TODO
			cppAnalyzer.showStatistics();
			logger.writeInfo("Total unique feature count: "
					+ FeatureTable.getFeatureCount());
			logger.writeInfo("Total requested feature count: "
					+ FeatureTable.calcNumberOfRequestedFeatures());

			logfile.close();

			// TODO makes external reporting obsolete?
			FileWriter xmlOut = new FileWriter(moduleDir + File.separator
					+ Configuration.XML_REPORT_FILE);
			logger.writeInfo("Starting write-back of xml output...please wait!");
			// @formatter:off
			xmlOut.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"  + Configuration.LINE_SEPARATOR);
			xmlOut.write("<fcreport>" + Configuration.LINE_SEPARATOR);
			xmlOut.write(String.format(" <stats if=\"%d\" ifdef=\"%d\" ifndef=\"%d\" ifXtotal=\"%d\" "
					+ "elif=\"%d\" else=\"%d\" endif=\"%d\" textsize=\"%d\"/>%s", //linebreak
					IfTree.count,
					IfdefTree.count,
					IfndefTree.count,
					(IfTree.count + IfdefTree.count + IfndefTree.count),
					ElifTree.count,
					ElseTree.count,
					CPPAnalyzer.endifCount,
					CPPAnalyzer.textSize,
					Configuration.LINE_SEPARATOR
					));
			FeatureTable.writeXmlTo(1, xmlOut);
			logger.writeInfo("Finished write-back of xml output!");
			xmlOut.write("</fcreport>" + Configuration.LINE_SEPARATOR);
			// @formatter:on
			xmlOut.close();

		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}

	public static void merge(final Path inputDir) {
		// basically insufficient^^ TODO how to identify a valid
		// compo-annot-project and its original base?
		if (!inputDir.toString().endsWith(Configuration.EXTRACT_DIR_SUFFIX)) {
			System.err
					.println(inputDir
							+ " is not suffixed with '"
							+ Configuration.EXTRACT_DIR_SUFFIX
							+ "' and therefore seems not to be a valid splitted project! Refusing...");
			return;
		}
		PrintStream logfile;
		try {
			logfile = new PrintStream(Configuration.MERGE_LOGFILE);
			Logger logger = new Logger();
			logger.setInfoStrms(System.out, logfile);
			logger.setFailStrms(System.err, logfile);

			// build absolute path to module/output directory
			String sOutputDir = inputDir.toString().replaceAll(
					Configuration.EXTRACT_DIR_SUFFIX,
					Configuration.MERGE_DIR_SUFFIX);
			Path outputDir = Filesystem.genPath(sOutputDir);

			// logger is ready
			logger.writeInfo("Starting merge for " + inputDir);
			Merger merger = new Merger(logger, inputDir, outputDir);

			// removing old output directory
			logger.writeInfo("Deleting " + outputDir);
			Filesystem.deleteDirRecursive(sOutputDir, null);

			// hackish - deduce original project folder from inputDir
			String origDir = inputDir.toString().replaceAll(
					Configuration.EXTRACT_DIR_SUFFIX, "");

			// copy original base to output
			logger.writeInfo("Duplicating original project: " + origDir
					+ " -> " + outputDir);
			Filesystem.copyDirRecursive(origDir.toString(), sOutputDir, null);

			// re-integration -> overwriting of files from orig which are
			// affected by split/merge
			long start = Time.getCurrentNanoSecs();
			try {
				Finder.FindParameter fparam = new Finder.FindParameter(
						inputDir.toString(), TYPE.FILE, 0,
						Configuration.FIND_GLOB_PATTERN, false, false, merger);
				logger.writeInfo(fparam.toString());
				logger.writeInfo("Processed files: " + Finder.find(fparam));
			} catch (java.nio.file.AccessDeniedException perm_e) {
				logger.writeFail("Error accessing " + perm_e.getMessage());
				Configuration.purgeOutputDir(logger, outputDir.toString());
				logfile.close();
				return;
			} catch (Merger.MergerException me) {
				logger.writeFail(me.getMessage());
				Configuration.purgeOutputDir(logger, outputDir.toString());
				logfile.close();
				return;
			}

			logger.writeInfo(String.format("Duration: %.3f secs",
					Time.nano2Sec(Time.elapsedNanoSecs(start))));

			logfile.close();
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}

	}

	/**
	 * Discriminate mode of invocation and perform corresponding action.
	 * 
	 * @param args
	 *            argument vector
	 */
	public static void main(String[] args) {
		// preset default to resolved '.'
		Path inputDir = Paths.get(".").toAbsolutePath().normalize();
		if (args.length < 1) {
			usage();
			return;
		}
		if (args.length > 0) {
			// input directory set?
			if (args.length >= 2) {
				inputDir = Paths.get(args[1]).toAbsolutePath().normalize();
			}
			switch (args[0]) {
			case "--areport": // report only, with analysis (expensive)
			case "--report": // xml report only without analysis (very cheap)
				Configuration.REPORT_ONLY = true;
			case "--asplit": // split with analysis (very expensive)
				if (!"--report".equals(args[0])) { // ugly
					Configuration.SKIP_ANALYSIS = false;
				}
			case "--split": { // split without analysis (cheap)
				// default is all feature expressions
				Pattern searchPattern = Pattern.compile(".*");
				// since eclipse expands ".*" as argument to cwd resulting in a list of entries (.e.g., .classpath, .settings,...) -> strange		
				// user 
				if (args.length >= 3 && ! searchPattern.matcher(args[2]).matches()) {
					searchPattern = Pattern.compile(args[2]);
				}
				split(inputDir, searchPattern);
				break;
			}
			case "--merge":
				merge(inputDir);
				break;
			case "--help":
				usage();
				return;
			default:
				System.out.println("Unknown option: " + args[0]);
				usage();
				return;
			}
		}
	}
}
