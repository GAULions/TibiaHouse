package bosses

import monsters.CrawlerApplication
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.jsoup.Jsoup
import java.sql.DriverManager
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class CrawlerApplication {

    companion object {
        private fun connect() {
            try {
                Class.forName("com.mysql.jdbc.Driver")
                DriverManager.getConnection("jdbc:mysql://localhost/tibia", "root", "")
            } catch (ex: SQLException) {
                // handle any errors
                ex.printStackTrace()
            } catch (ex: Exception) {
                // handle any errors
                ex.printStackTrace()
            }
        }
        private val relationBosses: HashMap<String, Int> = hashMapOf(
            "Ocyakao" to -1, "Zushuka" to -1, "The Welter" to -1, "Shlorg" to -1, "Tyrn" to -1, "Yeti" to -1
        )
        private val relationWorlds: MutableList<String> = mutableListOf(
            "Inabra"
        )
        data class KillStatistic(
            var name: String = "",
            var killed_last_day: Long = 0L,
            var killed_last_week: Long = 0L
        )

        private fun getMonsters(world: String): MutableList<KillStatistic> {
            val request = HttpPost("https://www.tibia.com/community/?subtopic=killstatistics")
            val params = arrayListOf(
                BasicNameValuePair("world", world)
            )
            request.entity = UrlEncodedFormEntity(params)
            val execute = HttpClients.createDefault().execute(request)
            val html = EntityUtils.toString(execute.entity)
            val table = Jsoup.parse(html).select(".BoxContent > form > table> tbody > tr")
            val kills: MutableList<KillStatistic> = mutableListOf()
            table.filterIndexed { index, _ -> index > 2 && index < table.size-1 }.map {
                val name = it.select("td:nth-child(1)").html().replace("&nbsp;", "")
                if (relationBosses.containsKey(name)) {
                    kills.add(KillStatistic(name,
                        it.select("td:nth-child(3)").html().replace("&nbsp;", "").toLong(),
                        it.select("td:nth-child(4)").html().replace("&nbsp;", "").toLong())
                    )
                }
            }
            return kills
        }

        private fun insertStatistic(monsters: MutableList<KillStatistic>, dbMonsters: HashMap<String, CrawlerApplication.Companion.Monster>) {
            val conexao = DriverManager.getConnection("jdbc:mysql://localhost/tibia", "root", "")
            val dateFormat = SimpleDateFormat("yyyyMMdd")
            val dtPosi = dateFormat.format(Date())
            println("Conexão realizada com sucesso")
            conexao.autoCommit = false
            for (monster in monsters) {
                val sql = """
                INSERT INTO tb_kill_statistic_boss(id_monster, killed_last_day, killed_last_week, dt_posi)
                VALUES(${dbMonsters[monster.name]!!.id}, ${monster.killed_last_day}, ${monster.killed_last_week}, ${dtPosi.toInt()}) ON DUPLICATE KEY
                UPDATE killed_last_day = ${monster.killed_last_day}, killed_last_week = ${monster.killed_last_week}
            """.trimIndent()
                with(conexao) {
                    createStatement().execute(sql)
                    commit()
                }
            }
            conexao.close()
            println("Conexão fechada com sucesso")
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val monstersDb = CrawlerApplication.getMonsters()
            for (world in relationWorlds) {
                val monsters = getMonsters(world)
                insertStatistic(monsters, monstersDb)
            }
        }
    }

}