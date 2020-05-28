#bin/bash

argc=$#
if ((argc < 3))
then
  echo "Usage: $0 <port> <known_address> <known_port>"
  exit 1
fi


port=$1
known_address=$2
known_port=$3

cd build/
java chord.ChordNode ${port} ${known_address} ${known_port}
