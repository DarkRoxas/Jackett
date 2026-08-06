package fr.cinepass.ticket.data

/**
 * Cinémas proposés d'office dans le formulaire, pour ne pas les retaper.
 *
 * La liste est volontairement courte et modifiable ici même ; les cinémas déjà
 * saisis dans un billet viennent s'y ajouter automatiquement, et le champ
 * reste libre.
 */
object KnownCinemas {
    val defaults: List<String> = listOf(
        "Le Lascaux — Montpon-Ménestérol",
        "Cinéma de Sainte-Foy-la-Grande",
    )

    /** Propositions = cinémas connus + ceux déjà utilisés, sans doublon. */
    fun suggestions(used: List<String>): List<String> =
        (defaults + used).distinctBy { it.trim().lowercase() }
}
