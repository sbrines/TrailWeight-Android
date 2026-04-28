package com.stephenbrines.trailweight.ui.trips

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.stephenbrines.trailweight.data.model.*
import com.stephenbrines.trailweight.service.WeightCalculator
import com.stephenbrines.trailweight.service.WeightParser
import com.stephenbrines.trailweight.ui.gear.GearViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackListScreen(
    packListId: String,
    navController: NavController,
    tripViewModel: TripViewModel = hiltViewModel(),
    gearViewModel: GearViewModel = hiltViewModel(),
) {
    val items by tripViewModel.getPackListItems(packListId).collectAsStateWithLifecycle(emptyList())
    val allGear by gearViewModel.filteredItems.collectAsStateWithLifecycle()
    val gear by tripViewModel.getGearForPackList(packListId).collectAsStateWithLifecycle(emptyList())

    var showGearPicker by remember { mutableStateOf(false) }

    val gearMap = remember(allGear) { allGear.associateBy { it.id } }
    val itemsWithGear = remember(items, gearMap) {
        items.map { PackListItemWithGear(it, gearMap[it.gearItemId]) }
    }
    val summary = remember(itemsWithGear) { WeightCalculator.calculate(itemsWithGear) }

    val grouped = remember(itemsWithGear) {
        itemsWithGear.groupBy { it.gear?.category ?: GearCategory.OTHER }
            .toSortedMap(compareBy { it.displayName })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pack List") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ChevronRight, "Back",
                             modifier = Modifier.rotate(180f))
                    }
                },
                actions = {
                    IconButton(onClick = { showGearPicker = true }) {
                        Icon(Icons.Default.Add, "Add item")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // Weight summary header
            item {
                WeightSummaryBanner(summary = summary)
            }

            grouped.forEach { (category, catItems) ->
                stickyHeader(key = "header_${category.name}") {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(category.displayName, style = MaterialTheme.typography.labelMedium,
                                 fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                            val catWeight = catItems.sumOf { it.totalWeightGrams }
                            Text(WeightParser.displayString(catWeight),
                                 style = MaterialTheme.typography.labelMedium,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                items(catItems, key = { it.item.id }) { itemWithGear ->
                    PackListItemRow(
                        item = itemWithGear,
                        onQuantityChange = { newQty ->
                            tripViewModel.updatePackListItem(itemWithGear.item.copy(packedQuantity = newQty))
                        },
                        onToggleWorn = {
                            tripViewModel.updatePackListItem(itemWithGear.item.copy(isWorn = !itemWithGear.item.isWorn))
                        },
                        onRemove = {
                            tripViewModel.removePackListItem(itemWithGear.item)
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }
        }
    }

    if (showGearPicker) {
        GearPickerSheet(
            alreadyPacked = gear.map { it.id }.toSet(),
            allGear = allGear,
            onAdd = { gearItem ->
                tripViewModel.addGearToPackList(packListId, gearItem.id)
                showGearPicker = false
            },
            onDismiss = { showGearPicker = false },
        )
    }
}

@Composable
private fun WeightSummaryBanner(summary: com.stephenbrines.trailweight.service.WeightSummary) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            WeightChip("Base", summary.baseWeightGrams)
            WeightChip("Worn", summary.wornWeightGrams)
            WeightChip("Food/Fuel", summary.consumableWeightGrams)
            WeightChip("Total", summary.totalWeightGrams)
        }
    }
}

@Composable
private fun WeightChip(label: String, grams: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(WeightParser.displayString(grams),
             style = MaterialTheme.typography.bodySmall.copy(
                 fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
             fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall,
             color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}

@Composable
private fun PackListItemRow(
    item: PackListItemWithGear,
    onQuantityChange: (Int) -> Unit,
    onToggleWorn: () -> Unit,
    onRemove: () -> Unit,
) {
    val gear = item.gear
    var showRemoveConfirm by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(gear?.name ?: "Unknown item") },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(WeightParser.displayString(item.totalWeightGrams))
                if (item.item.isWorn) {
                    SuggestionChip(onClick = {}, label = { Text("Worn", style = MaterialTheme.typography.labelSmall) })
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = {
                    if (item.item.packedQuantity > 1) onQuantityChange(item.item.packedQuantity - 1)
                }, modifier = Modifier.size(32.dp)) {
                    Text("−", style = MaterialTheme.typography.titleMedium)
                }
                Text("${item.item.packedQuantity}",
                     style = MaterialTheme.typography.bodyMedium,
                     modifier = Modifier.widthIn(min = 16.dp))
                IconButton(onClick = {
                    onQuantityChange(item.item.packedQuantity + 1)
                }, modifier = Modifier.size(32.dp)) {
                    Text("+", style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = { showRemoveConfirm = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(16.dp))
                }
            }
        },
    )

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Remove item?") },
            text = { Text("Remove ${gear?.name ?: "this item"} from the pack list?") },
            confirmButton = {
                TextButton(onClick = { onRemove(); showRemoveConfirm = false }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GearPickerSheet(
    alreadyPacked: Set<String>,
    allGear: List<com.stephenbrines.trailweight.data.model.GearItem>,
    onAdd: (com.stephenbrines.trailweight.data.model.GearItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val unpacked = remember(allGear, alreadyPacked) { allGear.filter { it.id !in alreadyPacked } }
    var search by remember { mutableStateOf("") }
    val filtered = remember(unpacked, search) {
        if (search.isBlank()) unpacked
        else unpacked.filter { it.name.contains(search, ignoreCase = true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 32.dp)) {
            Text("Add from Inventory", style = MaterialTheme.typography.titleLarge,
                 modifier = Modifier.padding(16.dp))
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                placeholder = { Text("Search…") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(filtered, key = { it.id }) { gear ->
                    ListItem(
                        headlineContent = { Text(gear.name) },
                        supportingContent = { Text("${gear.category.displayName} · ${gear.displayWeight}") },
                        trailingContent = {
                            TextButton(onClick = { onAdd(gear) }) { Text("Add") }
                        },
                    )
                    HorizontalDivider()
                }
                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No unpacked items found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

