package houses

import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document


class CrawlerApplication {
    companion object{

        data class Auctioned(
            var name: String = "",
            var size: String = "",
            var rent: String = "",
            var status: String = ""
        )

        private fun getHtml(): Document {
            val request = HttpPost("https://www.tibia.com/community/?subtopic=houses")
            val execute = HttpClients.createDefault().execute(request)
            val html = EntityUtils.toString(execute.entity)
            return Jsoup.parse(html)
        }

        private fun getWorlds(html: Document): MutableList<String> {
            val worldsRaw = html.getElementsByAttributeValue("name", "world")
            val worldsOptions = worldsRaw[0].getElementsByTag("option")
            val worlds = worldsOptions.map {
                it.html()
            }.filterIndexed { index, s -> index != 0 }
            return worlds.toMutableList()
        }

        private fun getTown(html: Document): MutableList<String> {
            val townsRaw = html.getElementsByAttributeValue("name", "town")
            val towns = townsRaw.map {
                it.attr("value")
            }
            return towns.toMutableList()
        }

        private fun getHouses(world: String, town: String): MutableList<Auctioned> {
            val request = HttpPost("https://www.tibia.com/community/?subtopic=houses")
            val params = arrayListOf(
                BasicNameValuePair("world", world),
                BasicNameValuePair("town", town),
                BasicNameValuePair("state", "auctioned"),
                BasicNameValuePair("order", null),
                BasicNameValuePair("type", "houses")
            )
            request.entity = UrlEncodedFormEntity(params)
            val execute = HttpClients.createDefault().execute(request)
            val html = EntityUtils.toString(execute.entity)
            val table = Jsoup.parse(html).select("#houses > div.Border_2 > div > div > table> tbody > tr")
            val td = table.filterIndexed { index, element -> index > 1 }.map {
                Auctioned(it.select("td:nth-child(1) > nobr").html().replace("&nbsp;", " "),
                    it.select("td:nth-child(2) > nobr").html().replace("&nbsp;", " "),
                    it.select("td:nth-child(3) > nobr").html().replace("&nbsp;", " "),
                    it.select("td:nth-child(4) > nobr").html().replace("&nbsp;", " "))
            }.toMutableList()
            return td
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val html = getHtml()
            val worlds = getWorlds(html)
            val towns = getTown(html)
            for (world in worlds) {
                for (town in towns) {
                    getHouses(world, town)
                }
            }
            println("oi")
        }
    }
}