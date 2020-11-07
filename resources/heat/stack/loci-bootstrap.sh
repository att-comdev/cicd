#!/bin/bash

set -e

echo "127.0.1.1   $(hostname)" >> /etc/hosts

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

# required for docker within docker
mkdir -p /etc/docker
cat << EOF > /etc/docker/daemon.json
{
  "storage-driver": "overlay2"
}
EOF

# git-ssh wrapper
cat << EOF > /usr/bin/git-ssh-wrapper
#!/bin/bash
ssh -i "\$SSH_KEY" "\$@"
EOF

chmod a+x /usr/bin/git-ssh-wrapper
