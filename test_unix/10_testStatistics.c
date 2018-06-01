#include <stdio.h>

#if defined(HAVE_A)
#include "stdlib.h"
double g(double x);
int f(int x) {
  return x * x;
}
#endif


#if defined(HAVE_A)
double g(double x) {
  return 1.0 / x;
}
#else
int f(int x) {
  return x + x;
}
typedef struct s {
  int i;
  double d;
  int (*fp)(int);
} S;
union t {
	int x;
	short y;
};
#endif

int main(int argc, char** argv) {
  int i = 3;
#if defined(HAVE_A)
  printf("%d\n", f(i));
#else
  S s1 = {
    .i = 4711,
    .d = 3.14,
    .fp = &f
  };
  printf("%d\n", s1.fp(i));
#endif
  return 0;
}
