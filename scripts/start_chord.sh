#bin/bash

argc=$#
if ((argc < 1))
then
  echo "Usage: $0 <port>"
  exit 1
fi


port=$1

cd build/
java chord.ChordNode ${port}
