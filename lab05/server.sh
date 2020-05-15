#!/usr/bin/env bash

BUILD_DIR="build/"

SSLServer="SSLServer"
ServerPort=12345
CypherSuite=("TLS_RSA_WITH_AES_128_CBC_SHA" "TLS_DHE_RSA_WITH_AES_128_CBC_SHA")


java -cp "$BUILD_DIR" "$SSLServer" "$ServerPort" "${CypherSuite[@]}"