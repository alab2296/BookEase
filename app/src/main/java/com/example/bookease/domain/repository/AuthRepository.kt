package com.example.bookease.domain.repository

import com.example.bookease.domain.model.User
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: FirebaseUser?
    fun login(email: String, password: String): Flow<Result<FirebaseUser>>
    fun register(email: String, password: String): Flow<Result<FirebaseUser>>
    fun logout()
    suspend fun getUserData(uid: String): User?
    suspend fun saveUserData(user: User): Result<Unit>
    suspend fun uploadProfileImage(uid: String, imageUri: android.net.Uri): Result<String>
}
