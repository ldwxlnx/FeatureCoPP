package de.ovgu.spldev.featurecopp.lang.cpp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Pattern;

import de.ovgu.spldev.featurecopp.config.Configuration;
import de.ovgu.spldev.featurecopp.lang.CDTParser;
import de.ovgu.spldev.featurecopp.lang.CParser;
import de.ovgu.spldev.featurecopp.lang.cpp.CPPAnalyzer.TYPE;
import de.ovgu.spldev.featurecopp.lang.cpp.ExpressionParser.ObjMacroHistogram;
import de.ovgu.spldev.featurecopp.log.Logger;
import de.ovgu.spldev.featurecopp.splmodel.ElifTree;
import de.ovgu.spldev.featurecopp.splmodel.ElseTree;
import de.ovgu.spldev.featurecopp.splmodel.FeatureModule;
import de.ovgu.spldev.featurecopp.splmodel.FeatureModule.FeatureOccurrence;
import de.ovgu.spldev.featurecopp.splmodel.FeatureTable;
import de.ovgu.spldev.featurecopp.splmodel.FeatureTree;

public class FeatureScopeManager {
	public static void initASTStrm(final String astLogfile)
			throws FileNotFoundException {
		ast_strm = new PrintStream(astLogfile);
	}

	public FeatureScopeManager(final Path moduleDir, Logger logger) {
		this.exprParseDrv = new ExpressionParserDriver();
		this.moduleDir = moduleDir;
		this.featureScope = new Stack<Stack<Node>>();
		this.logger = logger;
	}

	public void addBasefile(final Path currentSourceFile) {
		featureScope.push(new Stack<FeatureScopeManager.Node>());
		// prevTopMost -> provide access to previous top most -> directives are
		// written there themselves
		Node node = prevTopMost = basefileBucket = new Node(new FeatureModule(
				currentSourceFile), null, null, TYPE.UNDEF);
		featureScope.peek().push(node);
	}

	public boolean hasConditionals() {
		// first bucket is always basefile, no further buckets means no
		// conditional in process
		return featureScope.size() > 1;
	}
	
	public void dumpStack(PrintStream strm) {
		int level = 0;
		Enumeration<Stack<Node>> eSiblingGroups = featureScope.elements();
		while (eSiblingGroups.hasMoreElements()) {
			Enumeration<Node> eNodes = eSiblingGroups.nextElement().elements();
			strm.print(String.format("%6d\t", level++));
			while(eNodes.hasMoreElements()) {
				Node curr = eNodes.nextElement();
				strm.print("[" + curr + "]");
			}
			strm.println();
		}
	}
	public ObjMacroHistogram getObjMacroHistogram() {
		return exprParseDrv.getObjMacroHistogram();
	}

	/**
	 * Total stack size (basefile + siblings groups)
	 * 
	 * @return
	 */
	public int size() {
		return featureScope.size();
	}

	/**
	 * Effective size (all nested groups without basefile)
	 * 
	 * @return
	 */
	public int numOfConditionalInProcess() {
		return featureScope.size() - 1;
	}

	public void setCurrentOriginalSourceFile(
			final Path currentOriginalSourceFile) {
		this.currentOriginalSourceFile = currentOriginalSourceFile;
	}

