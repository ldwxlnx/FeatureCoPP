/* detect simple negations and else
#if A // false false
#else // true true
#endif

#if ! A // true false
#else // false true
#endif

#if defined A // false false
#else // false true
#endif

#if ! defined A // true false
#else // false true
#endif

#if defined(A) // false false
#else // true true
#endif

#if ! defined (A) // true false
#else // false true
#endif

#if A // false false
#elif ! B // true false
#else // false true
#endif

#if A // false false
#elif B // false false
#else // false true
#endif
