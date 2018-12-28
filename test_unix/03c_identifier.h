/************************************
 we strictly follow the c11-standard
 see:
 n1570.pdf §6.4.2.1
 and extend by following implementation specific:
 - VMS-system-identifiers: identifier incl. $
************************************/
#if FOO
#endif

#if -FOO // ok, expression -> minus IDENTIFIER
#endif

//#ifdef -FOO // err, invalid macro name
//#endif

/*******************************************
unicode -- only specific ranges are allowed,
which we actually neglect
see:
https://en.cppreference.com/w/cpp/language/identifiers
section: "Unicode Characters in Identifiers"
********************************************/
#ifdef \u2776\u2778\u2779 // ok, unicode -> (1)(2)(3)
#endif 

// #ifdef 1FOO2 // err, macro names must conform to identifier rules [A-Za-z_]+[A-Z...
//#endif

#ifndef SS$_NORMAL // vms-stuff
#endif

/*******************
func macro oddities
********************/
#if IBYTE_HDR_FEATURE(112x, PASS1)
#endif

#if __has_include (<stdfix-avrlibc.h>)
#endif

#if __has_include__(<complex>)
#endif

#if __has_attribute (gnu::noreturn)
#endif

#if __has_include__ "complex.h"
#endif

// GCC issues only a warning^^
//#ifdef FOO(1, 2, 3) // err, not MACRONAME instead funmac-call, hence const expr TODO relax, if necessary
//#endif


