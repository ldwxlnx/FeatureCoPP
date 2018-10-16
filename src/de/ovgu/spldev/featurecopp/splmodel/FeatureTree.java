package de.ovgu.spldev.featurecopp.splmodel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Pattern;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.nary.cnf.LogOp;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.MathUtils;

import de.ovgu.spldev.featurecopp.lang.cpp.ExpressionParser;

//import com.sun.org.apache.xalan.internal.xsltc.runtime.Node;

/**
 * Bottom-up built feature tree (AST) created during parsing of conditional
 * expressions. Tree structure represents the precedence of potentially complex
 * expressions within #*if*-clauses.
 * 
 * @author K. Ludwig
 */
public class FeatureTree {
	// public static enum HEURISTIC { OPERATOR, LITERAL, }

	/**
	 * Returns a string representation of a feature tree.
	 */
	@Override
	public String toString() {
		return keyword + " " + featureExprToString();
	}

	/**
	 * Returns string representation of feature expression ast.
	 * 
	 * @return feature expression as concatenated string
	 */
	public String featureExprToString() {
		return root.toString();
	}
	
	/**
	 * Tests, if at least one object macro exists within this tree, which matches given pattern.
	 * Operation stops after finding the first match.
	 * @param pattern regex pattern
	 * @return true for first found match, false otherwise
	 */
	public boolean containsObjMacro(Pattern pattern) {
		return tdMap.contains(pattern);
	}
	
	public void setTDMap(ExpressionParser.ObjMacroHistogram tdMap) {
		this.tdMap = tdMap;
	}
	public ExpressionParser.ObjMacroHistogram getTDMap() {
		return tdMap;
	}
//	public int getTanglingDegree() {
//		return tdMap.getTotalObjMacroCount();
//	}

	/**
	 * Finalizes bottom-up created tree with root node 'root'.
	 * 
	 * @param root
	 *            node
	 */
	public void setRoot(final Node root) {
		this.root = root;
	}

	/**
	 * Returns a cloned expression ast.
	 * 
	 * @return cloned expression ast
	 */
	public Node getRootCloned() {
		return root.clone();
	}

	public IntVar makeCSP(Model model, HashMap<String, IntVar> macros)
			throws Exception {
		// temporary symbol table for this and all enclosing feature trees
		FeatureTree.macros = macros;
		return makeCSP(model, root);
	}

	protected IntVar makeCSP(Model model, Node root) throws Exception {
		return root.makeCSP(model).intVar();
	}

	/**
	 * Returns root node. Root node can be used as child node in other feature
	 * trees (e.g. negated clauses).
	 * 
	 * @return root node
	 */
	public Node getRoot() {
		return root;
	}

	/**
	 * Sets directive name by given keyword. Accessed during parsing. e.g.
	 * #ifdef, #elif, etc.
	 * 
	 * @param keyword
	 *            name of directive
	 */
	public void setKeyword(final String keyword) {
		this.keyword = keyword;
	}

	/**
	 * @return keyword of directive
	 */
	public String getKeyword() {
		return keyword;
	}

	/**
	 * Binary tree node of feature expression ast
	 * 
	 * @author K. Ludwig
	 */
	public static abstract class Node implements Cloneable {
		/**
		 * Creates a new tree node.
		 * 
		 * @param left
		 *            child
		 * @param right
		 *            child
		 * @param symbol
		 *            corresponding terminal symbol
		 */
		public Node(final Node left, final Node right, final String symbol) {
			this.left = left;
			this.right = right;
			this.symbol = symbol;
		}

		public abstract IntVar makeCSP(Model model) throws Exception;

		/**
		 * Returns textual representation of this tree node and its left and
		 * right subtree.
		 */
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(isEmbracedByParentheses ? "(" : "");
			if (left != null) {
				sb.append(left.toString());
			}
			sb.append(symbol);
			if (right != null) {
				sb.append(right.toString());
			}
			sb.append(isEmbracedByParentheses ? ")" : "");
			return sb.toString();
		}

