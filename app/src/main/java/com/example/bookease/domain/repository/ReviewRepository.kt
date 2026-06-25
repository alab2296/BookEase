package com.example.bookease.domain.repository

import com.example.bookease.domain.model.Review
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    suspend fun addReview(review: Review): Result<Unit>
    fun getReviewsForBusiness(businessId: String): Flow<List<Review>>
}
