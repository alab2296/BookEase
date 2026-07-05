package se.w3footprint.bookease.domain.repository

import se.w3footprint.bookease.domain.model.Review
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    suspend fun addReview(review: Review, appointmentId: String): Result<Unit>
    fun getReviewsForBusiness(businessId: String): Flow<List<Review>>
}
