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

OS_PROJECT=admin
openstack quota set ${OS_PROJECT} --cores "$(($(grep -c ^processor /proc/cpuinfo) * 2))"
openstack quota set ${OS_PROJECT} --ram "$(($(($(awk '/^MemTotal/ { print $(NF-1) }' /proc/meminfo) / 1024)) * 75/100 ))"
openstack quota set ${OS_PROJECT} --instances 64
openstack quota set ${OS_PROJECT} --secgroups 128
openstack quota set ${OS_PROJECT} --floating-ips 256
openstack quota set ${OS_PROJECT} --ports -1
openstack quota set ${OS_PROJECT} --networks 128
openstack quota set ${OS_PROJECT} --subnets 256
openstack quota set ${OS_PROJECT} --routers 64
openstack quota show ${OS_PROJECT}
