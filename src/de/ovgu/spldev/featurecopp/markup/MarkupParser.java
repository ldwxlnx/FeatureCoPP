
//----------------------------------------------------
// The following code was generated by CUP v0.11a beta 20060608
// Wed Mar 28 10:15:15 CEST 2018
//----------------------------------------------------

package de.ovgu.spldev.featurecopp.markup;

import java_cup.runtime.*;
import de.ovgu.spldev.featurecopp.*;
import de.ovgu.spldev.featurecopp.markup.MarkupLexer.Markup;

/** CUP v0.11a beta 20060608 generated parser.
  * @version Wed Mar 28 10:15:15 CEST 2018
  */
public class MarkupParser extends java_cup.runtime.lr_parser {

  /** Default constructor. */
  public MarkupParser() {super();}

  /** Constructor which sets the default scanner. */
  public MarkupParser(java_cup.runtime.Scanner s) {super(s);}

  /** Constructor which sets the default scanner. */
  public MarkupParser(java_cup.runtime.Scanner s, java_cup.runtime.SymbolFactory sf) {super(s,sf);}

  /** Production table. */
  protected static final short _production_table[][] = 
    unpackFromStrings(new String[] {
    "\000\013\000\002\002\005\000\002\002\004\000\002\003" +
    "\003\000\002\003\003\000\002\004\007\000\002\005\010" +
    "\000\002\011\005\000\002\012\005\000\002\006\005\000" +
    "\002\007\005\000\002\010\005" });

  /** Access to production table. */
  public short[][] production_table() {return _production_table;}

  /** Parse-action table. */
  protected static final short[][] _action_table = 
    unpackFromStrings(new String[] {
    "\000\042\000\004\004\004\001\002\000\006\006\011\007" +
    "\010\001\002\000\004\002\006\001\002\000\004\002\000" +
    "\001\002\000\004\005\ufffe\001\002\000\004\010\036\001" +
    "\002\000\004\010\015\001\002\000\004\005\014\001\002" +
    "\000\004\005\uffff\001\002\000\004\002\001\001\002\000" +
    "\004\011\016\001\002\000\004\016\034\001\002\000\004" +
    "\012\021\001\002\000\004\013\024\001\002\000\004\016" +
    "\022\001\002\000\004\017\023\001\002\000\006\013\ufffa" +
    "\014\ufffa\001\002\000\004\016\032\001\002\000\004\015" +
    "\027\001\002\000\004\005\ufffc\001\002\000\004\016\030" +
    "\001\002\000\004\017\031\001\002\000\004\005\ufff7\001" +
    "\002\000\004\017\033\001\002\000\004\015\ufff9\001\002" +
    "\000\004\017\035\001\002\000\004\012\ufffb\001\002\000" +
    "\004\011\016\001\002\000\004\012\021\001\002\000\004" +
    "\014\042\001\002\000\004\005\ufffd\001\002\000\004\016" +
    "\043\001\002\000\004\017\044\001\002\000\004\005\ufff8" +
    "\001\002" });

  /** Access to parse-action table. */
  public short[][] action_table() {return _action_table;}

  /** <code>reduce_goto</code> table. */
  protected static final short[][] _reduce_table = 
    unpackFromStrings(new String[] {
    "\000\042\000\004\002\004\001\001\000\010\003\011\004" +
    "\012\005\006\001\001\000\002\001\001\000\002\001\001" +
    "\000\002\001\001\000\002\001\001\000\002\001\001\000" +
    "\002\001\001\000\002\001\001\000\002\001\001\000\004" +
    "\011\016\001\001\000\002\001\001\000\004\012\017\001" +
    "\001\000\004\006\024\001\001\000\002\001\001\000\002" +
    "\001\001\000\002\001\001\000\002\001\001\000\004\010" +
    "\025\001\001\000\002\001\001\000\002\001\001\000\002" +
    "\001\001\000\002\001\001\000\002\001\001\000\002\001" +
    "\001\000\002\001\001\000\002\001\001\000\004\011\036" +
    "\001\001\000\004\012\037\001\001\000\004\007\040\001" +
    "\001\000\002\001\001\000\002\001\001\000\002\001\001" +
    "\000\002\001\001" });

