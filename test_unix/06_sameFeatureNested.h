// inner feature refers to enclosing within same feature file
#if A
int i = 1;
#if A
int j = 2;
#endif
int k = 3;
#endif
