
if [ -z "$*" ]; then 
	round=3
	schedround=2
else
	round=1
	fixedsched=$1
	schedround=3

fi

sched=0
while [ "$sched" -lt $round ]
do
	if [ ! -z $fixedsched ]; then
		sched=$fixedsched
	fi

	case $sched in
		0)name=F_OTHER;;
		1)name=F_FIFO;;
		2)name=F_RR;;
		*)echo "Illegal scheduling policy"; exit 1 ;;
	esac


	i=0
	while [ "$i" -lt $schedround ]
	do
		eval test -f $name$i || ln -s test_sched_fib $name$i 
		echo Start Prozess $name$i $sched
		eval time /usr/src/test/$name$i 46 $sched > $name"$1".txt 2>&1 & 
		sleep 1
		i=`expr $i + 1`
		#rm -f F_FIFO*
	done
	sched=`expr $sched + 1`

done
