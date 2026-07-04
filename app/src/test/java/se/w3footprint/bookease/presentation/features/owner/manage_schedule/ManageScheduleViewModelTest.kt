package se.w3footprint.bookease.presentation.features.owner.manage_schedule

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import se.w3footprint.bookease.domain.model.TimeSlot
import se.w3footprint.bookease.domain.repository.BusinessRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ManageScheduleViewModelTest {

    private lateinit var repository: BusinessRepository
    private lateinit var auth: FirebaseAuth
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        auth = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = ManageScheduleViewModel(repository, auth)

    @Test
    fun `when user is null slots stay empty`() = runTest {
        every { auth.currentUser } returns null

        val viewModel = buildViewModel()

        assertTrue(viewModel.slotsList.isEmpty())
    }

    @Test
    fun `slots load on init when user is logged in`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "owner1"

        val slots = listOf(
            TimeSlot(id = "t1", businessId = "owner1", day = "Monday", startTime = "09:00", endTime = "10:00"),
            TimeSlot(id = "t2", businessId = "owner1", day = "Tuesday", startTime = "10:00", endTime = "11:00")
        )
        every { repository.getTimeSlots("owner1") } returns flowOf(slots)

        val viewModel = buildViewModel()

        assertEquals(slots, viewModel.slotsList)
        assertFalse(viewModel.isLoading)
    }

    @Test
    fun `empty slots list results in empty slotsList`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "owner1"
        every { repository.getTimeSlots("owner1") } returns flowOf(emptyList())

        val viewModel = buildViewModel()

        assertTrue(viewModel.slotsList.isEmpty())
        assertFalse(viewModel.isLoading)
    }

    @Test
    fun `deleteSlot calls repository with correct owner id and slot id`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "owner1"
        every { repository.getTimeSlots("owner1") } returns flowOf(emptyList())
        coEvery { repository.deleteTimeSlot("owner1", "slot-id-1") } returns Result.success(Unit)

        val viewModel = buildViewModel()
        viewModel.deleteSlot("slot-id-1")

        coVerify { repository.deleteTimeSlot("owner1", "slot-id-1") }
    }

    @Test
    fun `deleteSlot does nothing when user is null`() = runTest {
        every { auth.currentUser } returns null

        val viewModel = buildViewModel()
        viewModel.deleteSlot("slot-id-1")

        coVerify(exactly = 0) { repository.deleteTimeSlot(any(), any()) }
    }

    @Test
    fun `addSlot success calls onComplete with true`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "owner1"
        every { repository.getTimeSlots("owner1") } returns flowOf(emptyList())
        coEvery { repository.addTimeSlot(any()) } returns Result.success(Unit)

        val viewModel = buildViewModel()
        var callbackResult: Boolean? = null
        viewModel.addSlot("Monday", "09:00", "10:00") { callbackResult = it }

        assertEquals(true, callbackResult)
    }

    @Test
    fun `addSlot failure calls onComplete with false`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "owner1"
        every { repository.getTimeSlots("owner1") } returns flowOf(emptyList())
        coEvery { repository.addTimeSlot(any()) } returns Result.failure(Exception("Write error"))

        val viewModel = buildViewModel()
        var callbackResult: Boolean? = null
        viewModel.addSlot("Monday", "09:00", "10:00") { callbackResult = it }

        assertEquals(false, callbackResult)
    }

    @Test
    fun `addSlot does nothing when user is null`() = runTest {
        every { auth.currentUser } returns null

        val viewModel = buildViewModel()
        var callbackInvoked = false
        viewModel.addSlot("Monday", "09:00", "10:00") { callbackInvoked = true }

        coVerify(exactly = 0) { repository.addTimeSlot(any()) }
        assertFalse(callbackInvoked)
    }

    @Test
    fun `addSlot creates slot with correct data`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "owner1"
        every { repository.getTimeSlots("owner1") } returns flowOf(emptyList())
        coEvery { repository.addTimeSlot(any()) } returns Result.success(Unit)

        val viewModel = buildViewModel()
        viewModel.addSlot("Wednesday", "14:00", "15:30") {}

        coVerify {
            repository.addTimeSlot(
                match {
                    it.businessId == "owner1" &&
                    it.day == "Wednesday" &&
                    it.startTime == "14:00" &&
                    it.endTime == "15:30"
                }
            )
        }
    }
}
