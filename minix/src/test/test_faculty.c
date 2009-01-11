/* calculate faculty */

#include <stdlib.h>
#include <stdio.h>


int fak (double val) {
	if (val > 2) {
	  return fak(val-1) * val;
	}else return val;
}


int main(int argc, char *argv[]) {

	double i = 0;
	int arg = 0;

	if (argc > 1) {
		arg = atoi(argv[1]);
		i = fak((double)arg);
		printf("%e! = %e\n", arg, i);
	}

	return 0;
}
