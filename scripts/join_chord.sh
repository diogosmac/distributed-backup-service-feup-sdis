#bin/bash

argc=$#
if ((argc < 4))
then
  echo "Usage: $0 <public-ip/localhost> <port> <known_address> <known_port>"
  exit 1
fi


address=$1
port=$2
known_address=$3
known_port=$4

cd build/
java chord.ChordNode ${address} ${port} ${known_address} ${known_port}
