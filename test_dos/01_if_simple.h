/****************************
 * Check basic if directives 
 ****************************/
#ifndef _IF_SIMPLE_01_H
#define _IF_SIMPLE_01_H
#if HAVE_A 
int i = 0;
int j = 1;
#endif

#if HAVE_B && HAVE_C
int k = 2;
int l = 3;
#endif

#if(HAVE_D || HAVE_E) && HAVE_F
int m = 2;
int n = 3;
#endif

#endif/* _IF_SIMPLE_01_H */
