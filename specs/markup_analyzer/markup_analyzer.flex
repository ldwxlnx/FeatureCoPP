/* --------------------------Usercode Section------------------------ */
package de.ovgu.spldev.featurecopp.markup;

import java.nio.file.Path;
import de.ovgu.spldev.featurecopp.config.Configuration;
import de.ovgu.spldev.featurecopp.filesystem.Filesystem;
import java_cup.runtime.*;     
%%
   
/* -----------------Options and Declarations Section----------------- */
   
/* 
   The name of the class JFlex will create will be Lexer.
   Will write the code to the file Lexer.java. 
*/
%class MarkupLexer

/* Makes the generated class public (the class is only accessible in its own package by default).*/
%public

/*
  The current line number can be accessed with the variable yyline
  and the current column number with the variable yycolumn.
*/
%line
%column
    
/* 
   Will switch to a CUP compatibility mode to interface with a CUP
   generated parser.
*/
%cup
/*
  Declarations
   
  Code between %{ and %}, both of which must be at the beginning of a
  line, will be copied letter to letter into the lexer class source.
  Here you declare member variables and functions that are used inside
  scanner actions.  
*/
%{	
	// NOTE! any markup changes within Markup regarding used keywords (e.g. 'inline') have to be applied
	// to scanner specification below as well, since JFlex is not capable of using variables as macros/regexes
	public static class Markup {
		public static boolean isReference(final String markupLine) {
			return markupLine.matches("^/\\*\\$inline.*(\r|\n|\r\n)$");
		}
		public static boolean isBlock(final String markupLine) {
			return markupLine.matches("^/\\*@inline.*(\r|\n|\r\n)$");
		}
		public static boolean isEnd(final String line) {
			return line.matches("^/\\*@end\\*/.*(\r|\n|\r\n)$");
		}
		public static String genRefMarkup(long occ_id, long encl_occ_id, String file) {
			return genMarkup("$", "dst", occ_id, encl_occ_id, file)			
					+ "*/" + SRC_LINE_SEPARATOR;
		}
		public static String genBlockOpenMarkup(long occ_id, long encl_occ_id, String file, String directive) {
			return genMarkup("@", "src", occ_id, encl_occ_id, file)					
					+ MARKUP_DELIM
					+ "directive=\"" + directive + "\""
					+ "*/" + SRC_LINE_SEPARATOR;
		}
		private static String genMarkup(String head, String locator, long occ_id, long encl_occ_id, String file) {
			return "/*"
					+ head // $|@
					+ "inline"
					+ MARKUP_DELIM
					+ "occ_id=\""
					+ occ_id
					+ "\""
					+ MARKUP_DELIM
					+ "encl_occ_id=\""
					+ encl_occ_id
					+ "\""
					+ MARKUP_DELIM
					+ locator // src/dst
					+ "=\"" + file
					+ "\"";
		}			
		public static String genCloseMarkup() {
			return "/*@end*/" + SRC_LINE_SEPARATOR;
		}
		public Markup(long occ_id, long encl_occ_id, String file) {
			this.occ_id = occ_id;
			this.encl_occ_id = encl_occ_id;
			this.file = Filesystem.genPath(file);
		}
		@Override
		public String toString() {
			return "occ_id=[" + occ_id
					+ "];encl_occ_id=[" + encl_occ_id
					+ "];file=[" + file
					+ "]";
		}
		public Path getFile() {
			return file;
		}
		public long getOccId() {
			return occ_id;
		}
		protected long occ_id;
		protected long encl_occ_id;
		protected Path file;
		// permantently changed by CPP_ANALYZER -> markup should have same line terms as source
		public static String SRC_LINE_SEPARATOR = Configuration.LINE_SEPARATOR;
		private static final String MARKUP_DELIM = " ";
	}

	public static class MarkupBlock extends Markup {
		public MarkupBlock(long occ_id, long encl_occ_id, String file, String directive) {
			super(occ_id, encl_occ_id, file);
			this.directive = directive;
		}
		@Override
		public String toString() {
			return super.toString()
					+ ";directive=[" + directive + "]";
		}
		protected String directive;
	}
	MarkupLexer(boolean isDebug, java.io.Reader in) {
		this(in);
		this.isDebug = isDebug;
	}
	/** Reusable Lexer with changing java.io.Readers*/
	MarkupLexer() {

	}
	
	public static class MarkupLexerException extends Exception {
		public MarkupLexerException(String msg) {
			super(msg);
		}
	}
	public void setReader(java.io.Reader in) {
		zzReader = in;
	}
	/**
	 * if true, enables debug output, silencing otherwise
	 */
	public void debug(boolean isDebug) {
		this.isDebug = isDebug;
	}
    /* To create a new java_cup.runtime.Symbol with information about
       the current token, the token will have no value in this
       case. */
    private Symbol genSymbol(int type) {
        return new Symbol(type, yyline + 1, yycolumn + 1);
    }
    
    /* Also creates a new java_cup.runtime.Symbol with information
       about the current token, but this object has a value. */
    private Symbol genSymbol(int type, Object value) {
        return new Symbol(type, yyline + 1, yycolumn + 1, value);
        
    }
    // current markup characters are stored here (created at state change to value) 
    private StringBuffer currValue;
    private boolean isDebug = false;
    //private boolean isDebug = true;
%} 

