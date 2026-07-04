package se.w3footprint.bookease.presentation.features.auth.register

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import org.junit.Before
import org.junit.Test
import se.w3footprint.bookease.domain.repository.AuthRepository

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var db: FirebaseFirestore
    private lateinit var viewModel: RegisterViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        db = mockk()
        viewModel = RegisterViewModel(authRepository, db)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty fields and Customer role`() {
        assertEquals("", viewModel.email)
        assertEquals("", viewModel.password)
        assertEquals("", viewModel.confirmPassword)
        assertEquals("Customer", viewModel.userRole)
        assertFalse(viewModel.isLoading)
    }

    @Test
    fun `input change functions update state`() {
        viewModel.onEmailChange("a@b.com")
        viewModel.onPasswordChange("pass")
        viewModel.onConfirmPasswordChange("pass")
        viewModel.onRoleChange("Owner")

        assertEquals("a@b.com", viewModel.email)
        assertEquals("pass", viewModel.password)
        assertEquals("pass", viewModel.confirmPassword)
        assertEquals("Owner", viewModel.userRole)
    }

    @Test
    fun `register with blank fields emits error`() = runTest {
        val events = mutableListOf<RegisterViewModel.RegisterEvent>()
        val job = launch(testDispatcher) { viewModel.eventFlow.toList(events) }

        viewModel.register()

        job.cancel()
        val error = events.filterIsInstance<RegisterViewModel.RegisterEvent.Error>().first()
        assertEquals("Please fill in all fields", error.message)
    }

    @Test
    fun `register with invalid email emits error`() = runTest {
        val events = mutableListOf<RegisterViewModel.RegisterEvent>()
        val job = launch(testDispatcher) { viewModel.eventFlow.toList(events) }

        viewModel.onEmailChange("bademail")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.register()

        job.cancel()
        val error = events.filterIsInstance<RegisterViewModel.RegisterEvent.Error>().first()
        assertEquals("Please enter a valid email address", error.message)
    }

    @Test
    fun `register with short password emits error`() = runTest {
        val events = mutableListOf<RegisterViewModel.RegisterEvent>()
        val job = launch(testDispatcher) { viewModel.eventFlow.toList(events) }

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("abc")
        viewModel.onConfirmPasswordChange("abc")
        viewModel.register()

        job.cancel()
        val error = events.filterIsInstance<RegisterViewModel.RegisterEvent.Error>().first()
        assertEquals("Password must be at least 6 characters", error.message)
    }

    @Test
    fun `register with mismatched passwords emits error`() = runTest {
        val events = mutableListOf<RegisterViewModel.RegisterEvent>()
        val job = launch(testDispatcher) { viewModel.eventFlow.toList(events) }

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("different123")
        viewModel.register()

        job.cancel()
        val error = events.filterIsInstance<RegisterViewModel.RegisterEvent.Error>().first()
        assertEquals("Passwords do not match", error.message)
    }

    @Test
    fun `register failure from auth emits error`() = runTest {
        every { authRepository.register(any(), any()) } returns flowOf(Result.failure(Exception("Email already in use")))

        val events = mutableListOf<RegisterViewModel.RegisterEvent>()
        val job = launch(testDispatcher) { viewModel.eventFlow.toList(events) }

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.register()

        job.cancel()
        val error = events.filterIsInstance<RegisterViewModel.RegisterEvent.Error>().first()
        assertEquals("Email already in use", error.message)
    }

    @Test
    fun `register success with Firestore save emits Success with role`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { mockUser.uid } returns "uid-123"
        every { authRepository.register(any(), any()) } returns flowOf(Result.success(mockUser))

        val mockTask = mockk<Task<Void>>()
        val successListenerSlot = slot<OnSuccessListener<Void>>()
        val mockDocRef = mockk<DocumentReference>()
        val mockCollection = mockk<CollectionReference>()

        every { db.collection("users") } returns mockCollection
        every { mockCollection.document("uid-123") } returns mockDocRef
        every { mockDocRef.set(any()) } returns mockTask
        every { mockTask.addOnSuccessListener(capture(successListenerSlot)) } answers {
            successListenerSlot.captured.onSuccess(null)
            mockTask
        }
        every { mockTask.addOnFailureListener(any()) } returns mockTask

        val events = mutableListOf<RegisterViewModel.RegisterEvent>()
        val job = launch(testDispatcher) { viewModel.eventFlow.toList(events) }

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.onRoleChange("Owner")
        viewModel.register()

        job.cancel()
        val success = events.filterIsInstance<RegisterViewModel.RegisterEvent.Success>().first()
        assertEquals("Owner", success.role)
    }

    @Test
    fun `register success but Firestore failure emits error`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { mockUser.uid } returns "uid-123"
        every { authRepository.register(any(), any()) } returns flowOf(Result.success(mockUser))

        val mockTask = mockk<Task<Void>>()
        val failureListenerSlot = slot<OnFailureListener>()
        val mockDocRef = mockk<DocumentReference>()
        val mockCollection = mockk<CollectionReference>()

        every { db.collection("users") } returns mockCollection
        every { mockCollection.document("uid-123") } returns mockDocRef
        every { mockDocRef.set(any()) } returns mockTask
        every { mockTask.addOnSuccessListener(any<OnSuccessListener<Void>>()) } returns mockTask
        every { mockTask.addOnFailureListener(capture(failureListenerSlot)) } answers {
            failureListenerSlot.captured.onFailure(Exception("Firestore write failed"))
            mockTask
        }

        val events = mutableListOf<RegisterViewModel.RegisterEvent>()
        val job = launch(testDispatcher) { viewModel.eventFlow.toList(events) }

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.register()

        job.cancel()
        val error = events.filterIsInstance<RegisterViewModel.RegisterEvent.Error>().first()
        assertEquals("Firestore write failed", error.message)
    }
}
