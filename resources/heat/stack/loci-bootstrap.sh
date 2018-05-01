#!/bin/bash

set -e

echo "127.0.1.1   $(hostname)" >> /etc/hosts

# git-ssh wrapper
cat << EOF > /usr/bin/git-ssh-wrapper
#!/bin/bash
ssh -i "\$SSH_KEY" "\$@"
EOF

chmod a+x /usr/bin/git-ssh-wrapper

apt-get update
apt-get install -y docker.io
