package com.stephenbrines.trailweight.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stephenbrines.trailweight.data.model.GearItem
import com.stephenbrines.trailweight.data.model.PackListItem
import com.stephenbrines.trailweight.data.model.PackListItemWithGear
import com.stephenbrines.trailweight.data.model.Trip
import com.stephenbrines.trailweight.data.repository.GearRepository
import com.stephenbrines.trailweight.data.repository.TripRepository
import com.stephenbrines.trailweight.service.WeightCalculator
import com.stephenbrines.trailweight.service.WeightSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class WeightUnit(val label: String) {
    GRAMS("g"), OUNCES("oz"), KILOGRAMS("kg"), POUNDS("lb")
}

data class WeightUiState(
    val trips: List<Trip> = emptyList(),
    val selectedTrip: Trip? = null,
    val summary: WeightSummary = WeightSummary.EMPTY,
    val displayUnit: WeightUnit = WeightUnit.OUNCES,
)

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val tripRepo: TripRepository,
    private val gearRepo: GearRepository,
    private val snapshotDao: com.stephenbrines.trailweight.data.db.dao.WeightSnapshotDao,
) : ViewModel() {

    private val _selectedTrip = MutableStateFlow<Trip?>(null)
    private val _displayUnit = MutableStateFlow(WeightUnit.OUNCES)

    val trips: StateFlow<List<Trip>> = tripRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val summary: StateFlow<WeightSummary> = _selectedTrip.flatMapLatest { trip ->
        if (trip == null) return@flatMapLatest flowOf(WeightSummary.EMPTY)
        tripRepo.getPackLists(trip.id).flatMapLatest { packLists ->
            val packList = packLists.firstOrNull() ?: return@flatMapLatest flowOf(WeightSummary.EMPTY)
            combine(
                tripRepo.getPackListItems(packList.id),
                gearRepo.getAll(),
            ) { items, allGear ->
                val gearMap = allGear.associateBy { it.id }
                val withGear = items.map { item ->
                    PackListItemWithGear(item, gearMap[item.gearItemId])
                }
                WeightCalculator.calculate(withGear)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, WeightSummary.EMPTY)

    val displayUnit: StateFlow<WeightUnit> = _displayUnit.asStateFlow()
    val selectedTrip: StateFlow<Trip?> = _selectedTrip.asStateFlow()

    val snapshots = snapshotDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun saveSnapshot(tripName: String) = viewModelScope.launch(Dispatchers.IO) {
        val s = summary.value
        snapshotDao.insert(
            com.stephenbrines.trailweight.data.model.WeightSnapshot(
                tripName = tripName,
                baseWeightGrams = s.baseWeightGrams,
                totalWeightGrams = s.totalWeightGrams,
                itemCount = s.byCategory.sumOf { it.itemCount },
            )
        )
    }

    fun deleteSnapshot(snap: com.stephenbrines.trailweight.data.model.WeightSnapshot) =
        viewModelScope.launch(Dispatchers.IO) { snapshotDao.delete(snap) }

    fun selectTrip(trip: Trip?) { _selectedTrip.value = trip }
    fun setUnit(unit: WeightUnit) { _displayUnit.value = unit }

    fun format(grams: Double): String = when (_displayUnit.value) {
        WeightUnit.GRAMS -> "%.0f g".format(grams)
        WeightUnit.OUNCES -> "%.1f oz".format(grams / 28.3495)
        WeightUnit.KILOGRAMS -> "%.3f kg".format(grams / 1000)
        WeightUnit.POUNDS -> "%.2f lb".format(grams / 453.592)
    }
}
