package de.ovgu.spldev.featurecopp.lang;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;

public abstract class CDTParser {
	/**
	 * Creates a CDTParser for a given source code string.
	 * 
	 * @param srcCode
	 *            source code string
	 */
	public CDTParser(final String srcCode,
			final Map<String, String> macroDefinitions) {
		fileContent = FileContent.create("", srcCode.toCharArray());
		init(macroDefinitions);
	}

	/**
	 * Creates a CDTParser for a given source file.
	 * 
	 * @param srcFile
	 *            source file
	 */
	public CDTParser(final Path srcFile,
			final Map<String, String> macroDefinitions) {
		fileContent = FileContent.createForExternalFileLocation(srcFile
				.toString());
		init(macroDefinitions);
	}

	/**
	 * Displays translation unit on given stream.
	 * 
	 * @param strm
	 *            print stream where translation unit is written
	 * @throws Exception
	 */
	public void display(PrintStream strm) throws Exception {
		traverse(new Writer(strm));
	}

	public void display(PrintStream strm, int beginLine, int endLine)
			throws Exception {
		traverse(new Writer(strm, beginLine, endLine));
	}

	public static enum StatEntity {
		FUNCDEF, FUNCDECL, VARDECL, SYMTOTAL, SYMBOUND, SYMUNBOUND, STMTS, EXPRS, CPPDIR, CPPINC, COMM
	}

	public static class Stats {
		public static enum UNDISC_TYPE { 
			NONE(1),
			RET(2),
			PARAM(4),
			CASE(8),
			GOTO(16);
			private int value;
			private UNDISC_TYPE(int value) {
				this.value = value;
			}
		};
		@Override
		public String toString() {			
			return String
					.format(Locale.US,
							"SV=\"%.2f\" ER=\"%.2f\" funcdefs=\"%.1f\" totaldecls=\"%.1f\" funcdecls=\"%.1f\" structdecls=\"%.1f\" vardecls=\"%.1f\" "
									+ "symtotal=\"%.1f\" symbound=\"%.1f\" symunbound=\"%.1f\" stmts=\"%.1f\" "
									+ "exprs=\"%.1f\" funcalls=\"%.1f\" cppdir=\"%.1f\" include=\"%.1f\" comments=\"%.1f\" undisc=\"%s\"",
							syntacticalVolume,
							encapsulationRatio,
							numOfFuncDefs, numOfTotalDecls, numOfFuncDecls,
							numOfStructDecls, numOfVarDecls,
							numOfSymbolsTotal, numOfSymbolsBound,
							numOfSymbolsUnbound, numOfStmts,
							numOfExprs, numOfFuncalls, numOfCPPDirectives,
							numOfCPPInclude, numOfComments, undisc_type);
		}
		public static double euclideanNorm(double...elements) {
			if(elements != null && elements.length > 0) {
				double tmp = 0;
				for(int i = 0; i < elements.length; i++) {
					tmp += elements[i] * elements[i]; 
				}
				return Math.sqrt(tmp);
			}
			// should never happen in r^n
			return -1;
		}

		public String toXML(int indent) {
			return String.format("%" + indent + "s<semantics %s/>", " ",
					toString());
		}

		public String symbolsMapToString() {
			return "(" + symbols.size() + ")" + symbols.values().toString();
		}

		public String bindingMapToString() {
			return "(" + bindingMap.size() + ")"
					+ bindingMap.values().toString();
		}

		/**
		 * Registers symbol/binding maps as "garbage-collectable". Should only
		 * be used if related data is not longer needed. Prevents from
		 * mem-shortage
		 */
		public void unsetMaps() {
			bindingMap.clear();
			bindingMap = null;
			symbols.clear();
			symbols = null;
		}

