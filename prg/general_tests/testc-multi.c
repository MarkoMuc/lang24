#include <stdio.h>
#include <stdlib.h>


int main(){
	int tosave = 1;
	int idx = 0;
	int size = 9;
	int accum = 0;
	int array = 4;
	int cmps = 0;
	int **i = malloc(array * sizeof(int*));

	cmps = 0;
	while (cmps < array) {
	    idx = 0;
	    i[cmps] = malloc(size * sizeof(int));
	    while(idx < size){
		    i[cmps][idx] = tosave;
		    idx = idx + 1;
	    }
	    tosave = tosave * 10;
	    cmps = cmps + 1;
	}

	cmps = 0;
	while (cmps < array) {
	    idx = 0;
	    while(idx < size){
		    accum = accum + i[cmps][idx];
		    idx = idx + 1;
	    }
	    cmps = cmps + 1;
	}

	printf("%d\n", accum);

	exit(0);
}
