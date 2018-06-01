/**
 * test aggregation of syntactically different but semantically equivalent feature exprs
 * else-clause expresses same feature as disjunction in line 13.
 * This works only for simple unary conditionals!
 * Synthesis of more complex conditionals incorporating dyadic operations results in
 * string representations (parentheses are added to preserve precedence
 */
#if defined(A)

#elif defined(B)

#else

#endif

#if !(defined(A) || defined(B))

#endif
