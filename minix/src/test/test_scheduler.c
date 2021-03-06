/* set different scheduling policies for a process */

#include <lib.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/resource.h>

int main(int argc, char *argv[]) {

	int rs, rg;
	int pid = 0;
	int sched = SCHED_RR;

	if (argc > 1)
		pid =  atoi(argv[1]);

	if (argc > 2)
		sched =  atoi(argv[2]);


	rs = sched_setscheduler((pid_t) pid, sched, 0);

	rg = sched_getscheduler(pid);
	printf("r set:%d get:%d \n", rs, rg);


	return 0;
}
