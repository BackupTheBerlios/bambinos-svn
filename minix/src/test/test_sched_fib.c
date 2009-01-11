/* set different scheduling policies for a process */

#include <lib.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <time.h>
#include <sys/resource.h>
#include <sys/wait.h>


#define NR_OF_WORKERS 3

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
	static clock_t global_start_ticks;
	static int stats[NR_OF_WORKERS];

	if (argc > 1)
		n =  atoi(argv[1]); /*fibonacci(n)*/

	if (argc > 2)
		sched =  atoi(argv[2]);

	global_start_ticks = clock();

	pid = fork();

	if (pid == 0) {
		for (i = 0; i < NR_OF_WORKERS; i++) {
			pid_nested = fork();

			if (pid_nested == 0) {
				/* child calculates fibonacci */
				printf("w%d: calculating fib(%d) \n", i, n);

				start_ticks = clock();

				result = fib(n);

				elapsed = clock() - start_ticks;
				stats[i] = clock() - global_start_ticks;
				printf("w%d: fib(%d) = %d \n", i, n, result);
				printf("w%d: took %.2lf seconds = %d clockticks \n", i, (float)elapsed / (float)CLOCKS_PER_SEC, elapsed);

			}else {
				/* set the scheduling policy for the child */
				printf("w%d: setting scheduling policy to ", i);

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
					printf("w%d: set policy FAILED and returned: %d \n", i, rs);
			}
		}
	}
	wait(&status);
	for (i = 0; i < NR_OF_WORKERS; i++) {
		printf("w%d: %d \n", i, stats[i]);
	}

	return 0;
}
