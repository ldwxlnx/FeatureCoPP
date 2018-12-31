/*
 * test nested feature tree expressions
 * - test if macro values expand to satisfiable settings
 * - check evaluation
 */
#ifndef _CONSTRAINTS_H
# ifdef FEAT_A
#  if A + B == 5 //2.) A=IV_1 = 21474836, B=IV_2 = -21474831, _CONSTRAINTS_H=IV_5 = 0, FEAT_A=IV_8 = -21474836
	int i;
#  else //1.) A=IV_1 = -21474836, B=IV_2 = -21474836, _CONSTRAINTS_H=IV_6 = 0, FEAT_A=IV_9 = -21474836
	int k;
#  endif
//////////////////////////
#  if A + B != 5 //4.) A=IV_1 = -21474836, B=IV_2 = -21474836, _CONSTRAINTS_H=IV_5 = 0, FEAT_A=IV_8 = -21474836
	int i;
#  else //3.) A=IV_1 = 21474836, B=IV_2 = -21474831, _CONSTRAINTS_H=IV_6 = 0, FEAT_A=IV_9 = -21474836
	int k;
#  endif
# endif /* FEAT_A */
#endif /* _CONSTRAINTS_H */