	public void addIfElif(final Pattern requestPattern,
			final String conditionalExpr, int lineNumber,CPPAnalyzer.TYPE type) throws Exception {
		FeatureTree featureTree = null;
		try {
			featureTree = parseConditionalExpression(conditionalExpr, requestPattern);
		} catch (Exception e) {
			// rethrow
			throw new Exception(e.getMessage() + " for expression: "
					+ conditionalExpr);
		}

		// TODO remove
		// System.out.println(featureTree + "=> " + featureTree.eval());

		Node parent = null;
		// any case of if-directive?
		if (!(featureTree instanceof ElifTree)) {
			// indicate to perform a write back for collected base file code
			// (requested conditional expr)
			basefileHasDirective();
			// ENCLOSING ON TOP
			// 1.) parent -> interconnect to parent Feature (conditional or
			// basefile)
			// 2.) prevTopMost -> provide access to previous top most ->
			// directives are written there themselves
			parent = prevTopMost = featureScope.peek().peek();
			// start a new conditional scope
			featureScope.push(new Stack<Node>());
		}
		// an elif-directive!
		else {
			// SIBLING ON TOP (either if* or elif)
			Node sibling = featureScope.peek().peek();
			// ...or dangling ?
			if(! sibling.isIf() && ! sibling.isElif()) {
				throw new Exception("#elif dangling! Previous sibling is " + sibling.type + "!");
			}
			
			// this directive marks ending of previous sibling directive
			sibling.setOccurrenceEndline(lineNumber);
			// sibling conditional ended -> write closing mark-up of extracted
			// block
			if (sibling.feature.isRequested()) {
				cacheInMostRecent(FeatureModule.getCloseMarkup());
			}
			// parent interconnection is propagated forward by each sibling
			// (initially by if*)
			parent = featureScope.peek().peek().parent;
		}
		addScope(requestPattern, featureTree, parent, lineNumber, type);
	}

	public void addElse(final Pattern requestPattern, int lineNumber)
			throws Exception {
		// SIBLING ON TOP
		Node sibling = featureScope.peek().peek();
		// ...or dangling ?
		if(! sibling.isIf() && ! sibling.isElif()) {			
			throw new Exception("#else dangling! Previous sibling is " + sibling.type + "!");
		}
		// this directive marks ending of previous sibling directive
		sibling.setOccurrenceEndline(lineNumber);
		if (sibling.feature.isRequested()) {
			// sibling conditional ended -> write closing mark-up of extracted
			// block
			cacheInMostRecent(FeatureModule.getCloseMarkup());
		}
		// Obtain negated disjunctive feature expression from present sibling
		// clauses
		FeatureTree featureTree = negatedClausesToFeatureTree();

		// TODO remove
		// System.out.println(featureTree + "=> " + featureTree.eval());

		// parent interconnection is propagated forward by each sibling
		// (initially by if*)
		addScope(requestPattern, featureTree, sibling.parent, lineNumber, CPPAnalyzer.TYPE.ELSE);
	}

	public void handleEndif(int lineNumber) throws Exception {
		if (featureScope.peek().peek().feature.isRequested()) {
			// sibling conditional ended -> write closing mark-up of extracted
			// block
			cacheInMostRecent(FeatureModule.getCloseMarkup());
		}
		// remove top-most (stacked) conditional group
		Stack<Node> conditional = featureScope.pop();		
		// set end position of top-left-most feature
		conditional.peek().setOccurrenceEndline(lineNumber);
		// write back all controlled code caches
		while (!conditional.isEmpty()) {
			Node prevTopLeftMost = conditional.pop();
			prevTopLeftMost.writeBackControlledCodeCache();
		}
		// previous top most is now parent of parent of actual top most again
		prevTopMost = featureScope.peek().peek().parent;
	}

	/**
	 * Performs following cleansing actions:
	 * <ol>
	 * <li>write-back of controlled cache of base file</li>
	 * <li>stack purge: all items on FeatureScopeManager's internal stack are
	 * removed</li>
	 * </ol>
	 * 
	 * @throws Exception
	 */
	void cleanUp() throws Exception {
		// remove condition if unconditional write back wanted
		// -> corresponds to a complete src->dst-copy (even for directive-free
		// source files)
		// was at least on directive detected within basefile?
		// - potentially left behind directives (in error cases) are removed
		// unprocessed
		if (basefileBucket.hasDirective) {
			// write back is wanted
			basefileBucket.writeBackControlledCodeCache();
		}
		featureScope.clear();
	}

	/**
	 * Writes text to top-left-most feature occurrence source cache. Main
	 * I/O-interface, i.e. everything should be written to a certain feature via
	 * this method. In case this node represents an unwanted feature next wanted
	 * enclosing feature is located by unwinding through parent interconnection.
	 * 
	 * @param text
	 */
	public void cacheInMostRecent(final String text) {
		featureScope.peek().peek().appendToControlledCodeCache(text);
	}

	public void cacheInPrevMostRecent(final String text) {
		prevTopMost.appendToControlledCodeCache(text);
	}

