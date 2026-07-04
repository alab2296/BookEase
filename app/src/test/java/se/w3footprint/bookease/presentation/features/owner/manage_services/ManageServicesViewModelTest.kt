package se.w3footprint.bookease.presentation.features.owner.manage_services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
import se.w3footprint.bookease.domain.model.Service
import se.w3footprint.bookease.domain.repository.BusinessRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ManageServicesViewModelTest {

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

    private fun buildViewModel() = ManageServicesViewModel(repository, auth)

    @Test
    fun `when user is null no services are loaded and loading stays false`() = runTest {
        every { auth.currentUser } returns null

        val viewModel = buildViewModel()

        assertTrue(viewModel.servicesList.isEmpty())
    }

    @Test
    fun `services load on init when user is logged in`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "owner1"

        val services = listOf(
            Service(id = "s1", businessId = "owner1", name = "Haircut", price = 200.0, durationMinutes = 30),
            Service(id = "s2", businessId = "owner1", name = "Beard Trim", price = 100.0, durationMinutes = 15)
        )
        every { repository.getServices("owner1") } returns flowOf(services)

        val viewModel = buildViewModel()

        assertEquals(services, viewModel.servicesList)
        assertFalse(viewModel.isLoading)
    }

    @Test
    fun `empty service list results in empty servicesList`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "owner1"
        every { repository.getServices("owner1") } returns flowOf(emptyList())

        val viewModel = buildViewModel()

        assertTrue(viewModel.servicesList.isEmpty())
        assertFalse(viewModel.isLoading)
    }

    @Test
    fun `addService does nothing when user is null`() = runTest {
        every { auth.currentUser } returns null

        val viewModel = buildViewModel()
        var callbackResult: Boolean? = null
        viewModel.addService("Haircut", "200", "30") { callbackResult = it }

        coVerify(exactly = 0) { repository.addService(any()) }
        assertEquals(null, callbackResult)
    }

    @Test
    fun `addService success calls onComplete with true`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "owner1"
        every { repository.getServices("owner1") } returns flowOf(emptyList())
        coEvery { repository.addService(any()) } returns Result.success(Unit)

        val viewModel = buildViewModel()
        var callbackResult: Boolean? = null
        viewModel.addService("Haircut", "200.0", "30") { callbackResult = it }

        assertEquals(true, callbackResult)
    }

    @Test
    fun `addService failure calls onComplete with false`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "owner1"
        every { repository.getServices("owner1") } returns flowOf(emptyList())
        coEvery { repository.addService(any()) } returns Result.failure(Exception("Write failed"))

        val viewModel = buildViewModel()
        var callbackResult: Boolean? = null
        viewModel.addService("Haircut", "200", "30") { callbackResult = it }

        assertEquals(false, callbackResult)
    }

    @Test
    fun `addService with non-numeric price defaults to 0 0`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "owner1"
        every { repository.getServices("owner1") } returns flowOf(emptyList())
        coEvery { repository.addService(any()) } returns Result.success(Unit)

        val viewModel = buildViewModel()
        viewModel.addService("Haircut", "not-a-number", "30") {}

        coVerify {
            repository.addService(match { it.price == 0.0 })
        }
    }

    @Test
    fun `addService with non-numeric duration defaults to 0`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "owner1"
        every { repository.getServices("owner1") } returns flowOf(emptyList())
        coEvery { repository.addService(any()) } returns Result.success(Unit)

        val viewModel = buildViewModel()
        viewModel.addService("Haircut", "200", "bad-duration") {}

        coVerify {
            repository.addService(match { it.durationMinutes == 0 })
        }
    }

    @Test
    fun `deleteService calls repository with correct ids`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "owner1"
        every { repository.getServices("owner1") } returns flowOf(emptyList())
        coEvery { repository.deleteService("owner1", "service-id-1") } returns Result.success(Unit)

        val viewModel = buildViewModel()
        viewModel.deleteService("service-id-1")

        coVerify { repository.deleteService("owner1", "service-id-1") }
    }

    @Test
    fun `deleteService does nothing when user is null`() = runTest {
        every { auth.currentUser } returns null

        val viewModel = buildViewModel()
        viewModel.deleteService("service-id-1")

        coVerify(exactly = 0) { repository.deleteService(any(), any()) }
    }
}
