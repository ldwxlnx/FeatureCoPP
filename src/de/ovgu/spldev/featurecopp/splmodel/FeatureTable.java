package de.ovgu.spldev.featurecopp.splmodel;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import de.ovgu.spldev.featurecopp.config.Configuration;

/**
 * Stores unique instances of detected feature (expressions).
 * 
 * @author K. Ludwig
 * 
 */
public class FeatureTable {
	/**
	 * Returns a feature corresponding to given feature tree. If none such
	 * feature exists, a new one is created and delivered to the caller. Key for
	 * feature retrieval is indirectly FeatureTree.toString()
	 * 
	 * @param ftree
	 *            preprocessor feature expression ast
	 * @param moduleDir
	 *            directory where to store feature module file
	 * @return feature according to feature tree
	 * @see FeatureTree.toString
	 */
	public static FeatureModule get(final FeatureTree ftree,
			final Path moduleDir) {
		String featureExpression = ftree.featureExprToString();
		FeatureModule feature = featureTable.get(featureExpression);
		// if not already present, add feature to table
		if (feature == null) {
			featureTable.put(featureExpression, feature = new FeatureModule(
					ftree, moduleDir));
		}
		return feature;
	}

	public static void writeXmlTo(int indent, FileWriter fw) throws Exception {
		if (fw != null) {
			// @formatter:off
			fw.write(String.format(Locale.US, "%" + indent + "s<features count=\"%d\" requested=\"%d\" roles=\"%d\">%s", // linebreak
					Configuration.XML_INDENT_WHITESPACE, getFeatureCount(), calcNumberOfRequestedFeatures(), calcTotalNumberOfRoles(), Configuration.LINE_SEPARATOR));
			// pretty expensive but xml analysis for humans is easier with some kind of order
			// -> see FeatureModule.compare
			Object[] featureValues = featureTable.values().toArray();
			Arrays.sort(featureValues);
			for(Object o : featureValues) {
				FeatureModule fm = (FeatureModule)o;
				fm.writeXmlTo(indent + 1, fw);
			}
			fw.write(String.format(Locale.US, "%" + indent + "s</features>%s", // linebreak
					Configuration.XML_INDENT_WHITESPACE, Configuration.LINE_SEPARATOR));
			// @formatter:on
		}
	}

	/**
	 * Returns amount of currently mapped features.
	 * 
	 * @return amount of currently mapped features
	 */
	public static int getFeatureCount() {
		return featureTable.size();
	}

	/**
	 * Counts total amount of user-requested features.
	 * 
	 * @return requested feature count
	 */
	public static long calcNumberOfRequestedFeatures() {
		long count = 0;
		for (FeatureModule m : featureTable.values()) {
			if (m.isRequested()) {
				count++;
			}
		}
		return count;
	}
	/**
	 * Counts requested number of occurrences (roles)
	 * 
	 * @return requested feature count
	 */
	public static long calcNumberOfRequestedRoles() {
		long count = 0;
		for (FeatureModule m : featureTable.values()) {
			if (m.isRequested()) {
				count += m.numOfOccurrences();
			}
		}
		return count;
	}
	
	/**
	 * Counts total number of occurrences (roles)
	 * 
	 * @return requested role count
	 */
	public static long calcTotalNumberOfRoles() {
		long count = 0;
		for (FeatureModule m : featureTable.values()) {
			count += m.numOfOccurrences();
		}
		return count;
	}
	/**
	 * Counts all else directives in requested feature module 
	 * @return
	 */
	public static long countElse() {
		long count = 0;
		for (FeatureModule m : featureTable.values()) {
			if (m.isRequested()) {
				count += m.numOfElse();
			}
		}
		return count;
	}
	/**
	 * Accumulate all simple absence conditions including #else directives
	 * @return number of simple absence conditions
	 */
	public static long countSimpleAbsence() {
		long count = 0;
		for (FeatureModule m : featureTable.values()) {
			if (m.isRequested() && m.isSimpleAbsence()) {
				count++;
			}
		}
		return count;
	}
	public static class DirectiveCount {
		public long getIfCount() {
			return ifCount;
		}
		public void setIfCount(long ifCount) {
			this.ifCount = ifCount;
		}
		public long getIfdefCount() {
			return ifdefCount;
		}
		public void setIfdefCount(long ifdefCount) {
			this.ifdefCount = ifdefCount;
		}
		public long getIfndefCount() {
			return ifndefCount;
		}
		public void setIfndefCount(long ifndefCount) {
			this.ifndefCount = ifndefCount;
		}
		public long getElifCount() {
			return elifCount;
		}
		public void setElifCount(long elifCount) {
			this.elifCount = elifCount;
		}
		public long getElseCount() {
			return elseCount;
		}
		public void setElseCount(long elseCount) {
			this.elseCount = elseCount;
		}
		private long ifCount;
		private long ifdefCount;
		private long ifndefCount;
		private long elifCount;
		private long elseCount;
	}
	public static DirectiveCount countRequestedDirectives() {
		DirectiveCount count = new DirectiveCount();
		for (FeatureModule m : featureTable.values()) {
			if (m.isRequested()) {
				count.setIfCount(count.getIfCount() + m.numOfIf());
				count.setIfdefCount(count.getIfdefCount() + m.numOfIfdef());
				count.setIfndefCount(count.getIfndefCount() + m.numOfIfndef());
				count.setElifCount(count.getElifCount() + m.numOfElif());
				count.setElseCount(count.getElseCount() + m.numOfElse());
			}
		}
		return count;
	}
	public static DirectiveCount countRequestedSimpleAbsenceDirectives() {
		DirectiveCount count = new DirectiveCount();
		for (FeatureModule m : featureTable.values()) {
			if (m.isRequested() && m.isSimpleAbsence()) {
				count.setIfCount(count.getIfCount() + m.numOfIf());
				//count.setIfdefCount(count.getIfdefCount() + m.numOfIfdef());
				count.setIfndefCount(count.getIfndefCount() + m.numOfIfndef());
				count.setElifCount(count.getElifCount() + m.numOfElif());
				count.setElseCount(count.getElseCount() + m.numOfElse());
			}
		}
		return count;
	}
	public static DirectiveCount countRequestedSimplePresenceDirectives() {
		DirectiveCount count = new DirectiveCount();
		for (FeatureModule m : featureTable.values()) {
			if (m.isRequested() && m.isSimplePresence()) {
				count.setIfCount(count.getIfCount() + m.numOfIf());
				count.setIfdefCount(count.getIfdefCount() + m.numOfIfdef());
				//count.setIfndefCount(count.getIfndefCount() + m.numOfIfndef());
				count.setElifCount(count.getElifCount() + m.numOfElif());
				count.setElseCount(count.getElseCount() + m.numOfElse());
			}
		}
		return count;
	}

	/** FeatureTree.featureExpressionToString() => Feature */
	private static HashMap<String, FeatureModule> featureTable = new HashMap<String, FeatureModule>();
}
