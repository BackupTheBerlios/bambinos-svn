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
	// i_cdev enthaelt die cdev struktur die wir erstellt haben; Kernel gibt das in inode an
	// unser Device weiter
	//

	//
	mbcdd_put_msg();

	filep->private_data = dev;
//	  if ( (filep->f_flags & O_ACCMODE) = = O_WRONLY) {
//	      scull_trim(dev); /* ignore errors */
//	  }
//	  return 0;          /* success */
//	}



}

int mbcdd_release(struct inode *inode, struct file *filp) {


		return 0;

}

ssize_t mbcdd_write(struct file *filp, const char __user *buf, size_t count,
		loff_t *f_pos) {

		mbcdd_put_msg();

		return 0;

}

ssize_t mbcdd_read(struct file *filp, char __user *buf, size_t count,
		loff_t *f_pos) {

		mbcdd_get_msg();

		return 0;

}

static struct file_operations gbl_mbcdd_fops = {
		.open = mbcdd_open,
		.release = mbcdd_release,
		.write = mbcdd_write,
		.read = mbcdd_read,

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

	return 0;

}

static void __exit  mbcd_exit(void) {
	unregister_chrdev_region(gbl_device_nr, gbl_device_count);
	printk(KERN_NOTICE "mbcdd: Unregister module \n");
}

module_init(mbcd_init);
module_exit(mbcd_exit);

