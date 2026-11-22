package com.example.ui.superadmin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminRecord
import com.example.data.model.AppSettings
import com.example.data.model.AuditLog
import com.example.data.model.UserProfile
import com.example.ui.components.ErrorBannerCard
import com.example.ui.theme.ErrorRed
import com.example.utils.AppConstants
import com.example.utils.AppStrings
import com.example.viewmodel.AppPanelMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuperAdminScreen(
    strings: AppStrings,
    currentUid: String,
    admins: List<AdminRecord>,
    students: List<UserProfile>,
    appSettings: AppSettings?,
    auditLogs: List<AuditLog>,
    onUpsertAdmin: (targetUid: String, email: String, name: String, permissions: List<String>) -> Unit,
    onRevokeAdmin: (targetUid: String) -> Unit,
    onUpdateStudentStatus: (uid: String, status: String) -> Unit,
    onUpdateAppSettings: (maintenance: Boolean, allowReg: Boolean, defaultNeg: Double, banner: String) -> Unit,
    onSwitchPanel: (AppPanelMode) -> Unit
) {
    // Hard UI check in addition to server-side Firebase rules & Cloud Function verification
    if (currentUid != AppConstants.SUPER_ADMIN_UID) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            ErrorBannerCard(message = "Unauthorized: Super Admin UID mismatch.")
        }
        return
    }

    var activeTab by remember { mutableIntStateOf(0) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var editingAdmin by remember { mutableStateOf<AdminRecord?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (strings.isBn) "সুপার অ্যাডমিন নিয়ন্ত্রণ" else "Super Admin Control",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { onSwitchPanel(AppPanelMode.ADMIN) }) {
                    Text("Admin")
                }
                OutlinedButton(onClick = { onSwitchPanel(AppPanelMode.STUDENT) }) {
                    Text("Student")
                }
            }
        }

        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Admins (${admins.size})") })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Users (${students.size})") })
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Settings") })
            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }, text = { Text("Logs") })
        }

        when (activeTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Button(
                            onClick = {
                                editingAdmin = null
                                showAdminDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("super_admin_add_admin_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (strings.isBn) "নতুন অ্যাডমিন যোগ / অনুমতি দিন" else "Add or Update Admin")
                        }
                    }

                    items(admins, key = { it.uid }) { admin ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(admin.name.ifBlank { admin.email.ifBlank { admin.uid } }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("UID: ${admin.uid}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row {
                                        IconButton(onClick = { editingAdmin = admin; showAdminDialog = true }) {
                                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Edit permissions")
                                        }
                                        IconButton(onClick = { onRevokeAdmin(admin.uid) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Revoke admin", tint = ErrorRed)
                                        }
                                    }
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    admin.permissions.forEach { perm ->
                                        Text(
                                            text = "[$perm]",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(students, key = { it.uid }) { user ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.name.ifBlank { user.email }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${user.email} • ${user.role} • ${user.status}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (user.uid != AppConstants.SUPER_ADMIN_UID) {
                                    val nextStatus = if (user.status == "active") "suspended" else "active"
                                    OutlinedButton(onClick = { onUpdateStudentStatus(user.uid, nextStatus) }) {
                                        Text(if (user.status == "active") "Suspend" else "Activate")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                var maintenance by remember(appSettings) { mutableStateOf(appSettings?.maintenanceMode ?: false) }
                var allowReg by remember(appSettings) { mutableStateOf(appSettings?.allowRegistration ?: true) }
                var defaultNeg by remember(appSettings) { mutableStateOf((appSettings?.defaultNegativeMarking ?: 0.5).toString()) }
                var banner by remember(appSettings) { mutableStateOf(appSettings?.announcementBanner.orEmpty()) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (strings.isBn) "মেইনটেন্যান্স মোড" else "Maintenance Mode")
                        Switch(checked = maintenance, onCheckedChange = { maintenance = it })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (strings.isBn) "নতুন নিবন্ধন অনুমোদন" else "Allow Registration")
                        Switch(checked = allowReg, onCheckedChange = { allowReg = it })
                    }
                    OutlinedTextField(
                        value = defaultNeg,
                        onValueChange = { defaultNeg = it },
                        label = { Text(if (strings.isBn) "ডিফল্ট নেগেটিভ মার্কিং" else "Default Negative Marking") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = banner,
                        onValueChange = { banner = it },
                        label = { Text(if (strings.isBn) "ঘোষণা ব্যানার" else "Announcement Banner") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onUpdateAppSettings(maintenance, allowReg, defaultNeg.toDoubleOrNull() ?: 0.5, banner)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.save)
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(auditLogs, key = { it.id }) { log ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${log.action} • ${log.targetPath}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("Actor: ${log.actorUid} • ${log.details}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdminDialog) {
        var uid by remember(editingAdmin) { mutableStateOf(editingAdmin?.uid.orEmpty()) }
        var email by remember(editingAdmin) { mutableStateOf(editingAdmin?.email.orEmpty()) }
        var name by remember(editingAdmin) { mutableStateOf(editingAdmin?.name.orEmpty()) }
        var selectedPerms by remember(editingAdmin) {
            mutableStateOf(editingAdmin?.permissions?.toSet() ?: AppConstants.ALL_ADMIN_PERMISSIONS.toSet())
        }

        AlertDialog(
            onDismissRequest = { showAdminDialog = false },
            title = { Text(if (strings.isBn) "অ্যাডমিন ভূমিকা ও অনুমতি" else "Admin Role & Permissions") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uid,
                        onValueChange = { uid = it },
                        enabled = editingAdmin == null,
                        label = { Text("Firebase User UID *") },
                        singleLine = true
                    )
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("নাম") }, singleLine = true)
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("ইমেইল") }, singleLine = true)
                    Text(if (strings.isBn) "অনুমতিসমূহ:" else "Permissions:", style = MaterialTheme.typography.labelLarge)
                    AppConstants.ALL_ADMIN_PERMISSIONS.forEach { perm ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = perm in selectedPerms,
                                onCheckedChange = { c ->
                                    selectedPerms = if (c) selectedPerms + perm else selectedPerms - perm
                                }
                            )
                            Text(perm, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = uid.isNotBlank(),
                    onClick = {
                        onUpsertAdmin(uid.trim(), email.trim(), name.trim(), selectedPerms.toList())
                        showAdminDialog = false
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}
