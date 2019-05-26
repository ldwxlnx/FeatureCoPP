package de.ovgu.spldev.featurecopp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.function.Function;
import java.util.regex.Pattern;

import de.ovgu.spldev.featurecopp.config.ConfigParserDriver;
import de.ovgu.spldev.featurecopp.config.Configuration;
import de.ovgu.spldev.featurecopp.config.Configuration.UserConf;
import de.ovgu.spldev.featurecopp.filesystem.Filesystem;
import de.ovgu.spldev.featurecopp.filesystem.Finder;
import de.ovgu.spldev.featurecopp.filesystem.Finder.TYPE;
import de.ovgu.spldev.featurecopp.io.Merger;
import de.ovgu.spldev.featurecopp.lang.cpp.CPPAnalyzer;
import de.ovgu.spldev.featurecopp.lang.cpp.ExpressionLexer;
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
	public static void usage(UserConf defaultConf) {
		// TODO if more complex, use cli-arg-parser lib
		// @formatter:off
		System.out.println("usage: java -jar " + Configuration.APPLICATION_NAME + ".jar [--config=\"/path/to/config\"|--help]");
		System.out.println("--help:  shows this screen");
		System.out.println("--config: performs processing for each configuration setting");
		System.out.println("In case " + Configuration.APPLICATION_NAME + " was invoked without any arguments,");
		System.out.println("a single processing based on the default configuration is performed, i.e.:");
		System.out.println(defaultConf);
		// @formatter:on
	}

	public static Void split(final UserConf config) {
		try {
			Logger logger = new Logger();
			if(config.isMakeDebugOutput()) {
				logger.useDebug();
			}
			logger.addInfoStream(System.out)
					.addRotatedLogFileToAllStreams(config.getLogFormat(),
							config.getLogRotateN())
					.addFailStream(System.err).addDebugStream(System.out);
			// Logger is ready
			logger.writeInfo("Starting split with"
					+ (Configuration.SKIP_ANALYSIS ? "out" : "") + " analysis");
			logger.writeInfo(String.format("Conf=%s", config));			

			// remove output of previous runs -- set logger to null to
			// circumvent logging blowout
			// - since we write appending to feature files *.{h,c,fcp} a
			// previous
			// cleanup is necessary
			// - error message if dir non-existent can be ignored
			logger.writeInfo("Deleting "
					+ config.getOutputDirectory().toString() + " ...");
			Filesystem.deleteDirRecursive(
					config.getOutputDirectory().toString(), null);

			logger.writeInfo("Feature/SD pattern=" + config.getMacroPattern());
			CPPAnalyzer cppAnalyzer = new CPPAnalyzer(logger, config);
			long start = Time.getCurrentNanoSecs();
			// ...and re-create (potentially empty, if nothing processed)
			try {
				Filesystem.makePath(config.getOutputDirectory());
				Filesystem.makePath(config.getModuleDirectory());

				Finder.FindParameter fparam = new Finder.FindParameter(
						config.getInputDirectory().toString(), TYPE.FILE, 0,
						config.getFilePattern(),
						Configuration.FIND_PATTERN_STRATEGY, false, false,
						cppAnalyzer);
				logger.writeInfo(fparam.toString());
				logger.writeInfo("Processed files: " + Finder.find(fparam));
				if (config.isMakeDebugOutput()) {
					ExpressionLexer.writeExpressionSymbolsToLog(logger);
				}
			} catch (IOException e) {				
				logger.writeFail(String.format("Could not access %s", e.getMessage()));
				Configuration.purgeOutputDir(logger,
						config.getOutputDirectory().toString());
				logger.closeAllStreams();
				return null;
			} 
			logger.writeInfo(String.format("Duration: %.3f secs",
					Time.nano2Sec(Time.elapsedNanoSecs(start))));
			// TODO
			cppAnalyzer.showStatistics();
			logger.writeInfo(String.format("Unique features  [Total/Requested]=[%6d/%6d]",
					FeatureTable.getFeatureCount(),
					FeatureTable.calcNumberOfRequestedFeatures()));
			logger.writeInfo(String.format("Variation points [Total/Requested]=[%6d/%6d]",
					FeatureTable.calcTotalNumberOfRoles(),
					FeatureTable.calcNumberOfRequestedRoles()));
			logger.closeAllStreams();

			// TODO makes external reporting obsolete?
			FileWriter xmlOut = new FileWriter(config.getModuleDirectory()
					+ File.separator + Configuration.XML_REPORT_FILE);
			logger.writeInfo(
					"Starting write-back of xml output...please wait!");
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
		return null;
	}

	public static Void merge(final UserConf config) {
		try {
			Logger logger = new Logger();
			if(config.isMakeDebugOutput()) {
				logger.useDebug();
			}
			logger.addInfoStream(System.out)
					.addRotatedLogFileToAllStreams(config.getLogFormat(),
							config.getLogRotateN())
					.addFailStream(System.err).addDebugStream(System.out);

			// logger is ready
			logger.writeInfo(
					"Starting merge for " + config.getInputDirectory());
			logger.writeInfo(String.format("Conf=%s", config));
			Merger merger = new Merger(logger, config.getInputDirectory(),
					config.getOutputDirectory());

			// removing old output directory
			logger.writeInfo("Deleting " + config.getOutputDirectory());
			Filesystem.deleteDirRecursive(
					config.getOutputDirectory().toString(), null);

			// copy original base to output
			logger.writeInfo("Duplicating original project: "
					+ config.getOriginalDirectory() + " -> "
					+ config.getOutputDirectory());
			Filesystem.copyDirRecursive(
					config.getOriginalDirectory().toString(),
					config.getOutputDirectory().toString(), null);

			// re-integration -> overwriting of files from orig which are
			// affected by split/merge
			long start = Time.getCurrentNanoSecs();
			try {
				Finder.FindParameter fparam = new Finder.FindParameter(
						config.getInputDirectory().toString(), TYPE.FILE, 0,
						config.getFilePattern(),
						Configuration.FIND_PATTERN_STRATEGY, false, false,
						merger);
				logger.writeInfo(fparam.toString());
				logger.writeInfo("Processed files: " + Finder.find(fparam));
			} catch (IOException e) {
				logger.writeFail(String.format("Could not access %s", e.getMessage()));				
				Configuration.purgeOutputDir(logger, config.getOutputDirectory().toString());
				logger.closeAllStreams();
				return null;
			}

			logger.writeInfo(String.format("Duration: %.3f secs",
					Time.nano2Sec(Time.elapsedNanoSecs(start))));

			logger.closeAllStreams();
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Discriminate mode of invocation and perform corresponding action.
	 * 
	 * @param args
	 *            argument vector
	 */
	public static void main(String[] args) {
		HashSet<Configuration.UserConf> configs = null;

		try {
			configs = parseArgs(args);
		} catch (Exception e) {
			System.err
					.println(String.format("Fatal Error [%s]", e.getMessage()));
			//e.printStackTrace();
			System.exit(1);
		}
		if (configs != null) {
			// for every user conf, do respective processing
			configs.stream().sorted().forEach((userConf) -> {
				if (!userConf.isOverriden()) {
					// default action is split
					Function<UserConf, Void> modeFunction = Main::split;
					Configuration.Mode mode = userConf.getMode();
					switch (mode) {
					case merge: {
						modeFunction = Main::merge;
						break;
					}
					case areport: {
						Configuration.REPORT_ONLY = true;
						Configuration.SKIP_ANALYSIS = false;
						break;
					}
					case report:
						Configuration.REPORT_ONLY = true;
						Configuration.SKIP_ANALYSIS = true;
						break;
					case asplit: {
						Configuration.REPORT_ONLY = false;
						Configuration.SKIP_ANALYSIS = false;
						break;
					}
					case split:						
						Configuration.REPORT_ONLY = false;
						Configuration.SKIP_ANALYSIS = true;
						break;
					}
					modeFunction.apply(userConf);
					FeatureTable.reinit();
				}
			});
		}
	}

	private static HashSet<UserConf> parseArgs(String[] args) throws Exception {
		HashSet<Configuration.UserConf> configs = null;
		UserConf defaultConfig = Configuration.getDefault();
		// use built-in default config
		if (args.length == 0) {
			configs = new HashSet<>();
			configs.add(defaultConfig);
		}
		// config submitted by user OR --help
		else if (args.length == 1) {
			// --conf="/path/to/file.conf"
			String[] setting = args[0].split("=");
			// --help
			if (setting.length == 1 && setting[0].equals("--help")) {
				usage(defaultConfig);
			}
			// config submitted by user
			else if (setting.length == 2 && setting[0].equals("--config")) {

				ConfigParserDriver cpd = new ConfigParserDriver(setting[1],
						defaultConfig);
				// TODO disable debug for conf parser
				configs = cpd.run(true);

			} else {
				throw new Exception(String.format("Invalid argument passed (%s)", args[0]));
			}
		} else {
			throw new Exception(String.format("Invalid number of arguments passed (%d)", args.length));
		}
		return configs;
	}
}
