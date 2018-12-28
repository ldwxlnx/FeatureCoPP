package de.ovgu.spldev.featurecopp.config;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.regex.Pattern;

import de.ovgu.spldev.featurecopp.filesystem.Filesystem;
import de.ovgu.spldev.featurecopp.filesystem.Finder;
import de.ovgu.spldev.featurecopp.log.Logger;

public class Configuration {
	public static enum Mode {
		split, asplit, report, areport, merge
	}

	public static final class ConfigurationException extends Exception {
		/**
		 * gen uuid
		 */
		private static final long serialVersionUID = -4184219604556838621L;

		public ConfigurationException(String msg) {
			super(msg);
		}
	}

	public static final UserConf getDefault() {
		return DEFAULT_CONFIG;
	}

	public static final class UserConf implements Comparable<UserConf> {
		public UserConf(String name, UserConf parent) {
			this.name = name;
			this.parent = parent;
			uid = nextUid++;
		}

		private UserConf(String name, Path inputDirectory,
				String filePattern, String macroPattern, Path logDirectory,
				String logPrefix, Integer logRotateN, Boolean makeDebugOutput,
				Mode mode, UserConf parent) {
			this.name = name;
			this.inputDirectory = inputDirectory;
			this.filePattern = filePattern;
			this.macroPattern = Pattern.compile(macroPattern);
			this.logDirectory = logDirectory;
			this.logPrefix = logPrefix;
			this.logRotateN = logRotateN;
			this.makeDebugOutput = makeDebugOutput;
			this.mode = mode;
			this.parent = parent;
			uid = nextUid++;
		}
		@Override
		public int compareTo(UserConf other) {
			if(uid < other.uid) {
				return -1;
			}
			else if(uid > other.uid) {
				return 1;
			}
			else {
				return 0;
			}
		}

		@Override
		public boolean equals(Object other) {
			boolean equals = false;
			if (this == other) {
				equals = true;
			} else if (other instanceof UserConf) {
				equals = this.name.equals(((UserConf) other).name);
			}
			return equals;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public String toString() {
			// with parent
			return String.format(
					"{name=\"%s\";uid=%d;overridden=%b;input=\"%s\";output=\"%s\";%s;filepattern=\"%s\";macropattern=\"%s\";"
							+ "logformat=\"%s\";logrotate=\"%d\";debug=\"%b\";mode=\"%s\";blacklist=\"%s\";}",
					name, uid, isOverridden, inputDirectory, getOutputDirectory(),
					mode == Mode.merge ? 
							String.format("original=\"%s\"", getOriginalDirectory()) :String.format("modules=\"%s\"", getModuleDirectory()),
					filePattern, macroPattern, getLogFormat(), logRotateN,
					makeDebugOutput, mode, blacklist);
		}

		public String getName() {
			return name;
		}

		public Path getInputDirectory() {
			return inputDirectory;
		}

		public void setInputDirectory(String inputDirectory) {
			this.inputDirectory = Filesystem.genPath(inputDirectory);
		}

		public Path getOutputDirectory() {
			Path outputDirectory = Filesystem.genPath(String.format("%s_%s",
					inputDirectory.toString(), mode == Configuration.Mode.merge ? IO_MERGE_DIR_SUFFIX
									: IO_SPLIT_DIR_SUFFIX));
			return outputDirectory;
		}

		public Path getModuleDirectory() {
			return Filesystem.genPath(getOutputDirectory().toString(), IO_MODULE_DIR);
		}

		public String getFilePattern() {
			return filePattern;
		}

		public void setFilePattern(String filePattern) {
			this.filePattern = filePattern;
		}

		public Pattern getMacroPattern() {
			return macroPattern;
		}

		public void setMacroPattern(String macroPattern) {
			this.macroPattern = Pattern.compile(macroPattern);
		}

		public void setLogDirectory(String logDirectory) {
			this.logDirectory = Filesystem.genPath(logDirectory);
		}

		public Path getLogDirectory() {
			return logDirectory;
		}

		public void setLogPrefix(String logPrefix) {
			this.logPrefix = logPrefix;
		}

		public String getLogPrefix() {
			return logPrefix;
		}

		public String getLogFormat() {
			return String.format("%s%s%s_%s.%%d.%s", logDirectory,
					Filesystem.DIR_SEPARATOR, logPrefix, name,
					Configuration.LOGFILE_SUFFIX);
		}

		public void setLogRotateN(int logRotateN) {
			this.logRotateN = logRotateN;
		}

		public int getLogRotateN() {
			return logRotateN;
		}

		public boolean isMakeDebugOutput() {
			return makeDebugOutput;
		}

		public void setMakeDebugOutput(boolean makeDebugOutput) {
			this.makeDebugOutput = makeDebugOutput;
		}

		public Mode getMode() {
			return mode;
		}

		public void setMode(Mode mode) {
			this.mode = mode;
		}

		public boolean isOverriden() {
			return isOverridden;
		}

		public void setIsOverridden(boolean isOverridden) {
			this.isOverridden = isOverridden;
		}
		public void setOriginalDirectory(String originalDirectory) {
			this.originalDirectory = Filesystem.genPath(originalDirectory);
		}
		public Path getOriginalDirectory() {
			return originalDirectory;			
		}
		public void addToBlackList(String file) {
			Path absoluteFile = Filesystem.genPath(file).normalize();
			// only existing files are added -> save memory
			if(absoluteFile.toFile().isFile()) {
				blacklist.put(absoluteFile, absoluteFile.toString());
			}
		}
		public boolean isBlackListed(Path file) {
			return blacklist.get(file) != null;
		}

		protected void fixateBindings()
				throws IllegalArgumentException, IllegalAccessException,
				NoSuchFieldException, SecurityException {
			// for each private field, resolve and fixate value binding
			for (Field field : getClass().getDeclaredFields()) {
				// relax private fields for reflection only
				field.setAccessible(true);
				// search local -> global -> default for set value
				field.set(this, evaluateField(field));
			}
		}

		private Object evaluateField(Field field)
				throws IllegalArgumentException, IllegalAccessException,
				NoSuchFieldException, SecurityException {
			Object value = null;
			UserConf currConf = this;
			// walk along config hierarchy
			while (currConf != null) {
				value = field.get(currConf);
				// System.out.println(currConf.getName() + "::" + field +
				// "->" + value + " #parent=" + parent);
				// value set?
				if (value != null) {
					break;
				}
				// look up in parent
				currConf = currConf.parent;
			}
			// at least default config value was found on top of hierarchy
			return value;
		}

		/** config name, which is used as log infix name */
		private String name;
		/** source project directory */
		private Path inputDirectory;
		/** only set for nerge modes - source of original project */
		private Path originalDirectory;
		private String filePattern;
		private Pattern macroPattern;
		private Path logDirectory;
		private String logPrefix;
		private Integer logRotateN;
		private Boolean makeDebugOutput;
		private Mode mode;
		private UserConf parent;
		private boolean isOverridden;
		/** to sort by occurrence */
		private int uid;
		private static int nextUid;
		private HashMap<Path, String> blacklist = new HashMap<>();
	}

