package com.stephenbrines.packlight.ui.trips

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.stephenbrines.packlight.data.model.*
import com.stephenbrines.packlight.service.WeightParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    trip: Trip,
    navController: NavController,
    viewModel: TripViewModel = hiltViewModel(),
) {
    val packLists by viewModel.getPackLists(trip.id).collectAsStateWithLifecycle(emptyList())
    val resupplyPoints by viewModel.getResupplyPoints(trip.id).collectAsStateWithLifecycle(emptyList())
    val recs = remember(trip) { viewModel.recommendations(trip) }

    var showRecsSheet by remember { mutableStateOf(false) }
    var showAddResupply by remember { mutableStateOf(false) }

    val packList = packLists.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ChevronRight,
                             contentDescription = "Back",
                             modifier = Modifier.rotate(180f))
                    }
                },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Overview
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Overview", style = MaterialTheme.typography.titleMedium)
                        InfoRow("Dates", trip.formattedDateRange)
                        if (trip.distanceMiles > 0) InfoRow("Distance", "%.1f miles".format(trip.distanceMiles))
                        InfoRow("Terrain", trip.terrain.displayName)
                        InfoRow("Status", trip.status.displayName)
                        InfoRow("Duration", "${trip.durationDays} days")
                    }
                }
            }

            // Pack list summary
            if (packList != null) {
                item {
                    PackListSummaryCard(
                        packList = packList,
                        viewModel = viewModel,
                        navController = navController,
                    )
                }
            }

            // Resupply points
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Resupply Points", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showAddResupply = true }) { Text("Add") }
                }
            }

            if (resupplyPoints.isEmpty()) {
                item {
                    Text("No resupply points yet.",
                         style = MaterialTheme.typography.bodyMedium,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(resupplyPoints.sortedBy { it.mileMarker }, key = { it.id }) { point ->
                    ResupplyPointCard(point = point, onUpdate = viewModel::updateResupplyPoint,
                                     onDelete = viewModel::deleteResupplyPoint)
                }
            }

            // Recommendations
            item {
                OutlinedButton(
                    onClick = { showRecsSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("View Gear Recommendations (${recs.size})")
                }
            }
        }
    }

    if (showRecsSheet) {
        RecommendationsSheet(recommendations = recs, onDismiss = { showRecsSheet = false })
    }

    if (showAddResupply) {
        AddResupplySheet(
            onDismiss = { showAddResupply = false },
            onSave = { name, mile ->
                viewModel.addResupplyPoint(trip.id, name, mile)
                showAddResupply = false
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PackListSummaryCard(
    packList: PackList,
    viewModel: TripViewModel,
    navController: NavController,
) {
    val items by viewModel.getPackListItems(packList.id).collectAsStateWithLifecycle(emptyList())
    val gear by viewModel.getGearForPackList(packList.id).collectAsStateWithLifecycle(emptyList())

    val gearMap = remember(gear) { gear.associateBy { it.id } }
    val withGear = remember(items, gearMap) {
        items.map { item ->
            com.stephenbrines.packlight.data.model.PackListItemWithGear(item, gearMap[item.gearItemId])
        }
    }

    val baseWeight = withGear.filter { !it.item.isWorn && !(it.gear?.isConsumable ?: false) }
        .sumOf { (it.gear?.weightGrams ?: 0.0) * it.item.packedQuantity }
    val totalWeight = withGear.sumOf { (it.gear?.weightGrams ?: 0.0) * it.item.packedQuantity }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Pack List", style = MaterialTheme.typography.titleMedium)
                Text("${items.size} items", style = MaterialTheme.typography.labelMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            InfoRow("Base Weight", WeightParser.displayString(baseWeight))
            InfoRow("Total Weight", WeightParser.displayString(totalWeight))
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { navController.navigate("packlist/${packList.id}") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Build Pack List") }
        }
    }
}

@Composable
private fun ResupplyPointCard(
    point: ResupplyPoint,
    onUpdate: (ResupplyPoint) -> Unit,
    onDelete: (ResupplyPoint) -> Unit,
) {
    var showEdit by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(point.locationName) },
            supportingContent = {
                Text("Mile %.1f · %s".format(point.mileMarker, point.statusLabel))
            },
            trailingContent = {
                IconButton(onClick = { showEdit = true }) {
                    Icon(Icons.Default.Edit, "Edit")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendationsSheet(
    recommendations: List<com.stephenbrines.packlight.service.GearRecommendation>,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Gear Recommendations", style = MaterialTheme.typography.titleLarge)
            recommendations.forEach { rec ->
                ListItem(
                    headlineContent = { Text(rec.categoryName) },
                    supportingContent = { Text(rec.reason) },
                    trailingContent = {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(rec.priority.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddResupplySheet(onDismiss: () -> Unit, onSave: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var mileStr by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add Resupply Point", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Location Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = mileStr, onValueChange = { mileStr = it },
                label = { Text("Mile Marker") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
            Button(
                onClick = { onSave(name, mileStr.toDoubleOrNull() ?: 0.0) },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank(),
            ) { Text("Add Point") }
        }
    }
}

// Extension to support rotate modifier
private fun Modifier.rotate(degrees: Float) = this.then(
    androidx.compose.ui.draw.rotate(degrees)
)
