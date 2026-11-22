package com.example.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.data.model.UserSessionRole
import com.example.ui.auth.signOutUser
import com.example.utils.AppLanguage
import com.example.utils.AppStrings
import com.example.utils.ThemeMode
import com.example.viewmodel.AppPanelMode

@Composable
fun ProfileScreen(
    strings: AppStrings,
    userProfile: UserProfile?,
    sessionRole: UserSessionRole,
    currentThemeMode: ThemeMode,
    currentLanguage: AppLanguage,
    onUpdateProfile: (name: String, phone: String, langCode: String) -> Unit,
    onUploadPhoto: (Uri) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSwitchPanel: (AppPanelMode) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenWrongAnswers: () -> Unit,
    onOpenOfflineSets: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showEditDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUploadPhoto(uri)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Profile Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        val photo = userProfile?.photoUrl.orEmpty()
                        if (photo.isNotBlank()) {
                            AsyncImage(
                                model = photo,
                                contentDescription = "Profile photo",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile photo",
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change photo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = userProfile?.name?.ifBlank { null } ?: if (strings.isBn) "শিক্ষার্থী" else "Student",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (!userProfile?.email.isNullOrBlank()) {
                            Text(
                                text = userProfile!!.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!userProfile?.phone.isNullOrBlank()) {
                            Text(
                                text = userProfile!!.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val roleBadge = when {
                            sessionRole.isSuperAdmin -> if (strings.isBn) "সুপার অ্যাডমিন" else "Super Admin"
                            sessionRole.isAdmin -> if (strings.isBn) "অ্যাডমিন" else "Admin"
                            else -> if (strings.isBn) "শিক্ষার্থী" else "Student"
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = roleBadge,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit profile")
                    }
                }
            }
        }

        // Strict Role-Gated Panel Switcher (ONLY shown to Admin / Super Admin)
        if (sessionRole.isAdmin || sessionRole.isSuperAdmin) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (strings.isBn) "প্যানেল পরিবর্তন" else "Panel Switcher",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (sessionRole.isAdmin) {
                            Button(
                                onClick = { onSwitchPanel(AppPanelMode.ADMIN) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("switch_to_admin_panel_button")
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (strings.isBn) "অ্যাডমিন প্যানেল খুলুন" else "Open Admin Panel")
                            }
                        }
                        if (sessionRole.isSuperAdmin) {
                            OutlinedButton(
                                onClick = { onSwitchPanel(AppPanelMode.SUPER_ADMIN) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("switch_to_super_admin_button")
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (strings.isBn) "সুপার অ্যাডমিন প্যানেল খুলুন" else "Open Super Admin Panel")
                            }
                        }
                    }
                }
            }
        }

        // Quick Learning Shortcuts
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ProfileLinkRow(
                        icon = Icons.Default.Bookmark,
                        title = if (strings.isBn) "বুকমার্ক করা প্রশ্ন" else "Bookmarks",
                        onClick = onOpenBookmarks
                    )
                    ProfileLinkRow(
                        icon = Icons.Default.ErrorOutline,
                        title = if (strings.isBn) "ভুল উত্তর তালিকা" else "Wrong Answer Practice",
                        onClick = onOpenWrongAnswers
                    )
                    ProfileLinkRow(
                        icon = Icons.Default.CloudDownload,
                        title = if (strings.isBn) "অফলাইন সংরক্ষিত সেট" else "Offline Cached Sets",
                        onClick = onOpenOfflineSets
                    )
                }
            }
        }

        // Language & Theme Settings
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (strings.isBn) "ভাষা নির্বাচন" else "Language",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = currentLanguage == lang,
                                onClick = { onSetLanguage(lang) },
                                label = { Text(lang.displayLabel) }
                            )
                        }
                    }

                    Text(
                        text = if (strings.isBn) "থিম (ডার্ক মোড)" else "Theme Mode",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            val label = when (mode) {
                                ThemeMode.SYSTEM -> if (strings.isBn) "সিস্টেম" else "System"
                                ThemeMode.LIGHT -> if (strings.isBn) "লাইট" else "Light"
                                ThemeMode.DARK -> if (strings.isBn) "ডার্ক" else "Dark"
                            }
                            FilterChip(
                                selected = currentThemeMode == mode,
                                onClick = { onSetThemeMode(mode) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        // Sign Out
        item {
            OutlinedButton(
                onClick = {
                    signOutUser(credentialManager = credentialManager, scope = scope)
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (strings.isBn) "সাইন আউট" else "Sign Out")
            }
        }
    }

    if (showEditDialog) {
        var name by remember(userProfile) { mutableStateOf(userProfile?.name.orEmpty()) }
        var phone by remember(userProfile) { mutableStateOf(userProfile?.phone.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (strings.isBn) "প্রোফাইল সম্পাদনা" else "Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (strings.isBn) "পূর্ণ নাম" else "Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (strings.isBn) "মোবাইল নম্বর" else "Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateProfile(name, phone, currentLanguage.code)
                    showEditDialog = false
                }) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun ProfileLinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}