	/* APPLICATION */
	public static final String APPLICATION_NAME = "FeatureCoPP";
	/* PLATFORM */
	public static final String LINE_SEPARATOR = System.lineSeparator();
	/* CONFIGURATION */
	private static final String CONF_DEFAULT_NAME = "default";

	/* I/O */
	/** current working directory is default - data is read from */
	private static final Path IO_DEFAULT_INPUT_DIR = Filesystem.genPath(System.getProperty("user.dir"));
	private static final String IO_SPLIT_DIR_SUFFIX = "split";
	// TODO remove later for transparent rebase
	private static final String IO_MERGE_DIR_SUFFIX = "merged";
	public static final String IO_MODULE_DIR = String.format("___%s_modules", APPLICATION_NAME);
	public static final int IO_MERGE_READER_BUFFER = 8192;
	/** what kind of files should be located */
	// for syntax, see Finder -> FileSystems.getDefault().getPathMatcher
	private static final String IO_FIND_PATTERN = "^(\\w|-)+\\.[hc]";
	public static final String IO_FEATURE_FILE_SUFFIX = ".fcp";
	public static final Finder.FindParameter.PatternStrategy FIND_PATTERN_STRATEGY = Finder.FindParameter.PatternStrategy.regex;
	// public static final String FIND_PATTERN = "*.{c,h}";
	// public static final Finder.FindParameter.PatternStrategy
	// FIND_PATTERN_STRATEGY = Finder.FindParameter.PatternStrategy.glob;
	/* LOGGING */
	private static final Path LOGFILE_DEFAULTDIR = Filesystem.genPath(".");
	private static final String LOGFILE_PREFIX = APPLICATION_NAME;
	private static final String LOGFILE_SUFFIX = "log";
	private static final int LOGROTATE_N = 3;
	/* XML REPORT */
	public static final String XML_REPORT_FILE = APPLICATION_NAME
			+ "_report.xml";
	public static final String XML_INDENT_WHITESPACE = " "; // change to "\t"
															// here if necessary
															// (bloat)
	public static final char CSV_DELIMITER = ';';
	// TODO inappropriate regex?
	// public static final String FIND_GLOB_FEATUREFILE_PATTERN = "*"
	// + FEATURE_FILE_SUFFIX;
	public static final String CSP_TIMELIMIT = "5s"; // after which time period
														// solving should be
														// quit (prevent memory
														// overflows but
														// incomplete results!)
	public static final String HISTOGRAM_KEY_NAME = "FeatureOccurrence";
	public static final String HISTOGRAM_VAL_NAME = "Count";
	/* BEHAVIOR */
	private static final boolean BHV_DEFAULT_USEDEBUG = false;
	private static final String BHV_DEFAULT_MACRO_PATTERN = ".*";
	private static final Mode BHV_DEFAULT_MODE = Mode.report;
	/** configurable by user */
	public static boolean SKIP_ANALYSIS = true;
	/** no physical separation? xml journal only! */
	public static boolean REPORT_ONLY;


	public static void purgeOutputDir(Logger logger, String outputDir)
			throws Exception {
		logger.writeInfo("Deleting " + outputDir + " ...");
		logger.flushAllStrms();
		Filesystem.deleteDirRecursive(outputDir, null);
	}

	private final static UserConf DEFAULT_CONFIG = new UserConf(
			CONF_DEFAULT_NAME, IO_DEFAULT_INPUT_DIR,
			IO_FIND_PATTERN, BHV_DEFAULT_MACRO_PATTERN, LOGFILE_DEFAULTDIR,
			LOGFILE_PREFIX, LOGROTATE_N, BHV_DEFAULT_USEDEBUG, BHV_DEFAULT_MODE,
			null);
}
