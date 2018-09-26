package de.ovgu.spldev.featurecopp.lang;

/**
 * TODO
 * - collect asm usage statistics
 * - " scope sensitive statements statistics
 */

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNameOwner;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTCaseStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTGotoStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTParameterDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTReturnStatement;
import org.eclipse.cdt.internal.core.dom.rewrite.commenthandler.ASTCommenter;

import de.ovgu.spldev.featurecopp.lang.CDTParser.Stats.UNDISC_TYPE;

public class CParser extends CDTParser {
	/**
	 * Creates a CParser for a given source code string in conjunction with
	 * variable amount of macro definitions. The corresponding translation unit
	 * is created at instantiation time.
	 * 
	 * @param debug
	 *            actions printed to stdout if true
	 * @param srcCode
	 *            source code as string
	 * @param mDefs
	 *            macro definitions
	 * @throws Exception
	 *             in case a translation unit could not be created
	 */
	public CParser(boolean debug, final String srcCode,
			final Map<String, String> macroDefinitions) throws Exception {
		super(srcCode, macroDefinitions);
		this.debug = debug;
		init();
	}

	/**
	 * Creates a CParser for a given input file in conjunction with variable
	 * amount of macro definitions. The corresponding translation unit is
	 * created at instantiation time.
	 * 
	 * @param debug
	 *            actions printed to stdout if true
	 * @param srcFile
	 *            source file top parse
	 * @param mDefs
	 *            macro definitions
	 * @throws Exception
	 *             in case a translation unit could not be created
	 */
	public CParser(boolean debug, final Path srcFile,
			final Map<String, String> macroDefinitions) throws Exception {
		super(srcFile, macroDefinitions);
		this.debug = debug;
		init();
	}

	/**
	 * Performs analysis of translation unit inbetween closed line interval
	 * (beginLine, endLine). First line honored is beginLine + 1, and last line
	 * is endLine - 1 respectively. In case translation unit does not have given
	 * line numbers or line number interval depends on a certain
	 * cpp-macro-definition directive which was not submitted to the parser,
	 * analysis simply have no results. The following analysis actions are
	 * performed:
	 * <ol>
	 * <li>counting of function- and all other declarations (latter cannot be
	 * distinguished further)</li>
	 * <li>counting of statements</li>
	 * <li>counting of expressions</li>
	 * <li>counting of preprocessor directives and comments</li>
	 * </ol>
	 * 
	 * @param beginLine
	 *            lower line number bound (excluded)
	 * @param endLine
	 *            upper line number bound (excluded)
	 * @return statistics accumulator object
	 * @throws Exception
	 *             in case invalid line numbers are submitted or traversal
	 *             errors occurr
	 */
	public Stats analyze(final int beginLine, final int endLine)
			throws Exception {
		stats = new Stats();
		// used in symbol related visitors
		currBindingMap = new HashMap<IBinding, IASTName>();
		currSymbolMap = new HashMap<IBinding, IASTName>();
		countDeclarationsInbetween(beginLine, endLine);
		countNamesInbetween(beginLine, endLine);
		countStatementsInbetween(beginLine, endLine);
		countExpressionInbetween(beginLine, endLine);
		countStructDeclsInbetween(beginLine, endLine);
		visitFormalParameters(beginLine, endLine);
		// now all decls are counted
		stats.numOfVarDecls = stats.numOfTotalDecls - (stats.numOfFuncDefs
				+ stats.numOfFuncDecls + stats.numOfStructDecls);
		countFuncallsInbetween(beginLine, endLine);
		analyzeFlatInfos(beginLine, endLine);
		createHeuristics();
		// connected to stats for preservation
		stats.bindingMap = currBindingMap;
		stats.symbols = currSymbolMap;
		return stats;
	}

