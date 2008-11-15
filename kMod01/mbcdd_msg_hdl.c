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
	int i;
	message_t *msg;
	struct list_head *p;

	printk(KERN_NOTICE "mbcdd_msg_hdl: list messages\n");

	//travel list
	i=0;
	list_for_each(p, &msg_root) {
		msg = list_entry(p, struct message, list);
		printk(KERN_ALERT " - message with ID %i \n", msg->id);
		i++;
	}
}


static void test_list(void) {

	int i;
	message_t *msg;
	struct list_head *p;

	//INIT_LIST_HEAD(&messages);

	// build list
	for (i = 0; i < 3; i++) {
		msg = kmalloc(sizeof(message_t), GFP_KERNEL);

		spin_lock_irqsave(&msg_lock, msg_lock_flags);


		//sprintf(msg->data, "message %i", i);
		//printk(KERN_ALERT "adding: %s \n", msg->data);

		msg->id = new_msg_id();
		list_add(&msg->list, &msg_root);
		//printk(KERN_ALERT "adding: %i \n", i);


		spin_unlock_irqrestore(&msg_lock, msg_lock_flags);
	}

	//travel list
	i=0;
	list_for_each(p, &msg_root) {
		msg = list_entry(p, struct message, list);
		printk(KERN_NOTICE "list element %i: %i \n", i, msg->id);
		i++;
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


	//add the message to the list using spinlock
	spin_lock_irqsave(&msg_lock, msg_lock_flags);

		msg->id = new_msg_id();
		list_add(&msg->list, &msg_root);
		printk(KERN_NOTICE "added message with ID %i \n", msg->id);

	spin_unlock_irqrestore(&msg_lock, msg_lock_flags);

	return msg;

}

/**
 * get a pointer to the latest message
 */
message_t *mbcdd_get_msg(void){
	message_t *msg;

	struct list_head *p, *n;

	printk(KERN_NOTICE "mbcdd_msg_hdl: get message\n");

	list_for_each_safe(p, n, &msg_root) { //simple but a bit dirty
		msg = list_entry(p, struct message, list);
		break;
	}

	printk(KERN_NOTICE "got message with ID %i \n", msg->id);

	return msg;
}


/**
 * allocate memory for the data of a message_slot
 */
//static void init_message_slot(message_slot_t *slot) {
//	//slot->data = kmalloc(sizeof(char[DATA_SLOT_SIZE]), GFP_KERNEL);
//	memset(slot->data, 0, DATA_SLOT_SIZE);
//	return;
//}

/**
 * create a new message slot and get a pointer to the message slot-data
 */
void *mbcdd_new_data_slot(message_t *msg) {

	static int msg_data_slot_id = 0;

	printk(KERN_NOTICE "mbcdd_msg_hdl: new message data slot\n");

	//allocate memory for the new message data slot
	msg->slot = kmalloc(sizeof(message_slot_t), GFP_KERNEL);
	msg->slot->id = msg_data_slot_id;
	memset(msg->slot->data, 0, DATA_SLOT_SIZE);


	//add the message to the list using spinlock
	spin_lock_irqsave(&msg_lock, msg_lock_flags);

		list_add(&msg->slot->list, &msg->slot_root);
		printk(KERN_NOTICE "added slot with ID %i to message with ID %i \n", msg->slot->id, msg->id);

	spin_unlock_irqrestore(&msg_lock, msg_lock_flags);

	msg_data_slot_id ++;

	return &msg->slot->data;
}

void *mbcdd_get_data_slot(message_t *msg) {
	return;
}

/*
int mbcdd_put_msg(void){

	printk(KERN_ALERT "mbcdd_msg_hdl: put message \n");


	return 0;

}

int mbcdd_get_msg(void){

	printk(KERN_ALERT "mbcdd_msg_hdl: get message \n");
	return 0;

}
*/

EXPORT_SYMBOL(mbcdd_new_msg);
EXPORT_SYMBOL(mbcdd_get_msg);
EXPORT_SYMBOL(mbcdd_new_data_slot);
EXPORT_SYMBOL(mbcdd_get_data_slot);



void test_msg(void) {

	message_t *msg;
	char *p;


	msg = mbcdd_new_msg();
	//mbcdd_new_msg();

	p = mbcdd_new_data_slot(msg);
	*p = 'a';

	mbcdd_print_msg_list();

	mbcdd_get_msg();

}



static int __init  mbcd_init(void) {

	printk(KERN_ALERT "mbcdd_msg_hdl: Insert module \n");

	test_msg();
	//test_list();

	return 0;

}



static void __exit  mbcd_exit(void) {

	struct list_head *loopvar_outer, *tmp_outer, *loopvar_inner, *tmp_inner;
	message_t *msg;
	int i = 0, j = 0;

	printk(KERN_NOTICE "mbcdd_msg_hdl: Unregister module \n");

	//do some housekeeping and clean the list
	spin_lock_irqsave(&msg_lock, msg_lock_flags);

		list_for_each_safe(loopvar_outer, tmp_outer, &msg_root) {
			msg = list_entry(loopvar_outer, message_t, list);
			i = msg->id;

			//TODO: remove message data slots
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

module_init(mbcd_init);
module_exit(mbcd_exit);

