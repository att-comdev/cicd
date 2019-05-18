/* Handy method to use instead of retry before workflow-basic-steps 2.7
   Currently native retry does not throw up FlowInterruptedException
   that makes job abortion messy.
   Usage:
       retrier(3) {
           ...
       }
*/
def retrier(int retries, Closure body) {
    def lastException
    for(int i=0; i<retries; i++) {
        try {
            return body.call()
        } catch (hudson.AbortException | org.jenkinsci.plugins.workflow.steps.FlowInterruptedException fie) {
            throw fie
        } catch (Exception e) {
            lastException = e
            echo "${e}"
            continue
        }
    }
    throw lastException
}
