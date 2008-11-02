#include <linux/init.h>
#include <linux/module.h>
#include <linux/fs.h>

MODULE_LICENSE("GPL");
MODULE_AUTHOR("R. Gratz, M. Kasinger");
MODULE_DESCRIPTION("A message buffering char device driver, mainly implemented for educational purposes.");


static dev_t 	device_nr;
static int 		device_count = 1;

static int __init mbcd_init(void)
{
	int result_get_major;

	/* get major device number */
	result_get_major = alloc_chrdev_region(&device_nr, 0, device_count, "mbcd");

    printk(KERN_ALERT "mbcdd: Insert Module \n");
    return 0;
 }

static void __exit mbcd_exit(void)
{
   unregister_chrdev_region(device_nr, device_count);
}


module_init(mbcd_init);
module_exit(mbcd_exit);


