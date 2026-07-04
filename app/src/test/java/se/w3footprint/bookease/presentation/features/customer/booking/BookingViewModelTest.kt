package se.w3footprint.bookease.presentation.features.customer.booking

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import se.w3footprint.bookease.domain.model.Appointment
import se.w3footprint.bookease.domain.model.Business
import se.w3footprint.bookease.domain.model.OpeningHour
import se.w3footprint.bookease.domain.model.TimeSlot
import se.w3footprint.bookease.domain.repository.BookingRepository
import se.w3footprint.bookease.domain.repository.BusinessRepository

@OptIn(ExperimentalCoroutinesApi::class)
class BookingViewModelTest {

    private lateinit var bookingRepository: BookingRepository
    private lateinit var businessRepository: BusinessRepository
    private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: BookingViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        bookingRepository = mockk()
        businessRepository = mockk()
        auth = mockk()
        viewModel = BookingViewModel(bookingRepository, businessRepository, auth)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty slots and no selection`() {
        assertTrue(viewModel.timeSlotsList.isEmpty())
        assertNull(viewModel.selectedSlot)
        assertEquals("", viewModel.selectedDate)
        assertNull(viewModel.business)
        assertFalse(viewModel.isLoading)
        assertFalse(viewModel.isBooking)
    }

    @Test
    fun `loadBusiness sets business from repository`() = runTest {
        val business = Business(id = "biz1", name = "Test Salon", ownerId = "owner1")
        coEvery { businessRepository.getBusinessById("owner1") } returns business

        viewModel.loadBusiness("owner1")

        assertEquals(business, viewModel.business)
        assertFalse(viewModel.isLoading)
    }

    @Test
    fun `loadBusiness sets business to null when not found`() = runTest {
        coEvery { businessRepository.getBusinessById("unknown") } returns null

        viewModel.loadBusiness("unknown")

        assertNull(viewModel.business)
    }

    @Test
    fun `onDateSelected updates selectedDate and clears selectedSlot`() = runTest {
        val business = Business(
            id = "biz1",
            ownerId = "owner1",
            openingHours = listOf(OpeningHour(day = "Monday", isClosed = true))
        )
        coEvery { businessRepository.getBusinessById("owner1") } returns business
        viewModel.loadBusiness("owner1")

        viewModel.selectedSlot = TimeSlot(id = "slot1", day = "Sunday")
        viewModel.onDateSelected("owner1", "Monday", 60)

        assertEquals("Monday", viewModel.selectedDate)
        assertNull(viewModel.selectedSlot)
    }

    @Test
    fun `generateAvailableSlots returns empty list for closed day`() = runTest {
        val business = Business(
            id = "biz1",
            ownerId = "owner1",
            openingHours = listOf(OpeningHour(day = "Monday", isClosed = true))
        )
        coEvery { businessRepository.getBusinessById("owner1") } returns business
        viewModel.loadBusiness("owner1")

        viewModel.onDateSelected("owner1", "Monday", 60)

        assertTrue(viewModel.timeSlotsList.isEmpty())
    }

    @Test
    fun `generateAvailableSlots returns empty list when business not loaded`() = runTest {
        viewModel.onDateSelected("owner1", "Monday", 60)

        assertTrue(viewModel.timeSlotsList.isEmpty())
    }

    @Test
    fun `generateAvailableSlots produces slots for open day`() = runTest {
        val business = Business(
            id = "biz1",
            ownerId = "owner1",
            openingHours = listOf(
                OpeningHour(day = "Saturday", openTime = "09:00", closeTime = "11:00", isClosed = false)
            )
        )
        coEvery { businessRepository.getBusinessById("owner1") } returns business
        every { bookingRepository.getBookingsForOwnerByDay("owner1", "Saturday") } returns flowOf(emptyList())
        // Freeze clock to 1970-01-01 00:00 (Thursday) so "Saturday" is never treated as today
        viewModel.clock = { 0L }
        viewModel.loadBusiness("owner1")

        viewModel.onDateSelected("owner1", "Saturday", 60)

        assertTrue(viewModel.timeSlotsList.isNotEmpty())
    }

    @Test
    fun `confirmBooking without selected slot does nothing`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "user1"

        viewModel.confirmBooking("owner1", "Haircut", "200", "60")

        assertFalse(viewModel.isBooking)
    }

    @Test
    fun `confirmBooking without logged in user does nothing`() = runTest {
        every { auth.currentUser } returns null
        viewModel.selectedSlot = TimeSlot(id = "s1", day = "Monday", startTime = "09:00", endTime = "10:00")

        viewModel.confirmBooking("owner1", "Haircut", "200", "60")

        assertFalse(viewModel.isBooking)
    }

    @Test
    fun `confirmBooking success emits Success event`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "user1"
        coEvery { bookingRepository.createBooking(any()) } returns Result.success(Unit)

        viewModel.selectedSlot = TimeSlot(id = "s1", day = "Monday", startTime = "09:00", endTime = "10:00")

        val events = mutableListOf<BookingViewModel.BookingEvent>()
        val job = launch(testDispatcher) { viewModel.eventFlow.toList(events) }

        viewModel.confirmBooking("owner1", "Haircut", "200", "60")

        job.cancel()
        assertTrue(events.any { it is BookingViewModel.BookingEvent.Success })
        assertFalse(viewModel.isBooking)
    }

    @Test
    fun `confirmBooking failure emits Error event`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "user1"
        coEvery { bookingRepository.createBooking(any()) } returns Result.failure(Exception("Slot taken"))

        viewModel.selectedSlot = TimeSlot(id = "s1", day = "Monday", startTime = "09:00", endTime = "10:00")

        val events = mutableListOf<BookingViewModel.BookingEvent>()
        val job = launch(testDispatcher) { viewModel.eventFlow.toList(events) }

        viewModel.confirmBooking("owner1", "Haircut", "200", "60")

        job.cancel()
        val error = events.filterIsInstance<BookingViewModel.BookingEvent.Error>().first()
        assertEquals("Slot taken", error.message)
        assertFalse(viewModel.isBooking)
    }

    @Test
    fun `booked slots are excluded from available slots`() = runTest {
        val business = Business(
            id = "biz1",
            ownerId = "owner1",
            openingHours = listOf(
                OpeningHour(day = "Saturday", openTime = "09:00", closeTime = "11:00", isClosed = false)
            )
        )
        val existingBooking = Appointment(
            id = "b1",
            ownerId = "owner1",
            startTime = "09:00",
            duration = "60",
            status = "ACCEPTED",
            day = "Saturday"
        )
        coEvery { businessRepository.getBusinessById("owner1") } returns business
        every { bookingRepository.getBookingsForOwnerByDay("owner1", "Saturday") } returns flowOf(listOf(existingBooking))
        viewModel.clock = { 0L }
        viewModel.loadBusiness("owner1")

        viewModel.onDateSelected("owner1", "Saturday", 60)

        val bookedStartTimes = viewModel.timeSlotsList.map { it.startTime }
        assertTrue("09:00" !in bookedStartTimes)
    }
}
