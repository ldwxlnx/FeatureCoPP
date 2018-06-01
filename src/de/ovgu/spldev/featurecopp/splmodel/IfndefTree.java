package de.ovgu.spldev.featurecopp.splmodel;


public class IfndefTree extends SynthesizedTree {
	public IfndefTree() {
		count++;
	}
	/**
	 * Restructures FeatureTree-expr to equivalent #if ! defined(MACRO)-tree 
	 */
	@Override
	public void setRoot(final Node root) {
		FeatureTree.Node clonedSubtree = root.clone();
		clonedSubtree.setEmbracedByParentheses();
		super.setRoot(root);		
		synthesizedSemanticsRoot = new FeatureTree.UnaryLogNeg(null, new FeatureTree.Defined(null, clonedSubtree, "defined"), "!");
	}
	public static long count; 
}
