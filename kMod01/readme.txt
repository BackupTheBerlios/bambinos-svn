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
	
	
	

	
	
	
	
	
	