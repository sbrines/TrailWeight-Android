package com.stephenbrines.trailweight.data.repository

import com.stephenbrines.trailweight.data.db.dao.PackListDao
import com.stephenbrines.trailweight.data.db.dao.ResupplyDao
import com.stephenbrines.trailweight.data.db.dao.TripDao
import com.stephenbrines.trailweight.data.model.PackList
import com.stephenbrines.trailweight.data.model.PackListItem
import com.stephenbrines.trailweight.data.model.ResupplyPoint
import com.stephenbrines.trailweight.data.model.Trip
import com.stephenbrines.trailweight.sync.CloudSyncService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val tripDao: TripDao,
    private val packListDao: PackListDao,
    private val resupplyDao: ResupplyDao,
    private val sync: CloudSyncService,
) {
    fun getAll(): Flow<List<Trip>> = tripDao.getAll()
    suspend fun getById(id: String): Trip? = tripDao.getById(id)

    suspend fun createTrip(trip: Trip): String {
        tripDao.insert(trip)
        sync.syncTrip(trip)
        val packList = PackList(tripId = trip.id, name = "${trip.name} — Pack List")
        packListDao.insertPackList(packList)
        return trip.id
    }

    suspend fun update(trip: Trip) {
        tripDao.update(trip)
        sync.syncTrip(trip)
    }

    suspend fun delete(trip: Trip) {
        tripDao.delete(trip)
        sync.deleteTrip(trip.id)
    }

    fun getPackLists(tripId: String) = packListDao.getPackListsForTrip(tripId)
    fun getPackListItems(packListId: String) = packListDao.getItemsForPackList(packListId)
    fun getGearForPackList(packListId: String) = packListDao.getGearForPackList(packListId)

    suspend fun addGearToPackList(packListId: String, gearItemId: String,
                                   quantity: Int = 1, isWorn: Boolean = false) {
        val existing = packListDao.findItem(packListId, gearItemId)
        if (existing != null) {
            val updated = existing.copy(packedQuantity = existing.packedQuantity + quantity)
            packListDao.updatePackListItem(updated)
            sync.syncPackListItem(updated)
        } else {
            val item = PackListItem(packListId = packListId, gearItemId = gearItemId,
                                    packedQuantity = quantity, isWorn = isWorn)
            packListDao.insertPackListItem(item)
            sync.syncPackListItem(item)
        }
    }

    suspend fun updatePackListItem(item: PackListItem) {
        packListDao.updatePackListItem(item)
        sync.syncPackListItem(item)
    }

    suspend fun removePackListItem(item: PackListItem) {
        packListDao.deletePackListItem(item)
        sync.deletePackListItem(item.id)
    }

    fun getResupplyPoints(tripId: String) = resupplyDao.getPointsForTrip(tripId)
    fun getResupplyItems(pointId: String) = resupplyDao.getItemsForPoint(pointId)

    suspend fun insertResupplyPoint(point: ResupplyPoint) {
        resupplyDao.insertPoint(point)
        sync.syncResupplyPoint(point)
    }

    suspend fun updateResupplyPoint(point: ResupplyPoint) {
        resupplyDao.updatePoint(point)
        sync.syncResupplyPoint(point)
    }

    suspend fun deleteResupplyPoint(point: ResupplyPoint) {
        resupplyDao.deletePoint(point)
    }
}
