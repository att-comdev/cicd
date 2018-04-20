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

realpath() {
    [[ $1 = /* ]] && echo "$1" || echo "$PWD/${1#./}"
}
SCRIPT_DIR=$(realpath "$(dirname "${0}")")
TEMPLATES_DIR="${SCRIPT_DIR}/../templates/"

openstack keypair show osh-vm-key || \
  openstack keypair create --public-key ${HOME}/.ssh/id_rsa.pub osh-vm-key
openstack orchestration template validate -t ${TEMPLATES_DIR}./030-osh-cluster.yaml
openstack stack create --wait -t ${TEMPLATES_DIR}./030-osh-cluster.yaml osh-cluster
