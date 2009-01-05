/* The kernel call implemented in this file:
 *   m_type:	SYS_SETSCHEDULER
 *
 * The parameters for this kernel call are:
 *    m1_i1:	PR_ENDPT   	process number to change priority
 *    m1_i2:	PR_POLICY	the new policy
 */

#include "../system.h"
#include <minix/type.h>
#include <sys/resource.h>

#if USE_SETSCHEDULER

PUBLIC int do_setscheduler(message *m_ptr) {
	/* Change process priority or stop the process. */
	int proc_nr, policy, new_q;
	register struct proc *rp;

	/* Extract the message parameters and do sanity checking. */
	if (!isokendpt(m_ptr->PR_ENDPT, &proc_nr))
		return EINVAL;
	if (iskerneln(proc_nr)) return (EPERM);
	policy = m_ptr->PR_SCHED_POLICY;
	rp = proc_addr(proc_nr);

	lock_dequeue(rp);
	rp->p_scheduler = policy;
	if (!rp->p_rts_flags)
		lock_enqueue(rp);

	/* return (OK); */
	return (rp->p_scheduler);

}

#endif /* USE_SETSCHEDULER */

