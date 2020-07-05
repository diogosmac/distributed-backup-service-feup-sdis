#bin/bash

argc=$#
if ((argc < 2))
then
  echo "Usage: $0 <public-ip/localhost> <port>"
  exit 1
fi

address=$1
port=$2

cd build/
java chord.ChordNode ${address} ${port}