	public void basefileHasDirective() {
		basefileBucket.hasDirective = true;
	}

	private void addScope(final Pattern requestPattern,
			FeatureTree featureTree, Node parent, int lineNumber, CPPAnalyzer.TYPE type)
			throws IOException {
		// perform feature look-up: already present otherwise created
		FeatureModule featureModule = FeatureTable.get(featureTree, moduleDir);
		// register feature occurrence within corresponding feature module
		// (report-only)
//		FeatureModule.FeatureOccurrence currfeatureOccurrence = featureModule
//				.addOccurrence(currentSourceFile.toString(),
//						parent.getFeatureOccurrence(), lineNumber, featureTree);
		FeatureModule.FeatureOccurrence currfeatureOccurrence = featureModule
				.addOccurrence(currentOriginalSourceFile.toString(),
						parent.getFeatureOccurrence(), lineNumber, featureTree);
		// should conditional expression not be filtered by user request?		
		//if (!requestPattern.matcher(featureTree.toString()).matches()) {
		if (!requestPattern.matcher(featureTree.featureExprToString()).matches()) {			
			featureModule.unsetRequested();
		}
		// feature extraction requested!
		else {
			logger.writeInfo("Expression [" + featureTree.featureExprToString() + "] matched by " + requestPattern + " in line " + lineNumber);
			// indicate to perform a write back for collected base file code
			// (requested conditional expr)
			//basefileHasDirective();
			// insert module markup line to cache (file) where controlled code
			// gets extracted
			// <$inline...
			prevTopMost.appendToControlledCodeCache(featureModule
					.getOpenMarkup(false,
							parent.getMostRecentRequestedFeatureFile(),
							featureTree.toString(), currfeatureOccurrence));
		}
		// recognized conditional is now "top-left"-most
		featureScope.peek().push(
				new Node(featureModule, parent, currfeatureOccurrence, type));
		// was feature module extraction requested? ugly repeated case
		// discrimination but we can do that only AFTER previous push
		if (featureModule.isRequested()) {
			// AFTER PUSH! add corresponding beginning of markup of controlled
			// code to top-most cache
			// <@inline...
			cacheInMostRecent(featureModule.getOpenMarkup(true,
					parent.getMostRecentRequestedFeatureFile(),
					featureTree.toString(), currfeatureOccurrence));
		}
	}

	/**
	 * Container associating a Feature to its current textual occurrence.
	 * 
	 * @author K. Ludwig
	 */
	private class Node {
		private Node(FeatureModule feature, Node parent,
				FeatureModule.FeatureOccurrence featureOccurrence, CPPAnalyzer.TYPE type) {
			this.feature = feature;
			this.controlledCodeCache = new StringBuilder();
			this.parent = parent;
			this.featureOccurrence = featureOccurrence;
			this.type = type;
		}

		private void appendToControlledCodeCache(String symbol) {
			// search until a requested feature was found or basefile is reached
			// (always requested)
			findMostRecentRequestedParent().controlledCodeCache.append(symbol);
		}

		private Path getMostRecentRequestedFeatureFile() {
			return findMostRecentRequestedParent().feature.featureModuleFileToPath();
		}

		private void setOccurrenceEndline(int lineNumber) {
			if (featureOccurrence != null) {
				featureOccurrence.setLineEnd(lineNumber);
			}
		}

		private FeatureOccurrence getFeatureOccurrence() {
			return featureOccurrence;
		}

		private Node findMostRecentRequestedParent() {
			Node curr = this;
			while (!curr.feature.isRequested()) {
				curr = curr.parent;
			}
			return curr;
		}

		public String toString() {
			return feature.featureTreeToString();
		}
		
		public boolean isIf() {
			switch (type) {
			case IF:
			case IFNDEF:
			case IFDEF:
				return true;
			default:
				return false;
			}
		}
		
		public boolean isElif() {
			return type == CPPAnalyzer.TYPE.ELIF;
		}

