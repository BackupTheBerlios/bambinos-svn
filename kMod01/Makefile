EXTRA_CFLAGS=-O3
CC=/usr/bin/gcc


# If KERNELRELEASE is defined, we've been invoked from the
# kernel build system and can use its language.
ifneq ($(KERNELRELEASE),)
	obj-m := mbcdd.o mbcdd_msg_hdl.o
# Otherwise we were called directly from the command
# line; invoke the kernel build system.
else
	KERNELDIR ?= /lib/modules/$(shell uname -r)/build
	PWD := $(shell pwd)
default: producer consumer
	$(MAKE) -C $(KERNELDIR) M=$(PWD) modules
	
endif

producer: producer.o file.o

consumer: consumer.o file.o

clean:
	rm -rf *.o *.mod.c *.symvers *.ko
