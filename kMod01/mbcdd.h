/*
 * mbcdd.h
 *
 *  Created on: Nov 2, 2008
 *      Author: rgratz
 */

#ifndef MBCDD_H_
#define MBCDD_H_



#define MINOR_FIRST 0
#define MINOR_COUNT 0


void mbcdd_setup_cdev();


extern int mbcdd_put_msg();
extern int mbcdd_get_msg();



struct slot{

	void **data;
	struct slot *next;
};



typedef struct mbcdd_dev{

	struct slot *first_slot;
	int slot_size;
	struct cdev cdev;

};


/* Illustration of mbcdd_dev_wrapper:
 *
 * When user process x calls open, a new struct mbcdd_dev_wrapper will be
 * allocated.
 * Afterwards a new message will be requested from our message handler device.
 * The message will be stored in this struct and the struct will be held in the process'
 * filepointer. (filep->private_data)
 * When process x calls the read function, than we know the process' according message.
 */

struct mbcdd_dev_wrapper {
	struct mbcdd_dev *dev;
	struct message *msg;
};






#endif /* MBCDD_H_ */
