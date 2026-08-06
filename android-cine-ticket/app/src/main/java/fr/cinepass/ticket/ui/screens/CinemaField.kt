package fr.cinepass.ticket.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import fr.cinepass.ticket.R

/**
 * Champ « cinéma » avec propositions : les salles habituelles sont à portée
 * d'un appui, mais la saisie reste libre pour une salle de passage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaField(
    value: String,
    suggestions: List<String>,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    // On ne propose que ce qui correspond à la saisie en cours.
    val matching = remember(value, suggestions) {
        if (value.isBlank()) suggestions
        else suggestions.filter { it.contains(value.trim(), ignoreCase = true) && it != value }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && matching.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(stringResource(R.string.field_cinema)) },
            singleLine = true,
            isError = isError,
            supportingText = errorMessage?.let { message -> { Text(message) } },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && matching.isNotEmpty())
            },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )

        ExposedDropdownMenu(
            expanded = expanded && matching.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            matching.forEach { cinema ->
                DropdownMenuItem(
                    text = { Text(cinema, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        onValueChange(cinema)
                        expanded = false
                    },
                )
            }
        }
    }
}
