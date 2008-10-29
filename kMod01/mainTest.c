#include <linux/proc_fs.h>
#include <linux/init.h>
#include <linux/module.h>
#include <linux/fs.h>
#include <linux/cdev.h>
#include <asm/uaccess.h>




int mbcd_major =   0;
int mbcd_minor =   0;
int mbcd_nr_devs = 1;
int mbcd_quantum = 4000;
int mbcd_qset =    1000;


MODULE_LICENSE("GPL");
MODULE_AUTHOR("R. Gratz, M. Kasinger");
MODULE_DESCRIPTION("A message buffering char device driver.");


struct mbcd_dev *mbcd_devices;	/* allocated in mbcd_init_module */

/*
 * Representation of mbcd quantum sets.
 */
struct mbcd_qset {
	void **data;
	struct mbcd_qset *next;
};



struct mbcd_dev {
	struct mbcd_qset *data;  /* Pointer to first quantum set */
	int quantum;              /* the current quantum size */
	int qset;                 /* the current array size */
	unsigned long size;       /* amount of data stored here */
	unsigned int access_key;  /* used by mbcduid and mbcdpriv */
	struct semaphore sem;     /* mutual exclusion semaphore     */
	struct cdev cdev;	  /* Char device structure		*/
};


/*
 * Empty out the mbcd device; must be called with the device
 * semaphore held.
 */
int mbcd_trim(struct mbcd_dev *dev)
{
	struct mbcd_qset *next, *dptr;
	int qset = dev->qset;   /* "dev" is not-null */
	int i;

	for (dptr = dev->data; dptr; dptr = next) { /* all the list items */
		if (dptr->data) {
			for (i = 0; i < qset; i++)
				kfree(dptr->data[i]);
			kfree(dptr->data);
			dptr->data = NULL;
		}
		next = dptr->next;
		kfree(dptr);
	}
	dev->size = 0;
	dev->quantum = mbcd_quantum;
	dev->qset = mbcd_qset;
	dev->data = NULL;
	return 0;
}


/*
 * Open and close
 */

int mbcd_open(struct inode *inode, struct file *filp)
{
	struct mbcd_dev *dev; /* device information */

	dev = container_of(inode->i_cdev, struct mbcd_dev, cdev);
	filp->private_data = dev; /* for other methods */

	/* now trim to 0 the length of the device if open was write-only */
	if ( (filp->f_flags & O_ACCMODE) == O_WRONLY) {
		if (down_interruptible(&dev->sem))
			return -ERESTARTSYS;
		mbcd_trim(dev); /* ignore errors */
		up(&dev->sem);
	}
	return 0;          /* success */
}

int mbcd_release(struct inode *inode, struct file *filp)
{
	return 0;
}
/*
 * Follow the list
 */
struct mbcd_qset *mbcd_follow(struct mbcd_dev *dev, int n)
{
	struct mbcd_qset *qs = dev->data;

        /* Allocate first qset explicitly if need be */
	if (! qs) {
		qs = dev->data = kmalloc(sizeof(struct mbcd_qset), GFP_KERNEL);
		if (qs == NULL)
			return NULL;  /* Never mind */
		memset(qs, 0, sizeof(struct mbcd_qset));
	}

	/* Then follow the list */
	while (n--) {
		if (!qs->next) {
			qs->next = kmalloc(sizeof(struct mbcd_qset), GFP_KERNEL);
			if (qs->next == NULL)
				return NULL;  /* Never mind */
			memset(qs->next, 0, sizeof(struct mbcd_qset));
		}
		qs = qs->next;
		continue;
	}
	return qs;
}

/*
 * Data management: read and write
 */

ssize_t mbcd_read(struct file *filp, char __user *buf, size_t count,
                loff_t *f_pos)
{
	struct mbcd_dev *dev = filp->private_data;
	struct mbcd_qset *dptr;	/* the first listitem */
	int quantum = dev->quantum, qset = dev->qset;
	int itemsize = quantum * qset; /* how many bytes in the listitem */
	int item, s_pos, q_pos, rest;
	ssize_t retval = 0;

	if (down_interruptible(&dev->sem))
		return -ERESTARTSYS;
	if (*f_pos >= dev->size)
		goto out;
	if (*f_pos + count > dev->size)
		count = dev->size - *f_pos;

	/* find listitem, qset index, and offset in the quantum */
	item = (long)*f_pos / itemsize;
	rest = (long)*f_pos % itemsize;
	s_pos = rest / quantum; q_pos = rest % quantum;

	/* follow the list up to the right position (defined elsewhere) */
	dptr = mbcd_follow(dev, item);

	if (dptr == NULL || !dptr->data || ! dptr->data[s_pos])
		goto out; /* don't fill holes */

	/* read only up to the end of this quantum */
	if (count > quantum - q_pos)
		count = quantum - q_pos;

	if (copy_to_user(buf, dptr->data[s_pos] + q_pos, count)) {
		retval = -EFAULT;
		goto out;
	}
	*f_pos += count;
	retval = count;

  out:
	up(&dev->sem);
	return retval;
}

