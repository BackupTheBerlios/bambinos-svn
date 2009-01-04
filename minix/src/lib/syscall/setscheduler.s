.sect .text
.extern	_setscheduler
.define	setscheduler
.align 2

setscheduler:
	jmp	_setscheduler