	/**
	 * Some parsing information is not inserted into ast but recorded in flat
	 * structures. Gather information from translation unit within given
	 * interval such as:
	 * <ol>
	 * <li>c-preprocessor directives</li>
	 * <li>comments</li>
	 * </ol>
	 * 
	 * @param beginLine
	 *            lower bound line number
	 * @param endLine
	 *            upper bound line number
	 * @throws Exception
	 *             in case checkLineNumberInterval detects invalid lineNumbers
	 * @see {@link CParser#checkLineNumberInterval}
	 */
	private void analyzeFlatInfos(final int beginLine, final int endLine)
			throws Exception {
		checkLineNumberInterval(beginLine, endLine);
		// CPP-DIRECTIVES
		for (IASTPreprocessorStatement cppStmt : translationUnit
				.getAllPreprocessorStatements()) {
			IASTFileLocation fileLocation = cppStmt.getFileLocation();
			if (isFileLocationValid(beginLine, endLine, fileLocation)) {
				// count include separately
				if (cppStmt instanceof IASTPreprocessorIncludeStatement) {
					stats.numOfCPPInclude++;
				}
				stats.numOfCPPDirectives++;
			}
		}
		// COMMENTS
		// @formatter:off
		/* - Comments which are parsed unconditionally are kept twice (dunno why!)
		 *   e.g.
		 *   #ifndef A
		 *    // FOO 
		 *   #endif
		 * whereas e.g.
		 *   #ifdef A
		 *    // FOO 
		 *   #endif
		 * is counted correctly
		 * It seems that there's no accessor providing control for that^^ 
		 */
		//@formatter:on
		IASTComment[] comments = translationUnit.getComments();
		if (comments.length > 0) {
			ArrayList<Integer> uniqueLocations = new ArrayList<Integer>(
					comments.length);
			for (IASTComment comment : translationUnit.getComments()) {
				IASTFileLocation fileLocation = comment.getFileLocation();
				// no duplicate processed before AND inbetween boundaries?
				if (!uniqueLocations
						.contains(fileLocation.getStartingLineNumber())
						&& isFileLocationValid(beginLine - 1, endLine,
								fileLocation)) {
					// a comment can be on same line as directive, hence include
					// line of directive
					// -> begin - 1
					uniqueLocations.add(fileLocation.getStartingLineNumber());
					stats.numOfComments++;
				}
			}
		}
	}

	private void visitFormalParameters(final int beginLine, final int endLine)
			throws Exception {
		checkLineNumberInterval(beginLine, endLine);
		if (debug) {
			System.out.println(
					String.format("FPARAM (%d,%d):", beginLine, endLine));
		}
		ASTVisitor fparamVisitor = new ASTVisitor() {
			{
				shouldVisitParameterDeclarations = true;
			}

			@Override
			public int visit(IASTParameterDeclaration paramDecl) {
				IASTFileLocation fileLocation = paramDecl.getFileLocation();
				// does fparam occur INBETWEEN our requested code lines
				// AND param is topmost node in ast excerpt, hence
				// undisciplined
				if (isFileLocationValid(beginLine, endLine, fileLocation)
						&& !isFileLocationValid(beginLine, endLine,
								paramDecl.getParent().getFileLocation())) {					
					stats.undisc_type = UNDISC_TYPE.PARAM;
				}
				return ASTVisitor.PROCESS_CONTINUE;
			}
		};
		translationUnit.accept(fparamVisitor);
	}

	/**
	 * Count all declarations within given closed line number interval.
	 * 
	 * @param beginLine
	 *            lower line number bound (excluded)
	 * @param endLine
	 *            upper line number bound (excluded)
	 * @throws Exception
	 *             in case line numbers are invalid
	 * @see {@link CParser#checkLineNumberInterval}
	 */
	private void countDeclarationsInbetween(final int beginLine,
			final int endLine) throws Exception {
		checkLineNumberInterval(beginLine, endLine);
		if (debug) {
			System.out.println(
					String.format("DECLS (%d,%d):", beginLine, endLine));
		}
		ASTVisitor declVisitor = new ASTVisitor() {
			{
				shouldVisitDeclarators = true;
			}

			@Override
			public int visit(IASTDeclarator declarator) {
				IASTFileLocation fileLocation = declarator.getFileLocation();
				// does name occur INBETWEEN our requested code lines
				if (isFileLocationValid(beginLine, endLine, fileLocation)) {
					// re-use binding as symbol table key
					IASTName declName = declarator.getName();
					IBinding binding = declName.resolveBinding();

					// TODO unclear by doc why this could happen, but it does
					// see: tk+-3.20.0/gtk/gtkprinteroptionset.h for symbols
					// option (2x), user_data (1x) and 4x blank name!
					if (binding != null) {
						currBindingMap.put(binding, declName);
						if (debug) {
							System.out.println(String.format("\t|-(%d:%d) %s",
									fileLocation.getStartingLineNumber(),
									fileLocation.getEndingLineNumber(),
									declarator.getName()));
						}
						if (declarator instanceof IASTFunctionDeclarator) {
							switch (((IASTFunctionDeclarator) declarator)
									.getRoleForName(declarator.getName())) {
							case IASTNameOwner.r_definition:
								stats.numOfFuncDefs++;
								break;
							// TODO
							case IASTNameOwner.r_reference:
							case IASTNameOwner.r_declaration:
							case IASTNameOwner.r_unclear:
							default:
								stats.numOfFuncDecls++;
								break;
							}
						}
						stats.numOfTotalDecls++;
					}
				}
				return ASTVisitor.PROCESS_CONTINUE;
			}
		};
		translationUnit.accept(declVisitor);
	}

