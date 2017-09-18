#!/bin/bash
apt-get update -y
apt-get install -y docker.io curl

fix_slave(){
    echo "fix hostname and resolv.conf:"
    if ! grep $(hostname) /etc/hosts; then
        echo "127.0.1.1  $(hostname)" | tee -a /etc/hosts
    fi
}

apt-get install -y default-jdk

# push generic files to Artifactory
keytool -import -alias artifactory -file ca-crt.pem -keystore /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/cacerts -storepass changeit -noprompt

# push Docker images to Artifactory
mkdir -p /etc/docker/certs.d/{{ .Values.artifactory.url }}:{{ .Values.artifactory.url.node }}
sudo mv ca-crt.pem /etc/docker/certs.d/{{ .Values.artifactory.url }}:{{ .Values.artifactory.url.node }}/ca.crt

echo 'Testing $message' > /home/ubuntu/done.txt
