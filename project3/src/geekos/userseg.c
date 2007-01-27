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
	// lacki
	/*
 	* Create a new User_Context
 	*/ 
 	struct User_Context *pUser_Context;
 	pUser_Context = (struct User_Context*) Malloc( sizeof(struct User_Context) );
 	
 	if (pUser_Context == 0) {
 		DEBUG("Error creating User_Context")
 		return 0;
 	}

    (*pUser_Context).memory = (char *)Malloc(size);
    if ((*pUser_Context).memory == 0) {
    	DEBUG("Error assigning memory to User_Context");
    }
    
    (*pUser_Context).size = size;

	// Create a LDT, and its descriptor in the User_Context
	(*pUser_Context).ldtDescriptor = Allocate_Segment_Descriptor();
	
	if ((*pUser_Context).ldtDescriptor == 0) {
		DEBUG("Error Allocating Segment Descriptor for User_Context");
	}
	
	Init_LDT_Descriptor((*pUser_Context).ldtDescriptor, (*pUser_Context).ldt, NUM_USER_LDT_ENTRIES);
	//iLdt_Index_in_Gdt is the index of the ldtDescriptor in the gdt  
	int iLdt_Index_in_Gdt = Get_Descriptor_Index((*pUser_Context).ldtDescriptor);
		
	(*pUser_Context).ldtSelector = Selector(KERNEL_PRIVILEGE, true, iLdt_Index_in_Gdt);
	
	
	struct Segment_Descriptor sCode_Segment_Descriptor; 
	struct Segment_Descriptor sData_Segment_Descriptor;
	Init_Null_Segment_Descriptor(&sCode_Segment_Descriptor);
	Init_Null_Segment_Descriptor(&sData_Segment_Descriptor);
	
	
	//writing Segment-Descriptors and -Selectors in the LDT 
	//ulong_t luCode_Segment_Start_Address = (*exeFormat).segmentList[0].startAddress;
	ulong_t luCode_Segment_Start_Address = (unsigned long)(*pUser_Context).memory;
	//int iNumber_Of_Code_Segment_Pages = Round_Up_To_Page((*exeFormat).segmentList[0].sizeInMemory)/PAGE_SIZE;
		
	//ulong_t luData_Segment_Start_Address = (*exeFormat).segmentList[1].startAddress;
	ulong_t luData_Segment_Start_Address = (unsigned long)(*pUser_Context).memory;
	//ulong_t iNumber_Of_Data_Segment_Pages = Round_Up_To_Page((*exeFormat).segmentList[1].sizeInMemory)/PAGE_SIZE;
	
	ulong_t iNumber_Of_Code_Segment_Pages = size/PAGE_SIZE;
	ulong_t iNumber_Of_Data_Segment_Pages = size/PAGE_SIZE;
	
	Print("CodePages: %lu, DataPages: %lu \n", iNumber_Of_Code_Segment_Pages, iNumber_Of_Data_Segment_Pages);
	
	
	Init_Code_Segment_Descriptor(&sCode_Segment_Descriptor, luCode_Segment_Start_Address, iNumber_Of_Code_Segment_Pages, USER_PRIVILEGE);
	Init_Data_Segment_Descriptor(&sData_Segment_Descriptor, luData_Segment_Start_Address, iNumber_Of_Data_Segment_Pages, USER_PRIVILEGE);
	
	(*pUser_Context).ldt[0] = sCode_Segment_Descriptor;
	(*pUser_Context).ldt[1] = sData_Segment_Descriptor;
	
	(*pUser_Context).csSelector = Selector(USER_PRIVILEGE, false, 0);
	(*pUser_Context).dsSelector = Selector(USER_PRIVILEGE, false, 1);
		
	(*pUser_Context).refCount = 0;

    
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
	// lacki
    /*
     * Hints:
     * - you need to free the memory allocated for the user process
     * - don't forget to free the segment descriptor allocated
     *   for the process's LDT
     */
    //TODO("Destroy a User_Context");
	
	struct Segment_Descriptor *sSegment_Descriptor;
	sSegment_Descriptor = userContext->ldtDescriptor;
	
	// removes segment descriptor from gdt
	Free_Segment_Descriptor(sSegment_Descriptor);
	// frees userContext memory
	Free(userContext->memory);
	Free (userContext);

    
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
	// lacki
    //TODO("Load a user executable into a user memory space using segmentation");
    //Print("%d", *exeFormat->entryAddr);
    ulong_t luSegment_Size = 0;
    ulong_t luSegment_Start_Address = 0;
    ulong_t luMax_Virtual_Address = 0;
    ulong_t luArgument_Block_Size = 0;
    ulong_t luTotal_Memory_Size = 0;
    ulong_t luStack_Size = 0;
    int iNumber_Of_Arguments = 0;
    struct User_Context *pCurrent_User_Context;

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
			luMax_Virtual_Address = luCurrent_Segment_Max_Virtual_Address;
		} 

    }
    
	Print("max virt. Add: %lu\n", luMax_Virtual_Address);
	
	
	//The argument_block and DEFAULT_USER_STACK_SIZE form the size of the stack
	Get_Argument_Block_Size(command, &iNumber_Of_Arguments, &luArgument_Block_Size);
	luStack_Size = Round_Up_To_Page(DEFAULT_USER_STACK_SIZE + luArgument_Block_Size);
	
	
	//The highest virtual address of all segments and the size of the stack form the total memory size
	luTotal_Memory_Size = luMax_Virtual_Address + luStack_Size;
	
	Print("Total Memory: %lu\n", luTotal_Memory_Size);
	
	//*pUserContext = Create_User_Context(luTotal_Memory_Size);
	pCurrent_User_Context = Create_User_Context(luTotal_Memory_Size);
	
	//// Creating the argument block
	char *cArgument_Block;
	cArgument_Block = (char *)Malloc(luArgument_Block_Size);
	
	pCurrent_User_Context->argBlockAddr = Round_Up_To_Page(luMax_Virtual_Address);
	Format_Argument_Block(cArgument_Block, iNumber_Of_Arguments, pCurrent_User_Context->argBlockAddr, command);
	//Format_Argument_Block(pCurrent_User_Context->memory+Round_Up_To_Page(luMax_Virtual_Address), iNumber_Of_Arguments, Round_Up_To_Page(luMax_Virtual_Address), command);
	
	Print("argBlockAddr: %lu \n", pCurrent_User_Context->argBlockAddr);
	
	for (i=0; i < (*exeFormat).numSegments; i++) {
	memcpy(pCurrent_User_Context->memory + (*exeFormat).segmentList[i].startAddress,
			exeFileData + (*exeFormat).segmentList[i].offsetInFile,
			(*exeFormat).segmentList[i].lengthInFile);
	}
	
	pCurrent_User_Context->entryAddr = (*exeFormat).entryAddr;
	pCurrent_User_Context->stackPointerAddr = luTotal_Memory_Size;
	
	*pUserContext = pCurrent_User_Context;
	
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
	//lacki
    /*
     * Hints:
     * - the User_Context of the current process can be found
     *   from g_currentThread->userContext
     * - the user address is an index relative to the chunk
     *   of memory you allocated for it
     * - make sure the user buffer lies entirely in memory belonging
     *   to the process
     */
    //TODO("Copy memory from user buffer to kernel buffer");
	
	struct User_Context* pUser_Context = g_currentThread->userContext;
	
	int retval = Validate_User_Memory(pUser_Context, srcInUser, bufSize);
	if (retval == true) {
		memcpy(destInKernel, (*pUser_Context).memory + srcInUser, bufSize);
		return true;
	} else {
		Print("Copy_From_User: Error: Segmentation fault: Address out of UserContext\n");
		return false;
	}
	
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
	//lacki
    /*
     * Hints: same as for Copy_From_User()
     */
    //TODO("Copy memory from kernel buffer to user buffer");
	struct User_Context* pUser_Context = g_currentThread->userContext;
	
	int retval = Validate_User_Memory(pUser_Context, destInUser, bufSize);
	if (retval == true) {
		memcpy((*pUser_Context).memory + destInUser, srcInKernel, bufSize);
		return true;
	} else {
		Print("Copy_To_User: Error: Segmentation fault: Address out of UserContext\n");
		return false;
	}	
}

/*
 * Switch to user address space belonging to given
 * User_Context object.
 * Params:
 * userContext - the User_Context
 */
void Switch_To_Address_Space(struct User_Context *userContext)
{
	// lacki
    /*
     * Hint: you will need to use the lldt assembly language instruction
     * to load the process's LDT by specifying its LDT selector.
     */
    //TODO("Switch to user address space using segmentation/LDT");

	KASSERT(userContext->ldtSelector != 0);
	
	__asm__ __volatile__ (
            "lldt %0"
            :
            : "a" (userContext->ldtSelector)
            );	
	
}