		public void accumulate(final Stats stats) {
			if (stats != null) {
				numOfComments += stats.numOfComments;
				numOfCPPDirectives += stats.numOfCPPDirectives;
				numOfCPPInclude += stats.numOfCPPInclude;
				numOfExprs += stats.numOfExprs;
				numOfFuncDecls += stats.numOfFuncDecls;
				numOfVarDecls += stats.numOfVarDecls;
				numOfFuncDefs += stats.numOfFuncDefs;
				numOfStmts += stats.numOfStmts;				
				numOfSymbolsBound += stats.numOfSymbolsBound;
				numOfSymbolsUnbound += stats.numOfSymbolsUnbound;
				numOfSymbolsTotal += stats.numOfSymbolsTotal;
				numOfTotalDecls += stats.numOfTotalDecls;
				numOfStructDecls += stats.numOfStructDecls;
				numOfFuncalls += stats.numOfFuncalls;
				encapsulationRatio += stats.encapsulationRatio;
				syntacticalVolume += stats.syntacticalVolume;
				numOfAccumulations++;
			}
		}

		public void accumulateVarSigma(final Stats measurement, final Stats avg) {
			if (measurement != null && avg != null) {
				numOfComments += deltaSquare(measurement.numOfComments,
						avg.numOfComments);
				numOfCPPDirectives += deltaSquare(
						measurement.numOfCPPDirectives, avg.numOfCPPDirectives);
				numOfCPPInclude += deltaSquare(measurement.numOfCPPInclude,
						avg.numOfCPPInclude);
				numOfExprs += deltaSquare(measurement.numOfExprs,
						avg.numOfExprs);
				numOfFuncDecls += deltaSquare(measurement.numOfFuncDecls,
						avg.numOfFuncDecls);
				numOfVarDecls += deltaSquare(measurement.numOfVarDecls,
						avg.numOfVarDecls);
				numOfFuncDefs += deltaSquare(measurement.numOfFuncDefs,
						avg.numOfFuncDefs);
				numOfStmts += deltaSquare(measurement.numOfStmts,
						avg.numOfStmts);
				numOfSymbolsBound += deltaSquare(measurement.numOfSymbolsBound,
						avg.numOfSymbolsBound);
				numOfSymbolsUnbound += deltaSquare(
						measurement.numOfSymbolsUnbound,
						avg.numOfSymbolsUnbound);
				numOfSymbolsTotal += deltaSquare(measurement.numOfSymbolsTotal,
						avg.numOfSymbolsTotal);
				numOfTotalDecls += deltaSquare(measurement.numOfTotalDecls,
						avg.numOfTotalDecls);
				numOfStructDecls += deltaSquare(measurement.numOfStructDecls,
						avg.numOfStructDecls);
				numOfFuncalls += deltaSquare(measurement.numOfFuncalls,
						avg.numOfFuncalls);
				encapsulationRatio += deltaSquare(measurement.encapsulationRatio,
						avg.encapsulationRatio);
				syntacticalVolume += deltaSquare(measurement.syntacticalVolume, avg.syntacticalVolume);
				numOfAccumulations++;
			}
		}

		public void makeAVG() {
			if (numOfAccumulations != 0) {
				numOfComments /= numOfAccumulations;
				numOfCPPDirectives /= numOfAccumulations;
				numOfCPPInclude /= numOfAccumulations;
				numOfExprs /= numOfAccumulations;
				numOfFuncDecls /= numOfAccumulations;
				numOfVarDecls /= numOfAccumulations;
				numOfFuncDefs /= numOfAccumulations;
				numOfStmts /= numOfAccumulations;
				numOfSymbolsBound /= numOfAccumulations;
				numOfSymbolsUnbound /= numOfAccumulations;
				numOfSymbolsTotal /= numOfAccumulations;
				numOfTotalDecls /= numOfAccumulations;
				numOfStructDecls /= numOfAccumulations;
				numOfFuncalls /= numOfAccumulations;
				encapsulationRatio /= numOfAccumulations;
				syntacticalVolume /= numOfAccumulations;
			}
		}

