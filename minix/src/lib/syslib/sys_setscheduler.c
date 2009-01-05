#include "syslib.h"

PUBLIC int sys_setscheduler(int proc, int policy)
{
  message m;

  m.m1_i1 = proc;
  m.m1_i2 = policy;
  return(_taskcall(SYSTASK, SYS_SETSCHEDULER, &m));
}
