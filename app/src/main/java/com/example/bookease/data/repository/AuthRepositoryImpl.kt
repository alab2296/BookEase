package com.example.bookease.data.repository

import com.example.bookease.domain.model.User
import com.example.bookease.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    override fun login(email: String, password: String): Flow<Result<FirebaseUser>> = callbackFlow {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        trySend(Result.success(user))
                    } else {
                        trySend(Result.failure(Exception("User is null")))
                    }
                } else {
                    trySend(Result.failure(task.exception ?: Exception("Login failed")))
                }
            }
        awaitClose { }
    }

    override fun register(email: String, password: String): Flow<Result<FirebaseUser>> = callbackFlow {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        trySend(Result.success(user))
                    } else {
                        trySend(Result.failure(Exception("User is null")))
                    }
                } else {
                    trySend(Result.failure(task.exception ?: Exception("Registration failed")))
                }
            }
        awaitClose { }
    }

    override fun logout() {
        firebaseAuth.signOut()
    }

    override suspend fun getUserData(uid: String): User? {
        return try {
            firestore.collection("users").document(uid).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveUserData(user: User): Result<Unit> = try {
        firestore.collection("users").document(user.id).set(user).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun uploadProfileImage(uid: String, imageUri: android.net.Uri): Result<String> = try {
        val ref = storage.reference.child("profile_images/$uid.jpg")
        ref.putFile(imageUri).await()
        val url = ref.downloadUrl.await().toString()
        Result.success(url)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