		public void makeSqrt() {
			numOfComments = Math.sqrt(numOfComments);
			numOfCPPDirectives = Math.sqrt(numOfCPPDirectives);
			numOfCPPInclude = Math.sqrt(numOfCPPInclude);
			numOfExprs = Math.sqrt(numOfExprs);
			numOfFuncDecls = Math.sqrt(numOfFuncDecls);
			numOfVarDecls = Math.sqrt(numOfVarDecls);
			numOfFuncDefs = Math.sqrt(numOfFuncDefs);
			numOfStmts = Math.sqrt(numOfStmts);
			numOfSymbolsBound = Math.sqrt(numOfSymbolsBound);
			numOfSymbolsUnbound = Math.sqrt(numOfSymbolsUnbound);
			numOfSymbolsTotal = Math.sqrt(numOfSymbolsTotal);
			numOfTotalDecls = Math.sqrt(numOfTotalDecls);
			numOfStructDecls = Math.sqrt(numOfStructDecls);
			numOfFuncalls = Math.sqrt(numOfFuncalls);
			encapsulationRatio = Math.sqrt(encapsulationRatio);
			syntacticalVolume = Math.sqrt(syntacticalVolume);
		}
		public void calcEncapsulationRatio() {
			double symTotal = numOfSymbolsBound + numOfSymbolsUnbound;
			encapsulationRatio = symTotal == 0 ? 0
					: numOfSymbolsBound / symTotal;
		}
		
		public void calcSyntacticalVolume() {
			syntacticalVolume = Stats.euclideanNorm(numOfFuncDefs, numOfStructDecls, numOfFuncDecls, numOfVarDecls, numOfStmts, numOfExprs, numOfComments);
		}

		private double deltaSquare(double measurement, double avg) {
			return (measurement - avg) * (measurement - avg);
		}

		/** Number of function definitions in translation unit */
		protected double numOfFuncDefs;
		/** Number of function declarations in translation unit */
		protected double numOfFuncDecls;
		/** Number of composite structure declarations */
		protected double numOfStructDecls;
		/** Number of variable declarations */
		protected double numOfVarDecls;
		/** Number of all declarations in translation unit */
		protected double numOfTotalDecls;
		/** Number of symbols declared in this translation unit */
		protected double numOfSymbolsBound;
		/** Number of symbols not declared in this translation unit */
		protected double numOfSymbolsUnbound;
		/** Sum of all found symbols (bound and unbound)*/
		protected double numOfSymbolsTotal;
		/** Number of statements in translation unit */
		protected double numOfStmts;
		/** Number of expressions in translation unit */
		protected double numOfExprs;
		/** Number of function calls -- also accumulated in expression */
		protected double numOfFuncalls;
		/** Number of preprocessor directives */
		protected double numOfCPPDirectives;
		/** Number of preprocessor include directives */
		protected double numOfCPPInclude;
		/** Number of comments */
		protected double numOfComments;
		/** how many stat objects were used in accumulation (N) */
		protected double numOfAccumulations;
		/** relation of declared to existing symbols in portion */
		protected double encapsulationRatio;
//		/** metric indicating usefulness to physically separate a code portion */
//		protected double physicalSeparationPotential;
//		/** cosine similarity regarding all types of weighted declarations */
//		protected double comprehensibiltySupport;
		/** euclidean norm of amounts of syntactic entities*/
		protected double syntacticalVolume;
		protected HashMap<IBinding, IASTName> bindingMap;
		protected HashMap<IBinding, IASTName> symbols;
		protected UNDISC_TYPE undisc_type = UNDISC_TYPE.NONE;
	} // public class Stats

	public void writeStatsTo(PrintStream strm) {
		if (strm == null) {
			strm = System.out;
		}
		strm.println(stats);
	}

	/**
	 * Callback interface for AST traversal
	 * 
	 * @author K. Ludwig
	 * 
	 */
	protected interface Visitor {
		public void visit(NodeOccurrence currNode) throws Exception;
	}

	/**
	 * Visitor who writes a NodeOccurrence to a submitted PrintStream.
	 * 
	 * @author K. Ludwig
	 * 
	 */
	protected class Writer implements Visitor {
		public Writer(PrintStream strm, int beginLine, int endLine) {
			this.beginLine = beginLine;
			this.endLine = endLine;
			this.strm = strm;
		}

		public Writer(PrintStream strm) {
			this.strm = strm;
		}

		@Override
		public void visit(NodeOccurrence curr) {
			// do not display translation unit itself
			if (curr.node instanceof IASTTranslationUnit) {
				return;
			}
			// special source excerpt requested?
			if (beginLine != 0 && endLine != 0) {
				IASTFileLocation fileLocation = curr.node.getFileLocation();
				if (fileLocation != null
						&& beginLine < fileLocation.getStartingLineNumber()
						&& fileLocation.getEndingLineNumber() < endLine) {
					strm.println(curr);
				}
			}
			// no special source excerpt requested!
			else {
				strm.println(curr);
			}

		}

