package de.ovgu.spldev.featurecopp.splmodel;

public class IfdefTree extends SynthesizedTree {
	public IfdefTree() {
		count++;
	}
	/**
	 * Restructures FeatureTree-expr to equivalent #if defined(MACRO)-tree 
	 */
	@Override
	public void setRoot(final Node root) {
		FeatureTree.Node clonedSubtree = root.clone();
		clonedSubtree.setEmbracedByParentheses();
		super.setRoot(root);		
		synthesizedSemanticsRoot = new FeatureTree.Defined(null, clonedSubtree, "defined");
	}

	public boolean isSimpleAbsence() {
		return false;
	}

	public boolean isSimplePresence() {
		return true;
	}
	public static long count; 
}
