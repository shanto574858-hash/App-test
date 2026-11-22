package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.model.UserProfile
import com.example.data.model.UserSessionRole
import com.example.ui.auth.signOutUser
import com.example.utils.AppStrings
import com.example.viewmodel.AppPanelMode

@Composable
fun AdminSettingsScreen(
    strings: AppStrings,
    sessionRole: UserSessionRole,
    students: List<UserProfile>,
    onBroadcastNotification: (title: String, body: String, category: String) -> Unit,
    onSwitchPanel: (AppPanelMode) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var notifTitle by remember { mutableStateOf("") }
    var notifBody by remember { mutableStateOf("") }
    var notifCategory by remember { mutableStateOf("announcement") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = if (strings.isBn) "অ্যাডমিন সেটিংস ও বিজ্ঞপ্তি" else "Admin Settings & Notifications",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (strings.isBn) "সকল শিক্ষার্থীর কাছে বিজ্ঞপ্তি পাঠান" else "Broadcast Notification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = notifTitle,
                        onValueChange = { notifTitle = it },
                        label = { Text(if (strings.isBn) "শিরোনাম" else "Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notifBody,
                        onValueChange = { notifBody = it },
                        label = { Text(if (strings.isBn) "বার্তা" else "Message") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("announcement", "new_exam", "new_content").forEach { cat ->
                            FilterChip(selected = notifCategory == cat, onClick = { notifCategory = cat }, label = { Text(cat) })
                        }
                    }
                    Button(
                        onClick = {
                            if (notifTitle.isNotBlank() && notifBody.isNotBlank()) {
                                onBroadcastNotification(notifTitle, notifBody, notifCategory)
                                notifTitle = ""
                                notifBody = ""
                            }
                        },
                        enabled = notifTitle.isNotBlank() && notifBody.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (strings.isBn) "বিজ্ঞপ্তি পাঠান" else "Send Notification")
                    }
                }
            }
        }

        if (sessionRole.hasPermission("student_manage")) {
            item {
                Text(
                    text = if (strings.isBn) "নিবন্ধিত শিক্ষার্থী (${students.size})" else "Registered Students (${students.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(students.take(15), key = { it.uid }) { st ->
                OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(st.name.ifBlank { st.email }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${st.email} • পরীক্ষা: ${st.totalExams} • সঠিক: ${st.totalCorrect}/${st.totalAttempted} • ${st.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { onSwitchPanel(AppPanelMode.STUDENT) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (strings.isBn) "শিক্ষার্থী প্যানেলে ফিরে যান" else "Switch to Student Panel")
            }
        }

        if (sessionRole.isSuperAdmin) {
            item {
                Button(
                    onClick = { onSwitchPanel(AppPanelMode.SUPER_ADMIN) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (strings.isBn) "সুপার অ্যাডমিন প্যানেল খুলুন" else "Open Super Admin Panel")
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { signOutUser(credentialManager, scope) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (strings.isBn) "সাইন আউট" else "Sign Out")
            }
        }
    }
}
