#!/bin/bash

set -e

echo "127.0.1.1   $(hostname)" >> /etc/hosts

# required for docker within docker
mkdir -p /etc/docker
cat << EOF > /etc/docker/daemon.json
{
  "storage-driver": "overlay2"
}
EOF

# add internal gerrit to known hosts
ssh-keyscan -p 29418 10.24.20.18 >> /home/ubuntu/.ssh/known_hosts

# git-ssh wrapper
cat << EOF > /usr/bin/git-ssh-wrapper
#!/bin/bash
ssh -i "\$SSH_KEY" "\$@"
EOF

chmod a+x /usr/bin/git-ssh-wrapper


apt-get update
apt-get install -y docker.io
