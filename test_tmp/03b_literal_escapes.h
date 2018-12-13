/**************************************
 * Are escape char literals properly detected?
 **************************************/
 
// simple escapes  
#if ('\0' == 0)
#elif ('\a' == 7)
#elif ('\b' == 8)
#elif ('\t' == 9)
#elif ('\n' == 10)
#elif ('\v' == 11)
#elif ('\f' == 12)
#elif ('\r' == 13)
#elif ('\"' == 34)
#elif ('\'' == 39)
#elif ('\?' == 63)
#elif ('\\' == 92)
#endif

// octal digits
#if FOO == '\7'
#endif

#if FOO == '\10'
#endif

#if FOO == '\100'
#endif

// hex
#if FOO == '\x1'
#endif

#if FOO == '\x10'
#endif

#if FOO == '\xfF'
#endif

#if FOO == '\x100'
#endif

#if FOO == '\xCAFEBABE'
#endif

// wide-character constants
#if FOO == L'abc'
#endif