		/**
		 * Sets this node as expression surrounded by parentheses. Since tree
		 * hierarchy explains that naturally, this is only needed for
		 * presentation reasons.
		 */
		public void setEmbracedByParentheses() {
			isEmbracedByParentheses = true;
		}

		/**
		 * Indicates, if a node is originally surrounded by parentheses.
		 * 
		 * @return true, if surrounded in source, false otherwise
		 */
		public boolean isEmbracedByParentheses() {
			return isEmbracedByParentheses;
		}

		/**
		 * Returns left child node.
		 * 
		 * @return left child node
		 */
		public Node getLeftChild() {
			return left;
		}

		/**
		 * Returns right child node.
		 * 
		 * @return right child node
		 */
		public Node getRightChild() {
			return right;
		}

		/**
		 * Indicates if this node represents a logical negation.
		 * 
		 * @return true, if symbol is "!", false otherwise
		 */
		public boolean isLogicalNegation() {
			return "!".equals(symbol);
		}

		/**
		 * Indicates if this node is a leaf in the tree.
		 * 
		 * @return true, if node is leaf, false otherwise
		 */
		public boolean isLeaf() {
			return left == null && right == null;
		}

		/**
		 * Dyadic operators have two children.
		 * 
		 * @return true, if node has two children, false otherwise
		 */
		public boolean isDyadic() {
			return left != null && right != null;
		}

		/**
		 * Clones a node (including its child trees recursively). Each Node has
		 * to implement its appropriate cloning strategy
		 */
		@Override
		public abstract Node clone();

