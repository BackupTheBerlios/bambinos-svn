/*
 * mbcd.h
 *
 *  Created on: Oct 30, 2008
 *      Author: rgratz
 */

#ifndef MBCD_H_
#define MBCD_H_



struct mbcd_dev *mbcd_devices;	/* allocated in mbcd_init_module */

/*
 * Representation of mbcd quantum sets.
 */
struct mbcd_qset {
	void **data;
	struct mbcd_qset *next;
};



struct mbcd_dev {
	struct mbcd_qset *data;  /* Pointer to first quantum set */
	int quantum;              /* the current quantum size */
	int qset;                 /* the current array size */
	unsigned long size;       /* amount of data stored here */
	unsigned int access_key;  /* used by mbcduid and mbcdpriv */
	struct semaphore sem;     /* mutual exclusion semaphore     */
	struct cdev cdev;	  /* Char device structure		*/
};



#endif /* MBCD_H_ */
