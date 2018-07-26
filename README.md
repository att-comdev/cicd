# CI/CD

This repository is a collection of various scripts and tools that will be used to create some basic pipelines to test and deploy OpenStack-helm and Airship components. It is what AT&T may use for CI in the Dev environment.

Sincerely,

AT&T Integrated Cloud Community Development Team

# How it Works

There is a SuperSeed groovy job that is triggered with each patchset into gerrit.  It will checkout the patchset and determine if there is a seed.groovy file that it needs to run to create a Jenkins pipeline in the Development Jenkins.  After the pipelines have been tested with the Jenkins Development environment, once the patchset is merged, they will be created in the Jenkins Production environment.

# Configurations 

There are a list of configurations and credentials that are needed in order to run successfully.

We will be putting these configurations [here](https://github.com/att-comdev/cicd/tree/master/src/att/comdev/cicd/config).

# Shared Libraries

Folder `vars` is used for shared functions across pipelines.

See more information for this plugin [here](https://jenkins.io/doc/book/pipeline/shared-libraries).

# Docker image build pipelines

All Docker image pipelines can be found under the images directory of cicd.  Our goal is to have the development team control their tests and build process within their own repository, but giving the ability to the CI/CD team to override specific values for Release Management purposes.  In order to do this we work closely with the development team to have entrypoints that the Jenkins pipelines can call that are the same throughout OpenStack-Helm and Airship components.

# Image Tagging

We tag images with the commit_id.BUILD_TIMESTAMP.  We do this in order to tie back any merged code with the image that was built from that code.

# Image Labels

We use the Label Schema convention found [here](http://label-schema.org/rc1/).

vcs-url - Gerrit URL that this image was built from
vcs-ref - Commit ID that this image was built from
vcs-vendor - AT&T

# Helm packaging pipelines

Our Helm pipelines can be found under charts directory and all use helm-toolkit from the current master of https://github.com/openstack/openstack-helm-infra.
