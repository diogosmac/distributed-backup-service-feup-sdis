#bin/bash

argc=$#
if ((argc < 2))
then
  echo "Usage: $0 <access_point> <test_file>"
  exit 1
fi


access_point=$1
test_file=$2

cd build/
java Application ${access_point} RESTORE ${test_file}
