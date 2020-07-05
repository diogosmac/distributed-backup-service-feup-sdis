#bin/bash

argc=$#
if ((argc < 1))
then
  echo "Usage: $0 <access_point>"
  exit 1
fi

access_point=$1

cd build/
java Application ${access_point} STATE
