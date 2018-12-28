/* --------------------------Usercode Section------------------------ */
package de.ovgu.spldev.featurecopp.lang.cpp;   
import java_cup.runtime.*; 
import java.io.PrintStream;  
import java.util.Arrays;
import java.util.Comparator;
import java.lang.reflect.Field;
import de.ovgu.spldev.featurecopp.log.Logger;
%%
   
/* -----------------Options and Declarations Section----------------- */
   
/* 
   The name of the class JFlex will create will be Lexer.
   Will write the code to the file Lexer.java. 
*/
%class ExpressionLexer
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
	ExpressionLexer(Logger logger, java.io.Reader in) {
		this(in);
		this.logger = logger;
	}
	/** Reusable Lexer with changing java.io.Readers*/
	ExpressionLexer() {

	}
	public static final void writeExpressionSymbolsToLog(Logger logger)
			throws IllegalArgumentException, IllegalAccessException {
		Field[] fields = ExpressionSymbols.class.getFields();
		Arrays.sort(fields, new Comparator<Field>() {

			@Override
			public int compare(Field l, Field r) {
				int result = 0;
				try {
					int lVal = l.getInt(l);
					int rVal = r.getInt(r);
					if (lVal < rVal) {
						result = -1;
					} else if (lVal > rVal) {
						result = 1;
					} else {
						result = 0;
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					System.err.println("Reflection failed: " + e.getMessage());
				}
				return result;
			}

		});
		for (int i = 0; i < fields.length; i++) {
			if(logger != null) {
				logger.writeDebug(String.format("Type=[%3d]->[%s]", i + 1,
					fields[i].getName()));
			}
		}
	}
	
	public static class ExpressionLexerException extends Exception {
		public ExpressionLexerException(String msg) {
			super(msg);
		}
	}
	public void setReader(java.io.Reader in) {
		zzReader = in;
	}
	public void setLogger(Logger logger) {
		this.logger = logger;
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
    	writeTokenStatsTo(type, value);
        return new Symbol(type, yyline + 1, yycolumn + 1, value);
        
    }
    private void writeTokenStatsTo(int type, Object value) {
    	if(logger != null) {
    		logger.writeDebug(String.format("Type=[%3d]; Token=[%s]", type, value));
    	}
    }
    private Logger logger;
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

/****** IDENTIFIERS ******
C11 n1570 �6.4.2.1, p.59ff
see also:
https://en.cppreference.com/w/c/language/identifier
https://en.cppreference.com/w/cpp/language/identifiers#Unqualified_identifiers
**************************/
//identifier = [:jletter:] [:jletterdigit:]*
// we are not checking for valid identifiers! we use 'id' for macro names 
// and for primary exprs. so an 'id' can also occur as argument to macro
// calls (basically any kind of c-language tokens or macroname), e.g.:
// SIBYTE_HDR_FEATURE(112x, PASS1) 
//identifier = [A-Za-z0-9_]+
identifier = {identifier_nondigit}+({identifier_nondigit}|{digit})*
identifier_nondigit = {nondigit} | {universal_character_name} | {implementation_defined}
nondigit = [A-Za-z_]
digit = [0-9]
universal_character_name = {unicodeEscapeSequence} | {unicodeEscapeSequence}
implementation_defined = {vms_system_char}
vms_system_char = \$

/***** FUNCTION MACRO ARGUMENTS *****
Basically every macro arg is a const
expr, which we induce within Parser.
The following are exceptions from, e.g.,
Clang and GCC, which extend args by
far from "regular" const exprs:
e.g., "__has_include (<stdfix-avrlibc.h>)" (cf. gcc-7.3.0 or similar clang header tests)
e.g., "__has_include__(<complex>)"
*/
// recognized by macro in macro arg list rule
funmac_arg_glob_header = \<[A-Za-z0-9_\-/]+(\.h)?\>
funmac_arg_loc_header = \"[A-Za-z0-9_\-/]+ (\.h)?\"
// e.g. __has_attribute (gnu::noreturn)
funmac_arg_namespace = [A-Za-z0-9_:]+
// e.g. #if __has_include__ "complex.h"
funmac_no_parentheses = {identifier}\s+({funmac_arg_glob_header}|{funmac_arg_loc_header})

/****** CHARACTERS ********
 C11 n1570 �6.4.4.4, p. 67
 **************************/
character_constant = {c_char_prefix}?'{c_char_sequence}'
c_char_prefix = u8 | [LuU]
c_char_sequence = {c_char}+
c_char = [A-Za-z0-9] | [\[\]\"#\{\}()\!\$@%&/=\?`\*\+\~,;\.:\-_<>\|\^ ] | {escapeSequence} 
escapeSequence = {simpleEscapeSequence} | {octalEscapeSequence} | {hexadecimalEscapeSequence} | {unicodeEscapeSequence} | {UnicodeEscapeSequence}
simpleEscapeSequence = \\0 | \\' | \\\" | \\\? | \\\\ | \\a | \\b | \\f | \\n | \\r | \\t | \\v
octalEscapeSequence = \\{octal_digit}{1,3}
hexadecimalEscapeSequence = \\x{hexadecimal_digit}+
unicodeEscapeSequence = \\u{hexadecimal_digit}{4,4}
UnicodeEscapeSequence = \\U{hexadecimal_digit}{8,8}

/****** INTEGERS **********
 C11 n1570 �6.4.4.1, p. 62
 see also: http://en.cppreference.com/w/cpp/language/integer_literal
 TODO: C++14 "Optional single quotes(') may be inserted between the digits as a separator. They are ignored by the compiler."
 **************************/
 integer_constant = ({binary_constant}|{decimal_constant}|{octal_constant}|{hexadecimal_constant}){integer_suffix}?
 /**** SUFFIXES ****/
 integer_suffix = {unsigned_suffix}({long_suffix}|{long_long_suffix})? | ({long_suffix}|{long_long_suffix}){long_suffix}?
 unsigned_suffix = [uU]
 long_suffix = [lL]
 long_long_suffix = ll | LL
 /**** DECIMAL ****/
 decimal_constant = 0 | [1-9][0-9]*
/**** HEXADECIMAL ****/
hexadecimal_constant = {hexadecimal_prefix}{hexadecimal_digit}*
hexadecimal_prefix = 0(x|X)
hexadecimal_digit = [0-9a-fA-F]
/**** OCTAL *****/
octal_constant = 0{octal_digit}*
octal_digit = [0-7]
/**** BINARY CXX14 ****/
binary_constant = {binary_prefix}{binary_digit}+
binary_prefix = 0(b|B)
binary_digit = [0-1]

%%

<YYINITIAL> {
	"#" { // since whitespaces are allowed between hashmark '#' and keyword, we have to analyze syntactically!	
		return genSymbol(ExpressionSymbols.T_SHARP, yytext());	
	}
	"if"  {
		return genSymbol(ExpressionSymbols.T_IF, yytext());
	}
	"defined" {
		return genSymbol(ExpressionSymbols.T_DEFINED, yytext());
	}
	"ifdef"  {
		return genSymbol(ExpressionSymbols.T_IFDEF, yytext());
	}
	"ifndef"  {
		return genSymbol(ExpressionSymbols.T_IFNDEF, yytext());
	}
	"elif" {
		return genSymbol(ExpressionSymbols.T_ELIF, yytext());
	}
	"?" {
		return genSymbol(ExpressionSymbols.T_QM, yytext());		
	}
	":" {
		return genSymbol(ExpressionSymbols.T_COLON, yytext());		
	}
	"(" {
		return genSymbol(ExpressionSymbols.T_LPAR, yytext());		
	}
	")" {
		return genSymbol(ExpressionSymbols.T_RPAR, yytext());
	}
	"!" {
		return genSymbol(ExpressionSymbols.T_LOG_NEG, yytext());
	}
	"~" {
		return genSymbol(ExpressionSymbols.T_TILD, yytext());
	}	
	"^" {
		return genSymbol(ExpressionSymbols.T_CARET, yytext());
	}	
	"+" {
		return genSymbol(ExpressionSymbols.T_PLUS, yytext());
	}
	"-" {
		return genSymbol(ExpressionSymbols.T_MINUS, yytext());
	}
	"|" {
		return genSymbol(ExpressionSymbols.T_PIPE, yytext());
	}
	"*" {
		return genSymbol(ExpressionSymbols.T_TIMES, yytext());
	}
	"/" {
		return genSymbol(ExpressionSymbols.T_DIVIDE, yytext());
	}
	"%" {
		return genSymbol(ExpressionSymbols.T_MOD, yytext());
	}
	"&" {
		return genSymbol(ExpressionSymbols.T_AMP, yytext());
	}
	"," {
		return genSymbol(ExpressionSymbols.T_COMMA, yytext());
	}
	"<<" {
		return genSymbol(ExpressionSymbols.T_LSHIFT, yytext());
	}
	">>" {
		return genSymbol(ExpressionSymbols.T_RSHIFT, yytext());
	}
	"&&" {
		return genSymbol(ExpressionSymbols.T_LOG_AND, yytext());
	}
	"||" {
		return genSymbol(ExpressionSymbols.T_LOG_OR, yytext());
	}
	"<" {
		return genSymbol(ExpressionSymbols.T_LT, yytext());
	}
	">" {
		return genSymbol(ExpressionSymbols.T_GT, yytext());
	}	
	"<=" {
		return genSymbol(ExpressionSymbols.T_LE, yytext());
	}	
	">=" {
		return genSymbol(ExpressionSymbols.T_GE, yytext());
	}	
	"==" {
		return genSymbol(ExpressionSymbols.T_EQUIV, yytext());
	}
	"!=" {
		return genSymbol(ExpressionSymbols.T_ANTIV, yytext());
	}
	{character_constant} {
		return genSymbol(ExpressionSymbols.T_CHAR_LIT, yytext());	
	}	
	{integer_constant} {
		return genSymbol(ExpressionSymbols.T_INTEGER_LIT, yytext());	
	}
	{identifier} {
		return genSymbol(ExpressionSymbols.T_IDENTIFIER, yytext());	
	}
	{comment} { 
		// syntactically ignored
	}
	{directiveLineTerminator} {
		// syntactically ignored
	}
//	{commentOpening} { 
//		// syntactically ignored
//	}
	{whitespace} {
		// syntactically ignored
	}
	{funmac_arg_glob_header} {
		return genSymbol(ExpressionSymbols.T_FUNMAC_ARG_GLOB_HEADER, yytext());	
	}
	{funmac_arg_loc_header} {
		return genSymbol(ExpressionSymbols.T_FUNMAC_ARG_LOC_HEADER, yytext());	
	}
	{funmac_arg_namespace} {
		return genSymbol(ExpressionSymbols.T_FUNMAC_ARG_NAMESPACE, yytext());	
	}
	{funmac_no_parentheses} {
		return genSymbol(ExpressionSymbols.T_FUNMAC_NO_PARENTHESES, yytext());	
	}
} // <YYINITIAL>

<<EOF>>  { return genSymbol(ExpressionSymbols.EOF); }

[^] { yytext(); throw new java.io.IOException("Illegal character ["+yytext()+"] at line: "
			+ (yyline + 1)
			+ " column: " + (yycolumn + 1) + "!"); }