int mbcd_write(struct file *filp, const char __user *buf, size_t count,
                loff_t *f_pos)
{
	struct mbcd_dev *dev = filp->private_data;
	struct mbcd_qset *dptr;
	int quantum = dev->quantum, qset = dev->qset;
	int itemsize = quantum * qset;
	int item, s_pos, q_pos, rest;
	ssize_t retval = -ENOMEM; /* value used in "goto out" statements */

	if (down_interruptible(&dev->sem))
		return -ERESTARTSYS;

	/* find listitem, qset index and offset in the quantum */
	item = (long)*f_pos / itemsize;
	rest = (long)*f_pos % itemsize;
	s_pos = rest / quantum; q_pos = rest % quantum;

	/* follow the list up to the right position */
	dptr = mbcd_follow(dev, item);
	if (dptr == NULL)
		goto out;
	if (!dptr->data) {
		dptr->data = kmalloc(qset * sizeof(char *), GFP_KERNEL);
		if (!dptr->data)
			goto out;
		memset(dptr->data, 0, qset * sizeof(char *));
	}
	if (!dptr->data[s_pos]) {
		dptr->data[s_pos] = kmalloc(quantum, GFP_KERNEL);
		if (!dptr->data[s_pos])
			goto out;
	}
	/* write only up to the end of this quantum */
	if (count > quantum - q_pos)
		count = quantum - q_pos;

	if (copy_from_user(dptr->data[s_pos]+q_pos, buf, count)) {
		retval = -EFAULT;
		goto out;
	}
	*f_pos += count;
	retval = count;

        /* update the size */
	if (dev->size < *f_pos)
		dev->size = *f_pos;

  out:
	up(&dev->sem);
	return retval;
}


struct file_operations mbcd_fops = {
	.owner =    THIS_MODULE,
	.read =     mbcd_read,
	.write =    mbcd_write,
	.open =     mbcd_open,
	.release =  mbcd_release,
};

void mbcd_exit(void)
{
	int i;
	dev_t devno = MKDEV(mbcd_major, mbcd_minor);

	/* Get rid of our char dev entries */
	if (mbcd_devices) {
		for (i = 0; i < mbcd_nr_devs; i++) {
			mbcd_trim(mbcd_devices + i);
			cdev_del(&mbcd_devices[i].cdev);
		}
		kfree(mbcd_devices);
	}

	unregister_chrdev_region(devno, mbcd_nr_devs);

	printk(KERN_NOTICE "mbcd: Device de-registered!");


}


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
	if (err)
		printk(KERN_NOTICE "Error %d adding mbcd%d", err, index);
}


int mbcd_init(void)
{
	int result, i;
	dev_t dev = 0;

	// nur dynamisch
	result = alloc_chrdev_region(&dev, mbcd_minor, mbcd_nr_devs,"mbcd");
	mbcd_major = MAJOR(dev);

	mbcd_devices = kmalloc(mbcd_nr_devs * sizeof(struct mbcd_dev), GFP_KERNEL);
	memset(mbcd_devices, 0, mbcd_nr_devs * sizeof(struct mbcd_dev));

        /* Initialize each device. */
	for (i = 0; i < mbcd_nr_devs; i++) {
		mbcd_devices[i].quantum = mbcd_quantum;
		mbcd_devices[i].qset = mbcd_qset;
		init_MUTEX(&mbcd_devices[i].sem);
		mbcd_setup_cdev(&mbcd_devices[i], i);
	}


	dev = MKDEV(mbcd_major, mbcd_minor + mbcd_nr_devs);

	printk(KERN_NOTICE "mbcd: Device registered, major  %d", mbcd_major);

	return 0;

}

module_init(mbcd_init);
module_exit(mbcd_exit);
