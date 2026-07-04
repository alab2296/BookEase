package se.w3footprint.bookease.presentation.features.owner.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import se.w3footprint.bookease.domain.model.Business
import se.w3footprint.bookease.domain.model.OpeningHour
import se.w3footprint.bookease.domain.repository.BusinessRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BusinessProfileViewModel @Inject constructor(
    private val repository: BusinessRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    var businessName by mutableStateOf("")
    var description by mutableStateOf("")
    var category by mutableStateOf("")
    var address by mutableStateOf("")
    var phone by mutableStateOf("")
    var email by mutableStateOf("")
    var website by mutableStateOf("")
    var imageUrls by mutableStateOf("")
    
    var openingHours by mutableStateOf(listOf(
        OpeningHour("Monday", "09:00", "17:00"),
        OpeningHour("Tuesday", "09:00", "17:00"),
        OpeningHour("Wednesday", "09:00", "17:00"),
        OpeningHour("Thursday", "09:00", "17:00"),
        OpeningHour("Friday", "09:00", "17:00"),
        OpeningHour("Saturday", "10:00", "14:00"),
        OpeningHour("Sunday", "", "", isClosed = true)
    ))
    
    var isLoading by mutableStateOf(false)
        private set

    fun onImageSelected(uri: android.net.Uri) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            isLoading = true
            val result = repository.uploadBusinessImage(uid, uri)
            if (result.isSuccess) {
                val newUrl = result.getOrNull() ?: ""
                val currentList = imageUrls.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                currentList.add(newUrl)
                imageUrls = currentList.joinToString(", ")
            }
            isLoading = false
        }
    }

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            isLoading = true
            val business = repository.getBusinessById(uid)
            if (business != null) {
                businessName = business.name
                description = business.description
                category = business.category
                address = business.address
                phone = business.phone
                email = business.email
                website = business.website
                imageUrls = business.gallery.joinToString(", ")
                if (business.openingHours.isNotEmpty()) {
                    openingHours = business.openingHours
                }
            }
            isLoading = false
        }
    }

    fun updateOpeningHour(index: Int, updated: OpeningHour) {
        val list = openingHours.toMutableList()
        list[index] = updated
        openingHours = list
    }

    fun saveProfile(onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            isLoading = true
            val business = Business(
                ownerId = uid,
                name = businessName,
                description = description,
                category = category,
                address = address,
                phone = phone,
                email = email,
                website = website,
                gallery = imageUrls.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                openingHours = openingHours
            )
            val result = repository.saveBusinessProfile(business)
            isLoading = false
            onComplete(result.isSuccess)
        }
    }
}
