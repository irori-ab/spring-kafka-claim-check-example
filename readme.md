# Spring Kafka Claim Check example project

## Build with maven

```
./mvnw clean install
```

## Run locally

### Pre-requisites
* Running Kafka cluster (Assuming Strimzi mTLS for examples)
* A created Azure Storage account with valid SAS token in a file `<my-token>.sastoken`
    - See: https://github.com/irori-ab/claim-check-interceptors#azure-blob-storage
    

### Fetch Strimzi Kafka user/cluster certs
```
KAFKA_CLUSTER=kafka-sandbox
KAFKA_USER=test-user-1
KUBECONFIG=... # if non-default context

# truststore, truststore password
kubectl get secret -n kafka ${KAFKA_CLUSTER}-cluster-ca-cert -o=go-template='{{index .data "ca.p12"}}' | base64 -d > cluster-truststore.p12
kubectl get secret -n kafka ${KAFKA_CLUSTER}-cluster-ca-cert -o=go-template='{{index .data "ca.password"}}' | base64 -d > cluster-truststore.password
# keystore, keystore password
kubectl get secret -n kafka ${KAFKA_USER} -o=go-template='{{index .data "user.p12"}}' | base64 -d > user-keystore.p12
kubectl get secret -n kafka ${KAFKA_USER} -o=go-template='{{index .data "user.password"}}' | base64 -d > user-keystore.password
```

Verify:
```
keytool -list -v -keystore cluster-truststore.p12 -storepass "$(cat cluster-truststore.password)" -storetype PKCS12
keytool -list -v -keystore user-keystore.p12 -storepass "$(cat user-keystore.password)" -storetype PKCS12
```

### Create local config spring profile

Run the below, then replace the `<placeholder>` parts with values applicable to your environment:
```
cat << EOF > src/main/resources/application-localdev.yaml
spring:
  kafka:
    bootstrap-servers: <my-bootstrap-url>:9094
    ssl:
      key-store-location: "file:/$(pwd -L)/user-keystore.p12"
      key-store-password: "$(cat user-keystore.password)"
      trust-store-location: "file:/$(pwd -L)/cluster-truststore.p12"
      trust-store-password: "$(cat cluster-truststore.password)"
    consumer:
      group-id: <my-consumer-group>
    properties:
      claimcheck.backend.class: se.irori.kafka.claimcheck.azure.AzureBlobStorageClaimCheckBackend
      azure.blob.storage.account.endpoint: https://<my-storage-account>.blob.core.windows.net
      azure.blob.storage.account.sastoken.from: file:/$(pwd -L)/<my-sas-token-file>.sastoken
EOF
```

### Run the example app
Start with Maven `localdev` profile (maps to `application-localdev.yaml`):
```
./mvnw spring-boot:run -Dspring-boot.run.profiles=localdev
```

Open: http://localhost:8080/swagger-ui/index.html

Send and receive some messages using the "Try it out" buttons.

## Claim check example
```
seq 1 1000000 > file.txt
ls -lh file.txt 
# -rw-r--r--  1 ...  ...   6,6M 20 Maj 17:49 file.txt

curl -X 'POST' \
  'http://localhost:8080/produce/test-topic-1' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d @file.txt
  
curl http://localhost:8080/consume/test-topic-1

# huge response :)
```

## Inspect raw Kafka-topic with kcat
See [kcat](https://github.com/edenhill/kcat).

*Note*: raw kafka topic only has the claim check header.

```
# Get keys and certs
kubectl get secret -n kafka $KAFKA_CLUSTER-cluster-ca-cert -o=go-template='{{index .data "ca.crt"}}' | base64 -d > $KAFKA_CLUSTER-truststore.crt
kubectl get secret -n kafka $KAFKA_USER -o=go-template='{{index .data "user.crt"}}' | base64 -d > $KAFKA_USER-keystore-user.crt
kubectl get secret -n kafka $KAFKA_USER -o=go-template='{{index .data "user.key"}}' | base64 -d > $KAFKA_USER-keystore-user.key

# Inspect topic (replace <placeholders>)
kcat -C -b <bootstrapUrl> -X security.protocol=ssl -X ssl.ca.location=$KAFKA_CLUSTER-truststore.crt -X ssl.certificate.location=$KAFKA_USER-keystore-user.crt -X ssl.key.location=$KAFKA_USER-keystore-user.key -t <topic> -f 'Headers: %h: Message value: %s\n'
```

## Troubleshoot Jib builds

Configure for log in to GCR (or just `docker login` to other image registry):
```
gcloud auth configure-docker
mvn compile jib:dockerBuild
docker tag IMAGE_NAME:0.0.1-SNAPSHOT gcr.io/PROJECT/IMAGE_NAME:localtest
docker push gcr.io/PROJECT/IMAGE_NAME:localtest
```

# References
* https://github.com/irori-ab/claim-check-interceptors
* https://github.com/edenhill/kcat
* https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-kafka
* https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-application-properties.html#common-application-properties-integration
* https://spring.io/guides/tutorials/rest/
