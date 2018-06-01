/*
 * test basic feature tree expressions
 * - test if macro values expand to satisfiable settings
 * - check evaluation
 */
// condition is tautology -> evaluate 1 + 2 = 3 => active
#if 1 ? 1 + 2 : 3 - 3
#endif

// condition is unsatisfiable -> evaluate 3 - 3 = 0 => hence dead
#if 0 ? 1 + 2 : 3 - 3
#endif

//TODO
#if !A ? A == 5 && A < B : A == 3 && A > B
#endif
//TODO
#if A ? A == 5 && A < B : A == 3 && A > B
#endif

// A = -21474836, B= -21474836, EVAL=1
#if A || B
#endif

// A = -21474836, B = -21474836, EVAL=1
#if A && B
#endif

// A = -21474836, B = -21474836, EVAL=-21474836
#if A | B
#endif

// TODO
// {A=IV_1 = [-21474836,21474836], B=IV_2 = [-21474836,21474836]}
#if A ^ B
#endif

// EVAL=3
#if 1 ^ 2
#endif

// EVAL = 0, hence dead
#if 3 ^ 3
#endif

#if A & B
#endif

// EVAL = 2
#if 6 & 3
#endif

// EVAL = 0. hence dead
#if 1 & 2
#endif

// A = -21474836, B = -21474836, EVAL = 1
#if A == B
#endif

// EVAL = 0, hence dead
#if 2 == 3
#endif

// A = -21474835, B = -21474836, EVAL = 1
#if A != B
#endif

// EVAL = 0, hence dead
#if 1 != 1
#endif

// A = -21474836, B = -21474835, EVAL = 1
#if A < B
#endif

// A = -21474836, EVAL = 1
#if A <= 4711
#endif

// A = -21474835, B = -21474836, EVAL = 1
#if A > B
#endif

// B = 4711, EVAL = 1
#if  B >= 4711
#endif

// TODO dead? should be A = 1 B = 4 or A = 2 B = 3
#if (A << B) == 16
#endif

// EVAL = 0, hence dead
#if 0 << 2
#endif

// EVAL = 2
#if 16 >> 3
#endif

#if A + B
#endif

#if A - B
#endif

#if A * B
#endif

// very time consuming as bare A/B-expr (approx. 60s)
#if A / B == 2
#endif

// very time consuming as bare A%B-expr (approx. 300s)
#if A % B == 2
#endif

/*- Complete search - 1 solution found.
 Model[(CONFIG_PHYSICAL_START%CONFIG_PHYSICAL_ALIGN)!=0]
 Solutions: 1
 Building time : 0,001s
 Resolution time : 337,616s
 Nodes: 72.813.134 (215.668,2 n/s)
 Backtracks: 145.626.258
 Fails: 72.813.131
 Restarts: 0
 */
// TODO out-of-memory in (kernel)/arch/powerpc/include/asm/page_32.h
//#if (CONFIG_PHYSICAL_START % CONFIG_PHYSICAL_ALIGN) != 0
//#endif

// TODO line 182 in /home/miniztree/gtk+-3.20.0/gdk/win32/pktdef.h
// model else: [!(__MODE&PK_NORMAL_PRESSURE)&&(__DATA&PK_NORMAL_PRESSURE)]
// else-clause solved as "unsatisfiable" but already a setting like:
// __DATA = 1; PK_NORMAL_PRESSURE = 1; __MODE = 0 would be satisfiable!
#if (__DATA & PK_NORMAL_PRESSURE)
# if (__MODE & PK_NORMAL_PRESSURE)
/* relative */
int pkNormalPressure;
# else
/* absolute */
UINT pkNormalPressure;
# endif
#endif

#if A
#endif

// dead
#if 0
#endif

#if +A
#endif

#if -A
#endif

#if ! A
#endif

#if ~A
#endif

#if defined(A)
#endif

#if ! defined(B)
#endif

#ifdef C
#endif

#ifndef D
#endif

// octal: A = 8 decimal
#if A == 010
#endif

// hex: A = 255 decimal
#if A == 0xFF
#endif

// 21474836 decimal (choco max int value)
#if A == 0x147AE14
#endif

// TODO no solution (numerical bounds?)
#if A == 0x147AE15
#endif

// TODO no solution (numerical bounds?)
#if A == 0x7FFFFFFE
#endif

//TODO
// ERR: 2147483647: consider reducing the bounds to avoid unexpected results
#if A == 0x7FFFFFFF
#endif
