/*
 * Segmentation-based user mode implementation
 * Copyright (c) 2001,2003 David H. Hovemeyer <daveho@cs.umd.edu>
 * $Revision: 1.23 $
 * 
 * This is free software.  You are permitted to use,
 * redistribute, and modify it as specified in the file "COPYING".
 */

#include <geekos/ktypes.h>
#include <geekos/kassert.h>
#include <geekos/defs.h>
#include <geekos/mem.h>
#include <geekos/string.h>
#include <geekos/malloc.h>
#include <geekos/int.h>
#include <geekos/gdt.h>
#include <geekos/segment.h>
#include <geekos/tss.h>
#include <geekos/kthread.h>
#include <geekos/argblock.h>
#include <geekos/user.h>

/* ----------------------------------------------------------------------
 * Variables
 * ---------------------------------------------------------------------- */

#define DEFAULT_USER_STACK_SIZE 8192


/* ----------------------------------------------------------------------
 * Private functions
 * ---------------------------------------------------------------------- */


/*
 * Create a new user context of given size
 * This just initalizes a User_Context. Data need to be filled in. 
 */

/* TODO: Implement
static struct User_Context* Create_User_Context(ulong_t size)
*/
static struct User_Context* Create_User_Context(ulong_t size)
{
	
	/*
 	* Create a new User_Context
 	*/ 
 	struct User_Context *pUser_Context;
	pUser_Context = (struct User_Context*) Malloc( sizeof(struct User_Context) );

	(*pUser_Context).size = size;
	(*pUser_Context).memory = (char *)Malloc(size);
	
	return pUser_Context;
	
}

static bool Validate_User_Memory(struct User_Context* userContext,
    ulong_t userAddr, ulong_t bufSize)
{
    ulong_t avail;

    if (userAddr >= userContext->size)
        return false;

    avail = userContext->size - userAddr;
    if (bufSize > avail)
        return false;

    return true;
}

/* ----------------------------------------------------------------------
 * Public functions
 * ---------------------------------------------------------------------- */

/*
 * Destroy a User_Context object, including all memory
 * and other resources allocated within it.
 */
void Destroy_User_Context(struct User_Context* userContext)
{
    /*
     * Hints:
     * - you need to free the memory allocated for the user process
     * - don't forget to free the segment descriptor allocated
     *   for the process's LDT
     */
    TODO("Destroy a User_Context");
}

/*
 * Load a user executable into memory by creating a User_Context
 * data structure.
 * Params:
 * exeFileData - a buffer containing the executable to load
 * exeFileLength - number of bytes in exeFileData
 * exeFormat - parsed ELF segment information describing how to
 *   load the executable's text and data segments, and the
 *   code entry point address
 * command - string containing the complete command to be executed:
 *   this should be used to create the argument block for the
 *   process
 * pUserContext - reference to the pointer where the User_Context
 *   should be stored
 *
 * Returns:
 *   0 if successful, or an error code (< 0) if unsuccessful
 */
int Load_User_Program(char *exeFileData, ulong_t exeFileLength,
    struct Exe_Format *exeFormat, const char *command,
    struct User_Context **pUserContext)
{
    /*
     * Hints:
     * - Determine where in memory each executable segment will be placed
     * - Determine size of argument block and where it memory it will
     *   be placed
     * - Copy each executable segment into memory
     * - Format argument block in memory
     * - In the created User_Context object, set code entry point
     *   address, argument block address, and initial kernel stack pointer
     *   address
     */
    //TODO("Load a user executable into a user memory space using segmentation");
    //Print("%d", *exeFormat->entryAddr);
    ulong_t luSegment_Size = 0;
    ulong_t luSegment_Start_Address = 0;
    ulong_t luMax_Virtual_Address = 0;
    ulong_t luArgument_Block_Size = 0;
    ulong_t luTotal_Memory_Size = 0;
    ulong_t luStack_Size = 0;
    int iNumber_Of_Arguments = 0;


	/* detemines the highest virtual address of all segments */
	int i = 0;
	ulong_t luCurrent_Segment_Max_Virtual_Address = 0;
    for (i=0; i < (*exeFormat).numSegments; i++) {
    	Print("StartAddress: %lu \n", (*exeFormat).segmentList[i].startAddress);
    	Print("ProtFlags: %i\n", (*exeFormat).segmentList[i].protFlags);
    	Print("Size in Memory: %lu\n", (*exeFormat).segmentList[i].sizeInMemory);
    	Print("Offset: %lu \n", (*exeFormat).segmentList[i].offsetInFile);
    	
		luSegment_Size = (*exeFormat).segmentList[i].sizeInMemory;
		luSegment_Start_Address = (*exeFormat).segmentList[i].startAddress;
		
		luCurrent_Segment_Max_Virtual_Address = Round_Up_To_Page(luSegment_Start_Address + luSegment_Size); 
		if (luMax_Virtual_Address < luCurrent_Segment_Max_Virtual_Address) {
			luMax_Virtual_Address = Round_Up_To_Page(luSegment_Start_Address + luSegment_Size);			
		} 

    }
    
	Print("max virt. Add: %lu\n", luMax_Virtual_Address);
	