	/**
	 * Count all names (symbols) within given closed line number interval.
	 * 
	 * @param beginLine
	 *            lower line number bound (excluded)
	 * @param endLine
	 *            upper line number bound (excluded)
	 * @throws Exception
	 *             in case line numbers are invalid
	 * @see {@link CParser#checkLineNumberInterval}
	 */
	private void countNamesInbetween(final int beginLine, final int endLine)
			throws Exception {
		checkLineNumberInterval(beginLine, endLine);
		if (debug) {
			System.out.println(
					String.format("NAMES (%d,%d):", beginLine, endLine));
		}
		ASTVisitor nameVisitor = new ASTVisitor() {
			{
				shouldVisitNames = true;
				shouldVisitDeclarations = false; // visits anyway, wtf?
				includeInactiveNodes = true;
			}

			@Override
			public int visit(IASTName name) {
				IASTFileLocation fileLocation = name.getFileLocation();
				// visit only RVALs otherwise decl-inits are counted twice!
				if (name.isReference() && isFileLocationValid(beginLine,
						endLine, fileLocation)) {
					if (debug) {
						System.out.println(String.format("\t|-(%d:%d) %s",
								fileLocation.getStartingLineNumber(),
								fileLocation.getEndingLineNumber(), name));
					}
					analyzeName(name);
				}
				return ASTVisitor.PROCESS_CONTINUE;
			}
		};
		translationUnit.accept(nameVisitor);
		if (debug) {
			System.out.println(
					"=> unique symbols in perspective: " + currSymbolMap);
			System.out.println(
					"=> declarations in perspective: " + currBindingMap);
		}
	}

	/**
	 * Count all struct declarations within given closed line number interval.
	 * 
	 * @param beginLine
	 *            lower line number bound (excluded)
	 * @param endLine
	 *            upper line number bound (excluded)
	 * @throws Exception
	 *             in case line numbers are invalid
	 * @see {@link CParser#checkLineNumberInterval}
	 */
	private void countStructDeclsInbetween(final int beginLine,
			final int endLine) throws Exception {
		checkLineNumberInterval(beginLine, endLine);
		if (debug) {
			System.out.println(String.format("STRUCT/UNIONDECLS (%d,%d):",
					beginLine, endLine));
		}
		ASTVisitor decls = new ASTVisitor() {
			{
				shouldVisitNames = true;
				shouldVisitDeclarations = true;
				includeInactiveNodes = true;
				shouldVisitDeclSpecifiers = true;
			}

			@Override
			public int visit(IASTDeclSpecifier structDecl) {
				if (structDecl instanceof IASTCompositeTypeSpecifier) {
					IASTFileLocation fileLocation = structDecl
							.getFileLocation();
					// visit only usages (instead of declarations) inbetween
					// line interval
					if (isFileLocationValid(beginLine, endLine, fileLocation)) {
						stats.numOfStructDecls++;
						if (debug) {
							System.out.println(String.format("\t|-(%d:%d) %s",
									fileLocation.getStartingLineNumber(),
									fileLocation.getEndingLineNumber(),
									structDecl));
						}
					}
				}
				return ASTVisitor.PROCESS_CONTINUE;
			}
		};
		translationUnit.accept(decls);
	}