  /** Access to <code>reduce_goto</code> table. */
  public short[][] reduce_table() {return _reduce_table;}

  /** Instance of action encapsulation class. */
  protected CUP$MarkupParser$actions action_obj;

  /** Action encapsulation object initializer. */
  protected void init_actions()
    {
      action_obj = new CUP$MarkupParser$actions(this);
    }

  /** Invoke a user supplied parse action. */
  public java_cup.runtime.Symbol do_action(
    int                        act_num,
    java_cup.runtime.lr_parser parser,
    java.util.Stack            stack,
    int                        top)
    throws java.lang.Exception
  {
    /* call code in generated class */
    return action_obj.CUP$MarkupParser$do_action(act_num, parser, stack, top);
  }

  /** Indicates start state. */
  public int start_state() {return 0;}
  /** Indicates start production. */
  public int start_production() {return 1;}

  /** <code>EOF</code> Symbol index. */
  public int EOF_sym() {return 0;}

  /** <code>error</code> Symbol index. */
  public int error_sym() {return 1;}


  /** User initialization code. */
  public void user_init() throws java.lang.Exception
    {



    }

  /** Scan to get the next Symbol. */
  public java_cup.runtime.Symbol scan()
    throws java.lang.Exception
    {

	// just to obtain the line/column-occurences
	Symbol currRead = getScanner().next_token();
	line = currRead.left + 1;
	column = currRead.right + 1;
	return currRead;

    }


	public static class ParserException extends Exception {
		public ParserException(String msg) {
			super(msg);
		}
	}
	public int getLine() {
		return line;
	}
	public int getColumn() {
		return column;
	}
    /* Change the method report_error so it will display the line and
       column of where the error occurred in the input as well as the
       reason for the error which is passed into the method in the
       String 'message'. */
    public void report_error(String message, Object info) {
        /* Create a StringBuffer called 'm' with the string 'Error' in it. */
        m = new StringBuffer();
   
        /* Check if the information passed to the method is the same
           type as the type java_cup.runtime.Symbol. */
        if (info instanceof java_cup.runtime.Symbol) {
            /* Declare a java_cup.runtime.Symbol object 's' with the
               information in the object info that is being typecasted
               as a java_cup.runtime.Symbol object. */
            java_cup.runtime.Symbol s = ((java_cup.runtime.Symbol) info);
   
            /* Check if the line number in the input is greater or
               equal to zero. */
            if (s.left >= 0) {                
                /* Add to the end of the StringBuffer error message
                   the line number of the error in the input. */
                m.append(" in line "+ s.left);   
                /* Check if the column number in the input is greater
                   or equal to zero. */
                if (s.right >= 0)                    
                    /* Add to the end of the StringBuffer error message
                       the column number of the error in the input. */
                    m.append(", column "+ s.right);
            }
        }
   
        /* Add to the end of the StringBuffer error message created in
           this method the message that was passed into this method. */
        m.append(message);       
        /* Print the contents of the StringBuffer 'm', which contains
           an error message, out on a line. */
        //System.err.println(m);
    }
   
    /* Change the method report_fatal_error so when it reports a fatal
       error it will display the line and column number of where the
       fatal error occurred in the input as well as the reason for the
       fatal error which is passed into the method in the object
       'message' and then exit.*/
    public void report_fatal_error(String message, Object info) throws Exception {
        report_error(message, info);        
        throw new ParserException("Syntax error: " + m.toString());
    }
    	// TODO 2014-07-27 off-by-one(+1) at e.g. Var-decls, otherwise correct
	// current line where lexed symbol occured
	private int line;
	// current column where lexed symbol occured
	private int column;
	private StringBuffer m;

}

