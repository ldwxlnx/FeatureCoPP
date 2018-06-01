package de.ovgu.spldev.featurecopp.markup;

import java.io.StringReader;

public class MarkupParserDriver {

	public MarkupParserDriver() {
		lexer = new MarkupLexer();
		parser = new MarkupParser(lexer);			
	}
	public MarkupLexer.Markup run(boolean showLexerOutput, final String markupString) throws Exception {
		lexer.debug(showLexerOutput);
		if(currReader != null) {
			currReader.close();
		}
		currReader = new StringReader(markupString);
		lexer.yyreset(currReader);
		MarkupLexer.Markup markup = (MarkupLexer.Markup)parser.parse().value;
		if(markup == null) {
			// should never happen!
			throw new Exception("No markup generated");
		}
		return markup;
	}
	/** generated parser */
	private MarkupParser parser;
	private MarkupLexer lexer;
	private StringReader currReader;	
}
