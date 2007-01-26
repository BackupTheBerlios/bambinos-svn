/*
 * System call handlers
 * Copyright (c) 2003, Jeffrey K. Hollingsworth <hollings@cs.umd.edu>
 * Copyright (c) 2003,2004 David Hovemeyer <daveho@cs.umd.edu>
 * $Revision: 1.59 $
 * 
 * This is free software.  You are permitted to use,
 * redistribute, and modify it as specified in the file "COPYING".
 */

#include <geekos/syscall.h>
#include <geekos/errno.h>
#include <geekos/kthread.h>
#include <geekos/int.h>
#include <geekos/elf.h>
#include <geekos/malloc.h>
#include <geekos/screen.h>
#include <geekos/keyboard.h>
#include <geekos/string.h>
#include <geekos/user.h>
#include <geekos/timer.h>
#include <geekos/vfs.h>

/*
 * Null system call.
 * Does nothing except immediately return control back
 * to the interrupted user program.
 * Params:
 *  state - processor registers from user mode
 *
 * Returns:
 *   always returns the value 0 (zero)
 */
static int Sys_Null(struct Interrupt_State* state)
{
    return 0;
}

/*
 * Exit system call.
 * The interrupted user process is terminated.
 * Params:
 *   state->ebx - process exit code
 * Returns:
 *   Never returns to user mode!
 */
static int Sys_Exit(struct Interrupt_State* state)
{
	//lacki
    //TODO("Exit system call");
    int iExit_Code = state->ebx;
    
    
    if (Interrupts_Enabled() == false) {
    	Enable_Interrupts();
    } 
    
    Detach_User_Context(g_currentThread);

    // Exit frees kernel_thread memory
    Exit(iExit_Code);
    return 0;
}

/*
 * Print a string to the console.
 * Params:
 *   state->ebx - user pointer of string to be printed
 *   state->ecx - number of characters to print
 * Returns: 0 if successful, -1 if not
 */
static int Sys_PrintString(struct Interrupt_State* state)
{
	//lacki
	unsigned int uNumber_Of_Chars = state->ecx;
	int retval = 0;

	if (uNumber_Of_Chars > 1024) {
		Print("Error. Length of String exceeded 1024 chars");
		return(-1);
	}
	
	char *buffer = Malloc((uNumber_Of_Chars+1) * sizeof(char));
	
	if (buffer == 0) {
		Print("Error: unable to allocate memory for the StringBuffer");
	}
	
	
	retval = Copy_From_User(buffer, state->ebx, uNumber_Of_Chars);
	if (retval == false) {
		Print("Error copying register-content to kernel-memory\n");
	}
	
	buffer[uNumber_Of_Chars] = '\0';
	//Print("size: %i\n", sizeof(buffer));
	//Print("sizec: %u\n", uNumber_Of_Chars);
	Print("%s", buffer);
	Free(buffer);	
    
    return 0;

    
}

/*
 * Get a single key press from the console.
 * Suspends the user process until a key press is available.
 * Params:
 *   state - processor registers from user mode
 * Returns: the key code
 */
static int Sys_GetKey(struct Interrupt_State* state)
{
	//lacki
	Keycode keycode;
	keycode = Wait_For_Key();
	
	//Print ("keycode: %i", keycode);
	return keycode;
}

/*
 * Set the current text attributes.
 * Params:
 *   state->ebx - character attributes to use
 * Returns: always returns 0
 */
static int Sys_SetAttr(struct Interrupt_State* state)
{
	//lacki
    //TODO("SetAttr system call");
	Set_Current_Attr(state->ebx);
    return 0;
}

/*
 * Get the current cursor position.
 * Params:
 *   state->ebx - pointer to user int where row value should be stored
 *   state->ecx - pointer to user int where column value should be stored
 * Returns: 0 if successful, -1 otherwise
 */
static int Sys_GetCursor(struct Interrupt_State* state)
{
	//lacki
    //TODO("GetCursor system call");
    
	int row; // = state->ebx;
	int column; // = state->ecx;
	
	Get_Cursor(&row, &column);
	
	if (Copy_To_User(state->ebx, &row, sizeof(int)) & Copy_To_User(state->ecx, &column, sizeof(int))) {
		return 0;
	} else {
		return -1;
	}
	
}

