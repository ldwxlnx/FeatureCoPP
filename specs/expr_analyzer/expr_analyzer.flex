/* --------------------------Usercode Section------------------------ */
package de.ovgu.spldev.featurecopp.lang.cpp;   
import java_cup.runtime.*;     
%%
   
/* -----------------Options and Declarations Section----------------- */
   
/* 
   The name of the class JFlex will create will be Lexer.
   Will write the code to the file Lexer.java. 
*/
%class ExpressionLexer

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
	ExpressionLexer(boolean isDebug, java.io.Reader in) {
		this(in);
		this.isDebug = isDebug;
	}
	/** Reusable Lexer with changing java.io.Readers*/
	ExpressionLexer() {

	}
	
	public static class ExpressionLexerException extends Exception {
		public ExpressionLexerException(String msg) {
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

/////// DIRECTIVE LINEBREAK FOR CONTINUATION
directiveLineTerminator = \\{lineTerminator}

/* comments */
comment = {traditionalComment} | {endOfLineComment} | {documentationComment} | {commentOpening}
// #ifdef FOO /* occurs eg. in linux-4.10.4/crypto/fcrypt.c line 307
// negation is expensive (see jflex doc)
commentOpening 		 = "/*" [[^] && !"*/"]
traditionalComment   = "/*" [^*] ~"*/" | "/*" "*"+ "/" | "/*" {commentContent} "*" + "/"
// Comment can be the last line of the file, without line terminator.
endOfLineComment     = "//" {inputCharacter}* {lineTerminator}?
documentationComment = "/**" {commentContent} "*"+ "/"
commentContent       = ( [^*] | \*+ [^/*] )*

//identifier = [:jletter:] [:jletterdigit:]*
// we are not checking for valid identifiers! we use 'id' for macro names 
// and for primary exprs. so an 'id' can also occur as argument to macro
// calls (basically any kind of c-language tokens or macroname), e.g.:
// SIBYTE_HDR_FEATURE(112x, PASS1) 
identifier = [A-Za-z0-9_]+

/** Additional function macro argument lexical hassle (found in gcc,
e.g., "__has_include (<stdfix-avrlibc.h>)" (cf. gcc-7.3.0 or similar clang header tests)
e.g., "__has_include__(<complex>)"
*/
// recognized by macro in macro arg list rule
funmac_arg_glob_header = "<" [A-Za-z0-9_\-/]+ (\.h)? ">"
funmac_arg_loc_header = "\"" [A-Za-z0-9_\-/]+ (\.h)? "\""
// e.g. __has_attribute (gnu::noreturn)
funmac_arg_namespace = [A-Za-z0-9_::]+
// e.g. #if __has_include__ "complex.h"
funmac_no_parentheses = {identifier}\s+({funmac_arg_glob_header}|{funmac_arg_loc_header})


/* A character literal is a single letter enclosed by single-quotes.
   We ignore whole ascii-set for simplicity now.
   Add further ascii-symbols here if needed
   Quotes are needed to distinguish from 'id'(entifier)-class.
   Note the SPACE as last character within last brackets [], allowing
   us to write e.g. i := ' '; (meaning: 0x20{1})!
   - and " need to be quoted
 */
char_lit = '[A-Za-z0-9]'|'[\[\]#\{\}()\!\"\$@%&/=\?`\*\+\~,;\.:\-_<>\|\^ ]'|
	'\\0'|'\\a'|'\\b'|'\\t'|'\\n'|'\\v'|'\\f'|'\\r'|'\\''|'\\\\'
	
	
/* see: http://en.cppreference.com/w/cpp/language/integer_literal */
//TODO suffixes and intermediate '
dec_nat_lit = 0 | [1-9][0-9]*
hex_nat_lit = 0(x|X)[0-9a-fA-F]+
oct_nat_lit = 0[0-7]+
bin_nat_lit = 0(b|B)[0-1]+
int_lit = ({dec_nat_lit} | {hex_nat_lit} | {oct_nat_lit} | {bin_nat_lit})

%%

<YYINITIAL> {
	"#" { // since whitespaces are allowed between hashmark '#' and keyword, we have to analyze syntactically!	
		if(isDebug) {
			System.err.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_SHARP, yytext());	
	}
	"if"  {
		if(isDebug) {
			System.err.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_IF, yytext());
	}
	"defined" {
		if(isDebug) {
			System.err.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_DEFINED, yytext());
	}
	"ifdef"  {
		if(isDebug) {
			System.err.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_IFDEF, yytext());
	}
	"ifndef"  {
		if(isDebug) {
			System.err.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_IFNDEF, yytext());
	}
	"elif" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_ELIF, yytext());
	}
	"?" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_QM, yytext());		
	}
	":" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_COLON, yytext());		
	}
	"(" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_LPAR, yytext());		
	}
	")" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_RPAR, yytext());
	}
	"!" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_LOG_NEG, yytext());
	}
	"~" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_TILD, yytext());
	}	
	"^" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_CARET, yytext());
	}	
	"+" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_PLUS, yytext());
	}
	"-" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_MINUS, yytext());
	}
	"|" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_PIPE, yytext());
	}
	"*" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_TIMES, yytext());
	}
	"/" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_DIVIDE, yytext());
	}
	"%" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_MOD, yytext());
	}
	"&" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_AMP, yytext());
	}
	"," {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_COMMA, yytext());
	}
	"<<" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_LSHIFT, yytext());
	}
	">>" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_RSHIFT, yytext());
	}
	"&&" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_LOG_AND, yytext());
	}
	"||" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_LOG_OR, yytext());
	}
	"<" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_LT, yytext());
	}
	">" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_GT, yytext());
	}	
	"<=" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_LE, yytext());
	}	
	">=" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_GE, yytext());
	}	
	"==" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_EQUIV, yytext());
	}
	"!=" {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_ANTIV, yytext());
	}
	{char_lit} {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_CHAR_LIT, yytext());	
	}	
	{int_lit} {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_INTEGER_LIT, yytext());	
	}
	{identifier} {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_IDENTIFIER, yytext());	
	}
	{comment} { 
		if(isDebug) {
			System.out.print(yytext());
		}
		//System.out.println(yyline + 1 + ":[" + yytext() + "]");
		// syntactically ignored
	}
	{directiveLineTerminator} {
		if(isDebug) {
			System.out.print(yytext());
		}
		//System.out.println(yyline + 1 + ":[" + yytext() + "]");
		// syntactically ignored
	}
//	{commentOpening} { 
//		if(isDebug) {
//			System.out.print(yytext());
//		}
//		System.out.print("O[" + yytext() + "]");
//		// syntactically ignored
//	}
	{whitespace} {
		if(isDebug) {
			System.out.print(yytext());
		}
		// syntactically ignored
	}
	{funmac_arg_glob_header} {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_FUNMAC_ARG_GLOB_HEADER, yytext());	
	}
	{funmac_arg_loc_header} {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_FUNMAC_ARG_LOC_HEADER, yytext());	
	}
	{funmac_arg_namespace} {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_FUNMAC_ARG_GLOB_HEADER, yytext());	
	}
	{funmac_no_parentheses} {
		if(isDebug) {
			System.out.print(yytext());
		}
		return genSymbol(ExpressionSymbols.T_FUNMAC_NO_PARENTHESES, yytext());	
	}
} // <YYINITIAL>

<<EOF>>  { return genSymbol(ExpressionSymbols.EOF); }

[^] { yytext(); throw new java.io.IOException("Illegal character ["+yytext()+"] at line: "
			+ (yyline + 1)
			+ " column: " + (yycolumn + 1) + "!"); }
