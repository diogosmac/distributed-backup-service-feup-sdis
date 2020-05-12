#!/usr/bin/env bash
# This script was design to work on ubuntu (with gnome-terminal)

script_name=$0
n_peers=$1
protocol_version=$2

if [ "$#" -ne 2 ]; then
    echo "Usage: $script_name <n_peers_to_launch> <peer_protocol_version>"
    exit 1
fi

for i in $(seq 1 "$n_peers"); do
  access_point="ap$i"
  gnome-terminal -x sh -c "cd ../build/; java peer.Peer $protocol_version $i $access_point 224.0.0 8001 224.0.0 8002 224.0.0 8003"
done
