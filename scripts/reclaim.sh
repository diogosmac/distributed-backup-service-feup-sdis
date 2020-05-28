#bin/bash

argc=$#
if ((argc < 2))
then
  echo "Usage: $0 <access_point> <size>"
  exit 1
fi


access_point=$1
size=$2

cd build/
java Application ${access_point} RECLAIM ${size}
