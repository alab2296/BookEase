package se.w3footprint.bookease.data.repository

import se.w3footprint.bookease.domain.model.Review
import se.w3footprint.bookease.domain.repository.ReviewRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReviewRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewRepository {

    override suspend fun addReview(review: Review): Result<Unit> = try {
        firestore.collection("reviews").add(review).await()
        updateBusinessRating(review.businessId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun updateBusinessRating(businessId: String) {
        try {
            val reviewsQuery = firestore.collection("reviews")
                .whereEqualTo("businessId", businessId)
                .get()
                .await()
            
            val reviews = reviewsQuery.documents.mapNotNull { it.toObject(Review::class.java) }
            
            if (reviews.isNotEmpty()) {
                val averageRating = reviews.map { it.rating }.average().toFloat()
                val reviewCount = reviews.size
                
                firestore.collection("businesses").document(businessId)
                    .update(
                        "rating", averageRating,
                        "reviewCount", reviewCount
                    ).await()
            }
        } catch (_: Exception) { }
    }

    override fun getReviewsForBusiness(businessId: String): Flow<List<Review>> = callbackFlow {
        val listener = firestore.collection("reviews")
            .whereEqualTo("businessId", businessId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val reviews = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Review::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(reviews)
            }
        awaitClose { listener.remove() }
    }
}
