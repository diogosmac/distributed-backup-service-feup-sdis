#!/usr/bin/env bash

BUILD_DIR="build/"

SSLServer="SSLServer"
ServerPort=12345
CypherSuite=("TLS_RSA_WITH_AES_128_CBC_SHA" "TLS_DHE_RSA_WITH_AES_128_CBC_SHA")

KeyStore="server.keys"
KeyStore_PW="123456"

TrustStore="truststore"
TrustStore_PW="123456"

java -Djavax.net.ssl.keyStore="$KeyStore" -Djavax.net.ssl.keyStorePassword="$KeyStore_PW" \
     -Djavax.net.ssl.trustStore="$TrustStore" -Djavax.net.ssl.trustStorePassword="$TrustStore_PW" \
     -cp "$OUT_DIR" "$SSLServer" "$ServerPort" "${CypherSuite[@]}"