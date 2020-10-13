#!/bin/bash
echo "====== Installing JAVA packages ======"
apt-get update
apt-get install -y default-jre-headless iptables-persistent

echo "====== Updating /etc/hosts ======"
HOSTNAME=$(hostname)
cat << EOF | sudo tee -a /etc/hosts
127.0.1.1 $HOSTNAME
EOF

echo "====== Setup insterfaces ======"
netfile=$(find /etc/network/interfaces.d -name "*.cfg")
for interface in $(ls -1 /sys/class/net | grep ens); do
  if [ $interface != "ens3" ];then
    bash -c "echo 'auto $interface' >> ${netfile}"
    bash -c "echo 'iface $interface inet dhcp' >> ${netfile}"
    ifdown $interface
    ifup $interface
  fi
done

iptables -A FORWARD -i ens4 -o ens3 -j ACCEPT
iptables -A FORWARD -i ens3 -o ens4 -m state --state ESTABLISHED,RELATED -j ACCEPT
iptables -t nat -A POSTROUTING -o ens3 -j MASQUERADE
iptables-save > /etc/iptables/rules.v4