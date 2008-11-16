===========================================================================================================

Vertiefung Betriebssysteme Winter 2008/09

ASSIGNMENT 2 

Team: Kasinger, Gratz

============================================================================================================



What we have implemented:
-------------------------

	* All requests of Assignment 2
	



How we have implemented this:
-----------------------------

As demanded in the project description we have implemented 2 devices.
Our intention in designing the interfaces was to create a message management device only, 
with no more logical behavior.
This means the message management device is responsible for:
	* allocating resources needed for the messages and slots
	* list handling of messages and slots
	* freeing memory of messages and according slots. 
	
Our device driver is responsible for:
	* concurrency behavior for multiple processes
	* Logical behavior (flow control)
	* completion mechanism on message level
	
	

Our Interface, see mbcdd_msg_hdl.h:
-----------------------------------

Write process:
--------------

Get new message 
message_t *mbcdd_new_msg(void);

Get data slot, device driver writes data to the buffer returned here.
void *mbcdd_new_data(message_t *msg);

Add the written data slot the according message
void mbcdd_add_data_slot(message_t *msg, void *);



Read process:
-------------

Get previously added message
message_t *mbcdd_get_msg(void);

Get data slots
void *mbcdd_get_data_slot(message_t *msg);

When reader is finished, message management driver can delete the message and all slots
void mbcdd_del_msg(message_t *msg);


==============================================================================================================================

HOWTO BUILD:

launch make








	
	
	

	
	
	
	
	
	