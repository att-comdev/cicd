import jenkins.model.*
import hudson.plugins.promoted_builds.*
import groovy.json.JsonOutput

def FILE_HEADER = ['JOB Name','Build', 'Slave', 'Description', 'Branch', 'Result', 'Date']
def fileName = 'StatusReport.csv'
def allRecords = [FILE_HEADER]
def msg = ''


def status_report = {
    print "started status report"
    def docker_limit_fails = [:]
    def job_names = ['Airshipctl']
    print job_names
    def status_map = [TOTAL: 0, SUCCESS: 0, FAILURE: 0, ABORTED: 0, RUNNING: 0, UNSTABLE: 0, NOT_BUILT: 0]
    def slave_status = [:]
    for(String job_name in job_names){
        def job = Jenkins.instance.getItemByFullName( "development/"+job_name )
        def cutOfDate = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * NO_OF_DAYS.toInteger()
        for ( build in job.getBuilds().limit(LIMIT.toInteger()) ) {
            if ( build != null && build.getTimeInMillis() > cutOfDate) {
                def display_name = build.displayName
                def branch = display_name.substring(display_name.lastIndexOf(" ")+1)
                // Get console log in Plain text format to get away with wierd links
                def log = ''
                log = build.getLog()
                def slave = log =~ /Running on (.*) in/
                def desc = log =~ /Triggered by Gerrit: (.*)|Retriggered by user (.*)|Started by (.*)|Manually triggered by (.*)/
                slave_str = 'UNKNOWN'
                if (slave.size()) {
                    slave_str = slave[0][1]
                }
                slave_str = slave_str.toString()
                build_number = build.getNumber()
                build_url = build.getAbsoluteUrl()
                if (desc[0][1]) {
                    desc_str = desc[0][1]
                } else {
                    desc_str = desc[0][0]
                }
                desc_str = desc_str.toString()
                time = build.getTime().toString()

                result = build.getResult().toString()
                if (result == 'null') {
                    result = 'RUNNING'
                }
                def count = slave_status.find{ it.key == slave_str }?.value
                if (count) {
                    slave_status.put(slave_str, slave_status.get(slave_str) + 1)
                } else {
                    slave_status.put(slave_str, 1)
                }
                if (result == 'FAILURE') {
                    def limit_issue = log =~ /You have reached your pull rate limit./
                    if (limit_issue.size()) {
                        docker_limit_fails.put(build_number, slave_str)
                    }
                }
                status_map.put('TOTAL', status_map.get('TOTAL') + 1)
                status_map.put(result, status_map.get(result) + 1)
                allRecords.add([job_name, build_url, slave_str, desc_str, branch, result, time])
                //print job_name + " " + build_url + " " + " " + slave_str + " " + desc_str + " " + branch + " " + result + " " + time
            }
        }
    }
    timer_count = allRecords.count { it[3].contains('timer')}
    timer_success = allRecords.count { it[3].contains('timer') && it[5].contains('SUCCESS')}
    timer_failure = allRecords.count { it[3].contains('timer') && it[5].contains('FAILURE')}
    msg = '''
        ----------------------------------------------------------------------------------------------------
        Status: ''' + status_map + '''
        Workers: ''' +  slave_status + '''
        Timers: Total: ''' + timer_count + ''' SUCCESS: ''' + timer_success + ''' FAILURE: ''' + timer_failure + '''
        Failures due to Docker download limit : ''' + docker_limit_fails.size() + '''
        ----------------------------------------------------------------------------------------------------
        '''
    return msg
}

node (label: NODE_LABEL){
    msg = status_report()
    print msg
}
