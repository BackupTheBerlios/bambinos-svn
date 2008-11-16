#include <linux/init.h>
#include <linux/module.h>
#include <linux/fs.h>
#include <linux/cdev.h>
#include <linux/list.h>


#include "mbcdd_msg_hdl.h"
#include "mbcdd.h"


MODULE_LICENSE("GPL");
MODULE_AUTHOR("R. Gratz, M. Kasinger");
MODULE_DESCRIPTION("A message handler device driver, for the mbcdd device");




static spinlock_t msg_lock = SPIN_LOCK_UNLOCKED;
static unsigned long msg_lock_flags;

static LIST_HEAD(msg_root);

//TODO: check *msg for null-values

//TODO: check kmalloc for null

/*
 * retrieve unique message identifier
 */
static int new_msg_id(void) {
	static int id = 0; //NOTE: static
	return id++;
}


/**
 * for testin'...
 */
static void mbcdd_print_msg_list(void) {

	message_t *msg;
	struct list_head *p, *q;

	printk(KERN_NOTICE "mbcdd_msg_hdl: list messages\n");

	//travel the message-list
	list_for_each(p, &msg_root) {
		msg = list_entry(p, message_t, list);
		printk(KERN_ALERT "  - message with ID %i \n", msg->id);

		//travel the data slots of each message
		list_for_each(q, &msg->slot_root) {
			printk(KERN_ALERT "    - message-data-slot with ID %i \n", list_entry(q, message_slot_t, list)->id);
		}

	}
}

/**
 * allocate space for a new message, add it to the list
 * of messages and return a pointer to the new message
 */
message_t *mbcdd_new_msg(void){

	message_t *msg;


	printk(KERN_NOTICE "mbcdd_msg_hdl: new message\n");

	//allocate memory for the new message
	msg = kmalloc(sizeof(message_t), GFP_KERNEL);

	//init slot_list
	INIT_LIST_HEAD(&msg->slot_root);
	msg->slot_current = &(msg->slot_root);
	msg->slot_lock = SPIN_LOCK_UNLOCKED;

	msg->busy_reader = 0;
	msg->fin_writer= 0;

	//add the message to the list using spinlock
	spin_lock_irqsave(&msg_lock, msg_lock_flags);

		msg->id = new_msg_id();
		list_add(&msg->list, &msg_root);
		printk(KERN_NOTICE "added message with ID %i \n", msg->id);

	spin_unlock_irqrestore(&msg_lock, msg_lock_flags);

	return msg;

}

/**
 * get a pointer to the latest unread message
 */
message_t *mbcdd_get_msg(void){
	message_t *msg = NULL;

	printk(KERN_NOTICE "mbcdd_msg_hdl: get message\n");

	spin_lock_irqsave(&msg_lock, msg_lock_flags);

		if (list_empty(&msg_root)) {

			printk(KERN_NOTICE "no more message :-( \n");

		}else {

			msg = list_entry(msg_root.next, message_t, list);
			printk(KERN_NOTICE "got message with ID %i \n", msg->id);
			list_del(&msg->list);

		}

	spin_unlock_irqrestore(&msg_lock, msg_lock_flags);

	return msg;
}


/**
 * remove all data slots of a message and the message itself
 */
void mbcdd_del_msg(message_t *msg) {

	struct list_head *loop, *tmp;
	message_slot_t *slot;

	printk(KERN_NOTICE "mbcdd_msg_hdl: delete message \n");


	//lock the slots
	spin_lock_irqsave(&msg->slot_lock, msg->slot_lock_flags);

		list_for_each_safe(loop, tmp, &msg->slot_root) {
			slot = list_entry(loop, message_slot_t, list);

			printk(KERN_NOTICE "deleting data slot with ID %i from message with ID %i\n", slot->id, msg->id);
			list_del(loop);
			//free willy
			kfree(slot);
		}

	spin_unlock_irqrestore(&msg->slot_lock, msg->slot_lock_flags);

	//lock the messages
	spin_lock_irqsave(&msg_lock, msg_lock_flags);

		printk(KERN_NOTICE "deleting message with ID %i\n", msg->id);

		//check if message is still in the list
		list_for_each_safe(loop, tmp, &msg_root) {
			if (list_entry(loop, message_t, list) == msg) {
				//remove the message from the list
				list_del(&msg->list);
				break;
			}
		}

		kfree(msg);

	spin_unlock_irqrestore(&msg_lock, msg_lock_flags);

	return;
}


/**
 * allocate memory for new data and return the allocated size
 */
int mbcdd_new_data(void *p){

	p = kmalloc(DATA_SLOT_SIZE, GFP_KERNEL);
	memset(p, 0, DATA_SLOT_SIZE);

	return DATA_SLOT_SIZE;
}


