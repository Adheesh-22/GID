package com.adheesh.gid.feature.settings.viewmodel

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.R
import com.adheesh.gid.core.database.GIDPreferences
import com.adheesh.gid.core.database.LocalProfileEntity
import com.adheesh.gid.core.scheduling.EveningPromptScheduler
import com.adheesh.gid.core.util.nowMillis
import com.adheesh.gid.core.util.today
import com.adheesh.gid.feature.auth.data.AuthRepository
import com.adheesh.gid.feature.settings.data.ProfilePhotoStore
import com.adheesh.gid.feature.settings.data.ProfileRepository
import com.adheesh.gid.feature.settings.data.SaveOutcome
import com.adheesh.gid.feature.settings.domain.AgeCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loaded       : Boolean = false,
    val name         : String  = "",
    val dobEpochDay  : Long?   = null,
    val phone        : String  = "",
    val institution  : String  = "",
    val email        : String  = "",
    val saving       : Boolean = false,
    @param:StringRes val nameError : Int? = null,
    @param:StringRes val dobError  : Int? = null,
    /** One-shot message ('Saved' / 'Will sync when online'), shown as a snackbar. */
    @param:StringRes val message   : Int? = null,
) {
    val age: Int? get() = AgeCalculator.ageYears(dobEpochDay)
}

class SettingsViewModel(
    private val profiles    : ProfileRepository,
    private val auth        : AuthRepository,
    private val photos      : ProfilePhotoStore,
    private val preferences : GIDPreferences,
    private val evening     : EveningPromptScheduler,
) : ViewModel() {

    private val _ui = MutableStateFlow(SettingsUiState())
    val ui: StateFlow<SettingsUiState> = _ui.asStateFlow()

    val photoPath: StateFlow<String?> = photos.path.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val incomplete: StateFlow<Boolean> = profiles.observeIncomplete()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val eveningPromptMinutes: StateFlow<Int> = preferences.eveningPromptTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GIDPreferences.DEFAULT_EVENING_PROMPT_MINUTES)

    init {
        // Load once into editable fields; later DB changes must not overwrite typing.
        viewModelScope.launch {
            val user    = auth.signedInUser.first()
            val profile = profiles.get()
            _ui.update {
                it.copy(
                    loaded      = true,
                    email       = user?.email.orEmpty(),
                    name        = profile?.fullName.orEmpty(),
                    dobEpochDay = profile?.dobEpochDay,
                    phone       = profile?.phone.orEmpty(),
                    institution = profile?.institution.orEmpty(),
                )
            }
        }
    }

    fun setName(v: String)        = _ui.update { it.copy(name = v, nameError = null) }
    fun setPhone(v: String)       = _ui.update { it.copy(phone = v) }
    fun setInstitution(v: String) = _ui.update { it.copy(institution = v) }
    fun setDob(epochDay: Long?)   = _ui.update { it.copy(dobEpochDay = epochDay, dobError = null) }
    fun messageShown()            = _ui.update { it.copy(message = null) }

    fun save() {
        val s = _ui.value
        val nameError = if (s.name.isBlank()) R.string.settings_error_name else null
        val dobError  = when {
            s.dobEpochDay == null                       -> R.string.settings_error_dob
            s.dobEpochDay > today().toEpochDay()        -> R.string.settings_error_dob_future
            else                                        -> null
        }
        if (nameError != null || dobError != null) {
            _ui.update { it.copy(nameError = nameError, dobError = dobError) }
            return
        }
        _ui.update { it.copy(saving = true) }
        viewModelScope.launch {
            val user = auth.signedInUser.first() ?: run {
                _ui.update { it.copy(saving = false) }
                return@launch
            }
            val now      = nowMillis()
            val existing = profiles.get()
            val profile  = LocalProfileEntity(
                id          = user.id,
                fullName    = s.name.trim(),
                dobEpochDay = s.dobEpochDay,
                phone       = s.phone.trim().ifBlank { null },
                institution = s.institution.trim().ifBlank { null },
                needsSync   = true,
                createdAt   = existing?.createdAt ?: now,
                updatedAt   = now,
            )
            val outcome = profiles.save(profile, user.email)
            _ui.update {
                it.copy(
                    saving  = false,
                    message = if (outcome == SaveOutcome.SYNCED) R.string.settings_saved else R.string.settings_saved_offline,
                )
            }
        }
    }

    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            if (!photos.save(uri)) _ui.update { it.copy(message = R.string.settings_photo_failed) }
        }
    }

    fun setEveningPrompt(minutes: Int) {
        viewModelScope.launch {
            preferences.setEveningPromptTime(minutes)
            evening.schedule()
        }
    }

    /** Clears the account and its profile; productivity data stays on the phone. */
    fun signOut() {
        viewModelScope.launch {
            profiles.deleteAll()
            photos.clear()
            auth.signOut()
        }
    }
}
