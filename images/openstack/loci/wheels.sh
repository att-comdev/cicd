OPENSTACK_RELEASE=newton
OPENSTACK_COMPONENT_RELEASE="stable/${OPENSTACK_RELEASE}"
IMAGE_DISTRO=ubuntu
DOCKER_NAMESPACE=gantry

docker rm -f loci-build || true
docker run -d \
  --privileged \
  --name loci-build \
  -v ${HOME}/.docker:/root/.docker:ro \
  -v /var/lib/docker \
  -v $(pwd):/opt/loci \
  docker:17.07.0-dind \
    --storage-driver=overlay
docker exec loci-build sh -cx "apk update; apk add git"

docker exec loci-build docker build https://git.openstack.org/openstack/loci.git \
  --build-arg PROJECT=requirements \
  --build-arg PROJECT_REF=stable/${OPENSTACK_RELEASE} \
  --tag ${DOCKER_NAMESPACE}/loci-requirements:${OPENSTACK_RELEASE}-eol-${IMAGE_DISTRO}
docker exec loci-build docker push ${DOCKER_NAMESPACE}/loci-requirements:${OPENSTACK_RELEASE}-eol-${IMAGE_DISTRO}

