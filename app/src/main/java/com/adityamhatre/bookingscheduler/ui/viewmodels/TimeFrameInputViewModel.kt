package com.adityamhatre.bookingscheduler.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adityamhatre.bookingscheduler.adapters.BookingListAdapter
import com.adityamhatre.bookingscheduler.dtos.AdapterContainer
import com.adityamhatre.bookingscheduler.dtos.AppDateTime
import com.adityamhatre.bookingscheduler.enums.Accommodation
import com.adityamhatre.bookingscheduler.service.BookingsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration

class TimeFrameInputViewModel : ViewModel() {
    var adapterContainer: AdapterContainer<BookingListAdapter>? = null
    val accommodationCheckBoxIds = mutableListOf<Int>()
    var selectWholeResort = false
    var bungalow51Selected = false
    var bungalowAndRoomsSelected = false
    private val selectedAccommodations = MutableLiveData<Set<Accommodation>>(mutableSetOf())

    var alreadyChecked = false

    var isOneDayBooking = false

    fun addAccommodation(accommodation: Accommodation) {
        val value: HashSet<Accommodation> =
            (selectedAccommodations.value as HashSet<Accommodation>)
        value.add(accommodation)
        selectedAccommodations.value = selectedAccommodations.value
    }

    fun removeAccommodation(accommodation: Accommodation) {
        val value: HashSet<Accommodation> =
            (selectedAccommodations.value as HashSet<Accommodation>?)!!
        value.remove(accommodation)
        selectedAccommodations.value = selectedAccommodations.value
    }

    fun clearAccommodations() {
        val value: HashSet<Accommodation> =
            (selectedAccommodations.value as HashSet<Accommodation>)
        value.clear()
        selectedAccommodations.value = selectedAccommodations.value
    }

    fun getSelectedAccommodations(): LiveData<Set<Accommodation>> = selectedAccommodations

    private val bookingsService = BookingsService()
    var checkOutDateTime = AppDateTime(-1, -1, -1, -1, -1)

    var checkInDateTime = AppDateTime(-1, -1, -1, -1, -1)

    suspend fun checkAvailability(timeMin: AppDateTime, timeMax: AppDateTime): List<Accommodation> {
        return withContext(Dispatchers.IO) {
            return@withContext bookingsService.checkAvailability(timeMin, timeMax)
        }
    }

    fun isValid() =
        checkInDateTime.isValid() && checkOutDateTime.isValid() && selectedAccommodations.value!!.isNotEmpty()

    fun isDurationValid(): Boolean {
        if (!checkInDateTime.isValid() || !checkOutDateTime.isValid()) return false
        val duration = Duration.between(checkInDateTime.toInstant(), checkOutDateTime.toInstant())
        return duration.toDays() <= 15
    }
}
