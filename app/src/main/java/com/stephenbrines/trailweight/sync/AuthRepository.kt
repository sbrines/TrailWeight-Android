package com.stephenbrines.trailweight.sync

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val syncService: CloudSyncService,
) {
    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    val isSignedIn get() = auth.currentUser != null

    init {
        auth.addAuthStateListener { _user.value = it.currentUser }
    }

    fun getSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    suspend fun handleSignInResult(data: Intent?): Result<FirebaseUser> = runCatching {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user!!
        // Upload local data; pull from cloud if local is empty
        syncService.uploadAll()
        syncService.pullAll()
        user
    }

    suspend fun signOut() {
        auth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, gso).signOut().await()
    }

    companion object {
        // Replace with your OAuth 2.0 Web Client ID from Firebase console →
        // Project Settings → General → Your apps → Web app → Client ID
        // OR from Google Cloud Console → APIs & Services → Credentials
        const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID_HERE"
    }
}
