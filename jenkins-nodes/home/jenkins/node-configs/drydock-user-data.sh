#!/bin/bash
apt-get update -y
apt-get install -y default-jdk docker.io

#python dependencies
apt-get install -y python3-dev tox \
        git \
        netbase \
        python3-minimal \
        python3-setuptools \
        python3-pip \
        python3-dev \
        ca-certificates \
        gcc \
        g++ \
        make \
        libffi-dev \
        libssl-dev
