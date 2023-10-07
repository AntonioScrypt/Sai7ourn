package ani.saikou.parsers.anime

import ani.saikou.Lazier
import ani.saikou.lazyList
import ani.saikou.parsers.BaseParser
import ani.saikou.parsers.WatchSources
import ani.saikou.parsers.anime.sources.GOGOAnime
import ani.saikou.parsers.anime.sources.MonosChinos

object AnimeSources : WatchSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
        "GOGO" to ::GOGOAnime,
        "MONOSCHINOS" to :: MonosChinos,
    )
}

object HAnimeSources : WatchSources() {
    private val aList: List<Lazier<BaseParser>>  = lazyList(
        /*
        "HentaiMama" to ::HentaiMama,
        "Haho" to ::Haho,
        "HentaiStream" to ::HentaiStream,
        "HentaiFF" to ::HentaiFF,
        */
    )

    override val list = listOf(aList, AnimeSources.list).flatten()
}