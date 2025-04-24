#!/bin/bash

# Set up directory
OUTPUT_DIR="client_server_certs"
mkdir -p $OUTPUT_DIR
echo "Created output directory: $OUTPUT_DIR"

# Generate CA key and certificate
echo "Generating CA key and certificate..."
openssl genrsa -out $OUTPUT_DIR/ca.key 2048
if [ ! -f "$OUTPUT_DIR/ca.key" ]; then
    echo "ERROR: Failed to create ca.key"
    exit 1
else
    echo "✓ Created CA key: $OUTPUT_DIR/ca.key"
fi

# Fix for Windows Git Bash path issues - use double quotes for subject
openssl req -x509 -new -nodes -key $OUTPUT_DIR/ca.key -sha256 -days 1825 -out $OUTPUT_DIR/ca.crt \
    -subj "//CN=MyCertificateAuthority"
if [ ! -f "$OUTPUT_DIR/ca.crt" ]; then
    echo "ERROR: Failed to create ca.crt"
    exit 1
else
    echo "✓ Created CA certificate: $OUTPUT_DIR/ca.crt"
fi

# Generate server key and CSR
echo "Generating server key and certificate..."
openssl genrsa -out $OUTPUT_DIR/server.key 2048
if [ ! -f "$OUTPUT_DIR/server.key" ]; then
    echo "ERROR: Failed to create server.key"
    exit 1
else
    echo "✓ Created server key: $OUTPUT_DIR/server.key"
fi

# Simplified subject for Windows Git Bash
openssl req -new -key $OUTPUT_DIR/server.key -out $OUTPUT_DIR/server.csr \
    -subj "//CN=server"

# Sign the server CSR with the CA
openssl x509 -req -in $OUTPUT_DIR/server.csr -CA $OUTPUT_DIR/ca.crt -CAkey $OUTPUT_DIR/ca.key \
    -CAcreateserial -out $OUTPUT_DIR/server.crt -days 825 -sha256
if [ ! -f "$OUTPUT_DIR/server.crt" ]; then
    echo "ERROR: Failed to create server.crt"
    exit 1
else
    echo "✓ Created server certificate: $OUTPUT_DIR/server.crt"
fi

# Generate client key and CSR
echo "Generating client key and certificate..."
openssl genrsa -out $OUTPUT_DIR/client.key 2048
if [ ! -f "$OUTPUT_DIR/client.key" ]; then
    echo "ERROR: Failed to create client.key"
    exit 1
else
    echo "✓ Created client key: $OUTPUT_DIR/client.key"
fi

# Simplified subject for Windows Git Bash
openssl req -new -key $OUTPUT_DIR/client.key -out $OUTPUT_DIR/client.csr \
    -subj "//CN=client"

# Sign the client CSR with the CA
openssl x509 -req -in $OUTPUT_DIR/client.csr -CA $OUTPUT_DIR/ca.crt -CAkey $OUTPUT_DIR/ca.key \
    -CAcreateserial -out $OUTPUT_DIR/client.crt -days 825 -sha256
if [ ! -f "$OUTPUT_DIR/client.crt" ]; then
    echo "ERROR: Failed to create client.crt"
    exit 1
else
    echo "✓ Created client certificate: $OUTPUT_DIR/client.crt"
fi

# Create PKCS12 keystore for server (optional)
echo "Creating server keystore..."
openssl pkcs12 -export -in $OUTPUT_DIR/server.crt -inkey $OUTPUT_DIR/server.key \
    -out $OUTPUT_DIR/server.p12 -name serverkey -passout pass:changeit
if [ ! -f "$OUTPUT_DIR/server.p12" ]; then
    echo "ERROR: Failed to create server.p12"
    exit 1
else
    echo "✓ Created server keystore: $OUTPUT_DIR/server.p12"
fi

# Cleanup CSR files
rm $OUTPUT_DIR/*.csr
if [ -f "$OUTPUT_DIR/ca.srl" ]; then
    rm $OUTPUT_DIR/ca.srl
fi
echo "Cleaned up temporary files"

# Verify all required files exist
echo "----- VERIFICATION OF GENERATED FILES -----"
echo "The following files have been generated:"
ls -la $OUTPUT_DIR/

# Print file types for verification
echo "----- FILE TYPE VERIFICATION -----"
# shellcheck disable=SC2231
for file in $OUTPUT_DIR/*.crt $OUTPUT_DIR/*.key; do
    echo "File: $file"
    file "$file"
done

echo "----- CERTIFICATE INFO -----"
echo "CA Certificate Info:"
openssl x509 -in $OUTPUT_DIR/ca.crt -text -noout | grep -E "Subject:|Issuer:|Validity" -A 2

echo "Server Certificate Info:"
openssl x509 -in $OUTPUT_DIR/server.crt -text -noout | grep -E "Subject:|Issuer:|Validity" -A 2

echo "Client Certificate Info:"
openssl x509 -in $OUTPUT_DIR/client.crt -text -noout | grep -E "Subject:|Issuer:|Validity" -A 2

echo "----- COMPLETED SUCCESSFULLY -----"
echo "Files for client TLS connection:"
echo "1. $OUTPUT_DIR/ca.crt - CA certificate to verify server"
echo "2. $OUTPUT_DIR/client.key - Client private key"
echo "3. $OUTPUT_DIR/client.crt - Client certificate"