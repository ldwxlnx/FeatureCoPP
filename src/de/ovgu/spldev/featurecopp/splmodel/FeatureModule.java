package de.ovgu.spldev.featurecopp.splmodel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;

import de.ovgu.spldev.featurecopp.config.Configuration;
import de.ovgu.spldev.featurecopp.filesystem.Filesystem;
import de.ovgu.spldev.featurecopp.lang.CDTParser;
import de.ovgu.spldev.featurecopp.lang.CDTParser.Stats;
import de.ovgu.spldev.featurecopp.lang.cpp.ExpressionParser;
import de.ovgu.spldev.featurecopp.log.Logger;
import de.ovgu.spldev.featurecopp.markup.MarkupLexer;
import de.ovgu.spldev.featurecopp.splmodel.FeatureTree.LogAnd;
import de.ovgu.spldev.featurecopp.splmodel.FeatureTree.Node;

/**
 * Aggregates feature expression information in conjunction with feature
 * dependent information. A FeatureModule is independent from preprocessing
 * technology. Remarks: - Source file should not be stored since a feature
 * module can occur in multiple source files. -> changeable markup attribute
 * provided by DirectiveLocator - Module file can be generated here
 * automatically from 'moduleDir' and 'uid' + 'suffix'
 * 
 * @author K. Ludwig
 */
public class FeatureModule implements Comparable<FeatureModule> {

