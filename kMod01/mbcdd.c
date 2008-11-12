#include <linux/proc_fs.h>
#include <linux/init.h>
#include <linux/module.h>
#include <linux/fs.h>
#include <linux/cdev.h>
#include <asm/uaccess.h>
#include "mbcd.h"

#include <linux/proc_fs.h>
#include <linux/seq_file.h>

int mbcd_major = 0;
int mbcd_minor = 0;
int mbcd_nr_devs = 1;


MODULE_LICENSE("GPL");
MODULE_AUTHOR("R. Gratz, M. Kasinger");
MODULE_DESCRIPTION("A message buffering char device driver.");


int mbcd_open(struct inode *inode, struct file *filp) {
	struct mbcd_dev *dev; /* device information */



	return 0; /* success */
}

int mbcd_release(struct inode *inode, struct file *filp) {
	return 0;
}



ssize_t mbcd_read(struct file *filp, char __user *buf, size_t count,
		loff_t *f_pos) {



	printk(KERN_NOTICE "mbcdd: Reading \n");

	//mbcdd_get_msg();

	return count;
}

ssize_t mbcd_write(struct file *filp, const char __user *buf, size_t count,
		loff_t *f_pos) {

	struct mbcdd_dev_wrapper *dev_wrapper = filp->private_data;

			//TODO wait for Kasi
	//		void *to=mbcdd_new_data_slot(dev_wrapper->msg);
	//		count=DATA_SLOT_SIZE;
	//
	//		spin_lock_irqsave(&mr_lock, flags);
	//		// critical region
	//
	//
	//		copy_from_user(to, buf, count);
	//
	//
	//		spin_unlock_irqrestore(&mr_lock, flags);


			printk(KERN_NOTICE "mbcdd: Writing \n");

			return count;
}

struct file_operations mbcd_fops = { .owner = THIS_MODULE, .read = mbcd_read,
		.write = mbcd_write, .open = mbcd_open, .release = mbcd_release, };

void mbcd_exit(void) {

	dev_t devno = MKDEV(mbcd_major, mbcd_minor);
	kfree(mbcd_devices);
	unregister_chrdev_region(devno, mbcd_nr_devs);
	printk(KERN_NOTICE "mbcd: Device de-registered!");

}

/*
 * Set up the char_dev structure for this device.
 */
static void mbcd_setup_cdev(struct mbcd_dev *dev) {
	int err, devno = MKDEV(mbcd_major, mbcd_minor);

	cdev_init(&dev->cdev, &mbcd_fops);
	dev->cdev.owner = THIS_MODULE;
	dev->cdev.ops = &mbcd_fops;
	err = cdev_add(&dev->cdev, devno, 1);


	if(err)
		printk(KERN_ALERT "Error %d adding mbcd", err);
}

int __init  mbcd_init(void) {

	int result;
	dev_t dev = 0;

	// nur dynamisch
	result = alloc_chrdev_region(&dev, mbcd_minor, mbcd_nr_devs, "mbcd");
	mbcd_major = MAJOR(dev);

	mbcd_devices = kmalloc(sizeof(struct mbcd_dev), GFP_KERNEL);
	memset(mbcd_devices, 0, sizeof(struct mbcd_dev));

	mbcd_setup_cdev(mbcd_devices);

	dev = MKDEV(mbcd_major, mbcd_minor);

	printk(KERN_ALERT "mbcd: Device registered, major  %d", mbcd_major);

	return 0;

}

module_init(mbcd_init);
module_exit(mbcd_exit);
