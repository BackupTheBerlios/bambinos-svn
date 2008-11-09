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


static spinlock_t s1;


//static LIST_HEAD(messages);
static struct list_head messages;


static void test_list(void) {

	int i;
	struct message *msg;
	struct list_head *p;

	// build list
	for (i = 0; i < 3; i++) {
		//msg = kmalloc(sizeof(struct message), GFP_KERNEL);

		//spin_lock(&s1);

		//sprintf(msg->data, "message %i", i);
		//printk(KERN_ALERT "adding: %s \n", msg->data);
		//msg->data = i;
		//printk(KERN_ALERT "adding: %i \n", msg->data);
		//list_add(&msg->list, &messages);
		printk(KERN_ALERT "adding: %i \n", i);


		//spin_unlock(&s1);
	}

	//travel list
//	i=0;
//	list_for_each(p, &messages) {
//		msg = list_entry(p, struct message, list);
//		printk(KERN_ALERT "list element %i: %s \n", i, msg->data);
//		i++;
//	}

}



static int __init  mbcd_init(void) {

	printk(KERN_ALERT "mbcdd_msg_hdl: Insert module \n");

	test_list();

	return 0;

}


struct message *mbcdd_new_msg(void){

	printk(KERN_ALERT "mbcdd_msg_hdl: new message\n");

	return kmalloc(sizeof(struct message), GFP_KERNEL);

}


int mbcdd_put_msg(void){

	printk(KERN_ALERT "mbcdd_msg_hdl: put message \n");
	return 0;

}

int mbcdd_get_msg(void){

	printk(KERN_ALERT "mbcdd_msg_hdl: suck your dick \n");
	return 0;

}

EXPORT_SYMBOL(mbcdd_new_msg);
EXPORT_SYMBOL(mbcdd_put_msg);
EXPORT_SYMBOL(mbcdd_get_msg);

static void __exit  mbcd_exit(void) {

	printk(KERN_NOTICE "mbcdd_msg_hdl: Unregister module \n");
}

module_init(mbcd_init);
module_exit(mbcd_exit);

