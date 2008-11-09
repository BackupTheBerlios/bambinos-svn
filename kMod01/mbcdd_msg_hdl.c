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
static LIST_HEAD(msg_root);



/*
 * retrieve unique message identifier
 */
static int new_msg_id(void) {
	static int id = 0; //NOTE: static
	return id++;
}


static void test_list(void) {

	int i;
	message_t *msg;
	struct list_head *p;

	//INIT_LIST_HEAD(&messages);

	// build list
	for (i = 0; i < 3; i++) {
		msg = kmalloc(sizeof(message_t), GFP_KERNEL);

		spin_lock(&msg_lock);

		//sprintf(msg->data, "message %i", i);
		//printk(KERN_ALERT "adding: %s \n", msg->data);

		msg->id = new_msg_id();

		msg->data = i;
		printk(KERN_ALERT "adding: %i \n", msg->data);
		list_add(&msg->list, &msg_root);
		//printk(KERN_ALERT "adding: %i \n", i);


		spin_unlock(&msg_lock);
	}

	//travel list
	i=0;
	list_for_each(p, &msg_root) {
		msg = list_entry(p, struct message, list);
		printk(KERN_ALERT "list element %i: %i \n", i, msg->id);
		i++;
	}

}



/**
 * allocate space for a new message, add it to the list
 * of messages and return a pointer to the new message
 */
message_t *mbcdd_new_msg(void){

	message_t *msg;


	printk(KERN_ALERT "mbcdd_msg_hdl: new message\n");

	//allocate memory for the new message
	msg = kmalloc(sizeof(message_t), GFP_KERNEL);

	//add the message to the list using spinlock
	spin_lock(&msg_lock);

		msg->id = new_msg_id();
		list_add(&msg->list, &msg_root);

	spin_unlock(&msg_lock);

	return msg;

}


int mbcdd_put_msg(void){

	printk(KERN_ALERT "mbcdd_msg_hdl: put message \n");


	return 0;

}

int mbcdd_get_msg(void){

	printk(KERN_ALERT "mbcdd_msg_hdl: get message \n");
	return 0;

}

EXPORT_SYMBOL(mbcdd_new_msg);
EXPORT_SYMBOL(mbcdd_put_msg);
EXPORT_SYMBOL(mbcdd_get_msg);



void test_new_msg(void) {


}


static int __init  mbcd_init(void) {

	printk(KERN_ALERT "mbcdd_msg_hdl: Insert module \n");

	test_new_msg();

	return 0;

}



static void __exit  mbcd_exit(void) {

	struct list_head *loopvar, *tmp;
	int i = 0;

	printk(KERN_NOTICE "mbcdd_msg_hdl: Unregister module \n");

	//do some housekeeping and clean the list
	spin_lock(&msg_lock);

		list_for_each_safe(loopvar, tmp, &msg_root) {
			 i = list_entry(loopvar, message_t, list)->id;
			 printk(KERN_NOTICE "mbcdd_msg_hdl: Deleting message with ID %i\n", i);
			 //delete list element
			 list_del(loopvar);
			 //free willy
			 kfree(list_entry(loopvar, message_t, list));
		}

	spin_unlock(&msg_lock);

}

module_init(mbcd_init);
module_exit(mbcd_exit);

