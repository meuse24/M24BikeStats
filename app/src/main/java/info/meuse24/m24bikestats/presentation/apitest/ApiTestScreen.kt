package info.meuse24.m24bikestats.presentation.apitest

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.meuse24.m24bikestats.domain.model.BoschEndpoint

/**
 * Stateless API-Test-Screen. Kein ViewModel-Zugriff – nur [ApiTestUiState] + Callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiTestScreen(
    uiState: ApiTestUiState,
    onSelectEndpoint: (BoschEndpoint) -> Unit,
    onFetch: () -> Unit,
    onClear: () -> Unit,
    onLogout: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Test") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Abmelden")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            EndpointDropdown(
                selected = uiState.selectedEndpoint,
                onSelect = onSelectEndpoint,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = uiState.selectedEndpoint.path,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onFetch, enabled = !uiState.isLoading) {
                    Text("Abrufen")
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onClear, enabled = uiState.jsonOutput.isNotEmpty()) {
                    Icon(Icons.Default.Clear, contentDescription = "Ausgabe leeren")
                }
                if (uiState.isLoading) {
                    Spacer(modifier = Modifier.width(12.dp))
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            JsonOutputBox(json = uiState.jsonOutput)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndpointDropdown(
    selected: BoschEndpoint,
    onSelect: (BoschEndpoint) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Endpunkt") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            BoschEndpoint.entries.forEach { endpoint ->
                DropdownMenuItem(
                    text = { Text(endpoint.label) },
                    onClick = {
                        onSelect(endpoint)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun JsonOutputBox(json: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = json.ifEmpty { "– noch kein Ergebnis –" },
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = if (json.isEmpty())
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
