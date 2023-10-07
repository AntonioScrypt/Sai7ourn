package ani.saikou.parsers.anime.sources

import android.annotation.SuppressLint
import android.net.Uri
import ani.saikou.client
import ani.saikou.findBetween
import ani.saikou.parsers.ShowResponse
import ani.saikou.parsers.anime.AnimeParser
import ani.saikou.parsers.anime.Episode
import ani.saikou.parsers.anime.VideoExtractor
import ani.saikou.parsers.anime.VideoServer
import ani.saikou.parsers.anime.extractors.FPlayer
import ani.saikou.parsers.anime.extractors.JKAnime
import ani.saikou.parsers.anime.extractors.OK
import kotlinx.serialization.Serializable

class JKAnime : AnimeParser() {

    override val name = "Jkanime"
    override val saveName = "jkanime"
    override val hostUrl = "https://jkanime.net"
    override val isDubAvailableSeparately = false
    override val language = "Spanish"

    override suspend fun loadEpisodes(
        animeLink: String,
        extra: Map<String, String>?
    ): List<Episode> {
        val pageBody = client.get(animeLink).document
        val animeId =
            pageBody.select("div.anime__details__text div.anime__details__title div#guardar-anime.btn.btn-light.btn-sm.ml-2")
                .attr("data-anime")
        return episodesParser(animeLink, animeId)
    }

    private suspend fun episodesParser(animeLink: String, animeId: String): List<Episode> {
        val episodes = mutableListOf<Episode>()
        val lastEp =
            client.get("https://jkanime.net/ajax/last_episode/$animeId/").document.body().toString()
                .findBetween("{\"number\":\"", "\",")
                ?.toInt()

        for (i in 0 until lastEp!!) {
            episodes.add(
                Episode(
                    (i + 1).toString(),
                    "$animeLink/${(i + 1)}"
                )
            )
        }
        return episodes
    }

    @SuppressLint("SuspiciousIndentation")
    override suspend fun loadVideoServers(
        episodeLink: String,
        extra: Map<String, String>?
    ): List<VideoServer> {
        val videos = mutableListOf<VideoServer>()
        client.get(episodeLink).document.select("div.col-lg-12.rounded.bg-servers.text-white.p-3.mt-2 a")
            .forEach {
                val server = it.text()
                val serverId = it.attr("data-id")
                client.get(episodeLink).document.select("script").forEach { script ->
                    if (script.data().contains("var video = [];")) {
                        val url1 = hostUrl + script.data()
                            .substringAfter("video[$serverId] = '<iframe class=\"player_conte\" src=\"")
                            .substringBefore("\"")
                            .replace("$hostUrl/jkfembed.php?u=", "https://fembed.com/v/")
                            .replace("$hostUrl/jkokru.php?u=", "http://ok.ru/videoembed/")
                            .replace("$hostUrl/jkvmixdrop.php?u=", "https://mixdrop.co/e/")
                        val url = url1.replace("$hostUrl/jkokru.php?u=", "http://ok.ru/videoembed/")
                            .replace("$hostUrl/jkvmixdrop.php?u=", "https://mixdrop.co/e/")
                            .replace("$hostUrl/jkfembed.php?u=", "https://embedsito.com/v/")

                        if (url.contains("um2")) {
                            val doc = client.get(url, referer = episodeLink).document
                            val dataKey = doc.select("form input[value]").attr("value")
                            //Log.i("bruh","Data: $dataKey")
                            client.post(
                                "$hostUrl/gsplay/redirect_post.php",
                                headers = mapOf(
                                    "Host" to "jkanime.net",
                                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                                    "Accept-Language" to "en-US,en;q=0.5",
                                    "Referer" to episodeLink,
                                    "Content-Type" to "application/x-www-form-urlencoded",
                                    "Origin" to "https://jkanime.net",
                                    "DNT" to "1",
                                    "Connection" to "keep-alive",
                                    "Upgrade-Insecure-Requests" to "1",
                                    "Sec-Fetch-Dest" to "iframe",
                                    "Sec-Fetch-Mode" to "navigate",
                                    "Sec-Fetch-Site" to "same-origin",
                                    "TE" to "trailers",
                                    "Pragma" to "no-cache",
                                    "Cache-Control" to "no-cache",
                                ),
                                data = mapOf(Pair("data", dataKey)),
                                allowRedirects = false
                            ).okhttpResponse.headers.values("location").forEach() { loc ->
                                val postkey = loc.replace("/gsplay/player.html#", "")
                                val nozomiText = client.post(
                                    "$hostUrl/gsplay/api.php",
                                    headers = mapOf(
                                        "Host" to "jkanime.net",
                                        "Accept" to "application/json, text/javascript, */*; q=0.01",
                                        "Accept-Language" to "en-US,en;q=0.5",
                                        "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                                        "X-Requested-With" to "XMLHttpRequest",
                                        "Origin" to "https://jkanime.net",
                                        "DNT" to "1",
                                        "Connection" to "keep-alive",
                                        "Sec-Fetch-Dest" to "empty",
                                        "Sec-Fetch-Mode" to "cors",
                                        "Sec-Fetch-Site" to "same-origin",
                                    ),
                                    data = mapOf(Pair("v", postkey)),
                                    allowRedirects = false
                                ).parsed<Nozomi>()
                                videos.add(VideoServer("Nozomi", nozomiText.file.toString(), null))
                            }
                        }

                        videos.add(VideoServer(server, url))
                    }
                }
            }
        return videos
    }

    @Serializable
    data class Nozomi(
        val file: String?
    )

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val domain = Uri.parse(server.embed.url).host ?: return null
        val extractor: VideoExtractor? = when {
            "fembed" in domain -> FPlayer(server)
            "embedsito" in domain -> FPlayer(server)
            "ok" in domain -> OK(server)
            "jkanime" in domain -> JKAnime(server)
            else -> null
        }
        return extractor
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val encoded = encode(query + if (selectDub) " (Sub)" else "")
        return client.get("$hostUrl/buscar/$encoded/1/?filtro=nombre&tipo=none&estado=none&orden=desc").document.select(
            "div.anime__page__content div.row div.col-lg-2"
        ).map {
            val link = it.select("div.anime__item a").attr("href")
            val title = it.select("div.anime__item div#ainfo div.title").text()
            val cover =
                it.select("div.anime__item a div.anime__item__pic.set-bg").attr("data-setbg")
            ShowResponse(title, link, cover)
        }
    }
}


