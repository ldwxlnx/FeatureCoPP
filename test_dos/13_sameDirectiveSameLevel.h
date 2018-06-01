/*********************************
 * Test separation behavior when
 * directives are at same level
 *********************************/

#if A
int i = 0;
# if A
int a = 0;
# elif A
int a = 1;
# else
int b = 2;
# endif
#elif A
int i = 1;
#elif A
int i = 2;
#else
int j = 3;
#endif
