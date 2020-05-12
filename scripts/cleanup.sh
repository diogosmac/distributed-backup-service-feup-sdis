#!/usr/bin/env bash

argc=$#

if ((argc == 3))
then
  echo "argc3"
  peer_id=$1
  rm -rf peer$peer_id
else
  if ((argc == 2))
  then
    echo "argc2"
    rm -rf peer*
  else
    echo "argcNOT2"
    echo "Usage: $0 [<peer_id>]"
    exit 1
  fi
fi
