/*********************************
 * Test behavior of undisciplined
 * annotations.
 *********************************/

#ifdef HAVE_A
void p(int i) {
#endif

int tmp = i + 1;

#ifdef HAVE_A
}
void q(int i) {
  i = 2;
} 
#endif

// return statement
int f(int x) {
#ifdef HAVE_A
if(1) {
  return x + 1;
}
#endif
  return x - 1; 
}
// switch-case
void r(int x) {
	switch(x) {
		case 1:
			return x + 1;
#ifdef HAVE_A			
		case 2:
			return x + 2;
#endif			
		default:
			return x + 3;
	}
}
#ifdef HAVE_A
int s() {
	return 1;
}
#endif
void t(int x
#ifdef HAVE_A
, int y, int z
#endif
) {
	
}