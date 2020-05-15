#!/usr/bin/env bash

BUILD_DIR="build/"

SSLClient="SSLClient"
Host="localhost"
Port=12345
CypherSuite=("TLS_RSA_WITH_AES_128_CBC_SHA" "TLS_DHE_RSA_WITH_AES_128_CBC_SHA")


nArgs="$#"

if [[ nArgs -lt 1 || nArgs -gt 3 ]]; then
  echo "Usage: client.sh <oper> <opnd>*"
  echo "Possible <oper>: register | lookup | reset | close"
  exit 1
fi

if [[ "$#" -eq 1 ]]; then
    java -cp "$BUILD_DIR" "$SSLClient" "$Host" "$Port" "$1" "${CypherSuite[@]}"
elif [[ "$#" -eq 2 ]]; then
    java -cp "$BUILD_DIR" "$SSLClient" "$Host" "$Port" "$1" "$2" "${CypherSuite[@]}"
elif [[ "$#" -eq 3 ]]; then
    java -cp "$BUILD_DIR" "$SSLClient" "$Host" "$Port" "$1" "$2" "$3" "${CypherSuite[@]}"
fi
