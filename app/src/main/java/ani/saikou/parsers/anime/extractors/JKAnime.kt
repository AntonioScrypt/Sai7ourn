package ani.saikou.parsers.anime.extractors

import ani.saikou.client
import ani.saikou.findBetween
import ani.saikou.parsers.anime.Video
import ani.saikou.parsers.anime.VideoContainer
import ani.saikou.parsers.anime.VideoExtractor
import ani.saikou.parsers.anime.VideoServer
import ani.saikou.parsers.anime.VideoType

class JKAnime(override val server: VideoServer) : VideoExtractor() {

    override suspend fun extract(): VideoContainer {
        val videos = mutableListOf<Video>()
        val url = server.embed.url.replace("um2", "um")

        if (url.contains("jk.php")) {
            return VideoContainer(
                listOf(
                    Video(
                        null,
                        VideoType.CONTAINER,
                        url.replace("jk.php?u=", "")
                    )
                )
            )
        }
        client.get(url).document.select("script").forEach { script ->
            if (script.data().contains("var parts = {")) {
                val data = script.data().substringAfter("customType")
                    .findBetween("video: ", "})")

                val videoUrl = data.toString().substringAfter("url: '").substringBefore("'")
                val type = data.toString().substringAfter("type: '").substringBefore("'")

                if (type == "hls" || type == "custom") {
                    videos.add(Video(null, VideoType.M3U8, videoUrl))
                } else {
                    videos.add(Video(null, VideoType.CONTAINER, videoUrl))
                }
            }

        }
        return VideoContainer(
            videos
        )
    }
}