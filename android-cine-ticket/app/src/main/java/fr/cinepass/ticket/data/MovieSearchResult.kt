package fr.cinepass.ticket.data

/** Un film renvoyé par la recherche TMDB. */
data class MovieSearchResult(
    val id: Int,
    val title: String,
    val originalTitle: String?,
    val releaseYear: Int?,
    /** URL de l'affiche en taille d'affichage, ou null si le film n'en a pas. */
    val posterUrl: String?,
    /** URL de la vignette utilisée dans la liste de résultats. */
    val thumbnailUrl: String?,
    val overview: String?,
) {
    val label: String get() = releaseYear?.let { "$title ($it)" } ?: title
}
