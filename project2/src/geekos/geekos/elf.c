/*
 * ELF executable loading
 * Copyright (c) 2003, Jeffrey K. Hollingsworth <hollings@cs.umd.edu>
 * Copyright (c) 2003, David H. Hovemeyer <daveho@cs.umd.edu>
 * $Revision: 1.29 $
 * 
 * This is free software.  You are permitted to use,
 * redistribute, and modify it as specified in the file "COPYING".
 */

#include <geekos/errno.h>
#include <geekos/kassert.h>
#include <geekos/ktypes.h>
#include <geekos/screen.h>  /* for debug Print() statements */
#include <geekos/pfat.h>
#include <geekos/malloc.h>
#include <geekos/string.h>
#include <geekos/user.h>
#include <geekos/elf.h>


/**
 * From the data of an ELF executable, determine how its segments
 * need to be loaded into memory.
 * @param exeFileData buffer containing the executable file
 * @param exeFileLength length of the executable file in bytes
 * @param exeFormat structure describing the executable's segments
 *   and entry address; to be filled in
 * @return 0 if successful, < 0 on error
 */
int Parse_ELF_Executable(char *exeFileData, ulong_t exeFileLength,
    struct Exe_Format *exeFormat)
{
	// ruap, lacki
    int i=0;
    int iNumber_Of_Segments = 0;

    elfHeader *pElfHeader= (elfHeader*) exeFileData;

    exeFormat->entryAddr = pElfHeader->entry;

    KASSERT(exeFileData!=NULL);
    KASSERT(ELF_MAGIC_NUMBER == (pElfHeader->ident[0]<<24 | pElfHeader->ident[1]<<16 | pElfHeader->ident[2]<<8 | pElfHeader->ident[3]));
    KASSERT(EXE_MAX_SEGMENTS >= pElfHeader->phnum);

    for(i=0; i<(pElfHeader->phnum); i++){
		programHeader *pProgramHeader = (programHeader *)(exeFileData+((*pElfHeader).phoff)+((*pElfHeader).phentsize*i));
		/* only valid segments are counted since the compiler adds an additional segment "GNU_STACK" to the FileData
		 * So if the Size of a program in memory is 0, it's counted as invalid
		 * */	
		
		if ((*pProgramHeader).memSize == 0) {
			continue;	
		}
	
		exeFormat->segmentList[i].lengthInFile = pProgramHeader->fileSize;
		exeFormat->segmentList[i].offsetInFile = pProgramHeader->offset;
		exeFormat->segmentList[i].protFlags = pProgramHeader->flags;
		exeFormat->segmentList[i].sizeInMemory = pProgramHeader->memSize;
		exeFormat->segmentList[i].startAddress = pProgramHeader->vaddr;
		
		iNumber_Of_Segments++;	
    }	
    
    
    exeFormat->numSegments = iNumber_Of_Segments;
    
	return 0;	
}

