#include <stdio.h>

#ifdef FEAT_A
int f(int i) {
  return i * i;
}
#else
double f(double d) {
  return d * d;
}
#endif


int main(int argc, char** argv) {
  printf(
#ifdef FEAT_A
	 "%d\n", f(2)
#else
	 "%f\n", f(3.141)
#endif
	 );
  return 0;
}
