/* --------------------------Usercode Section------------------------ */
package de.ovgu.spldev.featurecopp.lang.cpp;       
import de.ovgu.spldev.featurecopp.markup.MarkupLexer;
%%
   
/* -----------------Options and Declarations Section----------------- */
   
/* 
   The name of the class JFlex will create will be Lexer.
   Will write the code to the file Lexer.java. 
*/
%class CPPScanner

/*
  The current line number can be accessed with the variable yyline
  and the current column number with the variable yycolumn.
*/
%line
%column
/* 
  Turns character counting on. The int member variable yychar contains
  the number of characters (starting with 0) from the beginning of input
  to the beginning of the current token.
*/  
%char
// activate unicode parsing
%unicode
// %8bit // results in out of bound errors (cf. jflex manual)
    
%type CPPAnalyzer.Token
/*
  Declarations
   
  Code between %{ and %}, both of which must be at the beginning of a
  line, will be copied letter to letter into the lexer class source.
  Here you declare member variables and functions that are used inside
  scanner actions.  
*/
%{	
	CPPScanner(boolean isDebug, java.io.Reader in) {
		this(in);
		this.isDebug = isDebug;
	}
	/** Reusable Lexer with changing java.io.Readers*/
	CPPScanner() {

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
	public int numOfChars() {
		return yychar;
	}
    /* To create a new java_cup.runtime.Symbol with information about
       the current token, the token will have no value in this
       case. */
    private CPPAnalyzer.Token genSymbol(CPPAnalyzer.TYPE type) {
    	return new CPPAnalyzer.Token(type, yyline + 1, yycolumn);
    }
    
    /* Also creates a new java_cup.runtime.Symbol with information
       about the current token, but this object has a value. */
    private CPPAnalyzer.Token genSymbol(CPPAnalyzer.TYPE type, String value) {
    	return new CPPAnalyzer.Token(type, yyline + 1, yycolumn, value);        
    }
    private boolean isDebug;
%} 

/*
  Macro Declarations -- These declarations are regular expressions that will be used latter
  in the Lexical Rules Section.  
*/

/////// LINEBREAK
lineTerminator = \r|\n|\r\n
inputCharacter = [^\r\n]
//whitespace     = {lineTerminator} | [ \t\f]

/////// COMMENTS
//comment = {traditionalComment} | {endOfLineComment} 
//| {documentationComment}
traditionalComment   = "/*" [^*] ~"*/" | "/*" "*"+ "/" | "/*" {commentContent} "*" + "/"
// Comment can be the last line of the file, without line terminator.
endOfLineComment     = "//" {inputCharacter}* {lineTerminator}?
//documentationComment = "/**" {commentContent} "*"+ "/"
commentContent       = ( [^*] | \*+ [^/*] )*

/////// DIRECTIVE LINEBREAK FOR CONTINUATION
directiveLineTerminator = \\{lineTerminator}

/* string and character literals */
//StringCharacter = [^\r\n\"\\]
//SingleCharacter = [^\r\n\'\\]
//OctDigit          = [0-7]
//HexDigit          = [0-9a-fA-F]


%state STRING_LIT
%state CHAR_LIT
// ambiguous tokens after such directives possible (e.g., mistaken as char literal: "Won't work")
%state ERROR_DIR  

%%


<YYINITIAL> {
	^[ \t\f]*"#"[ \t\f]*"if" {
		String yytext = yytext(); 	
		if(isDebug) {
			System.out.print(yytext);
		}
		return genSymbol(CPPAnalyzer.TYPE.IF, yytext);	
	}
	^[ \t\f]*"#"[ \t\f]*"ifdef" { 	
		String yytext = yytext(); 	
		if(isDebug) {
			System.out.print(yytext);
		}
		return genSymbol(CPPAnalyzer.TYPE.IFDEF, yytext);	
	}
	^[ \t\f]*"#"[ \t\f]*"ifndef" { 	
		String yytext = yytext(); 	
		if(isDebug) {
			System.out.print(yytext);
		}
		return genSymbol(CPPAnalyzer.TYPE.IFNDEF, yytext);		
	}
	^[ \t\f]*"#"[ \t\f]*"elif" { 	
		String yytext = yytext(); 	
		if(isDebug) {
			System.out.print(yytext);
		}
		return genSymbol(CPPAnalyzer.TYPE.ELIF, yytext);	
	}
	^[ \t\f]*"#"[ \t\f]*"else" { 	
		String yytext = yytext(); 	
		if(isDebug) {
			System.out.print(yytext);
		}
		return genSymbol(CPPAnalyzer.TYPE.ELSE, yytext);		
	}
	^[ \t\f]*"#"[ \t\f]*"endif" { 	
		String yytext = yytext(); 	
		if(isDebug) {
			System.out.print(yytext);
		}
		return genSymbol(CPPAnalyzer.TYPE.ENDIF, yytext);	
	}
	^[ \t\f]*"#"[ \t\f]*"error" { 	
		String yytext = yytext(); 	
		if(isDebug) {
			System.out.print(yytext);
		}
		yybegin(ERROR_DIR);
		return genSymbol(CPPAnalyzer.TYPE.ERROR, yytext);	
	}
/* switch to string state */
	\"  {
		String yytext = yytext();
		//System.out.print("String open");
		if(isDebug) {
			System.out.print(yytext);
		}
		yybegin(STRING_LIT);
		return genSymbol(CPPAnalyzer.TYPE.SRC, yytext);
	}
/* switch to char state */	
	\'  {
		String yytext = yytext();
		//System.out.print("Char open");
		if(isDebug) {
			System.out.print(yytext);
		}
		yybegin(CHAR_LIT);
		return genSymbol(CPPAnalyzer.TYPE.SRC, yytext);
	}
	// separate handling - do not summarize as 'comment'
	{traditionalComment} {
		String yytext = yytext(); 	
		if(isDebug) {
			System.out.print(yytext);
		}
		return genSymbol(CPPAnalyzer.TYPE.COMMENT, yytext);	
	}
	// separate handling - do not summarize as 'comment'
	{endOfLineComment} {
		String yytext = yytext(); 	
		if(isDebug) {
			System.out.print(yytext);
		}
		return genSymbol(CPPAnalyzer.TYPE.LINECOMMENT, yytext);	
	}
	{directiveLineTerminator} {
		String yytext = yytext(); 	
		if(isDebug) {
			System.out.print(yytext);
		}
		return genSymbol(CPPAnalyzer.TYPE.DIRLT, yytext);
	}
	{lineTerminator} {
		String yytext = yytext();
		if(isDebug) {
			System.out.print(yytext);
		}
		// every line separator designates which linesep Markup has to append
		// -> at least per file since mixed projects can have unix/win/old mac sources
		// -> we do it uncondinationally since (dirty) assignment is cheaper than evaluating if-stmt everytime!
		// => Merge parses linewise, hence potentially appended line spearators should match the src project:
		//		-> i.e. diff -r would show differences regarding those   
		MarkupLexer.Markup.SRC_LINE_SEPARATOR = yytext;
		return genSymbol(CPPAnalyzer.TYPE.LINETERM, yytext);	
	}
	// treat everything else as code
	[^] {
		String yytext = yytext();
		if(isDebug) {
			System.out.print(yytext);
		}
		return genSymbol(CPPAnalyzer.TYPE.SRC, yytext);		
	}
} // <YYINITIAL>
<ERROR_DIR> {
	{lineTerminator} {
		String yytext = yytext();
		if(isDebug) {
			System.out.print(yytext);
		}
		yybegin(YYINITIAL);
		return genSymbol(CPPAnalyzer.TYPE.LINETERM, yytext);	
	}
	/* anything else */
    [^] {
    	String yytext = yytext();
    	if(isDebug) {
			System.out.print(yytext);
		}
		return genSymbol(CPPAnalyzer.TYPE.SRC, yytext);
    }
}
// problems in linux -- necessary for e.g., glibc
/*JAVA EXAMPLE JFLEX SELF EXTENDED */
<STRING_LIT> {
	/* end of string literal - switch back to initial */
  \"  { 
      	String yytext = yytext();
      	//System.out.print("String close");
    	if(isDebug) {
			System.out.print(yytext);
		}
  		yybegin(YYINITIAL);
  		return genSymbol(CPPAnalyzer.TYPE.SRC, yytext);
  	}
  /* quoted double quote */
  "\\\"" {
      	String yytext = yytext();
    	if(isDebug) {
			System.out.print(yytext);
		}
  		return genSymbol(CPPAnalyzer.TYPE.SRC, yytext);
  	}
  /* quoted backspace - to distinguish above */
  "\\\\" {
      	String yytext = yytext();
    	if(isDebug) {
			System.out.print(yytext);
		}
  		return genSymbol(CPPAnalyzer.TYPE.SRC, yytext);
  	}
  /* anything else */
    [^] {
    	String yytext = yytext();
    	if(isDebug) {
			System.out.print(yytext);
		}
		return genSymbol(CPPAnalyzer.TYPE.SRC, yytext);
    }
}

<CHAR_LIT> {
	/* end of character literal - switch back to initial */
	\'  {
		String yytext = yytext();
		//System.out.print("Char close ");
		if(isDebug) {
			System.out.print(yytext);
		}
		yybegin(YYINITIAL);
		return genSymbol(CPPAnalyzer.TYPE.SRC, yytext);
	}
  /* quoted single quote */
  "\\'"\' {
      	String yytext = yytext();
    	if(isDebug) {
			System.out.print(yytext);
		}
  		yybegin(YYINITIAL);
  		return genSymbol(CPPAnalyzer.TYPE.SRC, yytext);
  	}
  	/* quoted backslash to distinguish from above */
  	"\\\\"\' {
      	String yytext = yytext();
    	if(isDebug) {
			System.out.print(yytext);
		}
  		yybegin(YYINITIAL);
  		return genSymbol(CPPAnalyzer.TYPE.SRC, yytext);
  	}
  /* anything else */
    [^] {
    	String yytext = yytext();
    	if(isDebug) {
			System.out.print(yytext);
		}
		return genSymbol(CPPAnalyzer.TYPE.SRC, yytext);
    }
}
/**/