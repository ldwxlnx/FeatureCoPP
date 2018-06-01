/* test aggregation of syntactically different but semantically equivalent feature exprs
 * #ifdef MACRO is  synthesized to #if defined(MACRO)
 * #ifndef MACRO is  synthesized to #if ! defined(MACRO)
 * hence, this file should show up following featurefiles:
 * 1.) 1.fcp for #if defined(HAVE_A) (3x) + 2x else clauses as negation for 2.)
 * 2.) 2.fcp for for #if ! defined(HAVE_A) (2x) + 3x else clauses as negation for 1.)
 */
#if defined HAVE_A
	int IFDEFINEDNOPAR;
#else
	int ELSEDEFINEDNOPAR;
#endif

#if defined( HAVE_A )
	int IFDEFINED;
#else
	int ELSEDEFINED;
#endif

#ifdef HAVE_A
	int IFDEF;
#else
	int IFDEF_ELSE;
#endif

#if ! defined(HAVE_A)
	int IFNOTDEFINED;
#else
	int ELSENOTDEFINED;
#endif

#ifndef HAVE_A
	int IFNDEF;
#else
	int IFNDEF_ELSE;
#endif
