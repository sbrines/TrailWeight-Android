package com.stephenbrines.trailweight.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stephenbrines.trailweight.sync.SyncViewModel

@Composable
fun SyncSettingsScreen(
    viewModel: SyncViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result.data)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Cloud Sync") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (user != null) {
                // ── Signed in ───────────────────────────────────────────────
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Syncing to Google", style = MaterialTheme.typography.titleSmall)
                            Text(user!!.email ?: "", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                    }
                }

                Text(
                    "Your gear, trips, and pack lists are automatically synced to your Google account " +
                    "and available on any device where you sign in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedButton(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sign out and disable sync")
                }
            } else {
                // ── Signed out ──────────────────────────────────────────────
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.CloudOff, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column {
                            Text("Sync disabled", style = MaterialTheme.typography.titleSmall)
                            Text("Data stored locally only",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Text(
                    "Sign in with Google to sync your gear inventory, trips, and pack lists across " +
                    "all your devices. Your data stays private and is only accessible with your account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = { signInLauncher.launch(viewModel.getSignInIntent()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign in with Google")
                }
            }
        }
    }
}
