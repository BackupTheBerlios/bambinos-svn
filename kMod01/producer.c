#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <string.h>


int main(int argc, char *argv[]) {

	FILE *fh;

	char msg[20];

	struct timespec tm, tm2;
	int nr_msg, sleep_ms;
	int i, count;

	memset(&msg,' ',sizeof(msg));
	sprintf(msg, "write messages ");
	nr_msg = 10;
	sleep_ms = 100;

	if (argc>1) {
		nr_msg = atoi(argv[1]);
	}else {
		printf("NOTE: number of messages not specified, using %i as default.\n", nr_msg);
	}

	if (argc>2) {
		sleep_ms = atoi(argv[2]);
	}else {
		printf("NOTE: no period specified, using %i ms as default.\n", sleep_ms);
	}

   	tm.tv_sec = 0;
   	tm.tv_nsec = 1000000 * sleep_ms;

	fh = fowrite("/dev/mbcdd");
	i = 0;

	while(i < nr_msg) {

		printf("write #%i: %s\n", i, msg);
		//fwrite(m, 1, sizeof(m), fh);
		fwrite(msg, 1, sizeof(msg), fh);

		nanosleep(&tm, &tm2);
		i++;
	}

	fclose(fh);

	return 0;
}

