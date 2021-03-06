#!/bin/sh
# $Id: mbcdd_load,v 1.4 2004/11/03 06:19:49 rubini Exp $
module="mbcdd"
device="mbcdd"
mode="777"

# Group: since distributions do it differently, look for wheel or use staff
if grep -q '^staff:' /etc/group; then
    group="staff"
else
    group="wheel"
fi

/sbin/rmmod $device > /dev/null 2>&1

echo "Load module: $module:"

# invoke insmod with all arguments we got
# and use a pathname, as insmod doesn't look in . by default
/sbin/insmod ./$module.ko $* || exit 1

# retrieve major number
major=$(awk "\$2==\"$module\" {print \$1}" /proc/devices)
echo "Major Number: $major"

# Remove stale nodes and replace them, then give gid and perms
# Usually the script is shorter, it's mbcdd that has several devices in it.

rm -f /dev/${device}
mknod /dev/${device} c $major 0
if [ ! "$?" = 0 ]; then
	echo "Could not create Node"
	exit 1
fi
echo "Node: /dev/$device created, major: $major"
chgrp $group /dev/${device}
chmod $mode  /dev/${device}

