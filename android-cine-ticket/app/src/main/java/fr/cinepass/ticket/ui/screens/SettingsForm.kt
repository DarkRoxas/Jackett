package fr.cinepass.ticket.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.cinepass.ticket.data.AppSettings

/**
 * Formulaire de configuration partagé entre l'écran de bienvenue et l'écran de
 * réglages : les deux écrans éditent exactement les mêmes champs.
 */
@Composable
fun SettingsForm(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var advancedVisible by rememberSaveable { mutableStateOf(settings.walletServiceAccountKey.isNotBlank()) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {

        SectionCard(
            title = "Recherche de films",
            description = "Une clé TMDB gratuite permet de retrouver un film par son titre, " +
                "avec son année et son affiche. Sans clé, tout se saisit à la main.",
        ) {
            OutlinedTextField(
                value = settings.tmdbApiKey,
                onValueChange = { value -> onChange { it.copy(tmdbApiKey = value) } },
                label = { Text("Clé d'API TMDB") },
                singleLine = true,
                supportingText = { Text("themoviedb.org → Paramètres → API → clé v3") },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard(
            title = "Google Wallet",
            description = "Pour enregistrer vos billets dans Google Wallet, il faut un compte " +
                "émetteur (pay.google.com/business/console) et un service qui signe le pass.",
        ) {
            OutlinedTextField(
                value = settings.walletIssuerId,
                onValueChange = { value -> onChange { it.copy(walletIssuerId = value) } },
                label = { Text("Identifiant émetteur") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                supportingText = { Text("Un nombre à 19 chiffres, ex. 3388000000012345678") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = settings.walletJwtEndpoint,
                onValueChange = { value -> onChange { it.copy(walletJwtEndpoint = value) } },
                label = { Text("Adresse du service de signature") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
                supportingText = { Text("https://…/wallet/jwt — voir backend-sample/ dans le dépôt") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = settings.walletIssuerName,
                onValueChange = { value -> onChange { it.copy(walletIssuerName = value) } },
                label = { Text("Nom affiché sur le pass") },
                singleLine = true,
                supportingText = { Text("Vide = CinePass") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { advancedVisible = !advancedVisible },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Signature sur l'appareil (dépannage)",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (advancedVisible) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                )
            }

            AnimatedVisibility(visible = advancedVisible) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    WarningNote(
                        "Sans service de signature, l'app peut signer le pass elle-même avec la " +
                            "clé d'un compte de service. Cette clé reste alors sur le téléphone : " +
                            "n'utilisez ce mode que pour un essai, et révoquez la clé ensuite.",
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = settings.walletServiceAccountEmail,
                        onValueChange = { value ->
                            onChange { it.copy(walletServiceAccountEmail = value) }
                        },
                        label = { Text("Compte de service") },
                        singleLine = true,
                        supportingText = { Text("…@…iam.gserviceaccount.com") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = settings.walletServiceAccountKey,
                        onValueChange = { value -> onChange { it.copy(walletServiceAccountKey = value) } },
                        label = { Text("Clé privée (PKCS#8)") },
                        minLines = 3,
                        maxLines = 6,
                        supportingText = { Text("Le champ private_key du fichier JSON, en-têtes compris") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = settings.walletClassSuffix,
                        onValueChange = { value -> onChange { it.copy(walletClassSuffix = value) } },
                        label = { Text("Suffixe de classe") },
                        singleLine = true,
                        supportingText = { Text("Vide = cinepass_event_class") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun WarningNote(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(Modifier.padding(12.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(10.dp))
            Text(message, style = MaterialTheme.typography.bodySmall)
        }
    }
}
