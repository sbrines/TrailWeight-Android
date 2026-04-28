package com.stephenbrines.trailweight.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.stephenbrines.trailweight.data.db.dao.GearItemDao
import com.stephenbrines.trailweight.data.db.dao.PackListDao
import com.stephenbrines.trailweight.data.db.dao.ResupplyDao
import com.stephenbrines.trailweight.data.db.dao.TripDao
import com.stephenbrines.trailweight.data.db.dao.WeightSnapshotDao
import com.stephenbrines.trailweight.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val gearDao: GearItemDao,
    private val tripDao: TripDao,
    private val packListDao: PackListDao,
    private val resupplyDao: ResupplyDao,
    private val snapshotDao: WeightSnapshotDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val uid get() = auth.currentUser?.uid

    private fun userDoc() = uid?.let { firestore.collection("users").document(it) }

    // ── Upload all local data to Firestore on first sign-in ──────────────────

    fun uploadAll() {
        val root = userDoc() ?: return
        scope.launch {
            val gear = gearDao.getAll().first()
            val trips = tripDao.getAll().first()
            val snapshots = snapshotDao.getAll().first()

            val batch = firestore.batch()

            gear.forEach { item ->
                batch.set(root.collection("gear_items").document(item.id), item.toMap(), SetOptions.merge())
            }
            trips.forEach { trip ->
                batch.set(root.collection("trips").document(trip.id), trip.toMap(), SetOptions.merge())
                val packLists = packListDao.getPackListsForTrip(trip.id).first()
                packLists.forEach { pl ->
                    batch.set(root.collection("pack_lists").document(pl.id), pl.toMap(), SetOptions.merge())
                    val items = packListDao.getItemsForPackList(pl.id).first()
                    items.forEach { pli ->
                        batch.set(root.collection("pack_list_items").document(pli.item.id), pli.item.toMap(), SetOptions.merge())
                    }
                }
                val resupplyPoints = resupplyDao.getPointsForTrip(trip.id).first()
                resupplyPoints.forEach { rp ->
                    batch.set(root.collection("resupply_points").document(rp.id), rp.toMap(), SetOptions.merge())
                }
            }
            snapshots.forEach { snap ->
                batch.set(root.collection("weight_snapshots").document(snap.id), snap.toMap(), SetOptions.merge())
            }

            batch.commit().await()
        }
    }

    // ── Per-entity sync helpers called from repositories ─────────────────────

    fun syncGearItem(item: GearItem) {
        val root = userDoc() ?: return
        scope.launch {
            root.collection("gear_items").document(item.id).set(item.toMap(), SetOptions.merge()).await()
        }
    }

    fun deleteGearItem(id: String) {
        val root = userDoc() ?: return
        scope.launch { root.collection("gear_items").document(id).delete().await() }
    }

    fun syncTrip(trip: Trip) {
        val root = userDoc() ?: return
        scope.launch {
            root.collection("trips").document(trip.id).set(trip.toMap(), SetOptions.merge()).await()
        }
    }

    fun deleteTrip(id: String) {
        val root = userDoc() ?: return
        scope.launch { root.collection("trips").document(id).delete().await() }
    }

    fun syncPackListItem(item: PackListItem) {
        val root = userDoc() ?: return
        scope.launch {
            root.collection("pack_list_items").document(item.id).set(item.toMap(), SetOptions.merge()).await()
        }
    }

    fun deletePackListItem(id: String) {
        val root = userDoc() ?: return
        scope.launch { root.collection("pack_list_items").document(id).delete().await() }
    }

    fun syncResupplyPoint(point: ResupplyPoint) {
        val root = userDoc() ?: return
        scope.launch {
            root.collection("resupply_points").document(point.id).set(point.toMap(), SetOptions.merge()).await()
        }
    }

    fun syncWeightSnapshot(snap: WeightSnapshot) {
        val root = userDoc() ?: return
        scope.launch {
            root.collection("weight_snapshots").document(snap.id).set(snap.toMap(), SetOptions.merge()).await()
        }
    }

    fun deleteWeightSnapshot(id: String) {
        val root = userDoc() ?: return
        scope.launch { root.collection("weight_snapshots").document(id).delete().await() }
    }

    // ── Pull Firestore → Room (called on sign-in to a device with no local data) ──

    fun pullAll() {
        val root = userDoc() ?: return
        scope.launch {
            val localGearCount = gearDao.getAll().first().size
            if (localGearCount > 0) return@launch // local data takes priority

            root.collection("gear_items").get().await().documents.forEach { doc ->
                doc.toGearItem()?.let { gearDao.insert(it) }
            }
            root.collection("trips").get().await().documents.forEach { doc ->
                doc.toTrip()?.let { tripDao.insert(it) }
            }
            root.collection("pack_lists").get().await().documents.forEach { doc ->
                doc.toPackList()?.let { packListDao.insertPackList(it) }
            }
            root.collection("pack_list_items").get().await().documents.forEach { doc ->
                doc.toPackListItem()?.let { packListDao.insertPackListItem(it) }
            }
            root.collection("resupply_points").get().await().documents.forEach { doc ->
                doc.toResupplyPoint()?.let { resupplyDao.insertPoint(it) }
            }
            root.collection("weight_snapshots").get().await().documents.forEach { doc ->
                doc.toWeightSnapshot()?.let { snapshotDao.insert(it) }
            }
        }
    }
}

// ── Serialization helpers ─────────────────────────────────────────────────────

