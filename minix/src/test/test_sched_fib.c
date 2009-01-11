/* set different scheduling policies for a process */

#include <lib.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <time.h>
#include <sys/resource.h>
#include <sys/wait.h>


int fib (long n) {
	if (n <= 2) return 1;
	else return fib(n-1) + fib(n-2);
}


int main(int argc, char *argv[]) {

	int n = 42;
	long result = 0;
	int pid, pid_nested = 0;
	int sched = SCHED_RR;
	int rs, status, i;
	clock_t start_ticks, elapsed;

	if (argc > 1)
		n =  atoi(argv[1]); /*fibonacci(n)*/

	if (argc > 2)
		sched =  atoi(argv[2]);

	pid = fork();

	if (pid == 0) {
		for (i = 0; i < 3; i++) {
			pid_nested = fork();

			if (pid_nested == 0) {
				/* child calculates fibonacci */
				printf("calculating fib(%d) \n", n);

				start_ticks = clock();

				result = fib(n);

				elapsed = clock() - start_ticks;

				printf("fib(%d) = %d \n", n, result);
				printf("took %.2lf seconds = %d clockticks \n", (float)elapsed / (float)CLOCKS_PER_SEC, elapsed);

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
					printf("set policy FAILED and returned: %d \n", rs);
			}
		}
	}
	wait(&status);

	return 0;
}
