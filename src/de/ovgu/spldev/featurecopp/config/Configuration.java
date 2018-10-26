package de.ovgu.spldev.featurecopp.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.HashMap;

import de.ovgu.spldev.featurecopp.filesystem.Filesystem;
import de.ovgu.spldev.featurecopp.log.Logger;


public class Configuration {
	public static final String APPLICATION_NAME = "FeatureCoPP";
	public static final String LINE_SEPARATOR = System.lineSeparator();
	public static final String EXTRACT_DIR_SUFFIX = "_split";
	// TODO remove later for transparent rebase
	public static final String MERGE_DIR_SUFFIX = "_merged";
	public static final String MODULE_DIR = "___" + APPLICATION_NAME + "_modules";
	/** what kind of files should be located */
	//public static final String FIND_GLOB_PATTERN = "*.{c,cpp,h,hpp,l,y}";
	public static final String FIND_GLOB_PATTERN = "*.{c,h}";
	public static final String SPLIT_LOGFILE = APPLICATION_NAME + "_split.log";
	public static final String CSP_LOGFILE = APPLICATION_NAME + "_csp.log";
	public static final String AST_LOGFILE = APPLICATION_NAME + "_ast.log";
	public static final String MERGE_LOGFILE = APPLICATION_NAME + "_merge.log";
	public static final String XML_REPORT_FILE = APPLICATION_NAME + "_report.xml"; 
	public static final String XML_INDENT_WHITESPACE = " "; // change to "\t" here if necessary (bloat)
	public static final char CSV_DELIMITER = ';';
	public static final String FEATURE_FILE_SUFFIX = ".fcp";
	public static final String FIND_GLOB_FEATUREFILE_PATTERN = "*" + FEATURE_FILE_SUFFIX;
	public static final String CSP_TIMELIMIT = "5s"; // after which time period solving should be quit (prevent memory overflows but incomplete results!)
	public static final String HISTOGRAM_KEY_NAME = "FeatureOccurrence";
	public static final String HISTOGRAM_VAL_NAME = "Count";
	public static final int MERGE_READER_BUFFER = 8192;
	/** configurable by user */
	public static boolean SKIP_ANALYSIS = true;
	/** no physical separation? xml journal only!*/
	public static boolean REPORT_ONLY;
	public static final String BLACKLIST_FILE = "conf.d/blacklist.conf";
	/** files, which cannot be processed due lexical ambiguities, e.g., assembler (not inline) with conditionals
	 * or asymmetric conditional directive usage.
	 * values only for explanatory reasons (programatically unused)
	 * TODO blacklist file, editable by user*/
	private static HashMap<Path, String> BLACKLIST = new HashMap<Path, String>();
	public static final void readBlacklist(Logger logger) throws Exception {
		File blFile = new File(BLACKLIST_FILE);
		if(! blFile.isFile()) {
			return;
		}
		BufferedReader br = new BufferedReader(new FileReader(blFile));
		String line = null;
		while((line = br.readLine()) != null) {
			// ignore comments
			if(! line.startsWith("#")) {
				Path blacklistFile = Filesystem.genPath(line);
				// does blacklisted file exist? prevent from malformed entries (but not invalid priviledges!)
				if(blacklistFile.toFile().exists()) {
					logger.writeInfo("Adding file " + line + " to blacklist.");
					BLACKLIST.put(blacklistFile, line);
				}
				// to subtle to review in logs -- so exit, to assist and discipline user
//				else {
//					br.close();
//					throw new Exception("File " + blacklistFile + " does not exist! Refused blacklisting...");
//				}
			}
		}
		br.close();
	}
	public static final String isBlacklisted(Path fso) {
		return BLACKLIST.get(fso);
	}
	
//	static {// TODO matching by key eliminates further, identically named files
//		// cond dirs in pure assembler -- lexical ambiguities introduced in comments
//		BLACKLIST.put("entry-arcv2.h", "\\linux-4.10.4\\arch\\arc\\include\\asm\\entry-arcv2.h");
//		BLACKLIST.put("tlb-mmu1.h", "linux-4.10.4\\arch\\arc\\include\\asm\\tlb-mmu1.h");
//		// asymmetric
//		BLACKLIST.put("alloca.h", "gcc-7.3.0\\fixincludes\\tests\\base\\alloca.h");
//		BLACKLIST.put("math.h", "gcc-7.3.0\\fixincludes\\tests\\base\\architecture\\ppc\\math.h");
//		BLACKLIST.put("AvailabilityMacros.h", "gcc-7.3.0\\fixincludes\\tests\\base\\AvailabilityMacros.h");
//		BLACKLIST.put("ctype.h", "gcc-7.3.0\\fixincludes\\tests\\base\\ctype.h");
//		BLACKLIST.put("hsfs_spec.h", "gcc-7.3.0\\fixincludes\\tests\\base\\hsfs\\hsfs_spec.h");
//		BLACKLIST.put("stdio_iso.h", "gcc-7.3.0\\fixincludes\\tests\\base\\iso\\stdio_iso.h");
//		BLACKLIST.put("stdlib_iso.h", "gcc-7.3.0\\fixincludes\\tests\\base\\iso\\stdlib_iso.h");
//		BLACKLIST.put("malloc.h", "gcc-7.3.0\\fixincludes\\tests\\base\\malloc.h");
//		BLACKLIST.put("decc$types.h", "gcc-7.3.0\\fixincludes\\tests\\base\\rtldef\\decc$types.h");
//		BLACKLIST.put("setjmp.h", "gcc-7.3.0\\fixincludes\\tests\\base\\rtldef\\setjmp.h");
//		BLACKLIST.put("string.h", "gcc-7.3.0\\fixincludes\\tests\\base\\rtldef\\string.h");
//		BLACKLIST.put("stdio.h", "gcc-7.3.0\\fixincludes\\tests\\base\\stdio.h");
//		BLACKLIST.put("stdio_tag.h", "gcc-7.3.0\\fixincludes\\tests\\base\\stdio_tag.h");
//		BLACKLIST.put("stdlib.h", "gcc-7.3.0\\fixincludes\\tests\\base\\stdlib.h");
//		BLACKLIST.put("cdefs.h", "gcc-7.3.0\\fixincludes\\tests\\base\\sys\\cdefs.h");
//		BLACKLIST.put("socket.h", "gcc-7.3.0\\fixincludes\\tests\\base\\sys\\socket.h");
//		BLACKLIST.put("testing.h", "gcc-7.3.0\\fixincludes\\tests\\base\\sys\\testing.h");
//	}
	
	public static void purgeOutputDir(Logger logger, String outputDir) throws Exception {
		logger.writeInfo("Deleting " + outputDir + " ...");
		logger.flushAllStrms();
		Filesystem.deleteDirRecursive(outputDir, null);
	}
}
