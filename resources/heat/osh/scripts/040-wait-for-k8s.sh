#!/usr/bin/env bash

# Copyright 2018 The OpenStack-Helm Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -ex

FLOATING_IP=$(openstack stack output show \
    osh-cluster \
    floating_ip \
    -f value -c output_value)

function wait_for_ssh_port {
  # Default wait timeout is 300 seconds
  set +x
  end=$(date +%s)
  if ! [ -z $2 ]; then
   end=$((end + $2))
  else
   end=$((end + 300))
  fi
  while true; do
      # Use Nmap as its the same on Ubuntu and RHEL family distros
      nmap -Pn -p22 $1 | awk '$1 ~ /22/ {print $2}' | grep -q 'open' && \
          break || true
      sleep 1
      now=$(date +%s)
      [ $now -gt $end ] && echo "Could not connect to $1 port 22 in time" && exit -1
  done
  set -x
}
wait_for_ssh_port $FLOATING_IP
ssh-keyscan "$FLOATING_IP" >> ~/.ssh/known_hosts

function wait_for_command_sucess {
  # Default wait timeout is 600 seconds
  set +x
  end=$(date +%s)
  if ! [ -z $2 ]; then
   end=$((end + $2))
  else
   end=$((end + 1200))
  fi
  while true; do
      # Use Nmap as its the same on Ubuntu and RHEL family distros
      ssh -i ${HOME}/.ssh/id_rsa ubuntu@${FLOATING_IP} "${1}"  && \
          break || true
      sleep 1
      now=$(date +%s)
      [ $now -gt $end ] && echo "Could not run $1 on ${FLOATING_IP} in time" && exit -1
  done
  set -x
}

function run_command {
  ssh -i ${HOME}/.ssh/id_rsa ubuntu@${FLOATING_IP} "cd /opt/openstack-helm; ${1}"
}

wait_for_command_sucess '[ "x$(systemctl is-active cloud-final)" == "xactive" ]'
wait_for_command_sucess 'helm version'
wait_for_command_sucess '[ $(kubectl get nodes -o name | wc -l) -eq 3 ]'
run_command 'kubectl get nodes -o wide'
run_command 'helm ls --all'
