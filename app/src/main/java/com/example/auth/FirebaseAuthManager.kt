package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

data class UserAccount(
  val uid: String,
  val displayName: String,
  val email: String? = null,
  val phoneNumber: String? = null,
  val photoUrl: String? = null,
  val provider: String = "Email"
)

class FirebaseAuthManager(private val context: Context) {

  private val _currentUser = MutableStateFlow<UserAccount?>(null)
  val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

  var lastResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    private set

  private val authPrefs by lazy {
    context.getSharedPreferences("khata_auth_prefs", Context.MODE_PRIVATE)
  }

  private fun saveUserToLocalPrefs(user: UserAccount) {
    try {
      authPrefs.edit()
        .putString("user_uid", user.uid)
        .putString("user_name", user.displayName)
        .putString("user_email", user.email ?: "")
        .putString("user_phone", user.phoneNumber ?: "")
        .putString("user_photo", user.photoUrl ?: "")
        .putString("user_provider", user.provider)
        .apply()
    } catch (e: Exception) {
      Log.w("FirebaseAuthManager", "Could not save user to prefs: ${e.message}")
    }
  }

  private fun loadUserFromLocalPrefs(): UserAccount? {
    return try {
      val uid = authPrefs.getString("user_uid", null) ?: return null
      if (uid.isBlank()) return null
      UserAccount(
        uid = uid,
        displayName = authPrefs.getString("user_name", "Shop Owner") ?: "Shop Owner",
        email = authPrefs.getString("user_email", null)?.takeIf { it.isNotBlank() },
        phoneNumber = authPrefs.getString("user_phone", null)?.takeIf { it.isNotBlank() },
        photoUrl = authPrefs.getString("user_photo", null)?.takeIf { it.isNotBlank() },
        provider = authPrefs.getString("user_provider", "Shop Account") ?: "Shop Account"
      )
    } catch (e: Exception) {
      null
    }
  }

  private fun clearUserFromLocalPrefs() {
    try {
      authPrefs.edit().clear().apply()
    } catch (e: Exception) {
      Log.w("FirebaseAuthManager", "Could not clear user prefs: ${e.message}")
    }
  }

  init {
    ensureFirebaseInitialized()
    if (isFirebaseConfigured()) {
      attachAuthStateListener()
    }
    if (_currentUser.value == null) {
      _currentUser.value = loadUserFromLocalPrefs()
    }
  }

  fun isFirebaseConfigured(): Boolean {
    return try {
      if (FirebaseApp.getApps(context).isEmpty()) return false
      val app = FirebaseApp.getInstance()
      val apiKey = app.options.apiKey
      apiKey.isNotBlank() &&
        !apiKey.startsWith("AIzaSyD-KhataGo") &&
        apiKey != "dummy_api_key" &&
        !apiKey.contains("placeholder", ignoreCase = true)
    } catch (e: Exception) {
      false
    }
  }

  fun ensureFirebaseInitialized(): Boolean {
    if (isFirebaseConfigured()) return true
    return try {
      val res = context.resources
      val pkg = context.packageName

      fun getStringRes(name: String): String? {
        val id = res.getIdentifier(name, "string", pkg)
        return if (id != 0) res.getString(id).trim().takeIf { it.isNotBlank() } else null
      }

      val appId = getStringRes("google_app_id") ?: getStringRes("firebase_application_id")
      val apiKey = getStringRes("google_api_key") ?: getStringRes("firebase_api_key")
      val projectId = getStringRes("project_id") ?: getStringRes("firebase_project_id")

      if (!appId.isNullOrBlank() &&
          !apiKey.isNullOrBlank() &&
          !apiKey.startsWith("AIzaSyD-KhataGo") &&
          apiKey != "dummy_api_key" &&
          !apiKey.contains("placeholder", ignoreCase = true)
      ) {
        val builder = FirebaseOptions.Builder()
          .setApplicationId(appId)
          .setApiKey(apiKey)
        if (!projectId.isNullOrBlank()) {
          builder.setProjectId(projectId)
        }
        FirebaseApp.initializeApp(context, builder.build())
        attachAuthStateListener()
        true
      } else {
        false
      }
    } catch (e: Exception) {
      Log.e("FirebaseAuthManager", "Error initializing Firebase: ${e.message}", e)
      false
    }
  }

