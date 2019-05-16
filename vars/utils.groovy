/* Handy method to use instead of retry before workflow-basic-steps 2.7
   Currently native retry does not throw up FlowInterruptedException
   that makes job abortion messy.
   Usage:
       retrier(3) {
           ...
       }
*/
def retrier(int retries, Closure body) {
    for(int i=0; i<retries; i++) {
        try {
            body.call()
            break
        } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException fie) {
            throw fie
        } catch (Exception e) {
            echo "${e}"
            continue
        }
    }
}
