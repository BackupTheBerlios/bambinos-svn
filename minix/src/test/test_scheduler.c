#include <lib.h>
#include <stdio.h>
#include "/usr/src/include/sys/resource.h"

int mycall(int a,int b,int c){

	message m;
	m.m1_i1=a;
	m.m1_i2=b;
	m.m1_i3=c;

	return(_syscall(MM,57,&m));

}

void main(int argc, char argv[]){

	int r;
	pid_t pid;
	r=sched_getscheduler(pid);
	printf("r %d", r);
}
