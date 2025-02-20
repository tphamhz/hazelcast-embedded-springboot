Original reference https://docs.hazelcast.com/tutorials/kubernetes-embedded

## Build the jar
mvn clean install

## Create Dockerfile
Remember to pass $JAVA_OPTS

Example

```
FROM arm64v8/openjdk:21-ea-21-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

## Create Docker image
docker build -t tpham/hazelcast-embedded-kubernetes .

## Deploy Hazelcast Cluster
```
export WORK_DIR=$(pwd)
export HAZELCAST_LICENSE_KEY=<HAZELCAST_LICENSE_KEY>
kubectl config set-context --current --namespace <namespace>
```

Create hz-license-key secret

`kubectl create secret generic hz-license-key --from-literal key=$HAZELCAST_LICENSE_KEY`

Create TLS secret
```
kubectl create secret generic hz-certificates \
  --from-file=hazelcast-keystore.p12=$WORK_DIR/deploy_k8s/certs/keystore.p12 \
  --from-file=hazelcast-truststore.p12=$WORK_DIR/deploy_k8s/certs/truststore.p12
```

`cd $WORK_DIR/deploy_k8s`

Create hz-config configuration map from hazelcast.yaml

`kubectl create cm hz-config --from-file=hazelcast.yaml`

Deploy

`kubectl apply -f rbac.yaml`

`kubectl apply -f statefulset.yaml`

To expose WAN service,
`kuebctl apply -f service.yaml`

Validate deployment - sample output
```
> kubectl get pvc | grep persistence
hz-persistence-volume-my-app-0                                    Bound    pvc-5cd0aed9-1943-4e4d-b78a-9e0f3cde33b6   1Gi        RWO            standard       <unset>                 8s
hz-persistence-volume-my-app-1                                    Bound    pvc-99842640-566e-46f7-9549-d61dc2faea41   1Gi        RWO            standard       <unset>                 5s
hz-persistence-volume-my-app-2                                    Bound    pvc-d9453576-d8b7-4808-8973-9f8fde298344   1Gi        RWO            standard       <unset>                 2s
> kubectl get po
NAME       READY   STATUS    RESTARTS   AGE
my-app-0   1/1     Running   0          20s
my-app-1   1/1     Running   0          17s
my-app-2   1/1     Running   0          14s
> kubectl get svc
NAME             TYPE           CLUSTER-IP       EXTERNAL-IP   PORT(S)          AGE
kubernetes       ClusterIP      10.96.0.1        <none>        443/TCP          223d
my-app           ClusterIP      10.109.132.162   <none>        8080/TCP         3d5h
my-app-service   LoadBalancer   10.97.127.39     127.0.0.1     5710:32710/TCP   43m

Check TLS certificate binding on the configured port, i.e WAN 5710
> openssl s_client -connect localhost:5710
Connecting to ::1
CONNECTED(00000005)
Can't use SSL_get_servername
depth=2 C=SG, ST=Singapore, L=Singapore, O=TracyPham, Org, OU=Lab, CN=TPHAM CA
verify error:num=19:self-signed certificate in certificate chain
verify return:1
depth=2 C=SG, ST=Singapore, L=Singapore, O=TracyPham, Org, OU=Lab, CN=TPHAM CA
verify return:1
depth=1 C=SG, ST=Singapore, L=Singapore, O=TracyPham, Org, OU=Lab, CN=TPHAM Intermediate CA
verify return:1
depth=0 C=SG, ST=Singapore, L=Singapore, O=TracyPham, Org, OU=Lab, CN=hz.tpham.com
verify return:1
---
Certificate chain
 0 s:C=SG, ST=Singapore, L=Singapore, O=TracyPham, Org, OU=Lab, CN=hz.tpham.com
   i:C=SG, ST=Singapore, L=Singapore, O=TracyPham, Org, OU=Lab, CN=TPHAM Intermediate CA
   a:PKEY: rsaEncryption, 2048 (bit); sigalg: RSA-SHA256
   v:NotBefore: Aug 28 09:10:00 2024 GMT; NotAfter: Aug 28 09:10:00 2025 GMT
 1 s:C=SG, ST=Singapore, L=Singapore, O=TracyPham, Org, OU=Lab, CN=TPHAM Intermediate CA
   i:C=SG, ST=Singapore, L=Singapore, O=TracyPham, Org, OU=Lab, CN=TPHAM CA
   a:PKEY: rsaEncryption, 2048 (bit); sigalg: RSA-SHA256
   v:NotBefore: Jul 26 07:19:00 2024 GMT; NotAfter: Jul 24 07:19:00 2032 GMT
 2 s:C=SG, ST=Singapore, L=Singapore, O=TracyPham, Org, OU=Lab, CN=TPHAM CA
   i:C=SG, ST=Singapore, L=Singapore, O=TracyPham, Org, OU=Lab, CN=TPHAM CA
   a:PKEY: rsaEncryption, 2048 (bit); sigalg: RSA-SHA256
   v:NotBefore: Jul 26 06:33:00 2024 GMT; NotAfter: Jul 25 06:33:00 2029 GMT
---
```

## If you need to expose the app
```
kubectl create service clusterip my-app --tcp=8080:8080
kubectl port-forward service/my-app 8080:8080
```

Testing
```
$ curl --data "key=key1&value=hazelcast" "localhost:8080/put"
{"value":"hazelcast"}
$ curl "localhost:8080/get?key=key1"
{"value":"hazelcast"}
```

## Deploy Management Center

`cd $WORK_DIR/deploy_k8s`

hz-mancenter-admin-credentials stores Admin credentails for MC console login using Local Security Provider 

hz-mancenter-secrets contains TLS credentials 

```
kubectl create secret generic hz-license-key --from-literal key=$HAZELCAST_LICENSE_KEY
kubectl create cm hz-mancenter-config --from-file=hazelcast-client.yaml=mancenter-hazelcast-client.yaml
kubectl create secret generic hz-mancenter-admin-credentials --from-literal username=admin  --from-literal password=p@ssw0rd 
kubectl create secret generic hz-mancenter-secrets \
  --from-file=keystore.p12=$WORK_DIR/deploy_k8s/certs/keystore.p12 \
  --from-literal keystore_password=hazelcast \
  --from-file=truststore.p12=$WORK_DIR/deploy_k8s/certs/truststore.p12 \
  --from-literal truststore_password=changeit 

kubectl apply -f mancenter-statefulset.yaml
```

To access MC, port forward and access via web browser, login using username/password configured above.
`kubectl port-forward managementcenter-0 8443:8443`

Setup connection to Hazelcast clusters