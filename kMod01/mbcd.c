#include <linux/proc_fs.h>
#include <linux/init.h>
#include <linux/module.h>
#include <linux/fs.h>
#include <linux/cdev.h>
#include <asm/uaccess.h>
#include "mbcd.h"

#include <linux/proc_fs.h>
#include <linux/seq_file.h>



int mbcd_major =   0;
int mbcd_minor =   0;
int mbcd_nr_devs = 1;
int mbcd_quantum = 4000;
int mbcd_qset =    20;
int write_counts=0;
int read_counts=0;

MODULE_LICENSE("GPL");
MODULE_AUTHOR("R. Gratz, M. Kasinger");
MODULE_DESCRIPTION("A message buffering char device driver.");



/*
 * The proc filesystem: function to read and entry
 */
int mbcd_read_procmem(char *buf, char **start, off_t offset,
                   int count, int *eof, void *data)
{
	int i, j, len = 0;
	int limit = count - 80; /* Don't print more than this */

	for (i = 0; i < mbcd_nr_devs && len <= limit; i++) {
		struct mbcd_dev *d = &mbcd_devices[i];
		struct mbcd_qset *qs = d->data;
		if (down_interruptible(&d->sem))
			return -ERESTARTSYS;
		len += sprintf(buf+len,"\nDevice %i: qset %i, q %i, sz %li\n",
				i, d->qset, d->quantum, d->size);
		for (; qs && len <= limit; qs = qs->next) { /* scan the list */
			len += sprintf(buf + len, "  item at %p, qset at %p\n",
					qs, qs->data);
			if (qs->data && !qs->next) /* dump only the last item */
				for (j = 0; j < d->qset; j++) {
					if (qs->data[j])
						len += sprintf(buf + len,
								"    % 4i: %8p\n",
								j, qs->data[j]);
				}
		}
		up(&mbcd_devices[i].sem);
	}
	*eof = 1;
	return len;
}

/*
 * For now, the seq_file implementation will exist in parallel.  The
 * older read_procmem function should maybe go away, though.
 */

/*
 * Here are our sequence iteration methods.  Our "position" is
 * simply the device number.
 */
static void *mbcd_seq_start(struct seq_file *s, loff_t *pos)
{
	if (*pos >= mbcd_nr_devs)
		return NULL;   /* No more to read */
	return mbcd_devices + *pos;
}

static void *mbcd_seq_next(struct seq_file *s, void *v, loff_t *pos)
{
	(*pos)++;
	if (*pos >= mbcd_nr_devs)
		return NULL;
	return mbcd_devices + *pos;
}

static void mbcd_seq_stop(struct seq_file *s, void *v)
{

}

static int mbcd_seq_show(struct seq_file *s, void *v)
{
	struct mbcd_dev *dev = (struct mbcd_dev *) v;
	struct mbcd_qset *d;
	int i;

	if (down_interruptible(&dev->sem))
		return -ERESTARTSYS;
	seq_printf(s, "show meth");
	for (d = dev->data; d; d = d->next) { /* scan the list */
		if (d->data && !d->next) /* dump only the last item */

			for (i = 0; i < dev->qset; i++) {
				if (d->data[i])
					seq_printf(s, "count qsets %d",i);
			}
	}
	up(&dev->sem);
	return 0;
}

/*
 * Tie the sequence operators up.
 */
static struct seq_operations mbcd_seq_ops = {
	.start = mbcd_seq_start,
	.next  = mbcd_seq_next,
	.stop  = mbcd_seq_stop,
	.show  = mbcd_seq_show
};

/*
 * Now to implement the /proc file we need only make an open
 * method which sets up the sequence operators.
 */
static int mbcd_proc_open(struct inode *inode, struct file *file)
{
	return seq_open(file, &mbcd_seq_ops);
}

/*
 * Create a set of file operations for our proc file.
 */
static struct file_operations mbcd_proc_ops = {
	.owner   = THIS_MODULE,
	.open    = mbcd_proc_open,
	.read    = seq_read,
	.llseek  = seq_lseek,
	.release = seq_release
};


/*
 * Actually create (and remove) the /proc file(s).
 */

static void mbcd_create_proc(void)
{
	struct proc_dir_entry *entry;
	create_proc_read_entry("mbcd_cread", 0 /* default mode */,
			NULL /* parent dir */, mbcd_read_procmem,
			NULL /* client data */);
	entry = create_proc_entry("mbcd_cwrite", 0, NULL);
	if (entry)
		entry->proc_fops = &mbcd_proc_ops;
}

static void mbcd_remove_proc(void)
{
	/* no problem if it was not registered */
	remove_proc_entry("mbcd_cread", NULL /* parent dir */);
	remove_proc_entry("mbcd_cwrite", NULL);
}



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
	if (*f_pos >= dev->size){
		retval=1;
		goto out;
	}
	if (*f_pos + count > dev->size)
		count = dev->size - *f_pos;

	/* find listitem, qset index, and offset in the quantum */
	item = (long)*f_pos / itemsize;
	rest = (long)*f_pos % itemsize;
	s_pos = rest / quantum; q_pos = rest % quantum;

	/* follow the list up to the right position (defined elsewhere) */
	dptr = mbcd_follow(dev, item);

	if (dptr == NULL || !dptr->data || ! dptr->data[s_pos]){
		retval=1;
		goto out; /* don't fill holes */
	}

	// count = number of bytes to copy
	if (count > quantum - q_pos){
			count = quantum - q_pos;
	}

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

ssize_t mbcd_write(struct file *filp, const char __user *buf, size_t count,
                loff_t *f_pos)
{

	write_counts++;
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
	if (count > quantum - q_pos){
		count = quantum - q_pos;
	}

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
	mbcd_remove_proc();
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
		printk(KERN_ALERT "Error %d adding mbcd%d", err, index);
}


int __init mbcd_init(void)
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

	printk(KERN_ALERT "mbcd: Device registered, major  %d", mbcd_major);

	mbcd_create_proc();

	return 0;

}

module_init(mbcd_init);
module_exit(mbcd_exit);
