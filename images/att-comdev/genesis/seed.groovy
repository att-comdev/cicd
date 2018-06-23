JOB_FOLDER="images/att-comdev/genesis"
JOB_NAME="BuildGenesis-v2"

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {

  displayName('Build Genesis v2')
  description('\nThis job is supposed to build ISO image for '+
              'Genesis host, and boot it.')

  parameters {
    // ==================================================================
    stringParam {
      name ('GENESIS_HOST_ip')
      defaultValue('2.2.2.2')
      description('IP address of Genesis server')
      trim(true)
    }
    stringParam {
      name ('GENESIS_HOST_mask')
      defaultValue('255.255.255.0')
      description('IP address mask of Genesis server (dotted-decimal form)')
      trim(true)
    }
    stringParam {
      name ('GENESIS_HOST_gw')
      defaultValue('2.2.2.1')
      description('IP address of default gateway of Genesis server')
      trim(true)
    }
    stringParam {
      name ('GENESIS_HOST_dns')
      defaultValue('1.1.1.1')
      description('IP address of DNS server (only one)')
      trim(true)
    }
    stringParam {
      name ('GENESIS_HOST_interface')
      defaultValue('eno1')
      description('Name of network interface of Genesis server (eth0, eno0, em0, etc.)')
      trim(true)
    }
    // ==================================================================
    stringParam {
      name ('UPLOADS_PATH')
      defaultValue('genesis-build')
      description('URL path w/o / where we upload preseed-${GENESIS_HOST_ip}.cfg')
      trim(true)
    }
    stringParam {
      name ('PRESEED_CFG_URL_template')
      defaultValue('https://${ARTF_WEB_URL}/${UPLOADS_PATH}/preseed.cfg.template')
      description('URL path of preseed template file')
      trim(true)
    }
    // ==================================================================
    stringParam {
      name ('GENESIS_HOST_OOM_ip')
      defaultValue('1.1.1.1')
      description('IP address of Genesis out-of-band management interface (only iDRAC is tested); IPv6 is supported')
      trim(true)
    }
    stringParam {
      name ('GENESIS_HOST_OOM_user')
      defaultValue('user1')
      description('Genesis out-of-band management interface user')
      trim(true)
    }
    stringParam {
      name ('GENESIS_HOST_OOM_password')
      defaultValue('password1')
      description('Genesis out-of-band management interface user\'s password')
      trim(true)
    }
    // ==================================================================
  }

  definition {
    cps {
      sandbox()
      script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
    }
  }

}
