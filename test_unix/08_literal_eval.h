/**
 * Test feature tree logical/arithmetic evaluation
 */
#if 1 + 2 * 3 // 7
#elif 1 * 4 / 2 // 2
#elif -1+-1 // -2
#elif +2--1 // 3
#elif 2 % (5 - 2)// 2
//#elif 1 / 0 // DivBYZero-Excp
//#elif 1 % 0 // DivBYZero-Excp
#elif 1 | 2 // 3
#elif 5 & 2 // 0
#elif 2 ^ 1 // 3
#elif 8 >> 1 // 4
#elif 4 << 2 // 16
/*
 * 00000000 00000000 00000000 00000101 -> 5
 * 11111111 11111111 11111111 11111010 -> -6
 */
#elif ~5 // -6
#elif 1 < 2 // 1
#elif 2 < 1 // 0
#elif 1 <= 1 // 1
#elif 2 <= 1 // 0
#elif 1 > 2 // 0
#elif 2 > 1 // 1
#elif 1 >= 1 // 1
#elif 0 >= 2 // 0
#elif 5 || 0 // 1
#elif 3 && 1 // 1
#elif ! 0 // 1
#elif 0 // 0
#elif 2 // 2
#elif +1 // 1
#elif -1 // -1
#elif (1 > 2) < (2 > 0) // 1
#elif 1 ? 4 * 4 : 2 * 2 // 16
#elif 0 ? 4 * 4 : 2 * 2 // 4
#endif
