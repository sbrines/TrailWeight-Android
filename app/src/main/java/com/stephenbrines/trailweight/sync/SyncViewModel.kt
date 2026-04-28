package com.stephenbrines.trailweight.sync

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val authRepo: AuthRepository,
) : ViewModel() {

    val user: StateFlow<FirebaseUser?> = authRepo.user
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepo.user.value)

    fun getSignInIntent(): Intent = authRepo.getSignInIntent()

    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch { authRepo.handleSignInResult(data) }
    }

    fun signOut() {
        viewModelScope.launch { authRepo.signOut() }
    }
}
