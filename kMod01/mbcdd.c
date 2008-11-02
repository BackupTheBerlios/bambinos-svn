#include <linux/init.h>
#include <linux/module.h>
#include <linux/fs.h>
#include <linux/cdev.h>

#include "mbcdd.h"

MODULE_LICENSE("GPL");
MODULE_AUTHOR("R. Gratz, M. Kasinger");
MODULE_DESCRIPTION("A message buffering char device driver, mainly implemented for educational purposes.");

static dev_t gbl_device_nr;
static int gbl_device_count = 1;
static struct mbcdd_dev gbl_mbcdd_dev;
//static struct mbcdd_fops gbl_mbcdd_flops;


int mbcdd_open(struct inode *inode, struct file *filep) {

	struct mbcdd_dev *dev;
	dev = container_of(inode->i_cdev, struct mbcdd_dev, cdev);

	// TODO easy access for the future ???
	filep->private_data = dev;


}

int mbcdd_release() {

}

int mbcdd_write() {

}

int mbcdd_read() {

}

static struct file_operations gbl_mbcdd_fops = {
		.open = mbcdd_open,
		//		.release = mbcdd_release,
		//		.write = mbcdd_write,
		//		.read = mbcdd_read,

		};

static int __init  mbcd_init(void) {
	int result_get_major;

	/* get major device number */
	result_get_major = alloc_chrdev_region(&gbl_device_nr, 0, gbl_device_count,
			"mbcdd");

	printk(KERN_ALERT "mbcdd: Insert module \n");

	// setup device
	mbcdd_setup_cdev();

	return 0;

}

void mbcdd_setup_cdev() {

	int err;

	// initalize device to kernel
	cdev_init(&gbl_mbcdd_dev.cdev, &gbl_mbcdd_fops);
	gbl_mbcdd_dev.cdev.owner = THIS_MODULE;
	gbl_mbcdd_dev.cdev.ops = &gbl_mbcdd_fops;

	// add device to kernel
	err = cdev_add(&(gbl_mbcdd_dev.cdev),MINOR_FIRST,MINOR_COUNT);
	if (err) {
		printk(KERN_NOTICE "mbcdd: Error adding device \n");
	}

}

static void __exit  mbcd_exit(void) {
	unregister_chrdev_region(gbl_device_nr, gbl_device_count);
}

module_init(mbcd_init);
module_exit(mbcd_exit);