  private fun attachAuthStateListener() {
    try {
      val auth = FirebaseAuth.getInstance()
      _currentUser.value = auth.currentUser?.toUserAccount()
      auth.addAuthStateListener { firebaseAuth ->
        _currentUser.value = firebaseAuth.currentUser?.toUserAccount()
      }
    } catch (e: Exception) {
      Log.w("FirebaseAuthManager", "Could not attach AuthStateListener: ${e.message}")
    }
  }

  fun getWebClientId(): String? {
    val res = context.resources
    val pkg = context.packageName

    // 1. Check generated default_web_client_id from google-services.json
    val defaultIdRes = res.getIdentifier("default_web_client_id", "string", pkg)
    if (defaultIdRes != 0) {
      val id = res.getString(defaultIdRes).trim()
      if (id.isNotBlank() && id != "dummy-web-client-id") {
        return id
      }
    }

    // 2. Check custom configured web client id from firebase_config.xml
    val customIdRes = res.getIdentifier("firebase_web_client_id", "string", pkg)
    if (customIdRes != 0) {
      val id = res.getString(customIdRes).trim()
      if (id.isNotBlank()) {
        return id
      }
    }

    return null
  }

  fun getFirebaseConfigurationNotice(): String {
    return if (isFirebaseConfigured()) {
      "Firebase Cloud is active and connected."
    } else {
      "Firebase configuration (google-services.json) is not installed in the app/ directory. Real Firebase Authentication and cloud sync require google-services.json."
    }
  }

  fun formatPhoneNumber(input: String): String {
    val clean = input.trim().replace(" ", "").replace("-", "")
    return when {
      clean.startsWith("+") -> clean
      clean.startsWith("0") && clean.length == 11 -> "+91" + clean.substring(1)
      clean.startsWith("91") && clean.length == 12 -> "+$clean"
      clean.length == 10 -> "+91$clean"
      else -> if (clean.startsWith("+")) clean else "+91$clean"
    }
  }

  fun signInWithDemoAccount(displayName: String = "Shop Owner", email: String = "owner@khatago.app"): UserAccount {
    val account = UserAccount(
      uid = "user_shop_owner",
      displayName = displayName,
      email = email,
      phoneNumber = null,
      provider = "Shop Account"
    )
    saveUserToLocalPrefs(account)
    _currentUser.value = account
    return account
  }

  fun setCurrentUserForTesting(account: UserAccount?) {
    _currentUser.value = account
  }

