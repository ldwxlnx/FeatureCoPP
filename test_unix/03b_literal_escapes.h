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

#if FOO == '\10' // 8
#endif

#if FOO == '\100' // 64
#endif

// hex
#if FOO == '\x1'
#endif

#if FOO == '\x10' // 16
#endif

#if FOO == '\xfF' // 255
#endif

#if FOO == '\x100' // 256
#endif

#if FOO == '\xCAFEBABE' // 3405691582
#endif

// utf-8 character literal
#if FOO == u8'\u0039' // ascii '9', value 57
#endif

//16-bit wide character constant
#if FOO == u'\u0039' // ascii '9', value 57
#endif
//32-bit wide character constant
#if FOO == U'\U00000039' // ascii '9', value 57
#endif
// wide-character constants
#if FOO == L'A' // ascii 'A', value 65
#endif

// multi-character constants
#if FOO == 'ab'   // 0x0000000000006162 -> 24930
#endif
#if FOO == 'abc'  // 0x0000000000616263 -> 6382179
#endif
#if FOO == 'abcd' // 0x0000000061626364 -> 1633837924
#endif

