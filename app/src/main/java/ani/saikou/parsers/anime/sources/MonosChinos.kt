package ani.saikou.parsers.anime.sources

import android.util.Base64
import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.logger
import ani.saikou.parsers.ShowResponse
import ani.saikou.parsers.anime.AnimeParser
import ani.saikou.parsers.anime.Episode
import ani.saikou.parsers.anime.VideoServer

class MonosChinos : AnimeParser() {

    override val name = "Monoschinos"
    override val saveName = "monoschinos2"
    override val hostUrl = "https://monoschinos2.com"
    override val malSyncBackupName = "Monoschinos"
    override val isDubAvailableSeparately = false
    override val language = "Spanish"

    override suspend fun loadEpisodes(
        animeLink: String,
        extra: Map<String, String>?
    ): List<Episode> {
        val pageBody = client.get(animeLink).document
        return pageBody.select("div.heroarea2 div.heromain2 div.allanimes div.row.jpage.row-cols-md-6 div.col-item").map {
            val epNum = it.attr("data-episode")
            logger("Episode-$epNum")
            val url = it.select("a").attr("href")
            val thumb1 = it.select("a div.animedtlsmain div.animeimgdiv img.animeimghv").attr("data-src")
            Episode(epNum,url,thumbnail = thumb1)
        }
    }

    override suspend fun loadVideoServers(
        episodeLink: String,
        extra: Map<String, String>?
    ): List<VideoServer> {
        return client.get(episodeLink).document.select("ul.dropcaps li").map{
            val server = it.select("a").text()
            val urlBase64 = it.select("a").attr("data-player")
            val url1 = Base64.decode(urlBase64, Base64.DEFAULT)
            val url = String(url1).replace("https://monoschinos2.com/reproductor?url=", "")
            val embed = FileUrl(url,mapOf("referer" to hostUrl))
            VideoServer(server,embed)

        }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val encoded = encode(query + if(selectDub) " (Sub)" else "")
        return client.get("$hostUrl/buscar?q=$encoded").document.select("div.heromain div.row div.col-md-4").map {
            val link = it.select("a").attr("href")
            val title = it.select("a div.series div.seriesdetails h3").text()
            val cover = it.select("a div.series div.seriesimg img").attr("src")
            ShowResponse(title, link, cover)
        }
    }

}