#!/usr/bin/env bash

script_name=$0
peer_id=$1
final_space=$2

if [ "$#" -ne 2 ]; then
    echo "Usage: $script_name <peer_id> <desired_space_usage>"
    exit 1
fi

access_point="ap$peer_id"
gnome-terminal -x sh -c "cd ../build/; java Application $access_point RECLAIM $final_space"