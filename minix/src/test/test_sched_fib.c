/* set different scheduling policies for a process */

#include <lib.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/resource.h>


int fib (long n) {
	if (n <= 2) return 1;
	else return fib(n-1) + fib(n-2);
}


int main(int argc, char *argv[]) {

	int n = 42;
	long result = 0;
	int pid = 0;
	int sched = SCHED_RR;
	int rs, rg;

	if (argc > 1)
		n =  atoi(argv[1]); /*fibonacci(n)*/

	if (argc > 2)
		sched =  atoi(argv[2]);

	pid = fork();

	if (pid == 0) {
		/* child calculates fibonacci */
		printf("calculating fib(%d) \n", n);
		result = fib(n);
		printf("fib(%d) = %d \n", n, result);

	}else {
		/* set the scheduling policy for the child */
		printf("setting scheduling policy to ");

		switch(sched) {
		case SCHED_FIFO: printf("FIFO \n"); break;
		case SCHED_RR: printf("RR \n"); break;
		case SCHED_OTHER: printf("OTHER \n"); break;
		default:
			printf("wrong policy argument - using SCHED_OTHER. \n");
			sched = SCHED_OTHER;
		}

		rs = sched_setscheduler(pid, sched, 0);

		if (rs != sched)
			printf("set policy FAILED and returned: %d", rs);
	}

	return 0;
}
