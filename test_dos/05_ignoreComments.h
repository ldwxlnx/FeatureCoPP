/*******************************************
 * Test if commented directives are ignored
 * to assure stack symmetry of directives.
 *******************************************/

// if truncated
/*#if A && B
#endif
*/

// none truncated
/*
#if A && B
#endif
*/

// flag style commenting
/*********
#if A && B
#endif
**********/

// if/else ignored since not at line beginning
//#if A && B
/*#endif*/

// linux-4.10.4/arch/arm/include/asm/glue.h
#ifdef __KERNEL__
#ifdef __STDC__
#define ____glue(name,fn)       name##fn
#else
#define ____glue(name,fn)       name/**/fn
#endif
#define __glue(name,fn)         ____glue(name,fn)
#endif

/*/ */
int i;

///*//*///
//a/*b//b*/d//
///a*/b/*c//d/
/*/*/
/***
 ****/