		/** terminal symbol of this node */
		protected String symbol;
		/** left child (tree) */
		protected Node left;
		/** right child (tree) */
		protected Node right;
		/**
		 * indicates if this node is surrounded by parentheses - presentation
		 * only, affects stringfication and therefore possible expr-table keys
		 */
		protected boolean isEmbracedByParentheses;
	} /* class Node */

	public static class Ternary extends Node {

		public Ternary(Node condition, Node left, Node right, String symbol,
				String colon) {
			super(left, right, symbol);
			this.condition = condition;
			this.colon = colon;
		}

		@Override
		public String toString() {
			return condition + symbol + left + colon + right;
		}

		@Override
		public Node clone() {
			Ternary newTernary = new Ternary(condition.clone(), left.clone(),
					right.clone(), symbol, colon);
			newTernary.isEmbracedByParentheses = isEmbracedByParentheses;
			return newTernary;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			Model conditionModel = new Model(condition.toString());
			Solver solver = conditionModel.getSolver();
			IntVar evalCondition = condition.makeCSP(conditionModel);
			evalCondition.ne(0).post();
			// if condition is satisfiable connect with if-branch, then branch
			// otherwise
			IntVar cstExpr = solver.solve() ? left.makeCSP(model).intVar()
					: right.makeCSP(model).intVar();
			// System.out.println(condition.toString() + " => "
			// + evalCondition.getValue());
			// solver.printStatistics();
			// System.out.println(macros);
			return cstExpr;
		}

		private String colon;
		private Node condition;

	}

	public static class LogOr extends Node {
		public LogOr(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			LogOr newLogOr = new LogOr(left.clone(), right.clone(), symbol);
			newLogOr.isEmbracedByParentheses = isEmbracedByParentheses;
			return newLogOr;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return left.makeCSP(model).ne(0).or(right.makeCSP(model).ne(0))
					.boolVar();
		}
	}

	public static class LogAnd extends Node {
		public LogAnd(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			LogAnd newLogAnd = new LogAnd(left.clone(), right.clone(), symbol);
			newLogAnd.isEmbracedByParentheses = isEmbracedByParentheses;
			return newLogAnd;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return left.makeCSP(model).ne(0).and(right.makeCSP(model).ne(0))
					.boolVar();
		}
	}

	public static class BinOr extends Node {
		public BinOr(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			BinOr newBinOr = new BinOr(left.clone(), right.clone(), symbol);
			newBinOr.isEmbracedByParentheses = isEmbracedByParentheses;
			return newBinOr;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return model.intVar(left.makeCSP(model).getValue()
					| right.makeCSP(model).getValue());
		}
	}

	public static class BinXor extends Node {
		public BinXor(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			BinXor newBinXor = new BinXor(left.clone(), right.clone(), symbol);
			newBinXor.isEmbracedByParentheses = isEmbracedByParentheses;
			return newBinXor;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {

			// left.makeCSP(model).ne(right.makeCSP(model).getValue()).boolVar()
			return model.intVar(left.makeCSP(model).getValue()
					^ right.makeCSP(model).getValue());
		}
	}

	public static class BinAnd extends Node {
		public BinAnd(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			BinAnd newBinAnd = new BinAnd(left.clone(), right.clone(), symbol);
			newBinAnd.isEmbracedByParentheses = isEmbracedByParentheses;
			return newBinAnd;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return model.intVar(left.makeCSP(model).getValue()
					& right.makeCSP(model).getValue());
		}
	}

	public static class Eq extends Node {
		public Eq(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			Eq newEq = new Eq(left.clone(), right.clone(), symbol);
			newEq.isEmbracedByParentheses = isEmbracedByParentheses;
			return newEq;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return left.makeCSP(model).eq(right.makeCSP(model)).intVar();
		}
	}

	public static class NEq extends Node {
		public NEq(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			NEq newNEq = new NEq(left.clone(), right.clone(), symbol);
			newNEq.isEmbracedByParentheses = isEmbracedByParentheses;
			return newNEq;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return left.makeCSP(model).ne(right.makeCSP(model)).intVar();
		}
	}

	public static class Lt extends Node {
		public Lt(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			Lt newLt = new Lt(left.clone(), right.clone(), symbol);
			newLt.isEmbracedByParentheses = isEmbracedByParentheses;
			return newLt;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return left.makeCSP(model).lt(right.makeCSP(model)).intVar();
		}
	}

	public static class LEq extends Node {
		public LEq(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			LEq newLEq = new LEq(left.clone(), right.clone(), symbol);
			newLEq.isEmbracedByParentheses = isEmbracedByParentheses;
			return newLEq;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return left.makeCSP(model).le(right.makeCSP(model)).intVar();
		}
	}

	public static class Gt extends Node {
		public Gt(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			Gt newGt = new Gt(left.clone(), right.clone(), symbol);
			newGt.isEmbracedByParentheses = isEmbracedByParentheses;
			return newGt;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return left.makeCSP(model).gt(right.makeCSP(model)).intVar();
		}
	}

	public static class GEq extends Node {
		public GEq(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			GEq newGEq = new GEq(left.clone(), right.clone(), symbol);
			newGEq.isEmbracedByParentheses = isEmbracedByParentheses;
			return newGEq;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return left.makeCSP(model).ge(right.makeCSP(model)).intVar();
		}
	}

	public static class LShift extends Node {
		public LShift(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			LShift newLShift = new LShift(left.clone(), right.clone(), symbol);
			newLShift.isEmbracedByParentheses = isEmbracedByParentheses;
			return newLShift;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return model.intVar(left.makeCSP(model).getValue() << right
					.makeCSP(model).getValue());
		}
	}

	public static class RShift extends Node {
		public RShift(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			RShift newRShift = new RShift(left.clone(), right.clone(), symbol);
			newRShift.isEmbracedByParentheses = isEmbracedByParentheses;
			return newRShift;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return model.intVar(left.makeCSP(model).getValue() >> right
					.makeCSP(model).getValue());
		}
	}

	public static class Plus extends Node {
		public Plus(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			Plus newPlus = new Plus(left.clone(), right.clone(), symbol);
			newPlus.isEmbracedByParentheses = isEmbracedByParentheses;
			return newPlus;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return left.makeCSP(model).add(right.makeCSP(model)).intVar();
		}
	}

	public static class Minus extends Node {
		public Minus(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			Minus newMinus = new Minus(left.clone(), right.clone(), symbol);
			newMinus.isEmbracedByParentheses = isEmbracedByParentheses;
			return newMinus;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return left.makeCSP(model).sub(right.makeCSP(model)).intVar();
		}
	}

	public static class Mult extends Node {
		public Mult(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			Mult newMult = new Mult(left.clone(), right.clone(), symbol);
			newMult.isEmbracedByParentheses = isEmbracedByParentheses;
			return newMult;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return left.makeCSP(model).mul(right.makeCSP(model)).intVar();
		}
	}

	public static class Div extends Node {
		public Div(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			Div newDiv = new Div(left.clone(), right.clone(), symbol);
			newDiv.isEmbracedByParentheses = isEmbracedByParentheses;
			return newDiv;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return left.makeCSP(model).div(right.makeCSP(model)).intVar();
		}
	}

	public static class Mod extends Node {
		public Mod(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			Mod newMod = new Mod(left.clone(), right.clone(), symbol);
			newMod.isEmbracedByParentheses = isEmbracedByParentheses;
			return newMod;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return left.makeCSP(model).mod(right.makeCSP(model)).intVar();
		}
	}

	public static class IntLiteral extends Node {
		public IntLiteral(Node left, Node right, String symbol) {
			super(left, right, symbol);
			// since we have (hexa-)decimal or octal input (maybe greater than
			// 32bit)
			value = Long.decode(symbol);
		}

		@Override
		public Node clone() {
			IntLiteral newLiteral = new IntLiteral(null, null, symbol);
			newLiteral.isEmbracedByParentheses = isEmbracedByParentheses;
			newLiteral.value = value;
			return newLiteral;
		}

		private long value;

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			// TODO
			// RealVar v = model.realVar(value, 1d);
			// System.out.println(v);
			return model.intVar(MathUtils.safeCast(value));
		}
	}

	public static class CharLiteral extends Node {
		public CharLiteral(Node left, Node right, String symbol) {
			super(left, right, symbol);
			// multibytes rejected by lexer
			// escape sequences e.g. '\t'
			if (symbol.length() == 4) {
				switch (symbol) {
				case "'\0'":
					value = 0;
					break;
				case "'\\a'":
					value = 7;
					break;
				case "'\\b'":
					value = 8;
					break;
				case "'\\t'":
					value = 9;
					break;
				case "'\\n'":
					value = 10;
					break;
				case "'\\v'":
					value = 11;
					break;
				case "'\\f'":
					value = 12;
					break;
				case "'\\r'":
					value = 13;
					break;
				case "'\\''": // '\''
					value = 39;
					break;
				case "'\\\\'": // '\\'
					value = 92;
					break;
				default:
					// should never happen
					break;
				}
			}
			// simple literals (e.g. 'A')
			else if (symbol.length() == 3) {
				value = (long) symbol.charAt(1);
			}
		}

		@Override
		public Node clone() {
			CharLiteral newLiteral = new CharLiteral(null, null, symbol);
			newLiteral.isEmbracedByParentheses = isEmbracedByParentheses;
			newLiteral.value = value;
			return newLiteral;
		}

		private long value;

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			// TODO unfortunately source are not limited to restricted Int-range
			// (e.g. long values)
			return model.intVar(MathUtils.safeCast(value));
		}
	}

	public static class UnaryMinus extends Node {
		public UnaryMinus(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			UnaryMinus newUnaryMinus = new UnaryMinus(null, right.clone(),
					symbol);
			newUnaryMinus.isEmbracedByParentheses = isEmbracedByParentheses;
			return newUnaryMinus;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return right.makeCSP(model).neg().intVar();
		}
	}

	public static class UnaryPlus extends Node {
		public UnaryPlus(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			UnaryPlus newUnaryPlus = new UnaryPlus(null, right.clone(), symbol);
			newUnaryPlus.isEmbracedByParentheses = isEmbracedByParentheses;
			return newUnaryPlus;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return right.makeCSP(model);
		}
	}

	public static class UnaryLogNeg extends Node {
		public UnaryLogNeg(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			UnaryLogNeg newUnaryLogNeg = new UnaryLogNeg(null, right.clone(),
					symbol);
			newUnaryLogNeg.isEmbracedByParentheses = isEmbracedByParentheses;
			return newUnaryLogNeg;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return right.makeCSP(model).eq(0).boolVar();
			// return right.makeCSP(model).getValue() != 0 ?
			// model.boolVar(false) : model.boolVar(true);
		}
	}

	public static class UnaryBitNeg extends Node {
		public UnaryBitNeg(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			UnaryBitNeg newUnaryBitNeg = new UnaryBitNeg(null, right.clone(),
					symbol);
			newUnaryBitNeg.isEmbracedByParentheses = isEmbracedByParentheses;
			return newUnaryBitNeg;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			return model.intVar(~right.makeCSP(model).getValue());
		}
	}

	public static class Macro extends Node {
		public Macro(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			Macro newMacro = new Macro(null, null, symbol);
			newMacro.isEmbracedByParentheses = isEmbracedByParentheses;
			newMacro.value = value;
			return newMacro;
		}

		public void setValue(long value) {
			this.value = value;
		}

		public long getValue() {
			return value;
		}

		private long value;

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			IntVar macro = macros.get(symbol);
			// not previously registered? same var within different exprs!
			if (macro == null) {
				// System.out.println("Inserting " + symbol);
				macros.put(
						symbol,
						macro = model.intVar(IntVar.MIN_INT_BOUND,
								IntVar.MAX_INT_BOUND, true));
				//macros.put(symbol, macro = model.intVar(-100, 100, true));
			}
			// else {
			// System.out.println("Symbol " + symbol + " already defined");
			// }
			return macro;
		}
	}

	public static class FunctionMacro extends Macro {
		public FunctionMacro(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		/**
		 * Returns textual representation of this tree node.
		 */
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(symbol);			
			sb.append("(");
			// concat argument list
			if (args != null && !args.isEmpty()) {
				Iterator<Node> it = args.iterator();
				while (it.hasNext()) {
					sb.append(it.next() + (it.hasNext() ? "," : ""));
				}
			}
			sb.append(")");
			return sb.toString();
		}

		@Override
		public Node clone() {
			FunctionMacro newFMacro = new FunctionMacro(null, null, symbol);
			if (args != null) {
				LinkedList<Node> argsCloned = new LinkedList<FeatureTree.Node>();
				for (Node arg : this.args) {
					argsCloned.add(arg.clone());
				}
				newFMacro.setArgs(argsCloned);
			}
			return newFMacro;
		}

		/**
		 * Sets potential argument lists of parsed function macro nodes. Since
		 * these are also analyzed bottom-up within parser, an argument list can
		 * either be empty (null), or can otherwise contain an already linked
		 * argument structure (node->node->...).
		 * 
		 * @param args
		 *            argument node potential function macro argument(s)
		 */
		public void setArgs(LinkedList<Node> args) {
			this.args = args;
		}

		/** arguments for function macros */
		private LinkedList<Node> args;
	}

	public static class Defined extends Node {
		public Defined(Node left, Node right, String symbol) {
			super(left, right, symbol);
		}

		@Override
		public Node clone() {
			Defined newDefined = new Defined(null, right.clone(), symbol);
			newDefined.isEmbracedByParentheses = isEmbracedByParentheses;
			return newDefined;
		}

		@Override
		public IntVar makeCSP(Model model) throws Exception {
			// TODO
			return right.makeCSP(model).ne(0).boolVar();
		}
	}

	/** root node of feature expression ast */
	protected Node root;
	/** keyword of conditional (e.g. #if|#ifdef|#ifndef|#elif|#else) */
	protected String keyword;
	protected static HashMap<String, IntVar> macros = new HashMap<String, IntVar>();
	//protected int tangling_degree;
	protected ExpressionParser.ObjMacroHistogram tdMap;
}
