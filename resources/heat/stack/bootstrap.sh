#!/bin/bash
echo "====== Installing JAVA packages ======"
apt-get update
apt-get install -y default-jre-headless

echo "====== Updating /etc/hosts ======"
HOSTNAME=$(hostname)
cat << EOF | sudo tee -a /etc/hosts
127.0.1.1 $HOSTNAME
EOF

echo "====== Setup insterfaces ======"
netfile=$(find /etc/network/interfaces.d -name "*.cfg")
for interface in $(ls -1 /sys/class/net | grep ens); do
  if [ $interface != "ens3" ];then
    sudo bash -c "echo 'auto $interface' >> ${netfile}"
    sudo bash -c "echo 'iface $interface inet dhcp' >> ${netfile}"
    sudo ifdown $interface
    sudo ifup $interface
  fi
done
