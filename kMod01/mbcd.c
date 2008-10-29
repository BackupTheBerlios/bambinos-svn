#include <linux/init.h>
#include <linux/module.h>
#include <linux/fs.h>
#include <linux/cdev.h>

MODULE_LICENSE("GPL");
MODULE_AUTHOR("R. Gratz, M. Kasinger");
MODULE_DESCRIPTION("A message buffering char device driver.");


static int device_count = 1;
static int mbcd_major;
static int mbcd_minor;
static dev_t first=0;


/* TODO */
struct mbcd_dev {
	struct mbcd_qset *data;  /* Pointer to first quantum set */
	int quantum;              /* the current quantum size */
	int qset;                 /* the current array size */
	unsigned long size;       /* amount of data stored here */
	unsigned int access_key;  /* used by sculluid and scullpriv */
	struct semaphore sem;     /* mutual exclusion semaphore     */
	struct cdev cdev;	  /* Char device structure		*/
};

struct mbcd_dev *mbcd_devices;	/* allocated in scull_init_module */


// mbcd_dev represents our device interally
int mbcd_open(struct inode *inode, struct file *filp){
	/* device information */
	struct mbcd_dev *dev;
	dev = container_of(inode->i_cdev, struct mbcd_dev, cdev); // container_of is a Kernel Makro
	filp->private_data = dev;

	/* now trim to 0 the length of the device if open was write-only */
		if ( (filp->f_flags & O_ACCMODE) == O_WRONLY) {
			//if (down_interruptible(&dev->sem))
			//	return -ERESTARTSYS;
			mbcd_trim(dev); /* ignore errors */
			//up(&dev->sem);
		}
	return 0;
}

int mbcd_release(struct inode *inode, struct file *filp){
	return 0;
}

int mbcd_read(){
	return 0;
}

int mbcd_write(){
	return 0;
}

struct file_operations mbcd_fops = {
		.owner = THIS_MODULE,
		.read = mbcd_read,
		.write = mbcd_write,
		.open = mbcd_open,
		.release = mbcd_release,
		};


/*
 * Set up the char_dev structure for this device.
 */
static void mbcd_setup_cdev(struct mbcd_dev *dev, int index)
{
	int err, devno = MKDEV(mbcd_major, mbcd_minor + index);

	cdev_init(&dev->cdev, &mbcd_fops);
	dev->cdev.owner = THIS_MODULE;
	dev->cdev.ops = &mbcd_fops;
	err = cdev_add (&dev->cdev, devno, 1);
	/* Fail gracefully if need be */
	if (err){
		printk(KERN_NOTICE "Error %d adding mbcd%d", err, index);
	}
}


static int __init  mbcd_init(void) {
	int result_get_major;


	/* get major device number */
	result_get_major = alloc_chrdev_region(&first, mbcd_minor, device_count, "mbcd");
	mbcd_major = MAJOR(first);

	mbcd_setup_cdev(&mbcd_devices, 0);

	return 0;
}

static void __exit mbcd_exit(void)
{
	/* clean up */
	unregister_chrdev_region(first, device_count);
}

module_init(mbcd_init);
module_exit(mbcd_exit);