	/**
	 * Count all statements within given closed line number interval. Compound
	 * statements are excluded. Subcomponents of those are not.
	 * 
	 * @param beginLine
	 *            lower line number bound (excluded)
	 * @param endLine
	 *            upper line number bound (excluded)
	 * @throws Exception
	 *             in case line numbers are invalid
	 * @see {@link CParser#checkLineNumberInterval}
	 */
	private void countStatementsInbetween(final int beginLine,
			final int endLine) throws Exception {
		checkLineNumberInterval(beginLine, endLine);
		if (debug) {
			System.out.println(
					String.format("STMTS (%d,%d):", beginLine, endLine));
		}
		ASTVisitor statement = new ASTVisitor() {
			{
				shouldVisitStatements = true;
			}

			@Override
			public int visit(IASTStatement statement) {
				IASTFileLocation fileLocation = statement.getFileLocation();
				if (isFileLocationValid(beginLine, endLine, fileLocation)) {
					// compound statements and declaration statements are
					// ignored - their contents are
					// counted by other traversals
					if (!(statement instanceof IASTCompoundStatement)
							&& !(statement instanceof IASTDeclarationStatement)) {
						if (debug) {
							System.out.println(String.format("\t|-(%d:%d) %s",
									fileLocation.getStartingLineNumber(),
									fileLocation.getEndingLineNumber(),
									statement.getClass().getSimpleName()));
						}
						stats.numOfStmts++;
					}
					if (statement instanceof CASTReturnStatement) {
						if (!isFileLocationValid(beginLine, endLine,
								statement.getParent().getFileLocation())) {							
							stats.undisc_type = UNDISC_TYPE.RET;
						}
					}
					if (statement instanceof CASTCaseStatement) {
						if (!isFileLocationValid(beginLine, endLine,
								statement.getParent().getFileLocation())) {							
							stats.undisc_type = UNDISC_TYPE.CASE;
						}
					}
					if (statement instanceof CASTGotoStatement) {
						if (!isFileLocationValid(beginLine, endLine,
								statement.getParent().getFileLocation())) {							
							stats.undisc_type = UNDISC_TYPE.GOTO;
						}
					}
				}
				return ASTVisitor.PROCESS_CONTINUE;
			}
		};
		translationUnit.accept(statement);
	}

	/**
	 * Count all top-level expressions within given closed line number interval.
	 * Partial subexpression are excluded.
	 * 
	 * @param beginLine
	 *            lower line number bound (excluded)
	 * @param endLine
	 *            upper line number bound (excluded)
	 * @throws Exception
	 *             in case line numbers are invalid
	 * @see {@link CParser#checkLineNumberInterval}
	 */
	private void countExpressionInbetween(final int beginLine,
			final int endLine) throws Exception {
		checkLineNumberInterval(beginLine, endLine);
		if (debug) {
			System.out.println(
					String.format("EXPRS (%d,%d):", beginLine, endLine));
		}
		ASTVisitor expression = new ASTVisitor() {
			{
				shouldVisitExpressions = true;
			}

			@Override
			public int visit(IASTExpression expression) {
				IASTFileLocation fileLocation = expression.getFileLocation();
				if (isFileLocationValid(beginLine, endLine, fileLocation)) {
					// compound statements are ignored - their contents are
					// counted anyway
					if (debug) {
						System.out.println(String.format("\t|-(%d:%d) %s",
								fileLocation.getStartingLineNumber(),
								fileLocation.getEndingLineNumber(),
								expression.getClass().getSimpleName()));
					}
					stats.numOfExprs++;
				}
				// avoid traversal of further sub expression
				return ASTVisitor.PROCESS_SKIP;
			}
		};
		translationUnit.accept(expression);
	}

	/**
	 * Count all Funcalls within given closed line number interval. This could
	 * be basically be done while expression counting but since we are only
	 * interested in top level exprs nested calls would not be discovered!
	 * 
	 * @param beginLine
	 *            lower line number bound (excluded)
	 * @param endLine
	 *            upper line number bound (excluded)
	 * @throws Exception
	 *             in case line numbers are invalid
	 * @see {@link CParser#checkLineNumberInterval}
	 */
	private void countFuncallsInbetween(final int beginLine, final int endLine)
			throws Exception {
		checkLineNumberInterval(beginLine, endLine);
		if (debug) {
			System.out.println(
					String.format("FUNCALLS (%d,%d):", beginLine, endLine));
		}
		ASTVisitor expression = new ASTVisitor() {
			{
				shouldVisitExpressions = true;
			}

			@Override
			public int visit(IASTExpression expression) {
				IASTFileLocation fileLocation = expression.getFileLocation();
				if (isFileLocationValid(beginLine, endLine, fileLocation)
						&& expression instanceof IASTFunctionCallExpression) {
					if (debug) {
						System.out.println(String.format("\t|-(%d:%d) %s",
								fileLocation.getStartingLineNumber(),
								fileLocation.getEndingLineNumber(),
								expression.getClass().getSimpleName()));
					}
					stats.numOfFuncalls++;
				}
				// funcalls are the only kind of exprs we are "deeply"
				// interested in
				return ASTVisitor.PROCESS_CONTINUE;
			}
		};
		translationUnit.accept(expression);
	}

