#ifndef _SYS_RESOURCE_H
#define _SYS_RESOURCE_H

/* Priority range for the get/setpriority() interface.
 * It isn't a mapping on the internal minix scheduling
 * priority.
 */
#define PRIO_MIN	-20
#define PRIO_MAX	 20

/* Magic, invalid priority to stop the process. */
#define PRIO_STOP	 76

#define PRIO_PROCESS	0
#define PRIO_PGRP	1
#define PRIO_USER	2

/* Scheduling policies */
/* ATTENTION: these have to be in one order, have to start with 0, be increased by 1
 * The order defines the order of the scheduling policies for processes on the same prio-level
 * */
#define SCHED_FIFO 0
#define SCHED_RR 1
#define SCHED_OTHER 2

int getpriority(int, int);
int setpriority(int, int, int);

int sched_getscheduler(pid_t);
int sched_setscheduler(pid_t, int, int);


#endif
