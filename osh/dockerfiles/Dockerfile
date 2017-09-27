FROM docker.io/ubuntu:16.04
ENV KOLLA_VERSION="3.0.3" \
    KOLLA_IMAGE_TAG="newton" \
    KOLLA_IMAGE_MAINTAINER="Pete Birley (pete@port.direct)" \
    OPENSTACK_SOURCE="git" \
    OPENSTACK_SOURCE_ROOT="https://git.openstack.org/openstack" \
    OPENSTACK_VERSION="stable/newton"
RUN set -x \
    && apt-get update \
    && apt-get dist-upgrade -y \
    && apt-get install -y --no-install-recommends \
        git \
        python-pip \
        ca-certificates \
        docker.io \
        build-essential \
        python-dev \
        python-pip \
        python-virtualenv \
        libssl-dev \
    && pip install -U pip \
    && pip install -U wheel setuptools \
    && pip install tox \
    && git clone http://git.openstack.org/openstack/kolla.git /opt/kolla \
    && cd /opt/kolla \
    && git checkout tags/$KOLLA_VERSION \
    && tox -egenconfig \
    && mkdir -p /etc/kolla \
    && mv etc/kolla/kolla-build.conf /etc/kolla/ \
    && rm -rf  .tox \
    && pip install . -r /opt/kolla/requirements.txt \
    && pip install crudini
RUN set -x \
    && KOLLA_CONFIG="/etc/kolla/kolla-build.conf" \
    && crudini --set ${KOLLA_CONFIG} DEFAULT base "ubuntu" \
    && crudini --set ${KOLLA_CONFIG} DEFAULT base_image "docker.io/ubuntu" \
    && crudini --set ${KOLLA_CONFIG} DEFAULT base_tag "16.04" \
    && crudini --set ${KOLLA_CONFIG} DEFAULT base_arch "x86_64" \
    && crudini --set ${KOLLA_CONFIG} DEFAULT namespace "kolla" \
    && crudini --set ${KOLLA_CONFIG} DEFAULT install_type "source" \
    && crudini --set ${KOLLA_CONFIG} DEFAULT threads "1" \
    && crudini --set ${KOLLA_CONFIG} DEFAULT tag "${KOLLA_IMAGE_TAG}" \
    && mkdir -p /var/lib/kolla \
    && crudini --set ${KOLLA_CONFIG} DEFAULT work_dir "/var/lib/kolla" \
    && crudini --set ${KOLLA_CONFIG} DEFAULT maintainer "${KOLLA_IMAGE_MAINTAINER}" \
    && for PARAM in type location reference; do \
        sed -i "s/^#${PARAM}/${PARAM}/g" ${KOLLA_CONFIG}; \
       done \
    && crudini --get ${KOLLA_CONFIG} | while read SECTION; do \
        (LOCATION=$(crudini --get /etc/kolla/kolla-build.conf ${SECTION} location 2> /dev/null ) && \
        echo $LOCATION | grep -q ^\$tarballs_base/ && ( \
          crudini --set ${KOLLA_CONFIG} ${SECTION} type ${OPENSTACK_SOURCE}; \
          LOCATION=${LOCATION#\$tarballs_base/}; \
          crudini --set ${KOLLA_CONFIG} ${SECTION} location "${OPENSTACK_SOURCE_ROOT}/${LOCATION%/*}"; \
          crudini --set ${KOLLA_CONFIG} ${SECTION} reference "${OPENSTACK_VERSION}"; \
          ) || true) \
    done