	/**
	 * Sanity check for line number interval.
	 * 
	 * @param beginLine
	 *            lower line number bound
	 * @param endLine
	 *            upper line number bound
	 * @param fileLocation
	 *            a necessary IASTFileLocation of a certain IASTNode.
	 * @return true if fileLocation is not <code>null</code> and starting line
	 *         of an IASTNode is less greater than beginLine and ending line of
	 *         an IASTNode is lower than endLine
	 */
	private boolean isFileLocationValid(final int beginLine, final int endLine,
			final IASTFileLocation fileLocation) {
		return fileLocation != null
				&& fileLocation.getStartingLineNumber() > beginLine
				&& fileLocation.getEndingLineNumber() < endLine;
	}

	/**
	 * Tests if given line numbers are positive and if beginLine is less than
	 * endLine.
	 * 
	 * @param beginLine
	 *            lower line number bound
	 * @param endLine
	 *            upper line number bound
	 * @throws Exception
	 *             in case beginLine and/or endLine are negative or endLine is
	 *             less than beginLine
	 */
	private void checkLineNumberInterval(final int beginLine, final int endLine)
			throws Exception {
		if (beginLine < 0) {
			throw new Exception("Begin line (" + beginLine
					+ ") has to be a natural number!");
		}
		if (endLine < 0) {
			throw new Exception(
					"End line (" + endLine + ") has to be a natural number!");
		}
		if (beginLine >= endLine) {
			throw new Exception("Begin line (" + beginLine
					+ ") has to be smaller and not equal to end line ("
					+ endLine + ")!");
		}
	}

	/**
	 * Performs the following actions:
	 * <ul>
	 * <li>Lookup for <code>name</code> within own symbol map. An already
	 * analyzed name does not need to be processed again.</li>
	 * <li>If name was not already processed:
	 * <ol>
	 * <li>Register in symbol map</li>
	 * <li>Get declaration based on binding. Since declarations are processed
	 * before a binding should already exist If not, the declaration was not
	 * within our perspective (which does not mean it is not within this
	 * translation unit!).</li>
	 * <li>In case a declaration in perspective exists, bound-symbol-counter is
	 * incremented...</li>
	 * <li>... unbound-symbol-counter otherwise</li>
	 * </ol>
	 * </ul>
	 * 
	 * @param name
	 *            inspected symbol
	 */
	private void analyzeName(IASTName name) {
		IBinding binding = name.resolveBinding();
		// we do only honor variables and function calls
		if (binding instanceof IFunction || binding instanceof IVariable) {
			// was symbol not previously analyzed?
			if (currSymbolMap.get(binding) == null) {
				// symbol is registered from now on
				currSymbolMap.put(binding, name);
				IASTName declName = currBindingMap.get(binding);
				// does name have a declaration within our perspective?
				if (declName != null) {
					stats.numOfSymbolsBound++;
				}
				// symbol is not declared within our perspective!
				// -> can be declared in this translation unit anyway (but not
				// between viewed code lines)
				else {
					stats.numOfSymbolsUnbound++;
				}
				stats.numOfSymbolsTotal++;
			}
		}
	}

	private void createHeuristics() {
		stats.calcEncapsulationRatio();
		//stats.calcPhysicalSeparationPotential();
		stats.calcSyntacticalVolume();
	}

	/**
	 * Starts parsing of source, meaning initialization of language specific
	 * translation unit is done.
	 * 
	 * @throws Exception
	 */
	private void init() throws Exception {
		translationUnit = GCCLanguage.getDefault().getASTTranslationUnit(
				fileContent, scannerInfo, includeFileContentProvider, index,
				options, log);
	}

	/** identifies if symbol was declared in translation unit */
	private HashMap<IBinding, IASTName> currBindingMap;
	/** identifies a symbol (declared or undeclared) */
	private HashMap<IBinding, IASTName> currSymbolMap;
	/** show fancy output of analysis actions */
	private boolean debug;
}
