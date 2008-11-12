#include <linux/init.h>
#include <linux/module.h>
#include <linux/fs.h>
#include <linux/cdev.h>
#include <linux/uaccess.h>

#include "mbcdd.h"
#include "mbcdd_msg_hdl.h"


MODULE_LICENSE("GPL");
MODULE_AUTHOR("R. Gratz, M. Kasinger");
MODULE_DESCRIPTION("A message buffering char device driver, mainly implemented for educational purposes.");

static int gbl_device_count = 1;
spinlock_t mr_lock = SPIN_LOCK_UNLOCKED;
unsigned long flags;
int mbcdd_major = 0; // TODO check if necas.
struct mbcdd_dev gbl_mbcdd_dev;

//static struct mbcdd_fops gbl_mbcdd_flops;


int mbcdd_open(struct inode *inode, struct file *filep) {

	struct mbcdd_dev *dev;
	struct mbcdd_dev_wrapper dev_wrapper;

	dev = container_of(inode->i_cdev, mbcdd_dev_t, cdev);
	// i_cdev enthaelt die cdev struktur die wir erstellt haben; Kernel gibt das in inode an
	// unser Device weiter

	dev_wrapper.dev=dev;
	filep->private_data = &dev_wrapper;
	if ( (filep->f_flags & O_ACCMODE) == O_WRONLY) {
		// fopen for write
		dev_wrapper.msg=mbcdd_new_msg();
		printk(KERN_NOTICE "mbcd call message handler open file function \n");

	} else {

		//TODO read

	}

	return 0;

}

int mbcdd_release(struct inode *inode, struct file *filp) {


		return 0;

}


ssize_t mbcdd_write(struct file *filp, const char __user *buf, size_t count,
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

ssize_t mbcdd_read(struct file *filp, char __user *buf, size_t count,
		loff_t *f_pos) {




		printk(KERN_NOTICE "mbcdd: Reading \n");

		//mbcdd_get_msg();

		return count;

}

static struct file_operations gbl_mbcdd_fops = {
		.open = mbcdd_open,
		.release = mbcdd_release,
		.write = mbcdd_write,
		.read = mbcdd_read,

		};

static int __init  mbcd_init(void) {
	int result_get_major;

	dev_t dev = 0;

	/* get major device number */
	result_get_major = alloc_chrdev_region(&dev, 0, gbl_device_count,
			"mbcdd");
	mbcdd_major = MAJOR(dev);

	printk(KERN_NOTICE "mbcdd: Insert module, major: %d , %d \n",result_get_major, mbcdd_major );

	// setup device
	mbcdd_setup_cdev(&gbl_mbcdd_dev);

	return 0;

}


static mbcdd_setup_cdev(struct mbcdd_dev *dev) {

	int err;

	// initalize device to kernel
	cdev_init(&dev->cdev, &gbl_mbcdd_fops);
	dev->cdev.owner = THIS_MODULE;
	dev->cdev.ops = &gbl_mbcdd_fops;

	// add device to kernel
	err = cdev_add(&(dev->cdev),MINOR_FIRST,MINOR_COUNT);
	if (err) {
		printk(KERN_NOTICE "mbcdd: Error adding device \n");
	}

}

static void __exit  mbcd_exit(void) {
	dev_t devno = MKDEV(mbcdd_major, 0);
	unregister_chrdev_region(devno, gbl_device_count);
	printk(KERN_NOTICE "mbcdd: Unregister module \n");
}

module_init(mbcd_init);
module_exit(mbcd_exit);