/**
 * create a new message slot and add the given data
 */
void mbcdd_add_data_slot(message_t *msg, void *data){

	message_slot_t *slot;
	static int msg_data_slot_id = 0;


	slot = kmalloc(sizeof(message_slot_t), GFP_KERNEL);
	slot->id = msg_data_slot_id;
	slot->data = data;

	//add the message to the list using spinlock
	spin_lock_irqsave(&msg->slot_lock, msg->slot_lock_flags);

		list_add_tail(&slot->list, &msg->slot_root);
		printk(KERN_NOTICE "added slot with ID %i to message with ID %i \n", slot->id, msg->id);

	spin_unlock_irqrestore(&msg->slot_lock, msg->slot_lock_flags);


	msg_data_slot_id ++;

	return;
}




/**
 * get a pointer to the data of the next unread data slot of a message.
 *
 */
void *mbcdd_get_data_slot(message_t *msg) {
	void *data;
	message_slot_t *slot = NULL;

	printk(KERN_NOTICE "mbcdd_msg_hdl: get message data slot\n");

	spin_lock_irqsave(&msg->slot_lock, msg->slot_lock_flags);

		if (msg->slot_current->next != &msg->slot_root) {

			slot = list_entry(msg->slot_current->next, message_slot_t, list);
			data = slot->data;
			printk(KERN_NOTICE "got data slot with ID %i of message with ID %i \n", slot->id, msg->id);

			//hop to the next slot
			msg->slot_current = msg->slot_current->next;
		}else {
			data = NULL;
			printk(KERN_NOTICE "no more data slot in message with ID %i \n", msg->id);

		}

	spin_unlock_irqrestore(&msg->slot_lock, msg->slot_lock_flags);

	return data;
}



EXPORT_SYMBOL(mbcdd_new_msg);
EXPORT_SYMBOL(mbcdd_get_msg);
EXPORT_SYMBOL(mbcdd_del_msg);
EXPORT_SYMBOL(mbcdd_new_data_slot);
EXPORT_SYMBOL(mbcdd_new_data);
EXPORT_SYMBOL(mbcdd_add_data_slot);
EXPORT_SYMBOL(mbcdd_get_data_slot);



void test_msg(void) {

	message_t *msg;
	char *p = NULL;
	void *q = NULL;



	msg = mbcdd_new_msg();

	mbcdd_new_msg();
	mbcdd_new_msg();

//	p = mbcdd_new_data_slot(msg);
//	p = mbcdd_new_data_slot(msg);
	//p = mbcdd_new_data_slot(msg);
////	*p = 'a';

	mbcdd_new_data(q);
	mbcdd_add_data_slot(msg, q);

	mbcdd_get_data_slot(msg);
	mbcdd_get_data_slot(msg);
//	mbcdd_get_data_slot(msg);
//


	mbcdd_print_msg_list();
//
//	msg = mbcdd_get_msg();
//
//	mbcdd_del_msg(msg);
//
//	mbcdd_get_msg();
//	mbcdd_get_msg();
//	mbcdd_get_msg();

}



static int __init  mbcdd_msg_init(void) {

	printk(KERN_ALERT "mbcdd_msg_hdl: insert module \n");

	test_msg();


	return 0;

}



static void __exit  mbcdd_msg_exit(void) {

	struct list_head *loopvar_outer, *tmp_outer, *loopvar_inner, *tmp_inner;
	message_t *msg;
	int i = 0, j = 0;

	printk(KERN_NOTICE "mbcdd_msg_hdl: unregister module \n");

	//do some housekeeping and clean the list
	spin_lock_irqsave(&msg_lock, msg_lock_flags);

		list_for_each_safe(loopvar_outer, tmp_outer, &msg_root) {
			msg = list_entry(loopvar_outer, message_t, list);
			i = msg->id;

			list_for_each_safe(loopvar_inner, tmp_inner, &msg->slot_root) {
				j = list_entry(loopvar_inner, message_slot_t, list)->id;

				printk(KERN_NOTICE "deleting data slot with ID %i from message with ID %i\n", j, i);
				list_del(loopvar_inner);
				//free willy
				kfree(list_entry(loopvar_inner, message_slot_t, list));
			}

			printk(KERN_NOTICE "deleting message with ID %i\n", i);
			//delete list element
			list_del(loopvar_outer);
			kfree(list_entry(loopvar_outer, message_t, list));
		}

	spin_unlock_irqrestore(&msg_lock, msg_lock_flags);

}

module_init(mbcdd_msg_init);
module_exit(mbcdd_msg_exit);