/*
  Macro Declarations
  
  These declarations are regular expressions that will be used latter
  in the Lexical Rules Section.  
*/
lineTerminator = \r|\n|\r\n
inputCharacter = [^\r\n]
whitespace     = {lineTerminator} | [ \t\f]
COMM_BEGIN = MarkupLexer.COMM_BEGIN
	
	

%state VALUE
%state CHAR_LIT

%%

<YYINITIAL> {
	"/\*" { 
		if(isDebug) {
			System.err.print(yytext());
		}
		return genSymbol(MarkupSymbols.T_COMM_BEGIN, yytext());	
	}
	"\*/"  {
		if(isDebug) {
			System.err.print(yytext());
		}
		return genSymbol(MarkupSymbols.T_COMM_END, yytext());
	}
	"@"  {
		if(isDebug) {
			System.err.print(yytext());
		}
		return genSymbol(MarkupSymbols.T_AT, yytext());
	}
	"$"  {
		if(isDebug) {
			System.err.print(yytext());
		}
		return genSymbol(MarkupSymbols.T_DOLLAR, yytext());
	}
	"inline" {
		if(isDebug) {
			System.err.print(yytext());
		}
		return genSymbol(MarkupSymbols.T_INLINE, yytext());
	}
	"occ_id"  {
		if(isDebug) {
			System.err.print(yytext());
		}
		return genSymbol(MarkupSymbols.T_OCC_ID, yytext());
	}
	"encl_occ_id"  {
		if(isDebug) {
			System.err.print(yytext());
		}
		return genSymbol(MarkupSymbols.T_ENCL_OCC_ID, yytext());
	}
	"src" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(MarkupSymbols.T_SRC, yytext());
	}
	"dst" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(MarkupSymbols.T_DST, yytext());		
	}
	"directive" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(MarkupSymbols.T_DIRECTIVE, yytext());		
	}
	"=" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(MarkupSymbols.T_ASSIGN, yytext());		
	}
	\"  {
		if(isDebug) {
			System.out.print(yytext());
		}
		currValue = new StringBuffer();
		yybegin(VALUE);
	}
	{whitespace} {
		if(isDebug) {
			System.out.print(yytext());
		}
		// syntactically ignored
	}
} // <YYINITIAL>

<VALUE> {
	\"  {
		if(isDebug) {
			System.out.print(yytext());
		} 
		yybegin(YYINITIAL);
		return genSymbol(MarkupSymbols.T_VALUE, currValue.toString());	
	}
	\' {
        if(isDebug) {
			System.out.print(yytext());
		}
		currValue.append(yytext());
		yybegin(CHAR_LIT);
    }
	[^\"\\\']+ {
		if(isDebug) {
			System.out.print(yytext());
		} 
		currValue.append( yytext() );
	}
    \\\" {
    	if(isDebug) {
			System.out.print(yytext());
		} 
    	currValue.append('\"');
    }
    \\ {
    	if(isDebug) {
			System.out.print(yytext());
		} 
    	currValue.append('\\');
    }
} // VALUE
// since char literals within directives can contain '"', we have to avoid obfuscation by a further state (and collection buffer)
<CHAR_LIT> {
    \' {
        if(isDebug) {
			System.out.print(yytext());
		}
		currValue.append(yytext());
		yybegin(VALUE);
    }
    \\\' {
    	if(isDebug) {
			System.out.print(yytext());
		}
		currValue.append(yytext());
    }
    \\\\ {
    	if(isDebug) {
			System.out.print(yytext());
		} 
    	currValue.append(yytext());
    }
    [^] {
    	if(isDebug) {
			System.out.print(yytext());
		}
		currValue.append(yytext());
    }
} // CHAR_LIT

<<EOF>>  { return genSymbol(MarkupSymbols.EOF); }

[^] { yytext(); throw new java.io.IOException("Illegal character ["+yytext()+"] at line: "
			+ (yyline + 1)
			+ " column: " + (yycolumn + 1) + "!"); }
