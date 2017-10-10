#!/bin/bash

ARCHIVE_NAME=$(hostname)_"$(basename $1)"_$(date +%Y%m%d_%H%M%S).squash


if [ -z "$1" ]; then
    echo "USAGE: $0 folder_path"
    echo "Example: $0 jenkins/"
    echo "It will create squashfs image of the folder"
fi

echo ${ARCHIVE_NAME}

sudo mksquashfs "$1" ${ARCHIVE_NAME}

echo Done
