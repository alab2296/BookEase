package com.example.bookease.di

import com.example.bookease.data.repository.AuthRepositoryImpl
import com.example.bookease.data.repository.BookingRepositoryImpl
import com.example.bookease.data.repository.BusinessRepositoryImpl
import com.example.bookease.data.repository.ReviewRepositoryImpl
import com.example.bookease.domain.repository.AuthRepository
import com.example.bookease.domain.repository.BookingRepository
import com.example.bookease.domain.repository.BusinessRepository
import com.example.bookease.domain.repository.ReviewRepository
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
