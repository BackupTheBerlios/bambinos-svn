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

// TODO return value ... pointer to a new slot
void * mbcdd_new_data_slot(message_t *msg);

#define size_t DATA_SLOT_SIZE 20;


int mbcdd_put_msg(void);

int mbcdd_get_msg(void);


#endif /*MBCDD_MSG_HDL_H_*/