private fun GearItem.toMap() = mapOf(
    "id" to id, "name" to name, "brand" to brand, "category" to category.name,
    "weightGrams" to weightGrams, "quantityOwned" to quantityOwned,
    "isConsumable" to isConsumable, "notes" to notes, "purchaseUrl" to purchaseUrl,
    "imageUrl" to imageUrl, "createdAt" to createdAt, "updatedAt" to updatedAt,
)

private fun Trip.toMap() = mapOf(
    "id" to id, "name" to name, "notes" to notes, "trailName" to trailName,
    "startLocation" to startLocation, "endLocation" to endLocation,
    "startDateMs" to startDateMs, "endDateMs" to endDateMs,
    "distanceMiles" to distanceMiles, "maxElevationFeet" to maxElevationFeet,
    "minElevationFeet" to minElevationFeet, "terrain" to terrain.name,
    "status" to status.name, "createdAt" to createdAt,
)

private fun PackList.toMap() = mapOf(
    "id" to id, "tripId" to tripId, "name" to name, "notes" to notes, "createdAt" to createdAt,
)

private fun PackListItem.toMap() = mapOf(
    "id" to id, "packListId" to packListId, "gearItemId" to gearItemId,
    "packedQuantity" to packedQuantity, "isWorn" to isWorn, "isConsumed" to isConsumed,
    "notes" to notes, "addedAt" to addedAt,
)

private fun ResupplyPoint.toMap() = mapOf(
    "id" to id, "tripId" to tripId, "locationName" to locationName,
    "mileMarker" to mileMarker, "notes" to notes, "shippingAddress" to shippingAddress,
    "holdForPickup" to holdForPickup, "estimatedArrivalMs" to estimatedArrivalMs,
    "isSent" to isSent, "isPickedUp" to isPickedUp, "createdAt" to createdAt,
)

private fun WeightSnapshot.toMap() = mapOf(
    "id" to id, "tripName" to tripName, "baseWeightGrams" to baseWeightGrams,
    "totalWeightGrams" to totalWeightGrams, "itemCount" to itemCount, "recordedAt" to recordedAt,
)

// ── Deserialization helpers ───────────────────────────────────────────────────

private fun com.google.firebase.firestore.DocumentSnapshot.toGearItem() = runCatching {
    GearItem(
        id = id, name = str("name"), brand = str("brand"),
        category = GearCategory.valueOf(str("category").ifEmpty { "OTHER" }),
        weightGrams = dbl("weightGrams"), quantityOwned = int("quantityOwned"),
        isConsumable = bool("isConsumable"), notes = str("notes"),
        purchaseUrl = str("purchaseUrl"), imageUrl = str("imageUrl"),
        createdAt = long("createdAt"), updatedAt = long("updatedAt"),
    )
}.getOrNull()

private fun com.google.firebase.firestore.DocumentSnapshot.toTrip() = runCatching {
    Trip(
        id = id, name = str("name"), notes = str("notes"), trailName = str("trailName"),
        startLocation = str("startLocation"), endLocation = str("endLocation"),
        startDateMs = getLong("startDateMs"), endDateMs = getLong("endDateMs"),
        distanceMiles = dbl("distanceMiles"),
        maxElevationFeet = int("maxElevationFeet"), minElevationFeet = int("minElevationFeet"),
        terrain = TerrainType.fromString(str("terrain")),
        status = TripStatus.fromString(str("status")), createdAt = long("createdAt"),
    )
}.getOrNull()

private fun com.google.firebase.firestore.DocumentSnapshot.toPackList() = runCatching {
    PackList(id = id, tripId = str("tripId"), name = str("name"), notes = str("notes"), createdAt = long("createdAt"))
}.getOrNull()

private fun com.google.firebase.firestore.DocumentSnapshot.toPackListItem() = runCatching {
    PackListItem(
        id = id, packListId = str("packListId"), gearItemId = str("gearItemId"),
        packedQuantity = int("packedQuantity"), isWorn = bool("isWorn"),
        isConsumed = bool("isConsumed"), notes = str("notes"), addedAt = long("addedAt"),
    )
}.getOrNull()

private fun com.google.firebase.firestore.DocumentSnapshot.toResupplyPoint() = runCatching {
    ResupplyPoint(
        id = id, tripId = str("tripId"), locationName = str("locationName"),
        mileMarker = dbl("mileMarker"), notes = str("notes"),
        shippingAddress = str("shippingAddress"), holdForPickup = bool("holdForPickup"),
        estimatedArrivalMs = getLong("estimatedArrivalMs"),
        isSent = bool("isSent"), isPickedUp = bool("isPickedUp"), createdAt = long("createdAt"),
    )
}.getOrNull()

private fun com.google.firebase.firestore.DocumentSnapshot.toWeightSnapshot() = runCatching {
    WeightSnapshot(
        id = id, tripName = str("tripName"), baseWeightGrams = dbl("baseWeightGrams"),
        totalWeightGrams = dbl("totalWeightGrams"), itemCount = int("itemCount"),
        recordedAt = long("recordedAt"),
    )
}.getOrNull()

private fun com.google.firebase.firestore.DocumentSnapshot.str(f: String) = getString(f) ?: ""
private fun com.google.firebase.firestore.DocumentSnapshot.dbl(f: String) = getDouble(f) ?: 0.0
private fun com.google.firebase.firestore.DocumentSnapshot.int(f: String) = getLong(f)?.toInt() ?: 0
private fun com.google.firebase.firestore.DocumentSnapshot.long(f: String) = getLong(f) ?: 0L
private fun com.google.firebase.firestore.DocumentSnapshot.bool(f: String) = getBoolean(f) ?: false
