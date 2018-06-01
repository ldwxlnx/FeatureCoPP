/****************************
 * Check basic if directives 
 ****************************/
#ifndef _IF_NESTED_02_H
#define _IF_NESTED_02_H
#if HAVE_A 
int i = 0;
int j = 1;
# if HAVE_B1 && HAVE_C1
int k = 2;
int l = 3;
# endif // HAVE_B1 && HAVE_C1
# if(HAVE_D1||HAVE_E1)&&HAVE_F1
#  if(HAVE_G2 ^ HAVE_H2)
int x = 0;
#  endif// HAVE_G2 ^ HAVE_H2
int m = 2;
int n = 3;
#  if(HAVE_I2 ^ HAVE_K2)
int x = 0;
#   if ( ~HAVE_L31 )
int y = 0;
#   endif // ~HAVE_L31
int z = 0;
#  endif // HAVE_I2 ^ HAVE_K2
int z = 1;
# endif//(HAVE_D1||HAVE_E1)&&HAVE_F1
z = 2;
#endif // HAVE_A
z = 3;
#endif/* _IF_NESTED_02_H */
