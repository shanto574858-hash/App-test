package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminRecord
import com.example.data.model.AppSettings
import com.example.data.model.AuditLog
import com.example.data.repository.AdminRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SuperAdminViewModel(
    private val adminRepo: AdminRepository
) : ViewModel() {

    val admins: StateFlow<List<AdminRecord>> = adminRepo.observeAdmins()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSettings: StateFlow<AppSettings?> = adminRepo.observeAppSettings()
        .catch { if (it is CancellationException) throw it; emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val auditLogs: StateFlow<List<AuditLog>> = adminRepo.observeAuditLogs()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun upsertAdmin(
        targetUid: String,
        targetEmail: String,
        targetName: String,
        permissions: List<String>
    ) {
        viewModelScope.launch {
            val res = adminRepo.manageAdminRole(targetUid, targetEmail, targetName, permissions, revoke = false)
            _statusMessage.value = if (res.isSuccess) {
                "অ্যাডমিন ও অনুমতি হালনাগাদ হয়েছে।"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "অ্যাডমিন আপডেট ব্যর্থ হয়েছে।"
            }
        }
    }

    fun revokeAdmin(targetUid: String) {
        viewModelScope.launch {
            val res = adminRepo.manageAdminRole(targetUid, "", "", emptyList(), revoke = true)
            _statusMessage.value = if (res.isSuccess) {
                "অ্যাডমিন অধিকার বাতিল করা হয়েছে।"
            } else {
                res.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun updateAppSettings(
        maintenanceMode: Boolean,
        allowRegistration: Boolean,
        defaultNegativeMarking: Double,
        announcementBanner: String
    ) {
        viewModelScope.launch {
            val res = adminRepo.updateAppSettings(
                maintenanceMode,
                allowRegistration,
                defaultNegativeMarking,
                announcementBanner
            )
            _statusMessage.value = if (res.isSuccess) {
                "অ্যাপ সেটিংস সংরক্ষিত হয়েছে।"
            } else {
                res.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun updateStudentStatus(uid: String, status: String) {
        viewModelScope.launch {
            val res = adminRepo.updateStudentStatus(uid, status)
            _statusMessage.value = if (res.isSuccess) {
                "শিক্ষার্থীর অবস্থা পরিবর্তিত হয়েছে।"
            } else {
                res.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
