#include <lib.h>
#include <stdio.h>
#include <stdlib.h>


int fak (int val) {
	if (val > 2) {
	  return fak(val-1) * val;
	}else return val;
}


int main(int argc, char *argv[]) {

	int arg, i = 0;

	if (argc > 1) {
		arg = atoi(argv[1]);
		i = fak(arg);
		printf("%i! = %i\n", arg, i);
	}

	return 0;
}
