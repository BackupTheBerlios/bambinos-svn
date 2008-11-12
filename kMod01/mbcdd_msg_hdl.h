#ifndef MBCDD_MSG_HDL_H_
#define MBCDD_MSG_HDL_H_

struct message {
	int id;
	//char data[256];
	int data;
	struct list_head list;
};

#define DATA_BLOCK_SIZE 20;

typedef struct message message_t;


message_t *mbcdd_new_msg(void);

message_t *mbcdd_get_msg(void);


// TODO return value ... pointer to a new slot
void *mbcdd_new_data_block(message_t *msg);


//int mbcdd_put_msg(void);

//int mbcdd_get_msg(void);


#endif /*MBCDD_MSG_HDL_H_*/