/** Cup generated class to encapsulate user supplied action code.*/
class CUP$MarkupParser$actions {

	
	// init first

  private final MarkupParser parser;

  /** Constructor */
  CUP$MarkupParser$actions(MarkupParser parser) {
    this.parser = parser;
  }

  /** Method with the actual generated action code. */
  public final java_cup.runtime.Symbol CUP$MarkupParser$do_action(
    int                        CUP$MarkupParser$act_num,
    java_cup.runtime.lr_parser CUP$MarkupParser$parser,
    java.util.Stack            CUP$MarkupParser$stack,
    int                        CUP$MarkupParser$top)
    throws java.lang.Exception
    {
      /* Symbol object for return from actions */
      java_cup.runtime.Symbol CUP$MarkupParser$result;

      /* select the action based on the action number */
      switch (CUP$MarkupParser$act_num)
        {
          /*. . . . . . . . . . . . . . . . . . . .*/
          case 10: // n_directive ::= T_DIRECTIVE T_ASSIGN T_VALUE 
            {
              String RESULT =null;
		int valleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).left;
		int valright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).right;
		String val = (String)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.peek()).value;
		
		RESULT = val;
	
              CUP$MarkupParser$result = parser.getSymbolFactory().newSymbol("n_directive",6, ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-2)), ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()), RESULT);
            }
          return CUP$MarkupParser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 9: // n_dst ::= T_DST T_ASSIGN T_VALUE 
            {
              String RESULT =null;
		int valleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).left;
		int valright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).right;
		String val = (String)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.peek()).value;
		
		RESULT = val;
	
              CUP$MarkupParser$result = parser.getSymbolFactory().newSymbol("n_dst",5, ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-2)), ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()), RESULT);
            }
          return CUP$MarkupParser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 8: // n_src ::= T_SRC T_ASSIGN T_VALUE 
            {
              String RESULT =null;
		int valleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).left;
		int valright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).right;
		String val = (String)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.peek()).value;
		
		RESULT = val;
	
              CUP$MarkupParser$result = parser.getSymbolFactory().newSymbol("n_src",4, ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-2)), ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()), RESULT);
            }
          return CUP$MarkupParser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 7: // n_encl_occ ::= T_ENCL_OCC_ID T_ASSIGN T_VALUE 
            {
              Long RESULT =null;
		int valleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).left;
		int valright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).right;
		String val = (String)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.peek()).value;
		
		long eoid = -1;
		try {
			eoid = Long.parseLong(val);
		} catch(NumberFormatException nfe) {
			parser.report_fatal_error("encl_occ_id is not a number [" + nfe.getMessage() + "]", null);
		}
		RESULT = eoid;
	
              CUP$MarkupParser$result = parser.getSymbolFactory().newSymbol("n_encl_occ",8, ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-2)), ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()), RESULT);
            }
          return CUP$MarkupParser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 6: // n_occ ::= T_OCC_ID T_ASSIGN T_VALUE 
            {
              Long RESULT =null;
		int valleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).left;
		int valright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).right;
		String val = (String)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.peek()).value;
		
		long oid = -1;
		try {
			oid = Long.parseLong(val);
		} catch(NumberFormatException nfe) {
			parser.report_fatal_error("occ_id is not a number [" + nfe.getMessage() + "]", null);	
		}
		RESULT = oid;
	
              CUP$MarkupParser$result = parser.getSymbolFactory().newSymbol("n_occ",7, ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-2)), ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()), RESULT);
            }
          return CUP$MarkupParser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 5: // n_markup_block ::= T_AT T_INLINE n_occ n_encl_occ n_src n_directive 
            {
              MarkupLexer.Markup RESULT =null;
		int idleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-5)).left;
		int idright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-5)).right;
		String id = (String)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-5)).value;
		int oidleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-3)).left;
		int oidright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-3)).right;
		Long oid = (Long)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-3)).value;
		int eoidleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-2)).left;
		int eoidright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-2)).right;
		Long eoid = (Long)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-2)).value;
		int srcfileleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-1)).left;
		int srcfileright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-1)).right;
		String srcfile = (String)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-1)).value;
		int directiveleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).left;
		int directiveright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).right;
		String directive = (String)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.peek()).value;
		
		RESULT = new MarkupLexer.MarkupBlock(oid, eoid, srcfile, directive);
	
              CUP$MarkupParser$result = parser.getSymbolFactory().newSymbol("n_markup_block",3, ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-5)), ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()), RESULT);
            }
          return CUP$MarkupParser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 4: // n_markup_ref ::= T_DOLLAR T_INLINE n_occ n_encl_occ n_dst 
            {
              MarkupLexer.Markup RESULT =null;
		int idleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-4)).left;
		int idright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-4)).right;
		String id = (String)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-4)).value;
		int oidleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-2)).left;
		int oidright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-2)).right;
		Long oid = (Long)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-2)).value;
		int eoidleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-1)).left;
		int eoidright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-1)).right;
		Long eoid = (Long)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-1)).value;
		int dstfileleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).left;
		int dstfileright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).right;
		String dstfile = (String)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.peek()).value;
		
		RESULT = new MarkupLexer.Markup(oid, eoid, dstfile);
	
              CUP$MarkupParser$result = parser.getSymbolFactory().newSymbol("n_markup_ref",2, ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-4)), ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()), RESULT);
            }
          return CUP$MarkupParser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 3: // n_markup_base ::= n_markup_block 
            {
              MarkupLexer.Markup RESULT =null;
		int mleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).left;
		int mright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).right;
		MarkupLexer.Markup m = (MarkupLexer.Markup)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.peek()).value;
		
		RESULT = m;
	
              CUP$MarkupParser$result = parser.getSymbolFactory().newSymbol("n_markup_base",1, ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()), ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()), RESULT);
            }
          return CUP$MarkupParser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 2: // n_markup_base ::= n_markup_ref 
            {
              MarkupLexer.Markup RESULT =null;
		int mleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).left;
		int mright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()).right;
		MarkupLexer.Markup m = (MarkupLexer.Markup)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.peek()).value;
		
		RESULT = m;
	
              CUP$MarkupParser$result = parser.getSymbolFactory().newSymbol("n_markup_base",1, ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()), ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()), RESULT);
            }
          return CUP$MarkupParser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 1: // $START ::= n_markup EOF 
            {
              Object RESULT =null;
		int start_valleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-1)).left;
		int start_valright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-1)).right;
		MarkupLexer.Markup start_val = (MarkupLexer.Markup)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-1)).value;
		RESULT = start_val;
              CUP$MarkupParser$result = parser.getSymbolFactory().newSymbol("$START",0, ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-1)), ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()), RESULT);
            }
          /* ACCEPT */
          CUP$MarkupParser$parser.done_parsing();
          return CUP$MarkupParser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 0: // n_markup ::= T_COMM_BEGIN n_markup_base T_COMM_END 
            {
              MarkupLexer.Markup RESULT =null;
		int mleft = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-1)).left;
		int mright = ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-1)).right;
		MarkupLexer.Markup m = (MarkupLexer.Markup)((java_cup.runtime.Symbol) CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-1)).value;
		
		RESULT = m;
	
              CUP$MarkupParser$result = parser.getSymbolFactory().newSymbol("n_markup",0, ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.elementAt(CUP$MarkupParser$top-2)), ((java_cup.runtime.Symbol)CUP$MarkupParser$stack.peek()), RESULT);
            }
          return CUP$MarkupParser$result;

          /* . . . . . .*/
          default:
            throw new Exception(
               "Invalid action number found in internal parse table");

        }
    }
}

