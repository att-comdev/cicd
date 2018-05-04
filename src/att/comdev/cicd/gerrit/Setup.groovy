package att.comdev.cicd.gerrit

class Setup {

    public static String repoUser = ""
    public static String gerritUrl = ""
    public static String credId = ""

    public Setup(repoUser, gerritUrl, credId) {
        this.repoUser = repoUser
        this.gerritUrl = gerritUrl
        this.credId = credId
    }

}