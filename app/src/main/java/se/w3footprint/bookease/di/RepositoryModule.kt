package se.w3footprint.bookease.di

import se.w3footprint.bookease.data.repository.AuthRepositoryImpl
import se.w3footprint.bookease.data.repository.BookingRepositoryImpl
import se.w3footprint.bookease.data.repository.BusinessRepositoryImpl
import se.w3footprint.bookease.data.repository.ReviewRepositoryImpl
import se.w3footprint.bookease.domain.repository.AuthRepository
import se.w3footprint.bookease.domain.repository.BookingRepository
import se.w3footprint.bookease.domain.repository.BusinessRepository
import se.w3footprint.bookease.domain.repository.ReviewRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindBookingRepository(
        bookingRepositoryImpl: BookingRepositoryImpl
    ): BookingRepository

    @Binds
    @Singleton
    abstract fun bindBusinessRepository(
        businessRepositoryImpl: BusinessRepositoryImpl
    ): BusinessRepository

    @Binds
    @Singleton
    abstract fun bindReviewRepository(
        reviewRepositoryImpl: ReviewRepositoryImpl
    ): ReviewRepository
}
