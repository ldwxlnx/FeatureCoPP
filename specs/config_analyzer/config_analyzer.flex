/* --------------------------Usercode Section------------------------ */
package de.ovgu.spldev.featurecopp.config;   
import java_cup.runtime.*;   
import java.io.PrintStream;
%%
   
/* -----------------Options and Declarations Section----------------- */
   
/* 
   The name of the class JFlex will create will be Lexer.
   Will write the code to the file Lexer.java. 
*/
%class ConfigLexer

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
	ConfigLexer(boolean isDebug, java.io.Reader in) {
		this(in);
		this.isDebug = isDebug;
	}
	/** Reusable Lexer with changing java.io.Readers*/

	public static class ConfigLexerException extends Exception {
		public ConfigLexerException(String msg) {
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
    	if(isDebug) {
    		writeTokenStatsTo(System.err, type, value);
    	}
        return new Symbol(type, yyline + 1, yycolumn + 1, value);
        
    }
    private void writeTokenStatsTo(PrintStream strm, int type, Object value) {
    	if(strm == null) {
    		strm = System.out;
    	}
    	strm.println(String.format("Type=[%3d]; Token=[%s]", type, value));
    }
    
    private boolean isDebug;
    protected StringBuffer stringBuffer = new StringBuffer();
%} 

/*
  Macro Declarations
  
  These declarations are regular expressions that will be used latter
  in the Lexical Rules Section.  
*/
lineTerminator = \r|\n|\r\n
inputCharacter = [^\r\n]
whitespace     = {lineTerminator} | [ \t\f]


/****** COMMENTS ******
 **********************/
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
**************************/
global_identifier = "global"
user_identifier = [:jletter:] [:jletterdigit:]*

/****** VARIABLES ******
************************/
home_var = "${HOME}"
tmpdir_var = "${TMP}"
progname_var = "${PROG}"
username_var = "${USER}"
cwd_var = "${CWD}"


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

		
%state STRING		
%%

<YYINITIAL> {
	\"  { // switch to string state
		stringBuffer.setLength(0);
		yybegin(STRING);
	}
	"{" {
		return genSymbol(ConfigSymbols.T_LBRACE, yytext());	
	}	
	"}" {
		return genSymbol(ConfigSymbols.T_RBRACE, yytext());	
	}
	";" {
		return genSymbol(ConfigSymbols.T_SEMI, yytext());	
	}
	"=" {
		return genSymbol(ConfigSymbols.T_ASSIGN, yytext());	
	}
	"input" {
		return genSymbol(ConfigSymbols.T_INPUT, yytext());
	}
//	"output" {
//		return genSymbol(ConfigSymbols.T_OUTPUT, yytext());
//	}
	"filetype" {
		return genSymbol(ConfigSymbols.T_FILETYPE, yytext());
	}
	"macropattern" {
		return genSymbol(ConfigSymbols.T_MACROPATTERN, yytext());
	}
	"logdirectory" {
		return genSymbol(ConfigSymbols.T_LOGDIR, yytext());
	}
	"logprefix" {
		return genSymbol(ConfigSymbols.T_LOGPREFIX, yytext());
	}
	"logrotate" {
		return genSymbol(ConfigSymbols.T_LOGROTATE, yytext());
	}
	"debug" {
		return genSymbol(ConfigSymbols.T_DEBUG, yytext());
	}
	"mode" {
		return genSymbol(ConfigSymbols.T_MODE, yytext());
	}
	"report" {
		return genSymbol(ConfigSymbols.T_REPORT, Configuration.Mode.report);
	}
	"split" {
		return genSymbol(ConfigSymbols.T_SPLIT, Configuration.Mode.split);
	}
	"areport" {
		return genSymbol(ConfigSymbols.T_AREPORT, Configuration.Mode.areport);
	}
	"asplit" {
		return genSymbol(ConfigSymbols.T_ASPLIT, Configuration.Mode.asplit);
	}
	"merge" {
		return genSymbol(ConfigSymbols.T_MERGE, Configuration.Mode.merge);
	}
	"original" {
		return genSymbol(ConfigSymbols.T_ORIGINAL, yytext());
	}
	"blacklist" {
		return genSymbol(ConfigSymbols.T_BLACKLIST, yytext());
	}
	{global_identifier} {
		return genSymbol(ConfigSymbols.T_GLOBAL_ID, yytext());	
	}
	{user_identifier} {
		return genSymbol(ConfigSymbols.T_USER_ID, yytext());	
	}
//	{binary_digit} {
//		return genSymbol(ConfigSymbols.T_BINARY_DIGIT, Integer.parseInt(yytext()) == 1 ? true : false);
//	}
	{decimal_constant} {
		return genSymbol(ConfigSymbols.T_DECIMAL_NUMBER, Integer.parseInt(yytext()));
	}
	{comment} { 
		// syntactically ignored
	}
	{whitespace} {
		// syntactically ignored
	}
} // <YYINITIAL>
<STRING> {
    \" { // return from string state
        yybegin(YYINITIAL);
    	return genSymbol(ConfigSymbols.T_STRING, stringBuffer.toString());
    }
	{home_var} {		
		stringBuffer.append(System.getProperty("user.home"));
	}
	{tmpdir_var} {		
		stringBuffer.append(System.getProperty("java.io.tmpdir"));
	}
	{progname_var} {
		stringBuffer.append(Configuration.APPLICATION_NAME);
	}
	{username_var} {
		stringBuffer.append(System.getProperty("user.name"));
	}
	{cwd_var} {
		stringBuffer.append(System.getProperty("user.dir"));
	}
//    [^\n\r\"\\]+ {
//    	stringBuffer.append(yytext());
//    }
    \\t {
    	stringBuffer.append('\t');
    }
    \\n {
    	stringBuffer.append('\n');
    }
    \\r {
    	stringBuffer.append('\r');
    }
    \\\" {
    	stringBuffer.append('\"');
    }
    \\ {
    	stringBuffer.append('\\');
    }
    [^] {
    	stringBuffer.append(yytext());
    }
  }

<<EOF>>  { return genSymbol(ConfigSymbols.EOF); }

[^] { yytext(); throw new java.io.IOException("Illegal character ["+yytext()+"] at line: "
			+ (yyline + 1)
			+ " column: " + (yycolumn + 1) + "!"); }
