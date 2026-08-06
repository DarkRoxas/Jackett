package fr.cinepass.ticket.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.cinepass.ticket.ui.screens.FullScreenViewerScreen
import fr.cinepass.ticket.ui.screens.TicketDetailScreen
import fr.cinepass.ticket.ui.screens.TicketEditScreen
import fr.cinepass.ticket.ui.screens.TicketListScreen
import fr.cinepass.ticket.ui.screens.ViewerMode
import fr.cinepass.ticket.wallet.WalletRepository

private object Routes {
    const val LIST = "tickets"
    const val DETAIL = "tickets/{ticketId}"

    // Préfixe distinct de "tickets/…" : sinon le formulaire entrerait en
    // concurrence avec la route de détail lors du matching.
    const val EDIT = "ticket-form?ticketId={ticketId}"
    const val VIEWER = "ticket-viewer/{ticketId}/{mode}"

    fun detail(ticketId: String) = "tickets/$ticketId"
    fun edit(ticketId: String? = null) = "ticket-form?ticketId=${ticketId.orEmpty()}"
    fun viewer(ticketId: String, mode: ViewerMode) = "ticket-viewer/$ticketId/${mode.name}"
}

@Composable
fun CinePassNavHost(walletRepository: WalletRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            TicketListScreen(
                onOpenTicket = { navController.navigate(Routes.detail(it)) },
                onAddTicket = { navController.navigate(Routes.edit()) },
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("ticketId") { type = NavType.StringType }),
        ) { entry ->
            val ticketId = entry.arguments?.getString("ticketId").orEmpty()
            TicketDetailScreen(
                ticketId = ticketId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.edit(it)) },
                onOpenViewer = { id, mode -> navController.navigate(Routes.viewer(id, mode)) },
                walletRepository = walletRepository,
            )
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument("ticketId") { type = NavType.StringType },
                navArgument("mode") { type = NavType.StringType },
            ),
        ) { entry ->
            val ticketId = entry.arguments?.getString("ticketId").orEmpty()
            val mode = entry.arguments?.getString("mode")
                ?.let { runCatching { ViewerMode.valueOf(it) }.getOrNull() }
                ?: ViewerMode.POSTER

            FullScreenViewerScreen(
                ticketId = ticketId,
                mode = mode,
                onClose = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument("ticketId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val ticketId = entry.arguments?.getString("ticketId")?.takeIf { it.isNotBlank() }
            TicketEditScreen(
                ticketId = ticketId,
                onBack = { navController.popBackStack() },
                onSaved = { savedId ->
                    // On revient à la liste puis on ouvre le billet enregistré :
                    // le formulaire ne doit pas rester dans la pile de navigation.
                    navController.popBackStack(Routes.LIST, inclusive = false)
                    navController.navigate(Routes.detail(savedId))
                },
            )
        }
    }
}
