.sect .text
.extern	_getscheduler
.define	getscheduler
.align 2

getscheduler:
	jmp	_getscheduler
