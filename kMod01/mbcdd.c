#include <linux/proc_fs.h>
#include <linux/init.h>
#include <linux/module.h>
#include <linux/fs.h>
#include <linux/cdev.h>
#include <asm/uaccess.h>
#include <linux/proc_fs.h>
#include <linux/seq_file.h>
#include <linux/completion.h>

#include "mbcdd.h"
#include "mbcdd_msg_hdl.h"

int mbcdd_major = 0;
int mbcdd_minor = 0;
int mbcdd_nr_devs = 1;

spinlock_t write_lock = SPIN_LOCK_UNLOCKED;
spinlock_t read_lock = SPIN_LOCK_UNLOCKED;

unsigned long flags;

MODULE_LICENSE("GPL");
MODULE_AUTHOR("R. Gratz, M. Kasinger");
MODULE_DESCRIPTION("A message buffering char device driver.");

int mbcdd_open(struct inode *inode, struct file *filep) {
	struct mbcdd_dev *dev;
	struct mbcdd_dev_wrapper *dev_wrapper;



	if ((filep->f_flags & O_ACCMODE) == O_WRONLY) {
		// fopen for write
		dev_wrapper = kmalloc(sizeof(struct mbcdd_dev_wrapper), GFP_KERNEL);
		memset(dev_wrapper, 0, sizeof(struct mbcdd_dev_wrapper));
		dev = container_of(inode->i_cdev, struct mbcdd_dev, cdev);
		// i_cdev enthaelt die cdev struktur die wir erstellt haben; Kernel gibt das in inode an
		// unser Device weiter

		dev_wrapper->dev = dev;


		dev_wrapper->msg = mbcdd_new_msg();

	} else if (((filep->f_flags & O_ACCMODE) == O_RDONLY)) {

		dev_wrapper = filep->private_data;
		printk(KERN_NOTICE "mbcd open ro fin writer = %d  \n", dev_wrapper->fin_writer);


		dev_wrapper->msg = mbcdd_get_msg();

		printk(KERN_NOTICE "mbcd open ro msg id %d  \n", dev_wrapper->fin_writer);
		// wenn reader nicht fertig ist, dann completion mechanismus
		// initialiserien
		if (dev_wrapper->fin_writer == 0)
			init_completion(&dev_wrapper->hold_readers);

	} else {

		return 1;

	}

	filep->private_data = dev_wrapper;

	printk(KERN_NOTICE "mbcd open id %d  p %p \n", dev_wrapper->msg->id,
			dev_wrapper->msg);

	return 0;

}

int mbcdd_release(struct inode *inode, struct file *filep) {

	struct mbcdd_dev_wrapper *dev_wrapper;
	dev_wrapper = filep->private_data;

	if ((filep->f_flags & O_ACCMODE) == O_WRONLY) {

		// set writer finish flag
		dev_wrapper->fin_writer = 1;
		// wenn reader darauf liest, benachrichtigung an sleepers
		if (dev_wrapper->busy_reader == 1) {
			complete(&dev_wrapper->hold_readers);
		}

	} else if (((filep->f_flags & O_ACCMODE) == O_RDONLY)) {

		mbcdd_del_msg(dev_wrapper->msg);

	}

	printk(KERN_NOTICE "mbcd release %d, fin writer %d  \n",
			dev_wrapper->msg->id, dev_wrapper->fin_writer);

	return 0;
}

ssize_t mbcdd_read(struct file *filp, char __user *buf, size_t count,
		loff_t *f_pos) {

	int retval = -1;
	message_slot_t *to;
	struct mbcdd_dev_wrapper *dev_wrapper;

	count = DATA_SLOT_SIZE;
	dev_wrapper = filp->private_data;
	to = mbcdd_get_data_slot(dev_wrapper->msg);

	if (to == NULL) {
		// wenn writer finish flag ist gesetzt, dann gibts
		// keine weiteren slots mehr, dann wars schon der letzte
		if (dev_wrapper->fin_writer == 1) {
			return 0;

		} else {
			wait_for_completion_timeout(&dev_wrapper->hold_readers, 3000);
			to = mbcdd_get_data_slot(dev_wrapper->msg);
			if (to == NULL) {
				return 0; // TODO
			}
		}

	}

	spin_lock_irqsave(&read_lock, flags);

	// critical region
	retval = copy_to_user(buf, to, count);

	spin_unlock_irqrestore(&read_lock, flags);

	if (retval == 0) {
		retval = -EFAULT;
	}
	retval = count;

	printk(KERN_NOTICE "mbcdd: Reading \n");

	return retval;
}

ssize_t mbcdd_write(struct file *filep, const char __user *buf, size_t count,
		loff_t *f_pos) {

	void *to;
	ssize_t retval;
	struct mbcdd_dev_wrapper *dev_wrapper;

	count = DATA_SLOT_SIZE;
	retval = -ENOMEM;
	dev_wrapper = filep->private_data;

	printk(KERN_NOTICE "mbcd writing p1 %p , msg id %d \n", dev_wrapper->msg,
			dev_wrapper->msg->id);

	// Get a pointer to a new data slot
	to = mbcdd_new_data_slot(dev_wrapper->msg);

	spin_lock_irqsave(&write_lock, flags);
	// critical region

	if (copy_from_user(to, buf, count)) {
		retval = -EFAULT;
	} else {
		// if copy_from_user was ok, return the written size
		retval = count;
	}

	spin_unlock_irqrestore(&write_lock, flags);

	if (dev_wrapper->busy_reader == 1) {
		// wake up readers
		complete(&dev_wrapper->hold_readers);
	}

	printk(KERN_NOTICE "mbcdd: Writing of %d data \n", retval);

	return retval;

}

struct file_operations mbcdd_fops = { .owner = THIS_MODULE, .read = mbcdd_read,
		.write = mbcdd_write, .open = mbcdd_open, .release = mbcdd_release, };

void mbcdd_exit(void) {

	dev_t devno = MKDEV(mbcdd_major, mbcdd_minor);
	kfree(mbcdd_devices);
	unregister_chrdev_region(devno, mbcdd_nr_devs);
	printk(KERN_NOTICE "mbcdd: Device de-registered! \n");

}

/*
 * Set up the char_dev structure for this device.
 */
static void mbcdd_setup_cdev(struct mbcdd_dev *dev) {
	int err, devno = MKDEV(mbcdd_major, mbcdd_minor);

	cdev_init(&dev->cdev, &mbcdd_fops);
	dev->cdev.owner = THIS_MODULE;
	dev->cdev.ops = &mbcdd_fops;
	err = cdev_add(&dev->cdev, devno, 1);

	if (err)
		printk(KERN_NOTICE "Error %d adding mbcdd \n", err);
}

int mbcdd_init(void) {

	int result;
	dev_t dev = 0;

	// nur dynamisch
	result = alloc_chrdev_region(&dev, mbcdd_minor, mbcdd_nr_devs, "mbcdd");
	mbcdd_major = MAJOR(dev);

	mbcdd_devices = kmalloc(sizeof(struct mbcdd_dev), GFP_KERNEL);
	memset(mbcdd_devices, 0, sizeof(struct mbcdd_dev));

	mbcdd_setup_cdev(mbcdd_devices);

	dev = MKDEV(mbcdd_major, mbcdd_minor);

	printk(KERN_NOTICE "mbcdd: Device registered, major  %d \n", mbcdd_major);

	return 0;

}
module_init(mbcdd_init)
;
module_exit(mbcdd_exit)
;
