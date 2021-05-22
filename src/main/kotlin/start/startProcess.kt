package start

class startProcess {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            monsters.CrawlerApplication.main(args)
            bosses.CrawlerApplication.main(args)
        }
    }
}