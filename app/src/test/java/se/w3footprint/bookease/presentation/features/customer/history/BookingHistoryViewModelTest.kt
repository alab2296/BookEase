package se.w3footprint.bookease.presentation.features.customer.history

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import se.w3footprint.bookease.domain.model.Appointment
import se.w3footprint.bookease.domain.model.Review
import se.w3footprint.bookease.domain.repository.BookingRepository
import se.w3footprint.bookease.domain.repository.ReviewRepository

@OptIn(ExperimentalCoroutinesApi::class)
class BookingHistoryViewModelTest {

    private lateinit var bookingRepository: BookingRepository
    private lateinit var reviewRepository: ReviewRepository
    private lateinit var auth: FirebaseAuth
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        bookingRepository = mockk()
        reviewRepository = mockk()
        auth = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = BookingHistoryViewModel(bookingRepository, reviewRepository, auth)

    @Test
    fun `when user is null initial state stays Loading`() = runTest {
        every { auth.currentUser } returns null

        val viewModel = buildViewModel()

        assertTrue(viewModel.uiState.value is BookingHistoryUiState.Loading)
    }

    @Test
    fun `success state emitted when bookings load`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "user1"

        val bookings = listOf(
            Appointment(id = "a1", customerId = "user1", status = "COMPLETED"),
            Appointment(id = "a2", customerId = "user1", status = "PENDING")
        )
        every { bookingRepository.getBookingsForCustomer("user1") } returns flowOf(bookings)

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is BookingHistoryUiState.Success)
        assertEquals(bookings, (state as BookingHistoryUiState.Success).bookings)
    }

    @Test
    fun `empty bookings results in Success with empty list`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "user1"
        every { bookingRepository.getBookingsForCustomer("user1") } returns flowOf(emptyList())

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is BookingHistoryUiState.Success)
        assertTrue((state as BookingHistoryUiState.Success).bookings.isEmpty())
    }

    @Test
    fun `error state emitted when repository throws`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "user1"
        every { bookingRepository.getBookingsForCustomer("user1") } returns flow {
            throw Exception("Connection failed")
        }

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is BookingHistoryUiState.Error)
        assertEquals("Connection failed", (state as BookingHistoryUiState.Error).message)
    }

    @Test
    fun `error with null message uses fallback`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "user1"
        every { bookingRepository.getBookingsForCustomer("user1") } returns flow { throw Exception() }

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is BookingHistoryUiState.Error)
        assertEquals("Unknown error", (state as BookingHistoryUiState.Error).message)
    }

    @Test
    fun `submitReview calls reviewRepository with correct data`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "user1"
        every { bookingRepository.getBookingsForCustomer("user1") } returns flowOf(emptyList())
        coEvery { reviewRepository.addReview(any()) } returns Result.success(Unit)

        val viewModel = buildViewModel()
        viewModel.submitReview("biz1", 4.5f, "Great service")

        coVerify {
            reviewRepository.addReview(
                match { it.customerId == "user1" && it.businessId == "biz1" && it.rating == 4.5f && it.comment == "Great service" }
            )
        }
    }

    @Test
    fun `submitReview does nothing when user is not logged in`() = runTest {
        every { auth.currentUser } returns null

        val viewModel = buildViewModel()
        viewModel.submitReview("biz1", 5f, "Perfect")

        coVerify(exactly = 0) { reviewRepository.addReview(any()) }
    }

    @Test
    fun `cancelBooking calls repository with correct id`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "user1"
        every { bookingRepository.getBookingsForCustomer("user1") } returns flowOf(emptyList())
        coEvery { bookingRepository.cancelBooking("booking-123") } returns Result.success(Unit)

        val viewModel = buildViewModel()
        viewModel.cancelBooking("booking-123")

        coVerify { bookingRepository.cancelBooking("booking-123") }
    }

    @Test
    fun `deleteBooking calls repository with correct id`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "user1"
        every { bookingRepository.getBookingsForCustomer("user1") } returns flowOf(emptyList())
        coEvery { bookingRepository.deleteBooking("booking-456") } returns Result.success(Unit)

        val viewModel = buildViewModel()
        viewModel.deleteBooking("booking-456")

        coVerify { bookingRepository.deleteBooking("booking-456") }
    }
}