		private void writeBackControlledCodeCache() throws IOException {
			String sourceCode = controlledCodeCache.toString();
			// not base file?
			if (! Configuration.SKIP_ANALYSIS && featureOccurrence != null) {
				try {
					// TODO if feature tree evaluates to 1
					// -> parse directly
					// otherwise:
					// get all macro names and values (different from 0) to
					// create a
					// customized parser

					// TODO
					// if not already a valid setting exists for this feature
					// expr
					// (save csp time)
					HashMap<String, String> macroDefs = featureOccurrence
							.solveCSP();
					// solveCSP has set dead (or "valid")
					if (!featureOccurrence.isDeadFeature()) {
						// analyze controlled code syntactically and
						// semantically
						// TODO analyze with appropriate macro definitions
						// (maybe a
						// tu
						// alread exists?)
						CParser cParser = new CParser(false,
								currentOriginalSourceFile, macroDefs);
						// analyze only line interval of this feature occurrence
						CDTParser.Stats stats = null;
						featureOccurrence.setStats(stats = cParser.analyze(
								featureOccurrence.getBeginLine(),
								featureOccurrence.getEndLine()));
						if (ast_strm != null) {
							ast_strm.println("FILE="
									+ currentOriginalSourceFile + " lines ("
									+ featureOccurrence.getBeginLine() + ","
									+ featureOccurrence.getEndLine() + ")");
							ast_strm.println("DIRECTIVE=" + featureOccurrence);
							ast_strm.println("SYMBOLMAP=" + stats.symbolsMapToString());
							ast_strm.println("BINDINGMAP=" + stats.bindingMapToString());
							// symbol /binding map not longer needed -prevent from memory shortage
							stats.unsetMaps();
							ast_strm.println("ASTPART=");
							cParser.display(ast_strm,
									featureOccurrence.getBeginLine(),
									featureOccurrence.getEndLine());
						}
					}
					else {
						logger.writeFail("Expression " + feature.featureTreeToString() + " not evaluated due to elapsed time limit (" 
								+ Configuration.CSP_TIMELIMIT + ") or unsatisfiability");
					}
					// problems during analysis are not fatal
				} catch (Exception e) {
					if(logger != null) {
						featureOccurrence.setDead();
						logger.writeFail(e.getMessage());
					}
				}
			}
			// write role to feature module
			if(! Configuration.REPORT_ONLY) {
				feature.writeToFeatureFile(sourceCode);
			}
		}

		/** unique feature to identify writeback location */
		private FeatureModule feature;
		/**
		 * according feature occurrence - cparse and end line setting ; null for
		 * basefile
		 */
		private FeatureModule.FeatureOccurrence featureOccurrence;
		/** cache retrieved symbols until linebreak -- then writeback */
		private StringBuilder controlledCodeCache;
		/**
		 * linking to obtain structural information (file occurrence
		 * referencing) and locate appropriate write back location
		 */
		private Node parent;
		/**
		 * this flag is only honored in case of basefile node - write back only
		 * if true
		 */
		private boolean hasDirective;
		/**
		 * Is this node an if* directive -- needed as sanity check for dangling #elif/else-clauses
		 */
		CPPAnalyzer.TYPE type; 
	}

	/**
	 * Returns the current number of top-most branches.
	 * 
	 * @return number of top-most branches
	 */
	public int getCurrentNumOfBranches() {
		return featureScope.peek().size();
	}

