package monsters

import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.jsoup.Jsoup
import java.sql.DriverManager
import kotlin.collections.HashMap

class CrawlerApplication {

    companion object {

        data class Monster(
            var id: Int = -1,
            var name: String = ""
        )

        private fun insertNewMonsters(tibiaMonsters: MutableList<Monster>, tibiaFandomMonsters: MutableList<Monster>, dbMonsters: HashMap<String, Monster>) {
            val conexao = DriverManager.getConnection("jdbc:mysql://localhost/tibia", "root", "")
            println("Conexão realizada com sucesso")
            conexao.autoCommit = false
            for (monster in tibiaMonsters) {
                if (!dbMonsters.containsKey(monster.name)) {
                    val name = monster.name.replace("'", "\\'")
                    println("NOVO MONSTRO: $name")
                    val sql = """
                INSERT INTO tb_monsters(name)
                VALUES('$name')
            """.trimIndent()
                    with(conexao) {
                        createStatement().execute(sql)
                        commit()
                    }
                }
            }
            for (monster in tibiaFandomMonsters) {
                if (!dbMonsters.containsKey(monster.name)) {
                    val name = monster.name.replace("'", "\\'")
                    println("NOVO MONSTRO: $name")
                    val sql = """
                INSERT INTO tb_monsters(name)
                VALUES('$name')
            """.trimIndent()
                    with(conexao) {
                        createStatement().execute(sql)
                        commit()
                    }
                }
            }
            conexao.close()
            println("Conexão fechada com sucesso")
        }

        fun getMonsters(): HashMap<String, Monster> {
            val conexao = DriverManager.getConnection("jdbc:mysql://localhost/tibia", "root", "")
            val sql = "SELECT * FROM tb_monsters"
            val rs = conexao.createStatement().executeQuery(sql)
            val monsters: HashMap<String, Monster> = hashMapOf()
            while (rs.next()) {
                val id = rs.getInt("id_monster")
                val name = rs.getString("name")
                monsters[name] = Monster(id, name)
            }
            rs.close()
            conexao.close()
            return monsters
        }

        private fun getInTibiaMonsters(): MutableList<Monster> {
            val request = HttpPost("https://www.tibia.com/library/?subtopic=creatures")
            val params = arrayListOf(
                BasicNameValuePair("subtopic", "creatures")
            )
            request.entity = UrlEncodedFormEntity(params)
            val execute = HttpClients.createDefault().execute(request)
            val html = EntityUtils.toString(execute.entity)
            val div = Jsoup.parse(html).select(".BoxContent > div")
            val monsters: MutableList<Monster> = mutableListOf()
            div.filterIndexed { index, _ -> index > 0 }.map {
                val arrayNameMonsters = it.select("div > div > div > div").html().replace("&nbsp;", "").replace("\n", ",").split(",")
                for (monster in arrayNameMonsters) {
                    monsters.add(Monster(-1, monster))
                }
            }
            return monsters
        }

        private fun getInTibiaFandomBosses(): MutableList<Monster> {
            val request = HttpPost("https://tibia.fandom.com/wiki/Bosses")
            val execute = HttpClients.createDefault().execute(request)
            val html = EntityUtils.toString(execute.entity)
            val div = Jsoup.parse(html).select(".wikitable > tbody > tr")
            val monsters: MutableList<Monster> = mutableListOf()
            div.filterIndexed { index, _ -> index > 0 }.map {
                val nameMonster = it.select("td:nth-child(1) > a").html()
                monsters.add(Monster(-1, nameMonster))
            }
            return monsters
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val monsters = getInTibiaMonsters()
            val bosses = getInTibiaFandomBosses()
            val monstersDb = getMonsters()
            insertNewMonsters(monsters, bosses, monstersDb)
        }

    }
}