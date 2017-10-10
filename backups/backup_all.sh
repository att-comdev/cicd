#!/bin/bash

DIR_NAME=`date +%Y%m%d`
sudo mkdir -pv ${DIR_NAME}
cd ${DIR_NAME}

FOLDERS="
/home/
/var/lib/openstack-helm/nfs
"

for i in ${FOLDERS}; do
    bash ../backup.sh ${i}
done

#mirror host should be in the $HOME/.ssh/config file.

scp -r $(hostname)*.squash mirror:/opt/backups/${DIR_NAME}/
