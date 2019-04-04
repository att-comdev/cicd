#!/bin/bash
echo "====== Installing JAVA packages ======"
apt-get update
apt-get install -y default-jre-headless

echo "====== Updating /etc/hosts ======"
HOSTNAME=$(hostname)
cat << EOF | sudo tee -a /etc/hosts
127.0.0.1 $HOSTNAME
EOF
