package de.ovgu.spldev.featurecopp.splmodel;

import java.util.HashMap;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

import de.ovgu.spldev.featurecopp.splmodel.FeatureTree.Node;

/**
 * Base class for synthesized FeatureTree displaying their semantic equivalent
 * e.g. If(n)defTrees aggregating their pure natural structure and their semantic
 * equivalent (#if defined(MACRO) to be recognized as identical feature expression/module. 
 * @author K. Ludwig
 *
 */
public class SynthesizedTree extends FeatureTree {
	/**
	 * Returns a cloned expression ast from synthesized root node.
	 * @return cloned synthesized expression ast
	 */
	public Node getRootCloned() {
		return synthesizedSemanticsRoot.clone();
	}
	/**
	 * Returns synthesized root node as root node. Root node can be used as child node in other feature
	 * trees (e.g. negated clauses).
	 * 
	 * @return root node
	 */
	@Override
	public Node getRoot() {
		return synthesizedSemanticsRoot;
	}
	@Override
	public String toString() {
		return keyword + " " + root.toString();
	}
	/**
	 * Returns string representation of feature expression ast.
	 * @return feature expression as concatenated string
	 */
	@Override
	public String featureExprToString() {
		return synthesizedSemanticsRoot.toString();
	}
	@Override
	public IntVar makeCSP(Model model, HashMap<String, IntVar> macros)
			throws Exception {
		// temporary symbol table for this and all enclosing feature trees
		FeatureTree.macros = macros;
		return makeCSP(model, synthesizedSemanticsRoot);
	}
	protected FeatureTree.Node synthesizedSemanticsRoot;
}
