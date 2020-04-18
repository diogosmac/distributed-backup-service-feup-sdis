#!/usr/bin/env bash

script_name=$0
peer_id=$1
file_path=$2
rep_degree=$3

if [ "$#" -ne 3 ]; then
    echo "Usage: $script_name <peer_id> <file_path> <desired_replication_degree>"
    exit 1
fi

access_point="ap$peer_id"
gnome-terminal -x sh -c "cd ../build/; java Application $access_point BACKUP $file_path $rep_degree"