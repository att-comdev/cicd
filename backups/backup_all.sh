#!/bin/bash

#Description:
#Script creates backups and push them to the "mirror" host.
#Usage: bash <script_name> </path/file.txt>

#Requirements:
#system packages: squashfs-tools openssh-server
#other requirements:
# - text file with the list of folders to backup (2backup.txt)
# - configured ssh client (rsa keys, .ssh/config)

DIRS_LIST=${1:-2backup.txt}

rtfm(){
echo "
Usage: bash $0 </path/file.txt>
Example: bash $0 2backup.txt >> backup.log
File '2backup.txt' should contain folders, one per line.
# Commented lines will be skipped. Keep it simple.
"
}
input_check(){
    if [ ! "$#" -eq "0" ] && [ ! -f "$1" ]; then
        # script's first parameter should be a file
        echo "ERROR: $1 is not a text file"
        rtfm
    fi
}

prepare(){
    for i in `grep -v '#' $DIRS_LIST`;
        do BACKUP_LIST="$BACKUP_LIST $i"
    done
    echo "dirs to backup: $BACKUP_LIST"
    if [ -z "$BACKUP_LIST" ]; then
        echo "ERROR: empty $DIRS_LIST!!!"
        rtfm && exit 1
    fi

    DIR_NAME=`date +%Y%m%d`
    sudo mkdir -pv ${DIR_NAME}
    sudo chown ${USER}.${USER} ${DIR_NAME}
#    cd ${DIR_NAME}
}

backup(){
    set -x
    BACKUP_NAME=$(hostname)_$(date +%Y%m%d_%H%M%S).squash
    sudo mksquashfs ${BACKUP_LIST} ${DIR_NAME}/${BACKUP_NAME}
    set +x
}

mirror(){
    set -e
    #mirror host should present in $HOME/.ssh/config file.
    #as well as ssh keys etc.
    DEST="$(pwd)/${DIR_NAME}"
    ssh mirror "mkdir -pv ${DEST}"
    scp -r ${DEST}/$(hostname)*.squash mirror:${DEST}/
}

####main#####
input_check
echo "
======= Backup started: `date` ========"
prepare
backup
mirror
echo "
============ Done: `date` =============="