	/*
	 * The argument_block and DEFAULT_USER_STACK_SIZE form the size of the stack
	 */
	Get_Argument_Block_Size(command, &iNumber_Of_Arguments, &luArgument_Block_Size);
	luStack_Size = Round_Up_To_Page(DEFAULT_USER_STACK_SIZE + luArgument_Block_Size);
	
	/*
	 * The highest virtual address of all segments and the size of the stack form the total memory size
	 */ 
	luTotal_Memory_Size = luMax_Virtual_Address + luStack_Size;
	
	Print("Total Memory: %lu\n", luTotal_Memory_Size);
	
	(*pUserContext) = Create_User_Context(luTotal_Memory_Size);
	
	/*
	 * Create a LDT, and its descriptor in the User_Context
	 */
	(**pUserContext).ldtDescriptor = Allocate_Segment_Descriptor();
	
	Init_LDT_Descriptor((**pUserContext).ldtDescriptor, (**pUserContext).ldt, NUM_USER_LDT_ENTRIES);
	/* iLdt_Index_in_Gdt is the index of the ldtDescriptor in the gdt */  
	int iLdt_Index_in_Gdt = Get_Descriptor_Index((**pUserContext).ldtDescriptor);
		
	(**pUserContext).ldtSelector = Selector(KERNEL_PRIVILEGE, true, iLdt_Index_in_Gdt);
	
	
	struct Segment_Descriptor sCode_Segment_Descriptor; 
	struct Segment_Descriptor sData_Segment_Descriptor;
	Init_Null_Segment_Descriptor(&sCode_Segment_Descriptor);
	Init_Null_Segment_Descriptor(&sData_Segment_Descriptor);
	
	
	/*
	 * writing Segment-Descriptors and -Selectors in the LDT
	 */ 
	ulong_t luCode_Segment_Start_Address = (*exeFormat).segmentList[0].startAddress;
	int iNumber_Of_Code_Segment_Pages = Round_Up_To_Page((*exeFormat).segmentList[0].sizeInMemory)/PAGE_SIZE;
		
	ulong_t luData_Segment_Start_Address = (*exeFormat).segmentList[1].startAddress;
	int iNumber_Of_Data_Segment_Pages = Round_Up_To_Page((*exeFormat).segmentList[1].sizeInMemory)/PAGE_SIZE;
	
	Print("CodePages: %i, DataPages: %i \n", iNumber_Of_Code_Segment_Pages, iNumber_Of_Data_Segment_Pages);

	Init_Code_Segment_Descriptor(&sCode_Segment_Descriptor, luCode_Segment_Start_Address, iNumber_Of_Code_Segment_Pages, USER_PRIVILEGE);
	Init_Data_Segment_Descriptor(&sData_Segment_Descriptor, luData_Segment_Start_Address, iNumber_Of_Data_Segment_Pages, USER_PRIVILEGE);

	(**pUserContext).ldt[0] = sCode_Segment_Descriptor;
	(**pUserContext).ldt[1] = sData_Segment_Descriptor;
	
	(**pUserContext).csSelector = Selector(USER_PRIVILEGE, false, 0);
	(**pUserContext).dsSelector = Selector(USER_PRIVILEGE, false, 1);
	
	
	Format_Argument_Block(exeFileData, iNumber_Of_Arguments, (**pUserContext).argBlockAddr, command);
		
	return 0;    
        
}

/*
 * Copy data from user memory into a kernel buffer.
 * Params:
 * destInKernel - address of kernel buffer
 * srcInUser - address of user buffer
 * bufSize - number of bytes to copy
 *
 * Returns:
 *   true if successful, false if user buffer is invalid (i.e.,
 *   doesn't correspond to memory the process has a right to
 *   access)
 */
bool Copy_From_User(void* destInKernel, ulong_t srcInUser, ulong_t bufSize)
{
    /*
     * Hints:
     * - the User_Context of the current process can be found
     *   from g_currentThread->userContext
     * - the user address is an index relative to the chunk
     *   of memory you allocated for it
     * - make sure the user buffer lies entirely in memory belonging
     *   to the process
     */
    TODO("Copy memory from user buffer to kernel buffer");
    Validate_User_Memory(NULL,0,0); /* delete this; keeps gcc happy */
}

/*
 * Copy data from kernel memory into a user buffer.
 * Params:
 * destInUser - address of user buffer
 * srcInKernel - address of kernel buffer
 * bufSize - number of bytes to copy
 *
 * Returns:
 *   true if successful, false if user buffer is invalid (i.e.,
 *   doesn't correspond to memory the process has a right to
 *   access)
 */
bool Copy_To_User(ulong_t destInUser, void* srcInKernel, ulong_t bufSize)
{
    /*
     * Hints: same as for Copy_From_User()
     */
    TODO("Copy memory from kernel buffer to user buffer");
}

/*
 * Switch to user address space belonging to given
 * User_Context object.
 * Params:
 * userContext - the User_Context
 */
void Switch_To_Address_Space(struct User_Context *userContext)
{
    /*
     * Hint: you will need to use the lldt assembly language instruction
     * to load the process's LDT by specifying its LDT selector.
     */
    TODO("Switch to user address space using segmentation/LDT");
    //Print("value: %i", userContext->ldtSelector);
    
    //Allocate_Segment_Descriptor();
    //Selector(USER_PRIVILEGE, true, userContext->ldtSelector);
    //Init_Code_Segment_Descriptor(userContext->ldtDescriptor,)
    
}

