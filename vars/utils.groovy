ABORT_MESSAGE = "Job was aborted."

/* Method to run downstream jobs to be used in combination with retrier. */
def runBuild(name, parameters, retries=2) {
    retrier (retries) {
        job = build(
            job: name,
            wait: true,
            propagate: false,
            parameters: parameters
        )
        if (job.result == 'SUCCESS') { return job }
        else if (job.result == 'ABORTED') { throw new Exception("'${name}': ${ABORT_MESSAGE}.")  }
        else { throw new Exception("'${name}': Job failed.") }
    }
}


/* Method that allows to retry enclosed body that respects job aborts,
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
            lastLog = currentBuild.rawBuild.getLog(20).join()
            if (lastLog.matches("Aborted by|Calling pipeline was cancelled|${ABORT_MESSAGE}")) {
                throw err
            }
            lastError = err
            echo "${err}"
            continue
        }
    }
    if (lastError) {
        throw lastError
    }
    return result
}