	/**
	 * Creates a new artificial feature tree of all sibling features. From all
	 * present sibling clauses a negated disjunctive clause is expressed
	 * following the rule of de-morgan (!A&&!B <=> !(A||B)). Nodes are cloned
	 * recursively and decorated by additional parentheses to preserve
	 * precedence optically.
	 * 
	 * @return an artificial feature tree (negated disjunction) of all sibling
	 *         features
	 */
	private FeatureTree negatedClausesToFeatureTree() {
		ElseTree ftree = new ElseTree();
		// only single if*?
		if (getCurrentNumOfBranches() == 1) {
			// cloning just for presentation reasons (adding parentheses)
			FeatureTree.Node right = featureScope.peek().peek().featureOccurrence
					.getClonedFTreeRoot();
			// is sibling root a logical negation?
			if (right.isLogicalNegation()) {
				// use right child as root instead of prefixing with negation
				// (avoid double negation)
				// -> does not prune multiple negations submitted by user (only
				// first level is pruned)
				right = right.getRightChild();
				ftree.setRoot(right);
			}
			// sibling root is not a logical negation
			else {
				// dyadic operator?
				if (right.isDyadic()) {
					// add virtual parentheses to preserve optical precedence
					right.setEmbracedByParentheses();
				}
				// {!}->{right=null;left=root-of-feature-tree}
				FeatureTree.Node nodeLogNeg = new FeatureTree.UnaryLogNeg(null,
						right, "!");
				ftree.setRoot(nodeLogNeg);
			}
		}
		// arbitrary amount of siblings greater-equal 2
		else {
			Stack<FeatureTree.Node> rootNodes = new Stack<FeatureTree.Node>();
			Enumeration<Node> eFeatures = featureScope.peek().elements();
			while (eFeatures.hasMoreElements()) {
				// cloning just for presentational reasons (adding
				// parentheses)
				rootNodes.push(eFeatures.nextElement().featureOccurrence
						.getClonedFTreeRoot());
			}
			FeatureTree.Node prevRoot = null;
			FeatureTree.Node currRoot = null;
			boolean initial = true;
			// TODO maybe simpler?
			while (!rootNodes.isEmpty()) {
				// at least 2 nodes are on stack:
				// left/right childs of artificial OR-node
				if (initial) {
					FeatureTree.Node right = rootNodes.pop();
					if (right.isDyadic()) {
						// optically preserve precedence
						right.setEmbracedByParentheses();
					}
					FeatureTree.Node left = rootNodes.pop();
					if (left.isDyadic()) {
						// optically preserve precedence
						left.setEmbracedByParentheses();
					}
					// stack could now be empty or contain arbitrary amount
					// of nodes
					currRoot = new FeatureTree.LogOr(left, right, "||");
					initial = false;
				}
				// from now on pop single nodes and connect node or
				// prevRoot:
				// previous artificial OR becomes right and further exprs on
				// stack left
				else {
					FeatureTree.Node right = prevRoot;
					FeatureTree.Node left = rootNodes.pop();
					if (left.isDyadic()) {
						// optically preserve precedence
						left.setEmbracedByParentheses();
					}
					currRoot = new FeatureTree.LogOr(left, right, "||");
				}
				// topmost disjunction is embraced by parentheses to
				// preserve
				// precedence
				// in relation to soon-to-be-added logical negation
				if (rootNodes.isEmpty()) {
					currRoot.setEmbracedByParentheses();
				}
				prevRoot = currRoot;
			}
			// now add negation
			FeatureTree.Node nodeLogNeg = new FeatureTree.UnaryLogNeg(null,
					prevRoot, "!");
			ftree.setRoot(nodeLogNeg);
		}
		ftree.setKeyword("#else");
		return ftree;
	}

	/**
	 * Delegates conditional expression to ExpressionParserDriver.
	 * 
	 * @param conditionalExpr
	 *            expression to parse
	 * @return abstract syntax tree of dissected conditional expression
	 * @throws Exception
	 *             Exception in case of lexical or syntactic errors
	 */
	private FeatureTree parseConditionalExpression(final String conditionalExpr, final Pattern requestPattern)
			throws Exception {
		return exprParseDrv.run(false, conditionalExpr, requestPattern);
	}

	private static PrintStream ast_strm;
	/** driver of generated conditional parser (1 per run) */
	private ExpressionParserDriver exprParseDrv;
	/**
	 * directory where isolated modules are stored --provided to FeatureModule
	 * where actual filename is built (not stored there)
	 */
	private Path moduleDir;
	/**
	 * name of current original source file (source)
	 * -> needed for syntax/semantic analysis
	 * -> also used in xml journal, indicating original location
	 */
	private Path currentOriginalSourceFile;
	private Node prevTopMost;
	/** direct access to basefile node */
	private Node basefileBucket;
	/** preserves scope structure */
	private Stack<Stack<Node>> featureScope;
	/** log writer (1 per run), same as in CPPAnalyzer */
	private Logger logger;
}
