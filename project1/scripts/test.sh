argc=$#

if ((argc < 2))
then
	echo "Usage: $0 <peer_ap> BACKUP|RESTORE|DELETE|RECLAIM|STATE [<opnd_1> [<opnd_2>]]"
	exit 1
fi

peer_ap=$1
operation=$2

case $operation in
BACKUP)
	if(( argc != 4 ))
	then
		echo "Usage: $0 <peer_ap> BACKUP <filename> <rep degree>"
		exit 1
	fi
	opnd_1=$3
	opnd_2=$4
	;;
RESTORE)
	if(( argc != 3 ))
	then
		echo "Usage: $0 <peer_app> RESTORE <filename>"
	fi
	opnd_1=$3
	opnd_2=""
	;;
DELETE)
	if(( argc != 3 ))
	then
		echo "Usage: $0 <peer_app> DELETE <filename>"
		exit 1
	fi
	opnd_1=$3
	opnd_2=""
	;;
RECLAIM)
	if(( argc != 3 ))
	then
		echo "Usage: $0 <peer_app> RECLAIM <max space>"
		exit 1
	fi
	opnd_1=$3
	opnd_2=""
	;;
STATE)
	if(( argc != 2 ))
	then
		echo "Usage: $0 <peer_app> STATE"
		exit 1
	fi
	opnd_1=""
	opnd_2=""
	;;
*)
	echo "Usage: $0 <peer_ap> BACKUP|RESTORE|DELETE|RECLAIM|STATE [<opnd_1> [<opnd_2>]]"
	exit 1
	;;
esac

java Application ${peer_ap} ${operation} ${opnd_1} ${opnd_2}
