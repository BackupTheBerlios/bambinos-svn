#include <linux/init.h>
#include <linux/module.h>
#include <linux/fs.h>
#include <linux/cdev.h>

#include "mbcdd.h"

MODULE_LICENSE("GPL");
MODULE_AUTHOR("R. Gratz, M. Kasinger");
MODULE_DESCRIPTION("A message handler device driver, for the mbcdd device");


static int __init  mbcd_init(void) {

	printk(KERN_ALERT "mbcdd_msg_hdl: Insert module \n");



	return 0;

}


int mbcdd_put_msg(){

	printk(KERN_ALERT "mbcdd_msg_hdl: put message \n");

}

int mbcdd_get_msg(){

	printk(KERN_ALERT "mbcdd_msg_hdl: suck your dick \n");

}

EXPORT_SYMBOL(mbcdd_put_msg);
EXPORT_SYMBOL(mbcdd_get_msg);

static void __exit  mbcd_exit(void) {

	printk(KERN_NOTICE "mbcdd_msg_hdl: Unregister module \n");
}

module_init(mbcd_init);
module_exit(mbcd_exit);

