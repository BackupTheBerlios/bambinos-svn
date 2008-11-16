#ifndef MBCDD_MSG_HDL_H_
#define MBCDD_MSG_HDL_H_


#define DATA_SLOT_SIZE 20
#define FLAG_BUSY_READER 1
#define FLAG_FINISHED_WRITER 1


struct message_slot {
	int id;
	//char data[DATA_SLOT_SIZE];
	void *data;
	struct list_head list;
};

typedef struct message_slot message_slot_t;


struct message {
	int id;
	message_slot_t *slot;
	spinlock_t slot_lock;
	unsigned long slot_lock_flags;


	struct list_head slot_root;
	struct list_head *slot_current;

	struct list_head list;

	int busy_reader;
	int fin_writer;

};

typedef struct message message_t;



message_t *mbcdd_new_msg(void);

message_t *mbcdd_get_msg(void);

void mbcdd_del_msg(message_t *msg);

void *mbcdd_new_data(int *result);

int mbcdd_add_data_slot(message_t *msg, void *data);

void *mbcdd_get_data_slot(message_t *msg);

#endif /*MBCDD_MSG_HDL_H_*/
