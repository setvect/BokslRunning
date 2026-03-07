package com.boksl.running.ui.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boksl.running.domain.model.Gender
import com.boksl.running.domain.model.Profile
import com.boksl.running.domain.repository.ProfileRepository
import com.boksl.running.ui.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileFormViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val profileRepository: ProfileRepository,
    ) : ViewModel() {
        private val entryPoint =
            ProfileSetupEntryPoint.fromRouteValue(
                savedStateHandle.get<String>(AppRoute.ProfileSetup.ENTRY_POINT_ARG),
            )

        private val _uiState = MutableStateFlow(ProfileFormUiState())
        val uiState: StateFlow<ProfileFormUiState> = _uiState.asStateFlow()

        private val _navigationEvent = MutableSharedFlow<ProfileFormNavigationEvent>()
        val navigationEvent: SharedFlow<ProfileFormNavigationEvent> = _navigationEvent.asSharedFlow()

        init {
            _uiState.value = _uiState.value.copy(entryPoint = entryPoint)
            if (entryPoint == ProfileSetupEntryPoint.Settings) {
                viewModelScope.launch {
                    profileRepository.observeProfile().first()?.let { profile ->
                        _uiState.value = profile.toUiState(entryPoint = entryPoint)
                    }
                }
            }
        }

        fun onWeightChanged(value: String) {
            _uiState.value = _uiState.value.copy(weightKg = value, weightError = null)
        }

        fun onGenderChanged(gender: Gender) {
            _uiState.value = _uiState.value.copy(gender = gender)
        }

        fun onAgeChanged(value: String) {
            _uiState.value = _uiState.value.copy(age = value, ageError = null)
        }

        fun saveProfile(currentTimeMillis: Long = System.currentTimeMillis()) {
            val validation = validateProfileForm(_uiState.value)
            if (!validation.isValid) {
                _uiState.value =
                    _uiState.value.copy(
                        weightError = validation.weightError,
                        ageError = validation.ageError,
                    )
                return
            }

            val currentState = _uiState.value
            viewModelScope.launch {
                profileRepository.saveProfile(
                    Profile(
                        weightKg = currentState.weightKg.toFloat(),
                        gender = currentState.gender,
                        age = currentState.age.toInt(),
                        updatedAtEpochMillis = currentTimeMillis,
                    ),
                )
                _navigationEvent.emit(
                    when (entryPoint) {
                        ProfileSetupEntryPoint.Onboarding -> ProfileFormNavigationEvent.NavigateToPermissionGate
                        ProfileSetupEntryPoint.Settings -> ProfileFormNavigationEvent.NavigateBackToSettings
                    },
                )
            }
        }
    }

data class ProfileFormUiState(
    val weightKg: String = "",
    val gender: Gender = Gender.UNSPECIFIED,
    val age: String = "",
    val weightError: String? = null,
    val ageError: String? = null,
    val entryPoint: ProfileSetupEntryPoint = ProfileSetupEntryPoint.Onboarding,
) {
    val isSaveEnabled: Boolean
        get() = weightKg.isNotBlank() && age.isNotBlank()
}

enum class ProfileSetupEntryPoint(val routeValue: String) {
    Onboarding("onboarding"),
    Settings("settings"),
    ;

    companion object {
        fun fromRouteValue(value: String?): ProfileSetupEntryPoint =
            entries.firstOrNull { it.routeValue == value } ?: Onboarding
    }
}

sealed interface ProfileFormNavigationEvent {
    data object NavigateToPermissionGate : ProfileFormNavigationEvent

    data object NavigateBackToSettings : ProfileFormNavigationEvent
}

data class ProfileFormValidationResult(
    val isValid: Boolean,
    val weightError: String? = null,
    val ageError: String? = null,
)

internal fun validateProfileForm(uiState: ProfileFormUiState): ProfileFormValidationResult {
    val weight = uiState.weightKg.toFloatOrNull()
    val age = uiState.age.toIntOrNull()
    val weightError =
        if (weight == null || weight <= 0f) {
            "몸무게를 0보다 크게 입력해 주세요."
        } else {
            null
        }
    val ageError =
        if (age == null || age <= 0) {
            "나이를 0보다 크게 입력해 주세요."
        } else {
            null
        }

    return ProfileFormValidationResult(
        isValid = weightError == null && ageError == null,
        weightError = weightError,
        ageError = ageError,
    )
}

private fun Profile.toUiState(entryPoint: ProfileSetupEntryPoint): ProfileFormUiState =
    ProfileFormUiState(
        weightKg = weightKg.toString(),
        gender = gender,
        age = age.toString(),
        entryPoint = entryPoint,
    )
