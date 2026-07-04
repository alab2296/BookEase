package se.w3footprint.bookease.presentation.features.owner.manage_schedule

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import se.w3footprint.bookease.domain.model.TimeSlot
import se.w3footprint.bookease.domain.repository.BusinessRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageScheduleViewModel @Inject constructor(
    private val repository: BusinessRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    var slotsList by mutableStateOf<List<TimeSlot>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    init {
        loadSlots()
    }

    private fun loadSlots() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            isLoading = true
            repository.getTimeSlots(userId).collect { slots ->
                slotsList = slots
                isLoading = false
            }
        }
    }

    fun deleteSlot(slotId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.deleteTimeSlot(uid, slotId)
        }
    }

    fun addSlot(day: String, start: String, end: String, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val slot = TimeSlot(
                businessId = userId,
                day = day,
                startTime = start,
                endTime = end
            )
            val result = repository.addTimeSlot(slot)
            onComplete(result.isSuccess)
        }
    }
}
