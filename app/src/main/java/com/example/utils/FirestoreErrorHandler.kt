package com.example.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        null
    }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

fun friendlyFirebaseError(error: Throwable, isBn: Boolean): String {
    val msg = error.message.orEmpty()
    return when {
        msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
            if (isBn) "অনুমতি নেই: এই তথ্যে আপনার প্রবেশাধিকার নেই।"
            else "Permission denied: You do not have access to this resource."
        msg.contains("UNAVAILABLE", ignoreCase = true) || msg.contains("network", ignoreCase = true) ->
            if (isBn) "ইন্টারনেট সংযোগ ব্যর্থ হয়েছে। অনুগ্রহ করে সংযোগ পরীক্ষা করুন।"
            else "Network connection error. Please check your internet connection."
        msg.contains("NOT_FOUND", ignoreCase = true) ->
            if (isBn) "অনুরোধকৃত সেবা বা তথ্য পাওয়া যায়নি (Cloud Functions বা ডকুমেন্ট যাচাই করুন)।"
            else "Requested resource or Cloud Function was not found."
        msg.contains("UNAUTHENTICATED", ignoreCase = true) ->
            if (isBn) "অনুগ্রহ করে পুনরায় সাইন ইন করুন।"
            else "Authentication required. Please sign in again."
        else -> error.localizedMessage ?: (if (isBn) "একটি ত্রুটি ঘটেছে।" else "An unexpected error occurred.")
    }
}
