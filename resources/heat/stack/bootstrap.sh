#!/bin/bash
echo "====== Installing JAVA packages ======"
apt-get update
apt-get install -y default-jre-headless

echo "====== Updating /etc/hosts ======"
cmd='/127.0.1.1/!p;$a127.0.1.1'
cmd="${cmd} $(hostname)"
sudo sed -i -n -e "${cmd}" /etc/hosts
