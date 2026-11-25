package com.ipvio.stream

/**
 * #96: which automatic genre categories (Sport, Movies, ...) Live TV lists before the provider's categories
 * (pure logic, no Android). With the switch off there are none, so the provider's own categories follow "All channels".
 */
object LiveGenreLogic {
    fun visible(genreKeys: Collection<String>, hidden: Collection<String>, enabled: Boolean): List<String> =
        if (!enabled) emptyList() else genreKeys.filter { it !in hidden }
}
