package com.example.data.repository

import android.net.Uri
import android.util.Log
import com.example.data.model.AdminRecord
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.UserSessionRole
import com.example.utils.AppConstants
import com.example.utils.OperationType
import com.example.utils.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class AuthRepository(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = Firebase.auth
) {

    fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    suspend fun ensureUserProfile(): Result<UserProfile> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Not authenticated"))
        val uid = currentUser.uid
        val userRef = db.collection("users").document(uid)
        val path = userRef.path
        return try {
            val existing = userRef.get().await()
            val fcmToken = try {
                FirebaseMessaging.getInstance().token.await()
            } catch (_: Exception) {
                ""
            }

            if (!existing.exists()) {
                val initialRole = if (uid == AppConstants.SUPER_ADMIN_UID) "super_admin" else "student"
                val payload = mapOf(
                    "uid" to uid,
                    "name" to (currentUser.displayName?.ifBlank { null } ?: "শিক্ষার্থী"),
                    "email" to (currentUser.email ?: ""),
                    "phone" to (currentUser.phoneNumber ?: ""),
                    "photoUrl" to (currentUser.photoUrl?.toString() ?: ""),
                    "role" to initialRole,
                    "preferredLanguage" to "bn",
                    "fcmToken" to fcmToken,
                    "totalExams" to 0L,
                    "totalAttempted" to 0L,
                    "totalCorrect" to 0L,
                    "totalIncorrect" to 0L,
                    "bestScore" to 0.0,
                    "status" to "active",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(payload).await()
            } else if (fcmToken.isNotBlank()) {
                userRef.update(
                    mapOf(
                        "fcmToken" to fcmToken,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val finalSnap = userRef.get().await()
            val profile = finalSnap.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: UserProfile(uid = uid, name = currentUser.displayName ?: "", email = currentUser.email ?: "")
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> =
        db.collection("users").document(uid)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, "users/$uid")
                }
                throw error
            }

    fun observeUserSessionRole(uid: String): Flow<UserSessionRole> {
        if (uid == AppConstants.SUPER_ADMIN_UID) {
            return flow {
                emit(
                    UserSessionRole(
                        uid = uid,
                        role = UserRole.SUPER_ADMIN,
                        permissions = AppConstants.ALL_ADMIN_PERMISSIONS.toSet()
                    )
                )
            }
        }
        val userFlow = observeUserProfile(uid)
        val adminFlow = db.collection("admins").document(uid)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(AdminRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { emit(null) }

        return combine(userFlow, adminFlow) { profile, adminRecord ->
            if (adminRecord != null && adminRecord.status == "active") {
                UserSessionRole(
                    uid = uid,
                    role = UserRole.ADMIN,
                    permissions = adminRecord.permissions.toSet()
                )
            } else {
                val resolvedRole = UserRole.fromKey(profile?.role)
                UserSessionRole(
                    uid = uid,
                    role = if (resolvedRole == UserRole.SUPER_ADMIN) UserRole.STUDENT else resolvedRole,
                    permissions = emptySet()
                )
            }
        }
    }

    suspend fun updateProfile(
        uid: String,
        name: String,
        phone: String,
        preferredLanguage: String
    ): Result<Unit> {
        val path = "users/$uid"
        return try {
            require(name.trim().isNotEmpty() && name.length <= 120) { "নাম ১ থেকে ১২০ অক্ষরের মধ্যে হতে হবে।" }
            val currentUid = requireUserId()
            check(currentUid == uid) { "Unauthorized profile update." }
            db.collection("users").document(uid).update(
                mapOf(
                    "name" to name.trim(),
                    "phone" to phone.trim().take(32),
                    "preferredLanguage" to preferredLanguage,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun uploadProfilePhoto(uid: String, imageUri: Uri): Result<String> {
        val path = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Unauthorized photo upload." }
            val ref = storage.reference.child("profiles/$uid/avatar_${System.currentTimeMillis()}.jpg")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            db.collection("users").document(uid).update(
                mapOf(
                    "photoUrl" to downloadUrl,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(downloadUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AuthRepository", "Profile photo upload failed", e)
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
