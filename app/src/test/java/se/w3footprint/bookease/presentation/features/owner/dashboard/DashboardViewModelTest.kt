package se.w3footprint.bookease.presentation.features.owner.dashboard

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import se.w3footprint.bookease.domain.model.Appointment
import se.w3footprint.bookease.domain.model.Business
import se.w3footprint.bookease.domain.repository.BookingRepository
import se.w3footprint.bookease.domain.repository.BusinessRepository

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var businessRepository: BusinessRepository
    private lateinit var bookingRepository: BookingRepository
    private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: DashboardViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        businessRepository = mockk()
        bookingRepository = mockk()
        auth = mockk()

        val user = mockk<FirebaseUser>()
        every { user.uid } returns "owner123"
        every { auth.currentUser } returns user
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial businessName is empty`() = runTest {
        coEvery { businessRepository.getBusinessById(any()) } returns null
        every { bookingRepository.getBookingsForOwner(any()) } returns flowOf(emptyList())

        viewModel = DashboardViewModel(businessRepository, bookingRepository, auth)

        assertEquals("", viewModel.businessName.value)
    }

    @Test
    fun `loads business name from repository`() = runTest {
        val business = Business(ownerId = "owner123", name = "Test Salon")
        coEvery { businessRepository.getBusinessById("owner123") } returns business
        every { bookingRepository.getBookingsForOwner("owner123") } returns flowOf(emptyList())

        viewModel = DashboardViewModel(businessRepository, bookingRepository, auth)

        assertEquals("Test Salon", viewModel.businessName.value)
    }

    @Test
    fun `counts pending bookings correctly`() = runTest {
        val bookings = listOf(
            Appointment(status = "PENDING"),
            Appointment(status = "PENDING"),
            Appointment(status = "ACCEPTED")
        )
        coEvery { businessRepository.getBusinessById(any()) } returns null
        every { bookingRepository.getBookingsForOwner("owner123") } returns flowOf(bookings)

        viewModel = DashboardViewModel(businessRepository, bookingRepository, auth)

        assertEquals(2, viewModel.pendingCount.value)
    }

    @Test
    fun `counts total bookings correctly`() = runTest {
        val bookings = listOf(
            Appointment(status = "PENDING"),
            Appointment(status = "ACCEPTED"),
            Appointment(status = "COMPLETED")
        )
        coEvery { businessRepository.getBusinessById(any()) } returns null
        every { bookingRepository.getBookingsForOwner("owner123") } returns flowOf(bookings)

        viewModel = DashboardViewModel(businessRepository, bookingRepository, auth)

        assertEquals(3, viewModel.totalBookings.value)
    }

    @Test
    fun `handles null user gracefully`() = runTest {
        every { auth.currentUser } returns null

        viewModel = DashboardViewModel(businessRepository, bookingRepository, auth)

        assertEquals("", viewModel.businessName.value)
        assertEquals(0, viewModel.pendingCount.value)
    }

    @Test
    fun `logout calls auth signOut`() = runTest {
        coEvery { businessRepository.getBusinessById(any()) } returns null
        every { bookingRepository.getBookingsForOwner(any()) } returns flowOf(emptyList())
        every { auth.signOut() } returns Unit

        viewModel = DashboardViewModel(businessRepository, bookingRepository, auth)
        viewModel.logout()

        verify { auth.signOut() }
    }
}
