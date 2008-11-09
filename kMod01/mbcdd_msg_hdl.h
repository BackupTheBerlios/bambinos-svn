#ifndef MBCDD_MSG_HDL_H_
#define MBCDD_MSG_HDL_H_

struct message {
	int id;
	//char data[256];
	int data;
	struct list_head list;
};

typedef struct message message_t;

struct message *mbcdd_new_msg(void);

int mbcdd_put_msg(void);

int mbcdd_get_msg(void);


#endif /*MBCDD_MSG_HDL_H_*/
