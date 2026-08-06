package fr.cinepass.ticket.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.cinepass.ticket.data.AppSettings
import fr.cinepass.ticket.wallet.WalletIssuer

/**
 * Formulaire de configuration partagé entre l'écran de bienvenue et l'écran de
 * réglages : les deux écrans éditent exactement les mêmes champs.
 */
@Composable
fun SettingsForm(
    state: SettingsUiState,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onImportServiceAccount: (String) -> Unit,
    onIssuerChosen: (WalletIssuer) -> Unit,
    onClearWallet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings

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
            title = "Affiches animées",
            description = "Aucune base de cinéma ne diffuse d'affiche animée par API. GIPHY est " +
                "la seule source interrogeable : une clé gratuite ajoute un onglet « Animées » " +
                "au choix de l'affiche. Le contenu y est communautaire, donc inégal.",
        ) {
            OutlinedTextField(
                value = settings.giphyApiKey,
                onValueChange = { value -> onChange { it.copy(giphyApiKey = value) } },
                label = { Text("Clé d'API GIPHY") },
                singleLine = true,
                supportingText = { Text("developers.giphy.com → Create an App → clé beta") },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        WalletSection(
            state = state,
            onChange = onChange,
            onImportServiceAccount = onImportServiceAccount,
            onIssuerChosen = onIssuerChosen,
            onClearWallet = onClearWallet,
        )
    }
}

@Composable
private fun WalletSection(
    state: SettingsUiState,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onImportServiceAccount: (String) -> Unit,
    onIssuerChosen: (WalletIssuer) -> Unit,
    onClearWallet: () -> Unit,
) {
    val context = LocalContext.current
    val settings = state.settings
    var manualVisible by rememberSaveable { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (json != null) onImportServiceAccount(json)
    }

    SectionCard(
        title = "Google Wallet",
        description = "Importez le fichier JSON du compte de service : l'application y lit le " +
            "compte, sa clé, et demande à Google l'identifiant émetteur associé. Rien d'autre " +
            "n'est à saisir.",
    ) {
        val linked = settings.walletIssuerId.isNotBlank() &&
            settings.walletServiceAccountEmail.isNotBlank()

        when {
            state.importing -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(10.dp))
                Text("Vérification auprès de Google…", style = MaterialTheme.typography.bodySmall)
            }

            linked -> LinkedSummary(
                issuerName = settings.walletIssuerName.ifBlank { "Émetteur ${settings.walletIssuerId}" },
                issuerId = settings.walletIssuerId,
                account = settings.walletServiceAccountEmail,
                onClear = onClearWallet,
            )

            else -> Button(
                onClick = { filePicker.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Importer le fichier JSON")
            }
        }

        if (state.issuerChoices.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Plusieurs comptes émetteurs disponibles :", style = MaterialTheme.typography.titleSmall)
            state.issuerChoices.forEach { issuer ->
                TextButton(
                    onClick = { onIssuerChosen(issuer) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("${issuer.name} — ${issuer.id}", modifier = Modifier.fillMaxWidth())
                }
            }
        }

        state.importError?.let {
            Spacer(Modifier.height(12.dp))
            Note(message = it, error = true)
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().clickable { manualVisible = !manualVisible },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Réglages manuels",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (manualVisible) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
            )
        }

        AnimatedVisibility(visible = manualVisible) {
            Column {
                Spacer(Modifier.height(8.dp))
                Note(
                    message = "Le fichier importé est conservé sur le téléphone pour signer les " +
                        "pass. Si vous préférez qu'il n'y soit pas, renseignez plutôt l'adresse " +
                        "d'un service de signature et laissez les champs du compte vides.",
                    error = false,
                )

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = settings.walletIssuerId,
                    onValueChange = { value -> onChange { it.copy(walletIssuerId = value) } },
                    label = { Text("Identifiant émetteur") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = settings.walletJwtEndpoint,
                    onValueChange = { value -> onChange { it.copy(walletJwtEndpoint = value) } },
                    label = { Text("Service de signature") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                    supportingText = { Text("https://…/wallet/jwt — prioritaire sur la clé locale") },
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

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = settings.walletClassSuffix,
                    onValueChange = { value -> onChange { it.copy(walletClassSuffix = value) } },
                    label = { Text("Suffixe de classe") },
                    singleLine = true,
                    supportingText = { Text("Vide = cinepass_event_class") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onClearWallet() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Oublier le compte Wallet")
                }
            }
        }
    }
}

@Composable
private fun LinkedSummary(
    issuerName: String,
    issuerId: String,
    account: String,
    onClear: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(10.dp))
            Column {
                Text(issuerName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Émetteur $issuerId",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = account,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onClear) { Text("Changer de compte") }
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
private fun Note(message: String, error: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (error) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Row(Modifier.padding(12.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(10.dp))
            Text(message, style = MaterialTheme.typography.bodySmall)
        }
    }
}