	/**
	 * FeatureModules build a total order based on their (natural) amount of
	 * occurrences in an descending manner.
	 */
	@Override
	public int compareTo(FeatureModule fm) {
		if (featureOccurrences.size() > fm.featureOccurrences.size()) {
			return -1;
		} else if (featureOccurrences.size() < fm.featureOccurrences.size()) {
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * Retrieves the current amount of aggregated FeatureOccurrences.
	 * 
	 * @return current amount of aggregated FeatureOccurrences
	 */
	int numOfOccurrences() {
		return featureOccurrences.size();
	}
	
	/**
	 * Count all #if roles of this feature
	 * @return number of if roles
	 */
	public long numOfIf(boolean useLoF) {
		long count = 0;
		for(FeatureOccurrence fo : featureOccurrences) {
			if(fo.ftree instanceof IfTree) {
				count += useLoF ? (fo.lineEnd - fo.lineStart) + 1 : 1;
			}
		}
		return count;
	}
	/**
	 * Count all #ifdef roles of this feature
	 * @return number of ifdef roles
	 */
	public long numOfIfdef(boolean useLoF) {
		long count = 0;
		for(FeatureOccurrence fo : featureOccurrences) {
			if(fo.ftree instanceof IfdefTree) {
				count += useLoF ? (fo.lineEnd - fo.lineStart) + 1 : 1;
			}
		}
		return count;
	}
	/**
	 * Count all #ifndef roles of this feature
	 * @return number of ifndef roles
	 */
	public long numOfIfndef(boolean useLoF) {
		long count = 0;
		for(FeatureOccurrence fo : featureOccurrences) {
			if(fo.ftree instanceof IfndefTree) {
				count += useLoF ? (fo.lineEnd - fo.lineStart) + 1 : 1;
			}
		}
		return count;
	}
	/**
	 * Count all #elif roles of this feature
	 * @return number of elif roles
	 */
	public long numOfElif(boolean useLoF) {
		long count = 0;
		for(FeatureOccurrence fo : featureOccurrences) {
			if(fo.ftree instanceof ElifTree) {
				count += useLoF ? (fo.lineEnd - fo.lineStart) + 1 : 1;
			}
		}
		return count;
	}
	/**
	 * Count all #else roles of this feature
	 * @return number of else roles
	 */
	public long numOfElse(boolean useLoF) {
		long count = 0;
		for(FeatureOccurrence fo : featureOccurrences) {
			if(fo.ftree instanceof ElseTree) {
				count += useLoF ? (fo.lineEnd - fo.lineStart) + 1 : 1;
			}
		}
		return count;
	}
	/**
	 * Returns keyword (e.g., #if) from associated feature tree.
	 * Further roles in feature may have different keywords in their feature trees.
	 * @return keyword of feature associated tree
	 */
	public String getKeywordFromFeatureTree() {
		return featureExprAST.getKeyword();
	}
	
	public String getFilenameAt(int roleIdx) {
		// invalid index
		if(roleIdx < 0 || featureOccurrences.size() <= roleIdx) {
			return null;
		}
		FeatureOccurrence roleAt = featureOccurrences.get(roleIdx); 
		return roleAt.filename + "[lines: " + roleAt.lineStart + "-" + roleAt.lineEnd + "]";
	}
	/**
	 * Indicates if feature was introduced by #else directive
	 * @return true if introduced by #else directive, false otherwise
	 */
	public boolean isElse() {
		return featureExprAST instanceof ElseTree;
	}
	/**
	 * Indicates whether feature relies on simple absence condition
	 * @return true, if simple absence condition, false otherwise
	 */
	public boolean isSimpleAbsence() {
		return featureExprAST.isSimpleAbsence();
	}
	/**
	 * Indicates whether feature relies on simple presence condition
	 * @return true, if simple presence condition, false otherwise
	 */
	public boolean isSimplePresence() {
		return featureExprAST.isSimplePresence();
	}
	public int getTD() {
		return featureExprAST.getTanglingDegree();
	}
	/**
	 * Summarizes tangling degree of all feature occurrences
	 * @return summarized tangling degree of all feature occurrences
	 */
	public int sumTanglingDegreeWithElse() {
		int count = 0;
		for(FeatureOccurrence fo : featureOccurrences) {
			count += fo.getTDMap().getTotalObjMacroCount();
		}
		return count;
	}
	/**
	 * Summarizes tangling degree of all non-else feature occurrences
	 * @return summarized tangling degree of all non-else feature occurrences
	 */
	public int sumTanglingDegreeWithoutElse() {
		int count = 0;
		for(FeatureOccurrence fo : featureOccurrences) {
			if(!(fo.ftree instanceof ElseTree)) {
				count += fo.getTDMap().getTotalObjMacroCount();
			}
		}
		return count;
	}

	/**
	 * Creates a FeatureModule for base file. This feature module differs from
	 * regular conditional based feature modules by aways having a uid==0,
	 * having a module file (which is the main anchor) named as the original
	 * inspected sources file.
	 * 
	 * @param basemoduleFile
	 */
	public FeatureModule(final Path basemoduleFile) {
		this.featureModuleFile = basemoduleFile.toFile();
		// TODO compare performance to other Collection items
		this.featureOccurrences = new ArrayList<FeatureModule.FeatureOccurrence>();
	}

	/**
	 * Creates a new FeatureModule with given feature expression ast and module
	 * output directory. Each newly created FeatureModule gets an unique module
	 * id assigned on creation time. Corresponding feature module file name is
	 * created on instantiation, likewise.
	 * 
	 * @param featureExprAST
	 *            feature expression ast
	 * @param moduleDir
	 *            feature module output directory
	 */
	public FeatureModule(final FeatureTree featureExprAST,
			final Path moduleDir) {
		this.featureExprAST = featureExprAST;
		this.uid = FeatureModule.nextUID();
		this.featureModuleFile = makeFeatureModuleFile(moduleDir);
		// TODO compare performance to other Collection items
		this.featureOccurrences = new ArrayList<FeatureModule.FeatureOccurrence>();
	}

	public Path featureModuleFileToPath() {
		return featureModuleFile.toPath();
	}

	public boolean isRequested() {
		return isRequested;
	}

	public void unsetRequested() {
		this.isRequested = false;
	}

	/**
	 * Retrieves unique feature module id of this feature module.
	 * 
	 * @return unique feature module id
	 */
	public long getUID() {
		return uid;
	}

	public void writeXmlTo(int indent, FileWriter fw) throws Exception {
		if (fw != null) {
			double nd_sum = 0;
			double nd_n = featureOccurrences.size();
			for (int i = 0; i < nd_n; i++) {
				nd_sum += featureOccurrences.get(i).nestingDepth;
			}
			double nd_avg = nd_sum / nd_n;
			// calculate average and deviation and obtain number of dead
			// features (what the f*** is single responsibility? xD)
			int numOfDeadFeatures = Configuration.SKIP_ANALYSIS ? -1
					: genCommonStats();
			// @formatter:off
			fw.write(String.format(Locale.US, "%" + indent + "s<feature uid=\"%d\""
					+ " file=\"%s\"" + " requested=\"%b\">%s" // linebreak
					+ "%" + (indent + 1) + "s<expr><![CDATA[%s]]></expr>%s" // linebreak
					+ "%" + (indent + 1) + "s<td>%d</td>%s"
					+ "%" + (indent + 1) + "s<ndavg>%.3f</ndavg>%s"
					+ "%s" // statistic section or empty
					+ "%" + (indent + 1) + "s<occs count=\"%d\" dead=\"%d\" valid=\"%d\">%s", // linebreak
					Configuration.XML_INDENT_WHITESPACE, uid, isRequested ? featureModuleFile : "", isRequested, Configuration.LINE_SEPARATOR,
					Configuration.XML_INDENT_WHITESPACE, featureTreeToString(), Configuration.LINE_SEPARATOR,
					Configuration.XML_INDENT_WHITESPACE, featureExprAST.getTanglingDegree(), Configuration.LINE_SEPARATOR,
					Configuration.XML_INDENT_WHITESPACE, nd_avg, Configuration.LINE_SEPARATOR,
					// no analysis performed -> no statistics to display
					Configuration.SKIP_ANALYSIS ? "" : String.format("%" + (indent + 1) + "s<sem_avg %s />%s"
							+ "%" + (indent + 1) + "s<sem_dev %s />%s", 
							Configuration.XML_INDENT_WHITESPACE, average.toString(), Configuration.LINE_SEPARATOR,
							Configuration.XML_INDENT_WHITESPACE, variance.toString(), Configuration.LINE_SEPARATOR),
					Configuration.XML_INDENT_WHITESPACE, featureOccurrences.size(),
					numOfDeadFeatures,
					Configuration.SKIP_ANALYSIS ? -1 : featureOccurrences.size() - numOfDeadFeatures, Configuration.LINE_SEPARATOR
				)
			);			
			for (FeatureOccurrence fo : featureOccurrences) {
				fo.writeXmlTo(indent + 2, fw);
			}
			fw.write(String.format("%" + (indent + 1) + "s</occs>%s"// linebreak
				+ "%" + indent + "s</feature>%s",// linebreak
				Configuration.XML_INDENT_WHITESPACE, Configuration.LINE_SEPARATOR,
				Configuration.XML_INDENT_WHITESPACE, Configuration.LINE_SEPARATOR));
			// @formatter:on
		}
	}

	// TODO needed for CSV-write -> rethink / feature envy
	public String featureTreeToString() {
		return featureExprAST != null ? featureExprAST.featureExprToString()
				: null;
	}

	/**
	 * Creates an open-mark-up string for feature module.
	 * 
	 * @param isBlock
	 *            if true formatting is done as begin of block mark-up,
	 *            insertion line otherwise
	 * @param sourceFile
	 *            where module is hooked into (source or other feature module)
	 * @param parent
	 * @param directive
	 *            original directive of occurrence
	 * @param relatedFeatureOccurrence
	 *            an possibly enclosing feature occurrence
	 * @return formatted markup-line either for block or hook usage
	 */
	public String getOpenMarkup(boolean isBlock, final Path sourceFile,
			final String directive,
			final FeatureOccurrence relatedFeatureOccurrence) {
		long encl_occ_id = relatedFeatureOccurrence.enclosing != null
				? relatedFeatureOccurrence.enclosing.occ_uid
				: 0;
		if (isBlock) {
			return MarkupLexer.Markup.genBlockOpenMarkup(
					relatedFeatureOccurrence.occ_uid, encl_occ_id,
					sourceFile.toString(), directive);
		} else {
			return MarkupLexer.Markup.genRefMarkup(
					relatedFeatureOccurrence.occ_uid, encl_occ_id,
					featureModuleFile.toString());
		}
	}

	/**
	 * Creates an closing mark-up-line to terminate open-mark-ups (block-mode).
	 * 
	 * @return feature termination directive
	 */
	public static String getCloseMarkup() {
		return MarkupLexer.Markup.genCloseMarkup();
	}

	/**
	 * Writes String 'line' to feature module file. Writing is done in appending
	 * mode re-allocating a corresponding writer, which is expensive but
	 * unavoidable since file handles cannot be held open during runtime due to
	 * handle restrictions on e.g. linux machines (see e.g. ulimit -n).
	 * 
	 * @param line
	 *            text/code line to write
	 * @throws IOException
	 *             if open/write fails
	 */
	// public void writelnToFeatureFile(final String line) throws IOException {
	// //writeToFeatureFile(line + Configuration.LINE_SEPARATOR);
	// writeToFeatureFile(line);
	// }

	/**
	 * Writes String 's' to feature module file. Writing is done in appending
	 * mode re-allocating a corresponding writer, which is expensive but
	 * unavoidable since file handles cannot be held open during runtime due to
	 * handle restrictions on e.g. linux machines (see e.g. ulimit -n). If a
	 * feature module is not requested nothing is written,
	 * 
	 * @param line
	 *            text/code line to write
	 * @throws IOException
	 *             if open/write fails
	 */
	public void writeToFeatureFile(final String s) throws IOException {
		if (isRequested) {
			// path portion of feature file
			File parentDir = Filesystem.dirname(featureModuleFile);
			// does path portion already exist?
			if (!parentDir.exists()) {
				Filesystem.makePath(parentDir.toPath());
			}
			// writing to a by-feature-cached writer would be nice but
			// results in "too-many-open-files"-errors on linux
			// not very efficient (re/open|close)
			// -> instead we are caching content within FeatureScopManager.Node
			// before write-back
			FileWriter fw = new FileWriter(featureModuleFile, true);
			fw.write(s);
			fw.flush();
			fw.close();
		}
	}

	/**
	 * Adds a new FeatureOccurrence to this FeatureModule.
	 * 
	 * @param filename
	 *            where feature (directive occurred)
	 * @param parent
	 *            a potentially enclosing feature occurrence
	 * @param lineStart
	 *            line of occurrence
	 * @return newly created FeatureOccurrence
	 */
	public FeatureOccurrence addOccurrence(String filename,
			FeatureOccurrence parent, int lineStart, final FeatureTree ftree,
			int nestingDepth) {
		FeatureOccurrence fo = null;
		featureOccurrences.add(fo = new FeatureOccurrence(filename, parent,
				lineStart, ftree, nestingDepth));
		return fo;
	}

	public int genCommonStats() throws Exception {
		int deadCount = 0;
		// calulate average
		average = new CDTParser.Stats();
		// calculate var_sigma (sqrt(1/n Sigma (x_i - x_avg)^2)
		variance = new CDTParser.Stats();
		for (FeatureOccurrence fo : featureOccurrences) {
			// dead features don't have semantic/syntactic stats since not
			// parsable!
			if (!fo.isDeadFeature()) {
				average.accumulate(fo.stats);
			} else {
				deadCount++;
			}
		}
		average.makeAVG();
		for (FeatureOccurrence fo : featureOccurrences) {
			if (!fo.isDeadFeature()) {
				variance.accumulateVarSigma(fo.stats, average);
			}
		}
		variance.makeAVG();
		variance.makeSqrt();
		return deadCount;
	}

	public static void setLogger(final Logger logger) {
		FeatureModule.logger = logger;
	}

	/**
	 * Gathers information for each feature occurrence.
	 * 
	 * @author K. Ludwig
	 */
	public static class FeatureOccurrence {
		public FeatureOccurrence(String filename,
				final FeatureOccurrence enclosing, int lineStart,
				final FeatureTree ftree, int nestingDepth) {
			this.filename = filename;
			this.lineStart = lineStart;
			this.occ_uid = FeatureOccurrence.nextUID();
			this.enclosing = enclosing;
			this.ftree = ftree;
			this.nestingDepth = nestingDepth;
		}

		public void writeXmlTo(int indent, FileWriter xmlWriter)
				throws IOException {
			if (xmlWriter != null) {
				// TODO reference enclosing data
				// @formatter:off
				xmlWriter.write(String.format("%" + indent + "s<occ id=\"%d\""
						+ " status=\"%s\""
						+ " file=\"%s\""
						+ " keyword=\"%s\""
						+ " encl_occ_id=\"%d\""
						+ " nd=\"%d\""
						+ " begin=\"%d\""
						+ " end=\"%d\""
						+ ">"
						+ "%s" // syntax/sematic stats preceded by linebreak
						+ "%s</occ>%s", // indent + closing + linebreak
						Configuration.XML_INDENT_WHITESPACE, 
						occ_uid,
						isDead ? "dead" : "valid",
						filename,
						ftree.getKeyword(),
						enclosing != null ? enclosing.occ_uid : 0,
						nestingDepth,
						lineStart,
						lineEnd,
						! (Configuration.SKIP_ANALYSIS || stats == null) ? Configuration.LINE_SEPARATOR + stats.toXML(indent + 1) + Configuration.LINE_SEPARATOR 
								: "",
								// closing tag is on new line if stats available, otherwise on same line as opening
						! (Configuration.SKIP_ANALYSIS || stats == null) ? String.format("%" + indent + "s", Configuration.XML_INDENT_WHITESPACE) : "",
						Configuration.LINE_SEPARATOR
						));
				// @formatter:on
			}
		}

		public FeatureTree.Node getClonedFTreeRoot() {
			return ftree.getRootCloned();
		}

		public ExpressionParser.ObjMacroHistogram getTDMap() {
			return ftree.getTDMap();
		}

		public void setStats(final Stats stats) {
			this.stats = stats;
		}

		/**
		 * Retrieves the starting line of this feature occurrence.
		 * 
		 * @return starting line of this feature occurrence
		 */
		public int getBeginLine() {
			return lineStart;
		}

		/**
		 * Retrieves the ending line of this feature occurrence.
		 * 
		 * @return ending line of this feature occurrence
		 */
		public int getEndLine() {
			return lineEnd;
		}

		/**
		 * Sets end line number of this feature occurrence.
		 * 
		 * @param lineEnd
		 *            end line number
		 */
		public void setLineEnd(int lineEnd) {
			this.lineEnd = lineEnd;
		}

		public boolean isDeadFeature() {
			return isDead;
		}

		public void setDead() {
			this.isDead = true;
		}

		@Override
		public String toString() {
			return ftree.toString();
		}

		public static HashMap<String, String> makeMacroTable(
				Map<String, IntVar> csp_symbolTable) {
			HashMap<String, String> macroTable = null;
			if (csp_symbolTable != null) {
				macroTable = new HashMap<String, String>();
				for (Map.Entry<String, IntVar> item : csp_symbolTable
						.entrySet()) {
					String key = item.getKey();
					Integer value = item.getValue().getValue();
					// extract only macro values different from 0
					if (value != 0) {
						macroTable.put(key, value.toString());
					}
				}
			}
			return macroTable;
		}

		public HashMap<String, String> solveCSP() throws Exception {
			// Gather feature exprs for model naming - outermost becomes topmost
			// on stack (hence leaf nodes in upcoming ast)
			Stack<Node> enclosingFExprs = new Stack<Node>();
			for (FeatureOccurrence curr = enclosing; curr != null; curr = curr.enclosing) {
				enclosingFExprs.push(curr.ftree.getRoot());
			}
			FeatureTree conjunctiveExpr = null;
			// case 1: no enclosing feature exprs (stack is empty)
			if (enclosingFExprs.isEmpty()) {
				// use feature expression of this directly
				conjunctiveExpr = ftree;
			}
			// case 2: one enclosing feature exprs (|stack|=1)
			else if (enclosingFExprs.size() == 1) {
				// this feature expr AND the one enclosing
				LogAnd root = new LogAnd(ftree.getRoot(), enclosingFExprs.pop(),
						"&&");
				conjunctiveExpr = new FeatureTree();
				conjunctiveExpr.setRoot(root);
			}
			// case 3: multiple enclosing feature exprs (|stack|>1)
			else {
				// @formatter:off
				// create temporary new feature tree for all involved exprs
				// given F(C) enclosed by F(B) and F(A)
				// we want a feature ast like:
				// 			&&
				// 		F(C) 	 &&
				//			  F(B)  F(A) 					
				// @formatter:on				
				conjunctiveExpr = new FeatureTree();
				Node prevNode = null;
				boolean isInitial = true;
				while (!enclosingFExprs.isEmpty()) {
					// create leaf pair
					if (isInitial) {
						Node l = enclosingFExprs.pop();
						Node r = enclosingFExprs.pop();
						prevNode = new LogAnd(l, r, "&&");
						isInitial = false;
					}
					// add single node to previous AND
					else {
						Node l = enclosingFExprs.pop();
						prevNode = new LogAnd(l, prevNode, "&&");
					}
				}
				LogAnd root = new LogAnd(ftree.getRoot(), prevNode, "&&");
				conjunctiveExpr.setRoot(root);
			}
			// temporary symbol table for csp variables (macros)
			String conjunctiveFeatureExpr = conjunctiveExpr
					.featureExprToString();
			// was conjunctive feature expr solved before?
			MacroSettings macroSettings = conjunctiveFExprCache
					.get(conjunctiveFeatureExpr);
			if (macroSettings == null) {
				HashMap<String, IntVar> macros = new HashMap<String, IntVar>();
				Model cspModel = new Model(conjunctiveFeatureExpr);
				// totality of new tree has to evaluate to smth different from 0
				// to
				// become true
				IntVar eval = conjunctiveExpr.makeCSP(cspModel, macros);
				eval.ne(0).post();
				Solver solver = cspModel.getSolver();
				solver.limitTime(Configuration.CSP_TIMELIMIT);
				if (!solver.solve()) {
					isDead = true;
				}
				if (logger != null) {
					logger.writeDebug(
							String.format("RID#%d Choco={%s} => %d", occ_uid,
									solver.toOneLineString(), eval.getValue()));

					logger.writeDebug("Macro settings: " + macros.toString());
				}
				HashMap<String, String> macroTable = makeMacroTable(macros);
				// cache stringified solution model and contradiction indicator
				conjunctiveFExprCache.put(conjunctiveFeatureExpr,
						macroSettings = new MacroSettings(macroTable, isDead));
				logger.writeDebug("Caching " + conjunctiveFeatureExpr
						+ " with table: " + macroTable + " "
						+ (macroTable.isEmpty() ? "(No expansion necessary)"
								: ""));
			}
			// previously solved!
			else {
				// dead as previously solved siblings
				if (macroSettings.isDead) {
					this.isDead = macroSettings.isDead;
				}
				logger.writeDebug(
						String.format("RID=%d/Expr=[%s] previously solved",
								occ_uid, conjunctiveFeatureExpr));
				logger.writeDebug("Macro settings: "
						+ macroSettings.macroSettings.toString());
			}
			// deliver newly created/cached macro settings
			return macroSettings.macroSettings;
		}

		private static synchronized long nextUID() {
			return ++occ_uid_count;
		}

		private static long occ_uid_count;
		/** id of this occurrence */
		private long occ_uid;
		/** source file where feature occurred */
		private String filename;
		/** enclosing feature occurrence */
		private FeatureOccurrence enclosing;
		/** textual line start */
		private int lineStart;
		/** textual line end */
		private int lineEnd;
		/** syntax/semantics statistics */
		private Stats stats;
		/**
		 * individual feature tree - semantically but (maybe) not syntactically
		 * equivalent to other occurrences!
		 */
		private FeatureTree ftree;
		/** availability dead=true|selectable=false */
		private boolean isDead;
		/** nesting depth of role (1=top-lvl, 2=1 lvl below top-lvl,...) */
		private int nestingDepth;
	}

	/**
	 * Creates name of related feature module file.
	 * 
	 * @param moduleDir
	 *            path prefix of feature module file
	 * @return feature module file prefixed with 'moduleDir'
	 */
	private File makeFeatureModuleFile(final Path moduleDir) {
		return new File(moduleDir + File.separator + getUID()
				+ Configuration.IO_FEATURE_FILE_SUFFIX);
	}

	/**
	 * Retrieves next usable feature module id.
	 * 
	 * @return unique feature module id
	 */
	private static synchronized long nextUID() {
		return ++uid_count;
	}

	private static class MacroSettings {
		public MacroSettings(HashMap<String, String> macroSettings,
				boolean isDead) {
			this.macroSettings = macroSettings;
			this.isDead = isDead;
		}

		public HashMap<String, String> macroSettings;
		public boolean isDead;
	}

	// private static Logger csp_logger;
	// private static PrintStream csp_strm;
	/** Gathers all feature related occurrences */
	private ArrayList<FeatureOccurrence> featureOccurrences;
	/** provides unique id for feature module */
	private static long uid_count = 0;
	/** unique numeric feature id */
	private long uid;
	/** feature expression ast associated to feature */
	private FeatureTree featureExprAST;
	/** file where extractions are stored */
	private File featureModuleFile;
	/** indicates if feature module requested by user query */
	private boolean isRequested = true;
	/** averaged syntatic/semantic statistics */
	private CDTParser.Stats average;
	/** variance of syntactic/semantic stats sqrt(1/n Sigma (x_i - x_avg)^2) */
	private CDTParser.Stats variance;
	/** cache already solved macro settings */
	private static HashMap<String, MacroSettings> conjunctiveFExprCache = new HashMap<String, MacroSettings>();
	/** logging facility */
	private static Logger logger;
}
