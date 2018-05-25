JOB_FOLDER="images/att-comdev/genesis"
JOB_NAME="BuildGenesis"

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {

  displayName('Build Genesis')
  description('\nThis job is supposed to build ISO image for '+
              'Genesis host, and boot it.')

  parameters {
    
    choiceParam('iPXE_FROM', ['archive', 'git'], 'Where to get iPXE sources from')

    stringParam {
      name ('iPXE_ARCHIVE_URL')
      defaultValue('https://${ARTF_WEB_URL}/genesis-build/ipxe.20180525.tar.bz2')
      description('iPXE archive')
    }
    stringParam {
      name ('iPXE_GIT_URL')
      defaultValue('git://git.ipxe.org/ipxe.git')
      description('iPXE Git URL')
    }
    stringParam {
      name ('GENESIS_HOST_ip')
      defaultValue('2.2.2.2')
      description('IP address of Genesis server; IPv6 is supported')
    }
    stringParam {
      name ('GENESIS_HOST_mask')
      defaultValue('255.255.255.0')
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
      defaultValue('eno1')
      description('Name of network interface of Genesis server (eth0, eno0, em0, etc.)')
    }
    stringParam {
      name ('UPLOADS_PATH')
      defaultValue('genesis-build')
      description('URL path w/o / where we upload preseed-${GENESIS_HOST_ip}.cfg and ipxe-${GENESIS_HOST_ip}.iso')
    }
    stringParam {
      name ('PRESEED_CFG_URL')
      defaultValue('https://${ARTF_WEB_URL}/genesis-build/preseed-${GENESIS_HOST_ip}.cfg')
      description('URL path of preseed-${GENESIS_HOST_ip}.cfg file which will be used for installation')
    }
    stringParam {
      name ('PRESEED_CFG_URL_template')
      defaultValue('https://${ARTF_WEB_URL}/genesis-build/preseed.cfg.template')
      description('URL path of preseed template file')
    }
    stringParam {
      name ('iPXE_BOOT_CFG_URL_template')
      defaultValue('https://${ARTF_WEB_URL}/genesis-build/ipxe_boot.cfg.template')
      description('URL path of iPXE boot template file')
    }
    stringParam {
      name ('NETBOOT_URL')
      defaultValue('https://${ARTF_WEB_URL}/list/ubuntu/dists/xenial/main/installer-amd64/current/images/netboot/ubuntu-installer/amd64')
      description('URL of Ubuntu network boot files; should contain linux (kernel) and initrd.gz')
    }
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
  }

  definition {
    cps {
      sandbox()
      script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
    }
  }

}
