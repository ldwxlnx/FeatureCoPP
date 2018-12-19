package de.ovgu.spldev.featurecopp.lang.cpp;

import java.io.StringReader;
import java.util.regex.Pattern;

import de.ovgu.spldev.featurecopp.lang.cpp.ExpressionParser.ObjMacroHistogram;
import de.ovgu.spldev.featurecopp.splmodel.FeatureTree;

/**
 * Parser driver for conditional inspection
 * @author K. Ludwig
 *
 */
public final class ExpressionParserDriver {
	public ExpressionParserDriver() {
		lexer = new ExpressionLexer();
		parser = new ExpressionParser(lexer);			
	}
	public FeatureTree run(boolean showLexerOutput, final String cpp_directive, final Pattern requestPattern) throws Exception {
		lexer.debug(showLexerOutput);
		if(currReader != null) {
			currReader.close();
		}
		parser.setRequestPatternProj(requestPattern);
		currReader = new StringReader(cpp_directive);
		lexer.yyreset(currReader);
		FeatureTree ftree = (FeatureTree)parser.parse().value;
		if(ftree == null) {
			// should never happen!
			throw new Exception("No feature tree generated");
		}
		return ftree;
	}
	public void insertAndAccumulateValues(ObjMacroHistogram src) {
		ExpressionParser.getObjMacroHistogramProjInclElse().insertAndAccumulateValues(src);
	}
	public ObjMacroHistogram getObjMacroHistogramProj() {
		return ExpressionParser.getObjMacroHistogramProj();
	}
	/** generated parser */
	private ExpressionParser parser;
	private ExpressionLexer lexer;
	private StringReader currReader;
}
