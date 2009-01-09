#include <lib.h>
#include <stdio.h>
#include <stdlib.h>
#include "/usr/src/include/sys/resource.h"
/*
int mycall(int a, int b, int c) {

	message m;
	m.m1_i1 = a;
	m.m1_i2 = b;
	m.m1_i3 = c;

	return (_syscall(MM,57, &m));

}
*/

void main(int argc, char argv[]) {

	int rs, rg;
	pid_t pid = 0;
	int sched = SCHED_RR;

	if (argc > 1)
		pid =  atoi(argv[1]);

	if (argc > 2)
		sched =  atoi(argv[2]);


	rs = sched_setscheduler(pid,sched,0);

	rg = sched_getscheduler(pid);
	printf("r set:%d get:%d \n", rs, rg);
}
