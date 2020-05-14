#!/usr/bin/env bash

BUILD_DIR="build/"

SSLServer="SSLServer"
ServerPort=12345
CypherSuite=("TLS_RSA_WITH_AES_128_CBC_SHA" "TLS_DHE_RSA_WITH_AES_128_CBC_SHA")

KeyStore="keys/server.keys"
KeyStore_PW="123456"

TrustStore="keys/truststore"
TrustStore_PW="123456"

java -Djavax.net.ssl.keyStore="$KeyStore" -Djavax.net.ssl.keyStorePassword="$KeyStore_PW" \
     -Djavax.net.ssl.trustStore="$TrustStore" -Djavax.net.ssl.trustStorePassword="$TrustStore_PW" \
     -cp "$BUILD_DIR" "$SSLServer" "$ServerPort" "${CypherSuite[@]}"