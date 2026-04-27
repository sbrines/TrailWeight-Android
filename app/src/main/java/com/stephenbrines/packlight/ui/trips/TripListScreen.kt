package com.stephenbrines.packlight.ui.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.stephenbrines.packlight.data.model.TerrainType
import com.stephenbrines.packlight.data.model.Trip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    navController: NavController,
    padding: PaddingValues,
    viewModel: TripViewModel = hiltViewModel(),
) {
    val trips by viewModel.filteredTrips.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = { TopAppBar(title = { Text("Trips") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, "Add trip")
            }
        }
    ) { innerPadding ->
        if (trips.isEmpty()) {
            Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No trips yet. Tap + to plan one.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(Modifier.padding(innerPadding)) {
                items(trips, key = { it.id }) { trip ->
                    ListItem(
                        headlineContent = { Text(trip.name) },
                        supportingContent = {
                            Text("${trip.formattedDateRange} · ${trip.terrain.displayName}")
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(trip.status.displayName,
                                             style = MaterialTheme.typography.labelSmall)
                                    },
                                )
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        },
                        modifier = Modifier.clickable {
                            navController.navigate("trip/${trip.id}")
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddSheet) {
        AddTripSheet(
            onDismiss = { showAddSheet = false },
            onSave = { trip ->
                viewModel.createTrip(trip)
                showAddSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTripSheet(onDismiss: () -> Unit, onSave: (Trip) -> Unit) {
    var name by remember { mutableStateOf("") }
    var trailName by remember { mutableStateOf("") }
    var terrain by remember { mutableStateOf(TerrainType.MIXED) }
    var distanceStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("New Trip", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Trip Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(value = trailName, onValueChange = { trailName = it },
                label = { Text("Trail Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(value = distanceStr, onValueChange = { distanceStr = it },
                label = { Text("Distance (miles)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))

            var terrainExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = terrainExpanded, onExpandedChange = { terrainExpanded = it }) {
                OutlinedTextField(value = terrain.displayName, onValueChange = {}, readOnly = true,
                    label = { Text("Terrain") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(terrainExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = terrainExpanded, onDismissRequest = { terrainExpanded = false }) {
                    TerrainType.entries.forEach { t ->
                        DropdownMenuItem(text = { Text(t.displayName) },
                            onClick = { terrain = t; terrainExpanded = false })
                    }
                }
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it },
                label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)

            Button(
                onClick = {
                    onSave(Trip(name = name, trailName = trailName, terrain = terrain,
                        distanceMiles = distanceStr.toDoubleOrNull() ?: 0.0, notes = notes))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank(),
            ) { Text("Create Trip") }
        }
    }
}
