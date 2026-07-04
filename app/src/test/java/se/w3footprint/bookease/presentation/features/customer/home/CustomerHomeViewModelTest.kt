package se.w3footprint.bookease.presentation.features.customer.home

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
import se.w3footprint.bookease.domain.model.Business
import se.w3footprint.bookease.domain.repository.BusinessRepository

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerHomeViewModelTest {

    private lateinit var businessRepository: BusinessRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        businessRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading before data arrives`() = runTest {
        val flow = MutableSharedFlow<List<Business>>()
        every { businessRepository.getBusinesses() } returns flow

        val viewModel = CustomerHomeViewModel(businessRepository)

        assertTrue(viewModel.uiState.value is CustomerHomeViewModel.CustomerHomeUiState.Loading)
    }

    @Test
    fun `success state emitted when businesses load`() = runTest {
        val businesses = listOf(
            Business(id = "1", name = "Salon A"),
            Business(id = "2", name = "Barbershop B")
        )
        every { businessRepository.getBusinesses() } returns flowOf(businesses)

        val viewModel = CustomerHomeViewModel(businessRepository)

        val state = viewModel.uiState.value
        assertTrue(state is CustomerHomeViewModel.CustomerHomeUiState.Success)
        assertEquals(businesses, (state as CustomerHomeViewModel.CustomerHomeUiState.Success).businesses)
    }

    @Test
    fun `empty list results in Success with empty businesses`() = runTest {
        every { businessRepository.getBusinesses() } returns flowOf(emptyList())

        val viewModel = CustomerHomeViewModel(businessRepository)

        val state = viewModel.uiState.value
        assertTrue(state is CustomerHomeViewModel.CustomerHomeUiState.Success)
        assertTrue((state as CustomerHomeViewModel.CustomerHomeUiState.Success).businesses.isEmpty())
    }

    @Test
    fun `error state emitted when repository throws`() = runTest {
        every { businessRepository.getBusinesses() } returns flow { throw Exception("Network error") }

        val viewModel = CustomerHomeViewModel(businessRepository)

        val state = viewModel.uiState.value
        assertTrue(state is CustomerHomeViewModel.CustomerHomeUiState.Error)
        assertEquals("Network error", (state as CustomerHomeViewModel.CustomerHomeUiState.Error).message)
    }

    @Test
    fun `error with null message uses fallback`() = runTest {
        every { businessRepository.getBusinesses() } returns flow { throw Exception() }

        val viewModel = CustomerHomeViewModel(businessRepository)

        val state = viewModel.uiState.value
        assertTrue(state is CustomerHomeViewModel.CustomerHomeUiState.Error)
        assertEquals("Failed to load businesses", (state as CustomerHomeViewModel.CustomerHomeUiState.Error).message)
    }

    @Test
    fun `reload resets to Loading and re-fetches`() = runTest {
        val businesses = listOf(Business(id = "1", name = "Salon A"))
        every { businessRepository.getBusinesses() } returns flowOf(businesses)

        val viewModel = CustomerHomeViewModel(businessRepository)
        viewModel.reload()

        verify(exactly = 2) { businessRepository.getBusinesses() }
        assertTrue(viewModel.uiState.value is CustomerHomeViewModel.CustomerHomeUiState.Success)
    }
}