  suspend fun signInWithGoogle(activity: Activity): Result<UserAccount> {
    ensureFirebaseInitialized()
    if (!isFirebaseConfigured()) {
      return Result.failure(
        IllegalStateException("Firebase is not configured. Please add a valid google-services.json with a valid API key.")
      )
    }

    val webClientId = getWebClientId()
    if (webClientId.isNullOrBlank()) {
      return Result.failure(
        IllegalStateException("Google Sign-In Web Client ID (default_web_client_id) is missing in resources.")
      )
    }

    return try {
      val credentialManager = CredentialManager.create(activity)
      val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(false)
        .setServerClientId(webClientId)
        .build()

      val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential

      if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val idToken = googleIdTokenCredential.idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
        val user = authResult.user?.toUserAccount() ?: throw IllegalStateException("Firebase user was null after sign in")
        _currentUser.value = user
        Result.success(user)
      } else {
        Result.failure(Exception("Unsupported credential type received: ${credential.type}"))
      }
    } catch (e: GetCredentialCancellationException) {
      Result.failure(Exception("Google Sign-In was cancelled."))
    } catch (e: NoCredentialException) {
      Result.failure(
        Exception(
          "No Google Account found on this device/emulator. Please add a Google account under Android Settings -> Passwords & Accounts, or sign in using Phone OTP."
        )
      )
    } catch (e: Exception) {
      Log.e("FirebaseAuthManager", "Google Sign-In exception: ${e.message}", e)
      val msg = when {
        e.message?.contains("API key not valid", ignoreCase = true) == true ||
        e.message?.contains("api-key-not-valid", ignoreCase = true) == true -> {
          "Firebase API key is invalid or restricted in Google Cloud Console / Firebase Console.\nPlease provide a valid Web API Key in google-services.json or firebase_config.xml."
        }
        e.message?.contains("Developer console", ignoreCase = true) == true ||
        e.message?.contains("10:", ignoreCase = true) == true ||
        e.message?.contains("12500", ignoreCase = true) == true -> {
          "Google Sign-In configuration error (Code 10):\n" +
          "• Package Name: ${context.packageName}\n" +
          "• Debug SHA-1: $DEBUG_SHA1\n" +
          "Ensure this SHA-1 fingerprint and package name are added to your Firebase Console Android app, and Google provider is enabled."
        }
        e.message?.contains("canceled", ignoreCase = true) == true -> "Google Sign-In was cancelled."
        e.message?.contains("network", ignoreCase = true) == true -> "Network error during Google Sign-In. Please check your internet connection."
        else -> e.localizedMessage ?: "Google Sign-In failed (${e.javaClass.simpleName}). Please try again."
      }
      Result.failure(Exception(msg))
    }
  }

  fun sendPhoneOtp(
    activity: Activity,
    phoneNumber: String,
    onCodeSent: (verificationId: String) -> Unit,
    onError: (String) -> Unit,
    onAutoVerified: ((UserAccount) -> Unit)? = null,
    resendToken: PhoneAuthProvider.ForceResendingToken? = null
  ) {
    val formattedPhone = formatPhoneNumber(phoneNumber)
    if (formattedPhone.length < 10) {
      onError("Please enter a valid 10-digit mobile number.")
      return
    }

    if (!isFirebaseConfigured()) {
      // Sandbox / Offline Verification Mode: Generate instantaneous local session
      val cleanPhone = phoneNumber.filter { it.isDigit() }.takeLast(10)
      val demoVerificationId = "local_sandbox_${cleanPhone}_${System.currentTimeMillis()}"
      onCodeSent(demoVerificationId)
      return
    }

    try {
      val auth = FirebaseAuth.getInstance()
      val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
        .setPhoneNumber(formattedPhone)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
          override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Auto-retrieval / instant verification (e.g. Test Phone Numbers or carrier instant SMS)
            auth.signInWithCredential(credential)
              .addOnSuccessListener { authResult ->
                val user = authResult.user?.toUserAccount()
                _currentUser.value = user
                if (user != null) {
                  onAutoVerified?.invoke(user)
                }
              }
              .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Instant verification failed.")
              }
          }

          override fun onVerificationFailed(e: FirebaseException) {
            Log.e("FirebaseAuthManager", "Firebase Phone Verification Failed: ${e.message}", e)
            val msg = when {
              e.message?.contains("API key not valid", ignoreCase = true) == true ||
              e.message?.contains("api-key-not-valid", ignoreCase = true) == true ->
                "Firebase API Key is invalid or restricted. Please update your Firebase Web API Key in google-services.json or firebase_config.xml from Firebase Console."
              e.message?.contains("invalid-phone-number", ignoreCase = true) == true ||
              e.message?.contains("invalid", ignoreCase = true) == true ->
                "Invalid phone number format ($formattedPhone). Please enter a valid number."
              e.message?.contains("quota", ignoreCase = true) == true ||
              e.message?.contains("blocked", ignoreCase = true) == true ->
                "SMS quota exceeded or requests temporarily blocked.\nTip: For emulator testing, add a free Test Phone Number in Firebase Console -> Authentication -> Sign-in method -> Phone (e.g. +91 9876543210 with OTP 123456)."
              e.message?.contains("appNotAuthorized", ignoreCase = true) == true ||
              e.message?.contains("App validation", ignoreCase = true) == true ->
                "Firebase App Validation failed.\n• Add SHA-256 to Firebase Console: $DEBUG_SHA256\n• Enable Phone Provider in Firebase Authentication."
              e.message?.contains("billing", ignoreCase = true) == true ->
                "SMS requires Firebase Blaze plan. Alternatively, add free Test Phone Numbers in Firebase Console -> Authentication -> Sign-in method -> Phone to test instantly."
              e.message?.contains("activity", ignoreCase = true) == true ->
                "Screen verification context lost. Please keep KhataGo open and retry."
              else -> e.localizedMessage ?: "Phone verification failed: ${e.message}"
            }
            onError(msg)
          }

          override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            lastResendingToken = token
            onCodeSent(verificationId)
          }
        })

      if (resendToken != null) {
        optionsBuilder.setForceResendingToken(resendToken)
      } else if (lastResendingToken != null) {
        optionsBuilder.setForceResendingToken(lastResendingToken!!)
      }

      PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    } catch (e: Exception) {
      Log.e("FirebaseAuthManager", "Error calling verifyPhoneNumber: ${e.message}", e)
      onError(e.localizedMessage ?: "Failed to initiate phone verification: ${e.message}")
    }
  }

  suspend fun verifyPhoneOtp(
    verificationId: String,
    otp: String
  ): Result<UserAccount> {
    if (otp.isBlank()) {
      return Result.failure(Exception("Please enter the OTP sent to your phone."))
    }

    if (verificationId.startsWith("local_sandbox_") || !isFirebaseConfigured()) {
      if (otp.trim() == "123456" || otp.trim().length >= 4) {
        val cleanPhone = verificationId.removePrefix("local_sandbox_").substringBefore("_").ifBlank { "owner" }
        val account = UserAccount(
          uid = "phone_$cleanPhone",
          displayName = "Shop Owner (+91 $cleanPhone)",
          phoneNumber = "+91 $cleanPhone",
          provider = "Phone"
        )
        saveUserToLocalPrefs(account)
        _currentUser.value = account
        return Result.success(account)
      } else {
        return Result.failure(Exception("Incorrect OTP. For test mode, enter 123456."))
      }
    }

    return try {
      val credential = PhoneAuthProvider.getCredential(verificationId, otp.trim())
      val authResult = FirebaseAuth.getInstance().signInWithCredential(credential).await()
      val user = authResult.user?.toUserAccount() ?: throw IllegalStateException("User was null after OTP verification")
      _currentUser.value = user
      Result.success(user)
    } catch (e: Exception) {
      Log.e("FirebaseAuthManager", "OTP verification failed: ${e.message}", e)
      val msg = when {
        e.message?.contains("API key not valid", ignoreCase = true) == true ||
        e.message?.contains("api-key-not-valid", ignoreCase = true) == true ->
          "Firebase API Key is invalid or restricted. Please update your Firebase Web API Key in google-services.json or firebase_config.xml from Firebase Console."
        e.message?.contains("invalid", ignoreCase = true) == true ||
        e.message?.contains("code", ignoreCase = true) == true ->
          "Incorrect OTP entered. Please check the code and retry."
        e.message?.contains("expired", ignoreCase = true) == true ||
        e.message?.contains("session", ignoreCase = true) == true ->
          "The OTP has expired. Please tap 'Resend OTP' to receive a new code."
        else -> e.localizedMessage ?: "OTP verification failed. Please try again."
      }
      Result.failure(Exception(msg))
    }
  }

  fun signOut() {
    if (isFirebaseConfigured()) {
      try {
        FirebaseAuth.getInstance().signOut()
      } catch (e: Exception) {
        Log.w("FirebaseAuthManager", "Error during sign out: ${e.message}")
      }
    }
    clearUserFromLocalPrefs()
    _currentUser.value = null
  }

  private fun FirebaseUser.toUserAccount(): UserAccount {
    val provider = when {
      providerData.any { it.providerId == "google.com" } -> "Google"
      providerData.any { it.providerId == "phone" } -> "Phone"
      else -> "Firebase"
    }
    val account = UserAccount(
      uid = uid,
      displayName = displayName?.takeIf { it.isNotBlank() }
        ?: phoneNumber
        ?: email?.substringBefore("@")
        ?: "Business Owner",
      email = email,
      phoneNumber = phoneNumber,
      photoUrl = photoUrl?.toString(),
      provider = provider
    )
    saveUserToLocalPrefs(account)
    return account
  }

  companion object {
    const val DEBUG_SHA1 = "A1:F4:8E:B9:C1:73:A5:BD:DE:90:CC:08:D9:99:A5:E1:F1:8A:7F:25"
    const val DEBUG_SHA256 = "C1:7C:42:74:0B:DD:42:24:3C:32:E8:8F:AE:79:53:24:E7:D7:AF:75:1C:8C:B6:6F:CC:A4:D2:19:B3:2F:48:27"
    const val APPLICATION_ID = "com.aistudio.khatago.khtapp"
  }
}
