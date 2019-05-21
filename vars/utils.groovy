class Params {
    static def ABORT_MESSAGE = "Job was aborted."
    static def ABORT_ON = ["Aborted by", "Calling Pipeline was cancelled", ABORT_MESSAGE]
}

/* Method to run downstream jobs to be used in combination with retrier. */
def runBuild(name, parameters, retries=1) {
    retrier (retries) {
        job = build(
            job: name,
            wait: true,
            propagate: false,
            parameters: parameters
        )
        if (job.result == 'SUCCESS') { return job }
        else if (job.result == 'ABORTED') { throw new Exception("'${name}': ${Params.ABORT_MESSAGE}")  }
        else { throw new Exception("'${name}': Job failed.") }
    }
}


/* Method that allows to retry enclosed body that respects job abort,
   including upstream and downstream ones
      Usage:
         retrier(3) {
             ...
         }
*/
def retrier(int retries, Closure body) {
    def lastError
    for(int i=0; i<retries; i++) {
        lastError = null
        try {
            result = body.call()
            break
        } catch (err) {
            echo "${err}"
            sleep 1
            lastLog = currentBuild.rawBuild.getLog(20).join()
            if (lastLog.find(Params.ABORT_ON.join("|"))) {
                echo "Abort detected. Marking job as ABORTED."
                currentBuild.result = 'ABORTED'
                throw err
            }
            lastError = err
            continue
        }
    }
    if (lastError) {
        throw lastError
    }
    return result
}
