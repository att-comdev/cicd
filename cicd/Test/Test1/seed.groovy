path='cicd/Test/Test1'
jobname='cicd/Test/Test1/PathTest'

folder(path)
freeStyleJob(jobname) {
    label('master')
    steps{
        sleep(30000)
        println('OK')
    }
}