/*
 * Set the current cursor position.
 * Params:
 *   state->ebx - new row value
 *   state->ecx - new column value
 * Returns: 0 if successful, -1 otherwise
 */
static int Sys_PutCursor(struct Interrupt_State* state)
{
	//lacki
    //TODO("PutCursor system call");
	
	int row = state->ebx;
	int column = state->ecx;
		
    if(Put_Cursor(row, column)){
    	return 0;
    } else 
    {
    	return -1;
    }
}

/*
 * Create a new user process.
 * Params:
 *   state->ebx - user address of name of executable
 *   state->ecx - length of executable name
 *   state->edx - user address of command string
 *   state->esi - length of command string
 * Returns: pid of process if successful, error code (< 0) otherwise
 */
static int Sys_Spawn(struct Interrupt_State* state)
{
	//lacki
    //TODO("Spawn system call");
	
	// name of program 
    uint_t uExecutable_Name_Length = state->ecx;
    char *pExecutable_Name = Malloc((uExecutable_Name_Length) * sizeof(char));
    
    Copy_From_User(pExecutable_Name, state->ebx, uExecutable_Name_Length);
    pExecutable_Name[uExecutable_Name_Length] = '\0';
 
/*
    // display output
    Print ("invoking program: \"");
    int i;
    for (i=0; i < uExecutable_Name_Length; i++) {
    	Print("%c", pExecutable_Name[i]);
    }
    Print ("\"\n");
    // ! display output
*/ 
    
    // program plus args
    uint_t uCommand_String_Length = state->esi;
    if (uCommand_String_Length > VFS_MAX_PATH_LEN) {
    	return (-1);
    }
    
    char *pCommand_String = Malloc((uCommand_String_Length) * sizeof(char));
    Copy_From_User(pCommand_String, state->edx, uCommand_String_Length);
    pCommand_String[uCommand_String_Length] = '\0';
    

/*
     Print ("l: %u\n", uCommand_String_Length);
    // display output
    Print ("invoking command: \"");

    for (i=0; i < uCommand_String_Length; i++) {
    	Print("%c", pCommand_String[i]);
    }
    Print ("\"\n");
    // ! display output
*/
    
    // spawns the executable and creates a usercontext
    // retval is pid (> 0) or errorcode (< 0)
    
    if (Interrupts_Enabled() == false) {
    	Enable_Interrupts();
    }
    
    struct Kernel_Thread *kthread;
    int retval = Spawn(pCommand_String, pExecutable_Name, &kthread);
    
    Disable_Interrupts();
    
    
    Free(pCommand_String);
    Free(pExecutable_Name);
    
    return retval;
}

/*
 * Wait for a process to exit.
 * Params:
 *   state->ebx - pid of process to wait for
 * Returns: the exit code of the process,
 *   or error code (< 0) on error
 */
static int Sys_Wait(struct Interrupt_State* state)
{
	// lacki
    //TODO("Wait system call");
	
	uint_t pid = state->ebx;
	uint_t uExit_Code = 0;


    /* It is only legal for the owner to join */
    //KASSERT(kthread->owner == g_currentThread);
    
    struct Kernel_Thread *kthread = Lookup_Thread(pid);
  
    if (kthread == 0) {
    	Print("Interrupt SYS_WAIT: Not Thread with pid %u found.", pid);
    	return (-1);
    }
    
    
    if (Interrupts_Enabled() == false) {
    	Enable_Interrupts();	
    }

    uExit_Code = Join(kthread);
    
	return uExit_Code;
	
}

/*
 * Get pid (process id) of current thread.
 * Params:
 *   state - processor registers from user mode
 * Returns: the pid of the current thread
 */
static int Sys_GetPID(struct Interrupt_State* state)
{
    //TODO("GetPID system call");
	return g_currentThread->pid;
}


/*
 * Global table of system call handler functions.
 */
const Syscall g_syscallTable[] = {
    Sys_Null,
    Sys_Exit,
    Sys_PrintString,
    Sys_GetKey,
    Sys_SetAttr,
    Sys_GetCursor,
    Sys_PutCursor,
    Sys_Spawn,
    Sys_Wait,
    Sys_GetPID,
};

/*
 * Number of system calls implemented.
 */
const int g_numSyscalls = sizeof(g_syscallTable) / sizeof(Syscall);
