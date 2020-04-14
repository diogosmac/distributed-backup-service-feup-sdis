#!/usr/bin/env bash

script_name=$0
peer_id=$1

if [ "$#" -ne 1 ]; then
    echo "Usage: $script_name <peer_id>"
    exit 1
fi

access_point="ap$peer_id"
gnome-terminal -x sh -c "cd ../src/; java Application $access_point STATE; sleep 10"