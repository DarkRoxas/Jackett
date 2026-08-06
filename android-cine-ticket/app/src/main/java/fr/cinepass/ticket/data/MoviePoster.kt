package fr.cinepass.ticket.data

/** Une affiche de la galerie TMDB, avec ce qu'il faut pour juger sa qualité. */
data class MoviePoster(
    /** Image pleine résolution, telle qu'elle sera enregistrée sur le billet. */
    val url: String,
    /** Version réduite, pour la grille de sélection. */
    val thumbnailUrl: String,
    val width: Int,
    val height: Int,
    /** Code langue TMDB : "fr", "en", ou null pour une affiche sans texte. */
    val language: String?,
    val voteAverage: Double,
    val voteCount: Int,
) {
    /** « 2000 × 3000 » */
    val resolutionLabel: String get() = "$width × $height"

    companion object {
        /**
         * Sous 1000 px de large, une affiche pixellise dès qu'on la met en plein
         * écran sur une tablette : ces entrées sont écartées de la sélection.
         */
        const val MIN_WIDTH = 1000
    }
}
