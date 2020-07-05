#bin/bash

argc=$#
if ((argc < 3))
then
  echo "Usage: $0 <access_point> <test_file> <replication_degree>"
  exit 1
fi


access_point=$1
test_file=$2
replication_degree=$3

cd build/
java Application ${access_point} BACKUP ${test_file} ${replication_degree}
