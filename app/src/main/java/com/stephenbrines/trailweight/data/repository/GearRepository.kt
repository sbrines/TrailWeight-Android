package com.stephenbrines.trailweight.data.repository

import com.stephenbrines.trailweight.data.db.dao.GearItemDao
import com.stephenbrines.trailweight.data.model.GearItem
import com.stephenbrines.trailweight.sync.CloudSyncService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GearRepository @Inject constructor(
    private val dao: GearItemDao,
    private val sync: CloudSyncService,
) {
    fun getAll(): Flow<List<GearItem>> = dao.getAll()
    suspend fun getById(id: String): GearItem? = dao.getById(id)

    suspend fun insert(item: GearItem) {
        dao.insert(item)
        sync.syncGearItem(item)
    }

    suspend fun update(item: GearItem) {
        val updated = item.copy(updatedAt = System.currentTimeMillis())
        dao.update(updated)
        sync.syncGearItem(updated)
    }

    suspend fun delete(item: GearItem) {
        dao.delete(item)
        sync.deleteGearItem(item.id)
    }

    suspend fun deleteMany(ids: List<String>) {
        dao.deleteByIds(ids)
        ids.forEach { sync.deleteGearItem(it) }
    }
}
