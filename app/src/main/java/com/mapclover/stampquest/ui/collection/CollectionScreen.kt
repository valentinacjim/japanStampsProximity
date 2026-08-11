package com.mapclover.stampquest.ui.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mapclover.stampquest.data.local.SeenStampsManager
import com.mapclover.stampquest.data.model.Stamp
import com.mapclover.stampquest.data.repository.JsonRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onBackClick: () -> Unit,
    onStampClick: (Stamp) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { JsonRepository(context) }
    val seenStampsManager = remember { SeenStampsManager(context) }
    val stamps by produceState<List<Stamp>>(initialValue = emptyList(), repository) {
        value = repository.loadStamps()
    }
    var seenIds by remember { mutableStateOf(seenStampsManager.getSeenStamps()) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var selectedArea by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf(CollectionStatus.ALL) }

    val unlockedCount = stamps.count { it.id in seenIds }
    val progress = if (stamps.isEmpty()) 0f else unlockedCount.toFloat() / stamps.size
    val areas = stamps.mapNotNull { it.area }.distinct().sorted()
    val filteredStamps = stamps.filter { stamp ->
        (selectedArea == null || stamp.area == selectedArea) && when (selectedStatus) {
            CollectionStatus.ALL -> true
            CollectionStatus.FOUND -> stamp.id in seenIds
            CollectionStatus.PENDING -> stamp.id !in seenIds
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi colección") },
                navigationIcon = { TextButton(onClick = onBackClick) { Text("Volver") } },
                actions = {
                    if (seenIds.isNotEmpty()) {
                        TextButton(onClick = { showResetConfirmation = true }) { Text("Reiniciar") }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Text("$unlockedCount de ${stamps.size} sellos encontrados", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    Text("Zona", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedArea == null,
                                onClick = { selectedArea = null },
                                label = { Text("Todas") }
                            )
                        }
                        items(areas) { area ->
                            FilterChip(
                                selected = selectedArea == area,
                                onClick = { selectedArea = area },
                                label = { Text(area) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Estado", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CollectionStatus.entries) { status ->
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                                label = { Text(status.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("${filteredStamps.size} sellos", style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(filteredStamps, key = { it.id }) { stamp ->
                val isUnlocked = stamp.id in seenIds
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStampClick(stamp) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stamp.nombreEn, style = MaterialTheme.typography.titleMedium)
                            if (stamp.direccion.isNotBlank()) {
                                Text(stamp.direccion, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (isUnlocked) "✓ Encontrado" else "Pendiente")
                            TextButton(onClick = {
                                if (isUnlocked) seenStampsManager.clearSeen(stamp.id)
                                else seenStampsManager.markAsSeen(stamp.id)
                                seenIds = seenStampsManager.getSeenStamps()
                            }) {
                                Text(if (isUnlocked) "Marcar pendiente" else "Marcar encontrado")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("¿Reiniciar la colección?") },
            text = { Text("Se marcarán todos los sellos como pendientes.") },
            confirmButton = {
                Button(onClick = {
                    seenStampsManager.clearAll()
                    seenIds = emptySet()
                    showResetConfirmation = false
                }) { Text("Reiniciar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirmation = false }) { Text("Cancelar") }
            }
        )
    }
}

private enum class CollectionStatus(val label: String) {
    ALL("Todos"),
    FOUND("Encontrados"),
    PENDING("Pendientes")
}
