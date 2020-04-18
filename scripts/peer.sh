#!/usr/bin/env bash

protocol_version=$1
peer_id=$2
service_access_point=$3
mc_address=$4
mc_port=$5
mdb_address=$6
mdb_port=$7
mdr_address=$8
mdr_port=$9

java Peer ${protocol_version} ${peer_id} ${service_access_point} ${mc_address} ${mc_port} ${mdb_address} ${mdb_port} ${mdr_address} ${mdr_port}
