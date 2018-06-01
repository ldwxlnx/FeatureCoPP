/** in non-compound contexts EXPORT... will handled as
 * implicit declaration (ret-type int), argument now treated
 * as formal parameter (decl + 1)!
 * see gcc-4.7.2 output for "gcc -Wall -pedantic -S test.c"
test.c:9:1: warning: data definition has no type or storage class [enabled by default]
test.c:9:1: warning: type defaults to ‘int’ in declaration of ‘EXPORT_SYMBOL’ [-Wimplicit-int]
test.c:9:1: warning: parameter names (without types) in function declaration [enabled by default]
 *
 * */
#ifndef CONFIG_SMP
/* Initialize this to an actual value to force it into the .data
 * section so that we know it is properly initialized at entry into
 * the kernel but before bss is initialized to zero (which is where
 * it would live otherwise).  The 0x1f magic represents the IRQs we
 * cannot actually mask out in hardware.
 */
unsigned long bfin_irq_flags = 0x1f;
EXPORT_SYMBOL(bfin_irq_flags);
#endif
