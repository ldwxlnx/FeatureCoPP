/**********************************************
 * Test if comment disposal works as expected
 **********************************************/
// glibc-2.9/stdlib/longlong.h lines 690-699
//#if (defined (_ARCH_PPC)	/* AIX */				\
//     || defined (_ARCH_PWR)	/* AIX */				\
//     || defined (_ARCH_COM)	/* AIX */				\
//     || defined (__powerpc__)	/* gcc */				\
//     || defined (__POWERPC__)	/* BEOS */				\
//     || defined (__ppc__)	/* Darwin */				\
//     || (defined (PPC) && ! defined (CPU_FAMILY)) /* gcc 2.7.x GNU&SysV */    \
//     || (defined (PPC) && defined (CPU_FAMILY)    /* VxWorks */               \
//         && CPU_FAMILY == PPC)                                                \
//     ) && W_TYPE_SIZE == 32
//#endif*/

// handled by parser (ignored during lexical analysis)
#if/* c1 */(defined (_ARCH_PPC))
#endif
#if (/*c2*/defined (_ARCH_PPC))
#endif
#if (defined /*c3*/(_ARCH_PPC))
#endif
#if (defined (/*c4*/_ARCH_PPC))
#endif
#if (defined (_ARCH_PPC/*c5*/))
#endif
#if (defined (_ARCH_PPC)/*c6*/)
#endif
#if (defined (_ARCH_PPC)) /*c7*/
#endif
#if (defined (_ARCH_PPC)) //c8
#endif
#if (defined (_ARCH_PPC))
//c9
#endif
#if (defined (_ARCH_PPC))
/* c10 */
#endif
#if (defined (_ARCH_PPC))
#endif/*c11*/

// multi-liner between tokens should never be detected by pre-detection (slurped before parsing and handled by parser)
#if /* foo \
 bar \
 baz*/ \
(defined (_ARCH_PPC))
#endif

// multi-liner between tokens should never be detected by pre-detection (slurped before parsing and handled by parser)
#if \
/* foo \
 bar \
 baz*/ \
(defined (_ARCH_PPC))
#endif

// multi-liner after endif (affects our comment pre-detection, not expr parsing!)
#if (defined (_ARCH_PPC))
// must be detected but must not be applied to directive of occurrence (#endif)
#endif  /* foo
#if _ARCH_PPC
 bar
#endif
 baz*/

#ifdef _ARCH_PPC
int i = 0;
#endif /* foo
  #if FROBNICATE
  #endif
 int i = 1;  */ int j = 4711; /* int k = 0;// closing AND opening detected (stay open!)?
	#if SCHNARF
	#endif
  */

// PROHIBITED!
// gcc -Wall -pedantic -E [file]
// -> error: missing binary operator before token "int
// seems like cpp is consuming comments like we do ;-)
//#if _ARCH_PPC
//int i = 2;
//#elif _ARCH_PPC < 2 /*
//	SCHNARF
//	*/ int z = 2; /*
//	Holsten knallt am dollsten!
//	*/
//#else /*
//  foobarbaz
// */
//int k = 12;
//#endif

// directives tangled within comments
#if _ARCH_PPC
int i = 2;
#elif _ARCH_PPC < 2 \
/*
	SCHNARF
	*/ || defined(A) /*
	Holsten knallt am dollsten!
	*/
int k = 3;
#else /*
  foobarbaz
 */
int k = 12;
#endif

// directive in multiline directive
#if _ARCH_PPC
int i = 2;
#elif _ARCH_PPC < 2 /*
	SCHNARF
	*/ \
&& defined(HAVE_A) \
	/*
	 * bar
	 */ \
&& defined(HAVE_B)
	int z = 2; /*
	with me is not good cherry eating!
	*/
#endif

// INVALID CPP (#if 83 not at beginning)
//#ifdef _ARCH_PPC
//int i = 0;
//#endif /* foo
//  #if FROBNICATE
//  #endif
//*/#if _ARCH_PPC /* int k = 0;// closing AND opening detected (stay open!)?\
//	#if SCHNARF \
//	#endif \
//  */
//	int i = 3;
//#endif
//*/
