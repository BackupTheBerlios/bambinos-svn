#include <stdio.h>
#include <stdlib.h>
#include <time.h>

int main(int argc, char *argv[]) {
	
	FILE *fh;
	char msg[256];
	struct timespec tm, tm2;	
	int sleep_ms;
	int i, count;
	
	sleep_ms = 100;
	if (argc>1) {
		sleep_ms = atoi(argv[1]);
	
	}else {
		printf("no period specified, using %d ms as default\n", sleep_ms); 
	}		
	
   	tm.tv_sec = 0;
   	tm.tv_nsec = 1000000 * sleep_ms;

	fh = foread("/dev/mbcd");
	i = 0;
	
	while(1) {			
		
		fread(msg, 1, sizeof(msg), fh); 	
		printf("read #%i: %s\n", i, msg);
		
		nanosleep(&tm, &tm2);
		i++;
	}
	
	fclose(fh);

	return 0;
}
