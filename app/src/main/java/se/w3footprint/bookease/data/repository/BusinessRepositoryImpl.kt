package se.w3footprint.bookease.data.repository

import se.w3footprint.bookease.domain.model.Business
import se.w3footprint.bookease.domain.model.Service
import se.w3footprint.bookease.domain.model.TimeSlot
import se.w3footprint.bookease.domain.repository.BusinessRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class BusinessRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : BusinessRepository {

    override fun getBusinesses(): Flow<List<Business>> = callbackFlow {
        val listener = firestore.collection("businesses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val businesses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Business::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(businesses)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getBusinessById(ownerId: String): Business? {
        return try {
            firestore.collection("businesses").document(ownerId).get().await().toObject(Business::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun getServices(ownerId: String): Flow<List<Service>> = callbackFlow {
        val listener = firestore.collection("businesses").document(ownerId).collection("services")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val services = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Service::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(services)
            }
        awaitClose { listener.remove() }
    }

    override fun getTimeSlots(ownerId: String): Flow<List<TimeSlot>> = callbackFlow {
        val listener = firestore.collection("businesses").document(ownerId).collection("timeslots")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val slots = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TimeSlot::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(slots)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveBusinessProfile(business: Business): Result<Unit> = try {
        firestore.collection("businesses").document(business.ownerId).set(business).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun addService(service: Service): Result<Unit> = try {
        firestore.collection("businesses").document(service.businessId).collection("services").add(service).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteService(ownerId: String, serviceId: String): Result<Unit> = try {
        firestore.collection("businesses").document(ownerId).collection("services").document(serviceId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun addTimeSlot(timeSlot: TimeSlot): Result<Unit> = try {
        firestore.collection("businesses").document(timeSlot.businessId).collection("timeslots").add(timeSlot).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteTimeSlot(ownerId: String, timeSlotId: String): Result<Unit> = try {
        firestore.collection("businesses").document(ownerId).collection("timeslots").document(timeSlotId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun uploadBusinessImage(ownerId: String, imageUri: android.net.Uri): Result<String> = try {
        val fileName = "business_images/$ownerId/${System.currentTimeMillis()}.jpg"
        val ref = storage.reference.child(fileName)
        ref.putFile(imageUri).await()
        val url = ref.downloadUrl.await().toString()
        Result.success(url)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
