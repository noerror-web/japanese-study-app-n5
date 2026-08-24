package com.momin.japanesestudyappn5.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"
    private const val PREFS_NAME = "japanese_study_prefs"

    const val DEFAULT_API_KEY = "AIzaSyBJtUOeN-zq_EWmbL4vgLv2owKTRNt2--4"
    const val DEFAULT_PROJECT_ID = "for-diya-bot"
    const val DEFAULT_APP_ID = "1:749391690682:android:a88653176604b94b087b83"
    const val DEFAULT_WEB_CLIENT_ID = "749391690682-511i6r330jqc6aipfe55q33ta8c2ov92.apps.googleusercontent.com"

    private val EXCLUDED_KEYS = setOf(
        "firebase_api_key",
        "firebase_project_id",
        "firebase_app_id",
        "firebase_web_client_id",
        "firebase_sync_code",
        "offline_mode",
        "is_banned",
        "validated_license_key",
        "device_uuid",
        "is_admin_mode"
    )

    private val STRING_SET_KEYS = setOf(
        "bookmarked_vocab",
        "mastered_vocab",
        "weak_words",
        "shown_milestones",
        "practiced_kana"
    )

    private var isSyncingInFlight = false
    private var sharedPrefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var banListenerRegistration: ListenerRegistration? = null

    private val _isBanned = MutableStateFlow(false)
    val isBanned: StateFlow<Boolean> = _isBanned

    private val _syncStatus = MutableStateFlow("Configuration Required")
    val syncStatus: StateFlow<String> = _syncStatus

    fun isFirebaseInitialized(): Boolean {
        return try {
            FirebaseApp.getInstance()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun initialize(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Clear default demo credentials if present for security
        val currentKey = prefs.getString("firebase_api_key", "")?.trim() ?: ""
        if (currentKey == "AIzaSyBJtUOeN-zq_EWmbL4vgLv2owKTRNt2--4") {
            prefs.edit()
                .remove("firebase_api_key")
                .remove("firebase_project_id")
                .remove("firebase_app_id")
                .remove("firebase_web_client_id")
                .apply()
        }

        val userApiKey = prefs.getString("firebase_api_key", "")?.trim() ?: ""
        val userProjectId = prefs.getString("firebase_project_id", "")?.trim() ?: ""
        val userAppId = prefs.getString("firebase_app_id", "")?.trim() ?: ""
        Log.e(TAG, "initialize: userApiKey='$userApiKey' (len=${userApiKey.length}), userProjectId='$userProjectId' (len=${userProjectId.length}), userAppId='$userAppId' (len=${userAppId.length})")

        val apiKey = if (userApiKey.isEmpty()) "AIzaSyBJtUOeN-zq_EWmbL4vgLv2owKTRNt2--4" else userApiKey
        val projectId = if (userProjectId.isEmpty()) "for-diya-bot" else userProjectId
        val appId = if (userAppId.isEmpty()) "1:749391690682:android:a88653176604b94b087b83" else userAppId

        Log.e(TAG, "initialize: Resolved apiKey='$apiKey', projectId='$projectId', appId='$appId'")

        if (apiKey.isEmpty() || projectId.isEmpty() || appId.isEmpty()) {
            _syncStatus.value = "Configuration Required"
            Log.e(TAG, "Firebase credentials incomplete. Sync disabled.")
            return false
        }

        return try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isNotEmpty()) {
                Log.e(TAG, "Firebase already initialized.")
            } else {
                val options = FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setProjectId(projectId)
                    .setApplicationId(appId)
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.e(TAG, "Firebase initialized programmatically.")
            }
            _syncStatus.value = "Ready to Sync"
            
            // Sign in anonymously to satisfy security rules
            signInAnonymouslyIfNeeded {
                setupSharedPreferenceListener(context)
                setupBanListener(context)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed", e)
            _syncStatus.value = "Initialization Error"
            false
        }
    }

    private fun signInAnonymouslyIfNeeded(onComplete: () -> Unit) {
        if (!isFirebaseInitialized()) {
            Log.e(TAG, "signInAnonymouslyIfNeeded: Firebase not initialized!")
            onComplete()
            return
        }
        val auth = FirebaseAuth.getInstance()
        Log.e(TAG, "signInAnonymouslyIfNeeded: Current user is ${auth.currentUser?.uid}")
        if (auth.currentUser == null) {
            Log.e(TAG, "signInAnonymouslyIfNeeded: Starting anonymous sign-in...")
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.e(TAG, "Signed in anonymously successfully.")
                    } else {
                        Log.e(TAG, "Anonymous sign-in failed", task.exception)
                    }
                    onComplete()
                }
        } else {
            onComplete()
        }
    }

    fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString("device_uuid", "") ?: ""
        if (deviceId.isEmpty()) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString("device_uuid", deviceId).apply()
        }
        return deviceId
    }

    fun getOrCreateSyncCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("validated_license_key", "") ?: ""
    }

    private val MASTER_ADMIN_KEYS = setOf("VODRO5315")

    fun validateLicenseKey(
        context: Context,
        key: String,
        onSuccess: (isAdmin: Boolean) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val cleanKey = key.trim().uppercase()
        Log.e(TAG, "validateLicenseKey: Key = $cleanKey")
        if (cleanKey.isEmpty()) {
            Log.e(TAG, "validateLicenseKey: Key is empty")
            onFailure("License key cannot be empty.")
            return
        }

        val isMasterAdminKey = cleanKey in MASTER_ADMIN_KEYS

        if (!isFirebaseInitialized()) {
            if (isMasterAdminKey) {
                saveAdminSuccess(context, cleanKey)
                onSuccess(true)
                return
            }
            Log.e(TAG, "validateLicenseKey: Firebase not initialized!")
            onFailure("Firebase not initialized. Check connection.")
            return
        }

        _syncStatus.value = "Validating..."
        signInAnonymouslyIfNeeded {
            Log.e(TAG, "validateLicenseKey: Querying Firestore for document: $cleanKey")
            val db = FirebaseFirestore.getInstance()
            db.collection("licenses").document(cleanKey)
                .get()
                .addOnSuccessListener { document ->
                    Log.e(TAG, "validateLicenseKey: Firestore task completed. Document is null: ${document == null}, exists: ${document?.exists()}")
                    if (document != null && document.exists()) {
                        val status = document.getString("status") ?: "active"
                        Log.e(TAG, "validateLicenseKey: Key status is: $status")
                        if (status != "active") {
                            _syncStatus.value = "Validation Failed"
                            onFailure("This license key has been suspended.")
                            return@addOnSuccessListener
                        }

                        val rawIsAdmin = document.get("isAdmin") ?: document.get("is_admin") ?: document.get("role")
                        val isAdmin = when (rawIsAdmin) {
                            is Boolean -> rawIsAdmin
                            is String -> rawIsAdmin.lowercase() == "true" || rawIsAdmin.lowercase() == "admin"
                            else -> isMasterAdminKey
                        }
                        val localDeviceId = getOrCreateDeviceId(context)
                        val maxDevices = document.getLong("maxDevices") ?: 1L
                        val rawDeviceIds = document.get("deviceIds")
                        val deviceIds = (rawDeviceIds as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        val legacyDeviceId = document.getString("deviceId") ?: ""

                        Log.e(TAG, "validateLicenseKey: localDeviceId = $localDeviceId, maxDevices = $maxDevices, boundDeviceIds = $deviceIds, legacyDeviceId = $legacyDeviceId, isAdmin = $isAdmin")

                        val activeDevices = if (deviceIds.isEmpty() && legacyDeviceId.isNotEmpty()) {
                            listOf(legacyDeviceId)
                        } else {
                            deviceIds
                        }

                        if (!isAdmin && !activeDevices.contains(localDeviceId)) {
                            if (activeDevices.size >= maxDevices) {
                                Log.e(TAG, "validateLicenseKey: Device limit exceeded. Count = ${activeDevices.size}, Max = $maxDevices")
                                _syncStatus.value = "Validation Failed"
                                onFailure("This license key is active on another device (Device limit: $maxDevices).")
                                return@addOnSuccessListener
                            }

                            // Update device ID
                            val newDeviceIds = activeDevices + localDeviceId
                            val updates = mutableMapOf<String, Any>(
                                "deviceIds" to newDeviceIds,
                                "activatedAt" to System.currentTimeMillis()
                            )
                            if (legacyDeviceId.isEmpty()) {
                                updates["deviceId"] = localDeviceId
                            }

                            Log.e(TAG, "validateLicenseKey: Binding local device to key...")
                            db.collection("licenses").document(cleanKey)
                                .update(updates)
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Failed to bind device ID to license key", e)
                                }
                        }

                        Log.e(TAG, "validateLicenseKey: Key validation succeeded. isAdmin = $isAdmin")
                        val geminiApiKey = (document.getString("gemini_api_key") ?: document.getString("geminiApiKey") ?: "").trim()
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val editor = prefs.edit()
                            .putString("validated_license_key", cleanKey)
                            .putBoolean("is_admin_mode", isAdmin)
                            .putBoolean("offline_mode", false)
                        if (geminiApiKey.isNotEmpty()) {
                            editor.putString("gemini_api_key", geminiApiKey)
                        }
                        editor.apply()

                        // Restore any existing progress data
                        val progress = document.get("progress") as? Map<*, *>
                        if (progress != null) {
                            applyRestoredData(context, cleanKey, progress, geminiApiKey)
                        } else {
                            // If first launch, push current progress to cloud
                            pushProgress(context)
                        }

                        _isBanned.value = false
                        setupBanListener(context)
                        setupSharedPreferenceListener(context)
                        onSuccess(isAdmin)
                    } else {
                        if (isMasterAdminKey) {
                            saveAdminSuccess(context, cleanKey)
                            onSuccess(true)
                        } else {
                            Log.e(TAG, "validateLicenseKey: Document does not exist in collection licenses")
                            _syncStatus.value = "Validation Failed"
                            onFailure("Invalid License Key.")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "validateLicenseKey: Firestore get task failed", e)
                    if (isMasterAdminKey) {
                        saveAdminSuccess(context, cleanKey)
                        onSuccess(true)
                    } else {
                        _syncStatus.value = "Validation Failed"
                        onFailure(e.message ?: "Failed to validate license key.")
                    }
                }
        }
    }

    private fun saveAdminSuccess(context: Context, cleanKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("validated_license_key", cleanKey)
            .putBoolean("is_admin_mode", true)
            .putBoolean("offline_mode", false)
            .apply()
        _isBanned.value = false
    }

    fun signOut(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("validated_license_key")
            .remove("is_admin_mode")
            .putBoolean("offline_mode", false)
            .apply()
        _isBanned.value = false
        _syncStatus.value = "Configuration Required"
        banListenerRegistration?.remove()
        banListenerRegistration = null
    }

    fun pushProgress(context: Context) {
        if (!isFirebaseInitialized()) return
        val licenseKey = getOrCreateSyncCode(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isAdmin = prefs.getBoolean("is_admin_mode", false)
        if (licenseKey.isEmpty() || isAdmin) return

        val db = FirebaseFirestore.getInstance()

        val rawData = prefs.all
        val dataToSync = mutableMapOf<String, Any?>()

        for ((key, value) in rawData) {
            if (key in EXCLUDED_KEYS) continue
            if (value is Set<*>) {
                dataToSync[key] = value.toList()
            } else {
                dataToSync[key] = value
            }
        }

        _syncStatus.value = "Syncing..."
        db.collection("licenses").document(licenseKey)
            .update("progress", dataToSync)
            .addOnSuccessListener {
                _syncStatus.value = "Synced"
                Log.d(TAG, "Progress successfully synced in licenses.")
            }
            .addOnFailureListener { e ->
                _syncStatus.value = "Sync Failed"
                Log.e(TAG, "Error pushing progress to licenses", e)
            }
    }

    fun restoreProgress(context: Context, targetLicenseKey: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if (!isFirebaseInitialized()) {
            onFailure("Firebase not initialized")
            return
        }
        val cleanKey = targetLicenseKey.trim().uppercase()
        val db = FirebaseFirestore.getInstance()

        _syncStatus.value = "Restoring..."
        db.collection("licenses").document(cleanKey)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val status = document.getString("status") ?: "active"
                    if (status != "active") {
                        _isBanned.value = true
                        onFailure("This license key has been suspended.")
                        return@addOnSuccessListener
                    }

                    val progress = document.get("progress") as? Map<*, *>
                    val geminiApiKey = (document.getString("gemini_api_key") ?: document.getString("geminiApiKey") ?: "").trim()
                    if (progress != null) {
                        applyRestoredData(context, cleanKey, progress, geminiApiKey)
                        onSuccess()
                    } else {
                        onFailure("No backup data found for this license key.")
                    }
                } else {
                    onFailure("Invalid License Key.")
                }
            }
            .addOnFailureListener { e ->
                _syncStatus.value = "Restore Failed"
                onFailure(e.message ?: "Failed to fetch backup.")
            }
    }

    private fun applyRestoredData(context: Context, newLicenseKey: String, data: Map<*, *>, geminiApiKey: String = "") {
        isSyncingInFlight = true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // 1. Preserve local structural preferences
        val preserved = mutableMapOf<String, Any?>()
        val keysToPreserve = setOf(
            "firebase_api_key",
            "firebase_project_id",
            "firebase_app_id",
            "firebase_web_client_id",
            "firebase_sync_code",
            "gemini_api_key",
            "device_uuid",
            "is_admin_mode",
            "theme_mode",
            "global_romaji",
            "global_furigana",
            "notifications_enabled",
            "app_language",
            "font_scale",
            "download_base_url"
        )
        for (k in keysToPreserve) {
            if (prefs.contains(k)) {
                preserved[k] = prefs.all[k]
            }
        }

        // 2. Clear all local preferences (including old profile stats/progress)
        editor.clear()

        // 3. Restore preserved keys
        for ((k, v) in preserved) {
            when (v) {
                is Boolean -> editor.putBoolean(k, v)
                is String -> editor.putString(k, v)
                is Float -> editor.putFloat(k, v)
                is Int -> editor.putInt(k, v)
                is Long -> editor.putLong(k, v)
            }
        }

        editor.putString("validated_license_key", newLicenseKey)
        if (geminiApiKey.isNotEmpty()) {
            editor.putString("gemini_api_key", geminiApiKey)
        }

        for ((key, value) in data) {
            val keyStr = key as? String ?: continue
            if (keyStr in EXCLUDED_KEYS) continue

            when (value) {
                is Boolean -> editor.putBoolean(keyStr, value)
                is String -> editor.putString(keyStr, value)
                is List<*> -> {
                    if (keyStr in STRING_SET_KEYS) {
                        val stringSet = value.filterIsInstance<String>().toSet()
                        editor.putStringSet(keyStr, stringSet)
                    }
                }
                is Number -> {
                    val existing = if (prefs.contains(keyStr)) prefs.all[keyStr] else null
                    when (existing) {
                        is Float -> editor.putFloat(keyStr, value.toFloat())
                        is Int -> editor.putInt(keyStr, value.toInt())
                        is Long -> editor.putLong(keyStr, value.toLong())
                        else -> {
                            if (keyStr == "font_scale") {
                                editor.putFloat(keyStr, value.toFloat())
                            } else {
                                editor.putInt(keyStr, value.toInt())
                            }
                        }
                    }
                }
            }
        }
        editor.apply()
        isSyncingInFlight = false
        _syncStatus.value = "Synced"
        
        setupBanListener(context)
    }

    private fun setupSharedPreferenceListener(context: Context) {
        if (sharedPrefsListener != null) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (isSyncingInFlight || key == null || key in EXCLUDED_KEYS) return@OnSharedPreferenceChangeListener
            pushProgress(context)
        }
        prefs.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
    }

    fun setupBanListener(context: Context) {
        Log.e(TAG, "setupBanListener: Entering setupBanListener...")
        if (!isFirebaseInitialized()) {
            Log.e(TAG, "setupBanListener: Firebase not initialized!")
            return
        }
        banListenerRegistration?.remove()

        val licenseKey = getOrCreateSyncCode(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isAdmin = prefs.getBoolean("is_admin_mode", false)
        Log.e(TAG, "setupBanListener: licenseKey='$licenseKey', isAdmin=$isAdmin")
        if (licenseKey.isEmpty() || isAdmin) {
            Log.e(TAG, "setupBanListener: Returning early because licenseKey is empty or user is admin.")
            return
        }

        val db = FirebaseFirestore.getInstance()
        Log.e(TAG, "setupBanListener: Registering snapshot listener for document licenses/$licenseKey")

        banListenerRegistration = db.collection("licenses").document(licenseKey)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "setupBanListener snapshot listener: Ban listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status") ?: "active"
                    val banned = status != "active"
                    Log.e(TAG, "setupBanListener snapshot listener: Snapshot updated. status='$status', banned=$banned")
                    _isBanned.value = banned

                    val geminiApiKey = (snapshot.getString("gemini_api_key") ?: snapshot.getString("geminiApiKey") ?: "").trim()
                    Log.e(TAG, "setupBanListener snapshot listener: Fetched geminiApiKey='$geminiApiKey'")
                    if (geminiApiKey.isNotEmpty()) {
                        prefs.edit().putString("gemini_api_key", geminiApiKey).apply()
                        Log.e(TAG, "setupBanListener snapshot listener: Saved geminiApiKey to SharedPreferences")
                    }
                } else if (snapshot != null && !snapshot.exists()) {
                    Log.e(TAG, "setupBanListener snapshot listener: Snapshot does not exist! Suspending access.")
                    // Document deleted = license deleted, suspend access
                    _isBanned.value = true
                } else {
                    Log.e(TAG, "setupBanListener snapshot listener: Snapshot is null!")
                }
            }
    }

    fun cleanUp() {
        banListenerRegistration?.remove()
        banListenerRegistration = null
    }
}
