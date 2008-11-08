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







struct mbcdd_dev{

	int slots;
	int slot_size;

	struct cdev cdev;

};






#endif /* MBCDD_H_ */
