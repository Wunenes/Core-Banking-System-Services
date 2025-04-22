#!/bin/bash

# Create directories
mkdir -p src/main/resources/certs

# Generate CA key and certificate
openssl genrsa -out src/main/resources/certs/ca.key 4096
openssl req -new -x509 -key src/main/resources/certs/ca.key -out src/main/resources/certs/ca.crt -days 365 -subj "/CN=Account Service CA"

# Generate server key and CSR
openssl genrsa -out src/main/resources/certs/server.key 4096
openssl req -new -key src/main/resources/certs/server.key -out src/main/resources/certs/server.csr -subj "/CN=account-service"

# Sign the server certificate with CA
openssl x509 -req -in src/main/resources/certs/server.csr -CA src/main/resources/certs/ca.crt -CAkey src/main/resources/certs/ca.key -CAcreateserial -out src/main/resources/certs/server.crt -days 365

# Generate client key and CSR
openssl genrsa -out src/main/resources/certs/client.key 4096
openssl req -new -key src/main/resources/certs/client.key -out src/main/resources/certs/client.csr -subj "/CN=account-client"

# Sign the client certificate with CA
openssl x509 -req -in src/main/resources/certs/client.csr -CA src/main/resources/certs/ca.crt -CAkey src/main/resources/certs/ca.key -CAcreateserial -out src/main/resources/certs/client.crt -days 365

# Generate PKCS12 keystore for server
openssl pkcs12 -export -in src/main/resources/certs/server.crt -inkey src/main/resources/certs/server.key -name accountservice -out src/main/resources/keystore.p12 -password pass:changeit

# Cleanup
rm src/main/resources/certs/*.csr
rm src/main/resources/certs/*.srl

echo "TLS certificates generated successfully!"