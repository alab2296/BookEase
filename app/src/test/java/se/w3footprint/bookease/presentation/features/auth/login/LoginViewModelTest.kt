package se.w3footprint.bookease.presentation.features.auth.login

import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import se.w3footprint.bookease.domain.repository.AuthRepository

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: LoginViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty fields and not loading`() {
        assertEquals("", viewModel.email)
        assertEquals("", viewModel.password)
        assertFalse(viewModel.isLoading)
    }

    @Test
    fun `onEmailChange updates email`() {
        viewModel.onEmailChange("test@example.com")
        assertEquals("test@example.com", viewModel.email)
    }

    @Test
    fun `onPasswordChange updates password`() {
        viewModel.onPasswordChange("secret123")
        assertEquals("secret123", viewModel.password)
    }

    @Test
    fun `login with blank email emits error`() = runTest {
        val events = mutableListOf<LoginViewModel.LoginEvent>()
        val job = launch(testDispatcher) { viewModel.eventFlow.toList(events) }

        viewModel.onEmailChange("")
        viewModel.onPasswordChange("password")
        viewModel.login()

        job.cancel()
        assertEquals(1, events.size)
        assertTrue(events[0] is LoginViewModel.LoginEvent.Error)
        assertEquals("Please fill in all fields", (events[0] as LoginViewModel.LoginEvent.Error).message)
    }

    @Test
    fun `login with blank password emits error`() = runTest {
        val events = mutableListOf<LoginViewModel.LoginEvent>()
        val job = launch(testDispatcher) { viewModel.eventFlow.toList(events) }

        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("")
        viewModel.login()

        job.cancel()
        assertEquals(1, events.size)
        assertTrue(events[0] is LoginViewModel.LoginEvent.Error)
        assertEquals("Please fill in all fields", (events[0] as LoginViewModel.LoginEvent.Error).message)
    }

    @Test
    fun `login with invalid email format emits error`() = runTest {
        val events = mutableListOf<LoginViewModel.LoginEvent>()
        val job = launch(testDispatcher) { viewModel.eventFlow.toList(events) }

        viewModel.onEmailChange("not-an-email")
        viewModel.onPasswordChange("password")
        viewModel.login()

        job.cancel()
        assertTrue(events.any { it is LoginViewModel.LoginEvent.Error })
        val error = events.filterIsInstance<LoginViewModel.LoginEvent.Error>().first()
        assertEquals("Please enter a valid email address", error.message)
    }

    @Test
    fun `login success emits Success event`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { authRepository.login(any(), any()) } returns flowOf(Result.success(mockUser))

        val events = mutableListOf<LoginViewModel.LoginEvent>()
        val job = launch(testDispatcher) { viewModel.eventFlow.toList(events) }

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.login()

        job.cancel()
        assertTrue(events.any { it is LoginViewModel.LoginEvent.Success })
    }

    @Test
    fun `login failure emits Error event with message`() = runTest {
        every { authRepository.login(any(), any()) } returns flowOf(Result.failure(Exception("Invalid credentials")))

        val events = mutableListOf<LoginViewModel.LoginEvent>()
        val job = launch(testDispatcher) { viewModel.eventFlow.toList(events) }

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("wrongpass")
        viewModel.login()

        job.cancel()
        val error = events.filterIsInstance<LoginViewModel.LoginEvent.Error>().firstOrNull()
        assertEquals("Invalid credentials", error?.message)
    }

    @Test
    fun `login failure with null message uses fallback message`() = runTest {
        every { authRepository.login(any(), any()) } returns flowOf(Result.failure(Exception()))

        val events = mutableListOf<LoginViewModel.LoginEvent>()
        val job = launch(testDispatcher) { viewModel.eventFlow.toList(events) }

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.login()

        job.cancel()
        val error = events.filterIsInstance<LoginViewModel.LoginEvent.Error>().firstOrNull()
        assertEquals("Login failed", error?.message)
    }

    @Test
    fun `isLoading is false after successful login`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { authRepository.login(any(), any()) } returns flowOf(Result.success(mockUser))

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.login()

        assertFalse(viewModel.isLoading)
    }

    @Test
    fun `isLoading is false after failed login`() = runTest {
        every { authRepository.login(any(), any()) } returns flowOf(Result.failure(Exception("Error")))

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.login()

        assertFalse(viewModel.isLoading)
    }
}
