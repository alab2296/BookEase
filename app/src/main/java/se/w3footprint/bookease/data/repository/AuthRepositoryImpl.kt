package se.w3footprint.bookease.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import se.w3footprint.bookease.domain.model.User
import se.w3footprint.bookease.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
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

    override suspend fun uploadProfileImage(uid: String, imageUri: android.net.Uri): Result<String> {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return Result.failure(Exception("Cannot read the selected image"))

            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (original == null) {
                return Result.failure(Exception("Cannot decode the selected image"))
            }

            val maxDim = 300
            val scale = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt(),
                    (original.height * scale).toInt(),
                    true
                )
            } else original

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 75, out)
            val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)

            firestore.collection("users").document(uid)
                .set(mapOf("profileImageUrl" to base64), SetOptions.merge())
                .await()

            Result.success(base64)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
