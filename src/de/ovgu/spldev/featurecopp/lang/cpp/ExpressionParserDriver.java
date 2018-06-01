package de.ovgu.spldev.featurecopp.lang.cpp;

import java.io.StringReader;

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
	public FeatureTree run(boolean showLexerOutput, final String cpp_directive) throws Exception {
		lexer.debug(showLexerOutput);
		if(currReader != null) {
			currReader.close();
		}
		currReader = new StringReader(cpp_directive);
		lexer.yyreset(currReader);
		FeatureTree ftree = (FeatureTree)parser.parse().value;
		if(ftree == null) {
			// should never happen!
			throw new Exception("No feature tree generated");
		}
		return ftree;
	}
	/** generated parser */
	private ExpressionParser parser;
	private ExpressionLexer lexer;
	private StringReader currReader;
}
