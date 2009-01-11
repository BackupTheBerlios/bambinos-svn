/* calculate faculty */

#include <stdlib.h>
#include <stdio.h>


int fib (long n) {
	if (n <= 2) return 1;
	else return fib(n-1) + fib(n-2);
}


int main(int argc, char *argv[]) {

	long i = 0;
	int arg = 0;

	if (argc > 1) {
		arg = atoi(argv[1]);
		i = fib((long)arg);
		printf("fib(%d) = %d\n", arg, i);
	}

	return 0;
}
