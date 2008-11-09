#ifndef MBCDD_MSG_HDL_H_
#define MBCDD_MSG_HDL_H_

struct message {
	//char data[256];
	int data;
	struct list_head list;
};

struct message *mbcdd_new_msg(void);

int mbcdd_put_msg(void);

int mbcdd_get_msg(void);


#endif /*MBCDD_MSG_HDL_H_*/
