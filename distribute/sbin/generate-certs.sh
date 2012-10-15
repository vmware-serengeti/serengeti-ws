#!/bin/bash

echo "generating certificate and private keys"

SERENGETI_CERT_DIR=/opt/serengeti/.certs

if [ $# != 1 ];then
  echo "Usage: $0 keystore_password"
fi

KEYSTORE_PWD=$1


if [ ! -d $SERENGETI_CERT_DIR ]; then
  mkdir $SERENGETI_CERT_DIR -p
fi

# remove stale certificates
rm $SERENGETI_CERT_DIR/* -f

# generate keystore
keytool -genkey -keyalg rsa \
	-storetype jks \
	-alias serengeti \
	-keypass $KEYSTORE_PWD \
	-keystore $SERENGETI_CERT_DIR/serengeti.jks \
	-storepass $KEYSTORE_PWD \
	-dname "CN=serengeti, OU=VMware Aurora, O=VMware Inc., L=Palo Alto, S=CA, C=US" \
	-validity 3650

# export certificate
keytool -exportcert -alias serengeti \
	-keypass $KEYSTORE_PWD \
	-keystore $SERENGETI_CERT_DIR/serengeti.jks \
	-storepass $KEYSTORE_PWD \
	-rfc \
	-file $SERENGETI_CERT_DIR/serengeti.pem

# export to pkcs12 store
keytool -importkeystore \
	-alias serengeti \
	-srckeystore $SERENGETI_CERT_DIR/serengeti.jks \
	-srckeypass $KEYSTORE_PWD \
	-srcstorepass $KEYSTORE_PWD \
	-destkeystore $SERENGETI_CERT_DIR/serengeti.p12 \
	-deststoretype PKCS12 \
	-destkeypass $KEYSTORE_PWD \
	-deststorepass $KEYSTORE_PWD

# export private key
openssl pkcs12 -in $SERENGETI_CERT_DIR/serengeti.p12 \
	-passin pass:$KEYSTORE_PWD \
	-nocerts \
	-nodes \
	-out $SERENGETI_CERT_DIR/private.pem

# change owner and access mode
chown serengeti.serengeti $SERENGETI_CERT_DIR -R
chmod 400 $SERENGETI_CERT_DIR/*

echo "generated certificate and private key in $SERENGETI_CERT_DIR"