		private PrintStream strm;
		private int beginLine;
		private int endLine;
	}

	/**
	 * Wrapper storing an IASTNode and its occurrence level (in AST)
	 * 
	 * @author K. Ludwig
	 * 
	 */
	protected class NodeOccurrence {
		/**
		 * Creates a new NodeOccurrence for node 'node' occurring at tree level
		 * 'level'.
		 * 
		 * @param node
		 *            ASTNode
		 * @param level
		 *            AST occurrence level
		 */
		public NodeOccurrence(IASTNode node, int level) {
			this.node = node;
			this.level = level;
		}

		@Override
		/**
		 * Stringified representation of an ASTNode.
		 * Each node gets indented (by whitespaces) according to its AST level depth
		 */
		public String toString() {
			String fileOccurrence = "";
			try {
				fileOccurrence = node.getSyntax() != null ? "line ["
						+ node.getFileLocation().getStartingLineNumber() + ","
						+ node.getFileLocation().getEndingLineNumber() + "]"
						: "";
			} catch (ExpansionOverlapsBoundaryException e) {
				fileOccurrence = e.getMessage();
				// e.printStackTrace();
			} catch (UnsupportedOperationException e) {
				fileOccurrence = "UnsupportedOperationException";
			}

			return String.format("%" + level + "s%s(%s)-> %s", " ", node
					.getClass().getSimpleName(), fileOccurrence, node
					.getRawSignature().replaceAll("(\t|\n)", ""));
		}

		/** wrapped ASTNode */
		protected IASTNode node;
		/** AST level depth of ASTNode */
		protected int level;
	}

	/**
	 * Internal facade for translation unit traversal.
	 * 
	 * @param visitors
	 *            variable amount of visitors to be processed during traversal
	 * @throws Exception
	 *             basic error extension point for user defined visitors
	 */
	protected void traverse(Visitor... visitors) throws Exception {
		traverse(new NodeOccurrence(translationUnit, 0), visitors);
	}

	/**
	 * Recursive descent method for translation unit traversal.
	 * 
	 * @param currNode
	 *            currently processed node to be induced into each visitor
	 * @param visitors
	 *            variable amount of visitors to be processed during traversal
	 * @throws Exception
	 *             basic error extension point for user defined visitors
	 */
	protected void traverse(NodeOccurrence currNode, Visitor... visitors)
			throws Exception {
		// action submitted
		if (visitors != null) {
			for (Visitor visitor : visitors) {
				visitor.visit(currNode);
			}
		}
		for (IASTNode currChild : currNode.node.getChildren()) {
			// recursive descent
			traverse(new NodeOccurrence(currChild, currNode.level + 1),
					visitors);
		}
	}

	/**
	 * setup at CDTParser instantiation time
	 */
	private void init(final Map<String, String> macroDefinitions) {
		if (macroDefinitions != null) {
			this.macroDefinitions = macroDefinitions;
		} else {
			this.macroDefinitions = new HashMap<String, String>();
		}
		scannerInfo = new ScannerInfo(this.macroDefinitions, includeSearchPaths);
		options =
		// ILanguage.OPTION_PARSE_INACTIVE_CODE |
		ILanguage.OPTION_IS_SOURCE_UNIT | ILanguage.OPTION_NO_IMAGE_LOCATIONS;
	}

	/** CDT file/text handler wrapper */
	protected FileContent fileContent;
	/** locations of include files */
	protected String[] includeSearchPaths = new String[0];
	/** explicitly defined macro settings */
	protected Map<String, String> macroDefinitions;
	/** CDT scanner wrapper honoring include paths and additional preproc macros */
	protected IScannerInfo scannerInfo;
	protected IncludeFileContentProvider includeFileContentProvider = IncludeFileContentProvider
			.getEmptyFilesProvider();
	protected IIndex index = null;
	protected int options;
	protected IParserLogService log = new DefaultLogService();
	protected IASTTranslationUnit translationUnit;
	/** statistics accumulator instantiated before analysis calls */
	protected Stats stats;
}
