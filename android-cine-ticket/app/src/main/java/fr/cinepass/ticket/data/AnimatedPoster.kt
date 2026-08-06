package fr.cinepass.ticket.data

/** Une image animée proposée comme affiche. */
data class AnimatedPoster(
    val id: String,
    val title: String,
    /** Animation pleine taille, enregistrée sur le billet. */
    val url: String,
    /** Rendition légère pour la grille de sélection. */
    val previewUrl: String,
    val width: Int,
    val height: Int,
) {
    val resolutionLabel: String get() = "$width × $height"

    /** Une affiche est verticale : les GIF au format paysage passent après. */
    val isPortrait: Boolean get() = height > width
}
