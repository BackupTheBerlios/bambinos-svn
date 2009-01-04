/*
priority.c
*/

#include <errno.h>
#include <sys/types.h>
#include <sys/resource.h>
#include <lib.h>
#include <unistd.h>
#include <string.h>
#include <stddef.h>


int sched_getscheduler(pid_t pid)
{
	int v;
	message m;

	/* m.m1_i1 = which; */
	m.m1_i2 = pid; /* who */

	return _syscall(MM, GETSCHEDULER, &m);

}

int sched_setscheduler(pid_t pid, int policy, int priority)
{
	message m;

	m.m1_i1 = policy;
	m.m1_i2 = pid;
	m.m1_i3 = priority;

	return _syscall(MM, SETSCHEDULER, &m);
	/* return _syscall(MM, SETPRIORITY, &m); */
}

