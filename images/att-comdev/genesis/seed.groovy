JOB_FOLDER="images/att-comdev/genesis"
JOB_NAME="BuildGenesis"

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {

  displayName('Build Genesis')
  description('\nThis job is supposed to build ISO image for '+
              'Genesis host, and boot it.')

  parameters {
    //choiceParam('UPLOAD_PACKAGES', ['false', 'true'], 'Upload packages to repository')
    stringParam {
      name ('GENESIS_HOST_OOM_ip')
      defaultValue('1.1.1.1')
      description('IP address of Genesis out-of-band management interface (only iDRAC is tested); IPv6 is supported')
    }
    stringParam {
      name ('GENESIS_HOST_OOM_user')
      defaultValue('user1')
      description('Genesis out-of-band management interface user')
    }
    stringParam {
      name ('GENESIS_HOST_OOM_password')
      defaultValue('password1')
      description('Genesis out-of-band management interface user\'s password')
    }
    stringParam {
      name ('GENESIS_HOST_ip')
      defaultValue('2.2.2.2')
      description('IP address of Genesis server; IPv6 is supported')
    }
    stringParam {
      name ('GENESIS_HOST_mask')
      defaultValue('255.255.252.0')
      description('IP address mask of Genesis server (dotted-decimal form)')
    }
    stringParam {
      name ('GENESIS_HOST_gw')
      defaultValue('2.2.2.1')
      description('IP address of default gateway of Genesis server')
    }
    stringParam {
      name ('GENESIS_HOST_dns')
      defaultValue('1.1.1.1')
      description('IP address of DNS server (only one)')
    }
    stringParam {
      name ('GENESIS_HOST_interface')
      defaultValue('eno0')
      description('Name of network interface of Genesis server (eth0, eno0, em0, etc.)')
    }
    stringParam {
      name ('PRESEED_URL_PATH')
      defaultValue('/genesis-build')
      description('URL path of preseed-${GENESIS_HOST_ip}.cfg file')
    }
    stringParam {
      name ('NETBOOT_URL')
      defaultValue('${ARTF_WEB_URL}/list/ubuntu/dists/xenial/main/installer-amd64/current/images/netboot/ubuntu-installer/amd64')
      description('URL of Ubuntu network boot files')
    }
  }

  definition {
    cps {
      sandbox()
      script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
    }
  }

}
