#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "mytypes.h"
#include "file.h"


FILE * foread(str path) {
	FILE *fh;
	
	fh = fopen(path, "r");
	if (fh == NULL) {
		fprintf(stderr, "Error: unable to open file '%s' for reading\n", path);
		exit(2);	
	}
	return fh;
}

FILE * fowrite(str path) {
	FILE *fh;
	
	fh = fopen(path, "w");
	if (fh == NULL) {
		fprintf(stderr, "Error: unable to open file '%s' for writing\n", path);
		exit(2);	
	}
	return fh;
}



