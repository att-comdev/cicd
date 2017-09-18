#!/bin/bash

apt-get update
apt-get install -y git python-pip python-dev python3-dev python-flake8 \
	libffi-dev libssl-dev build-essential tox docker.io

apt-get install -y default-jre-headless
