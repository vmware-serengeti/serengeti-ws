#!/bin/bash

echo "generating certificate and private keys"

SERENGETI_CERT_DIR=/opt/serengeti/.certs

if [ ! -d $SERENGETI_CERT_DIR ]; then
  mkdir $SERENGETI_CERT_DIR -p
fi

# generate keystore
keytool -genkey -keyalg rsa \
	-storetype jks \
	-alias serengeti \
	-keypass p@ssw0rd \
	-keystore $SERENGETI_CERT_DIR/serengeti.jks \
	-storepass p@ssw0rd \
	-dname "CN=serengeti, OU=VMware Aurora, O=VMware Inc., L=Palo Alto, S=CA, C=US" \
	-validity 3650

# export certificate
keytool -exportcert -alias serengeti \
	-keypass p@ssw0rd \
	-keystore $SERENGETI_CERT_DIR/serengeti.jks \
	-storepass p@ssw0rd \
	-rfc \
	-file $SERENGETI_CERT_DIR/serengeti.pem

# export to pkcs12 store
keytool -importkeystore \
	-alias serengeti \
	-srckeystore $SERENGETI_CERT_DIR/serengeti.jks \
	-srckeypass p@ssw0rd \
	-srcstorepass p@ssw0rd \
	-destkeystore $SERENGETI_CERT_DIR/serengeti.p12 \
	-deststoretype PKCS12 \
	-destkeypass p@ssw0rd \
	-deststorepass p@ssw0rd

# export private key
openssl pkcs12 -in $SERENGETI_CERT_DIR/serengeti.p12 \
	-passin pass:p@ssw0rd \
	-nocerts \
	-nodes \
	-out $SERENGETI_CERT_DIR/private.pem

# change owner and access mode
chown serengeti.serengeti $SERENGETI_CERT_DIR -R
chmod 400 $SERENGETI_CERT_DIR/*

echo "generated certificate and private key in $SERENGETI_CERT_DIR"
