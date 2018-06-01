#if IS_FOO(A, 1, defined(BAR))
int i;
#endif

/* TODO: wrong csp solving - currently based on macro name which leads to contradiction here*/
#if IS_FOO(B)
int j;
# if ! IS_FOO(C)
	int k;
# endif
#endif
