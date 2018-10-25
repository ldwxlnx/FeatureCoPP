#ifndef A // ROLE #1 SD #1
// foo
#else // ROLE #2 SD not detected -> new SD #2
// bar
#endif


#ifndef B // ROLE #3 SD #2  -> new SD #3
// foo
#else // ROLE #4 SD not detected  -> new SD #4
// bar
#endif

#if defined(A) // ROLE #4 SD #3  -> new SD #5
// baz
#endif

#if ! defined(A) && ! defined(B) // ROLE #5 SD #4 and #5  -> new SD #6 and #7
// foo
#else // ROLE #6 SD not detected  -> new SD #8 and 9
// bar
#endif

#if ! defined(A) && ! defined(B) // ROLE #7 SD #6 and #7  -> new SD #10 and #11
// foo
#endif

#if ! defined(A) && ! defined(B) // ROLE #8 SD #8 and #9  -> new SD #12 and #13

#endif

#if ! defined(A) && ! defined(B) // ROLE #9 SD #10 and #11  -> new SD #14 and #15

#endif

// SD_OLD(A=6,B=5) SD_NEW=(A=8,B=7)
