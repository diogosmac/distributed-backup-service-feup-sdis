#!/usr/bin/env bash

argc=$#

if ((argc == 1))
then
  peer_id=$1
  rm -rf peer_$peer_id
else
  if ((argc == 0))
  then
    rm -rf peer_*
  else
    echo "Usage: $0 [<peer_id>]"
    exit 1
  fi
fi
