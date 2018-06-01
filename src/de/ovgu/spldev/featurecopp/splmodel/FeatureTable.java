package de.ovgu.spldev.featurecopp.splmodel;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
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
			fw.write(String.format("%" + indent + "s<features count=\"%d\" requested=\"%d\" roles=\"%d\">%s", // linebreak
					Configuration.XML_INDENT_WHITESPACE, getFeatureCount(), calcNumberOfRequestedFeatures(), calcTotalNumberOfRoles(), Configuration.LINE_SEPARATOR));
			// pretty expensive but xml analysis for humans is easier with some kind of order
			// -> see FeatureModule.compare
			Object[] featureValues = featureTable.values().toArray();
			Arrays.sort(featureValues);
			for(Object o : featureValues) {
				FeatureModule fm = (FeatureModule)o;
				fm.writeXmlTo(indent + 1, fw);
			}
			fw.write(String.format("%" + indent + "s</features>%s", // linebreak
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
	 * Counts total number of occurrences (roles)
	 * 
	 * @return requested feature count
	 */
	public static long calcTotalNumberOfRoles() {
		long count = 0;
		for (FeatureModule m : featureTable.values()) {
			count += m.numOfOccurrences();
		}
		return count;
	}

	/** FeatureTree.featureExpressionToString() => Feature */
	private static HashMap<String, FeatureModule> featureTable = new HashMap<String, FeatureModule>();
}
