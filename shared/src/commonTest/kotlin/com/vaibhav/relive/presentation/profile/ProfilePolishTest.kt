package com.vaibhav.relive.presentation.profile

import com.vaibhav.relive.domain.model.LockAfter
import com.vaibhav.relive.domain.model.MediaStorageRef
import com.vaibhav.relive.domain.model.ProfileSettings
import com.vaibhav.relive.domain.repository.ProfileSettingsRepository
import com.vaibhav.relive.platform.notifications.*
import com.vaibhav.relive.platform.system.*
import com.vaibhav.relive.ui.screens.aboutGuideSections
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import com.vaibhav.relive.domain.repository.ProfileRepository
import com.vaibhav.relive.domain.model.ProfileSnapshot
import com.vaibhav.relive.domain.time.Instant
import com.vaibhav.relive.platform.media.MediaStore
import com.vaibhav.relive.domain.model.MediaType
import kotlin.test.*

class ProfilePolishTest {
    @Test fun `display name is trimmed nonblank and bounded`() {
        assertEquals("Ada", validatedDisplayName("  Ada  "))
        assertNull(validatedDisplayName("   "))
        assertNull(validatedDisplayName("a".repeat(61)))
        assertEquals("a".repeat(60), validatedDisplayName("a".repeat(60)))
    }

    @Test fun `persisted photo replacement deletes the prior managed file`() = runTest {
        val old = MediaStorageRef("old.jpg")
        val replacement = MediaStorageRef("replacement.jpg")
        val repo = FakeProfileSettingsRepository(ProfileSettings(profilePhoto = old))
        val media = FakeMediaStore()
        val viewModel = ProfileViewModel(FakeProfileRepository(), repo, media, backgroundScope)
        val completed = CompletableDeferred<Boolean>()
        viewModel.setProfilePhoto(replacement) { completed.complete(it) }
        assertTrue(completed.await())
        assertEquals(replacement, repo.settings.value.profilePhoto)
        assertEquals(listOf(old), media.deleted)
    }
    @Test fun `safe diagnostics contain no archive data`() {
        val request = buildSafeDiagnosticMail("Problem", PlatformAppInfo("1.2", "7", "Android", "16"), "support@relive.invalid")
        assertEquals("support@relive.invalid", request.recipient)
        assertContains(request.body, "Relive 1.2 (7)")
        assertFalse(request.body.contains("moment", ignoreCase = true))
        assertFalse(request.body.contains("location", ignoreCase = true))
    }

    @Test fun `about guide covers implemented features and omits places article`() {
        val titles = aboutGuideSections.map { it.title }
        assertContains(titles, "Capture")
        assertContains(titles, "Rediscover")
        assertFalse(titles.any { it == "Places" })
        assertEquals(aboutGuideSections.mapNotNull { it.screenshot?.id }.size, aboutGuideSections.mapNotNull { it.screenshot?.id }.distinct().size)
    }

    @Test fun `app lock authenticates before enable and respects timeout`() = runTest {
        val repo = FakeProfileSettingsRepository()
        val auth = FakeAuthentication(AuthenticationResult.Authenticated)
        var time = 1_000L
        val controller = AppLockController(repo, auth) { time }
        assertEquals(AuthenticationResult.Authenticated, controller.setEnabled(true))
        assertTrue(repo.settings.value.appLockEnabled)
        repo.setLockAfter(LockAfter.OneMinute)
        controller.onBackground(); time += 59_999; controller.onForeground()
        assertFalse(controller.locked.value)
        controller.onBackground(); time += 60_000; controller.onForeground()
        assertTrue(controller.locked.value)
    }

    @Test fun `cancelled authentication leaves app lock off`() = runTest {
        val repo = FakeProfileSettingsRepository()
        val controller = AppLockController(repo, FakeAuthentication(AuthenticationResult.Cancelled)) { 0 }
        assertEquals(AuthenticationResult.Cancelled, controller.setEnabled(true))
        assertFalse(repo.settings.value.appLockEnabled)
    }

    @Test fun `reminders persist only after permission grant`() = runTest {
        val repo = FakeProfileSettingsRepository()
        val denied = FakeReminder(NotificationPermissionState.Denied)
        assertEquals(ReminderSchedulingResult.PermissionDenied, RediscoverReminderController(repo, denied).setEnabled(true))
        assertFalse(repo.settings.value.rediscoverRemindersEnabled)
        val granted = FakeReminder(NotificationPermissionState.Granted)
        assertEquals(ReminderSchedulingResult.Scheduled, RediscoverReminderController(repo, granted).setEnabled(true))
        assertTrue(repo.settings.value.rediscoverRemindersEnabled)
    }
}

private class FakeAuthentication(private val result: AuthenticationResult) : DeviceAuthentication {
    override val capabilities = AuthenticationCapabilities(true, true)
    override suspend fun authenticate(biometricsOnly: Boolean, reason: String) = result
}

private class FakeReminder(private val permission: NotificationPermissionState) : RediscoverReminderService {
    override fun permissionState() = permission
    override suspend fun requestPermission() = permission
    override suspend fun synchronize(enabled: Boolean) = if (enabled) ReminderSchedulingResult.Scheduled else ReminderSchedulingResult.Cancelled
}

private class FakeProfileSettingsRepository(initial: ProfileSettings = ProfileSettings()) : ProfileSettingsRepository {
    override val settings = MutableStateFlow(initial)
    override suspend fun setDisplayName(value: String) = update { it.copy(displayName = value) }
    override suspend fun setProfilePhoto(value: MediaStorageRef?) = update { it.copy(profilePhoto = value) }
    override suspend fun setAppLockEnabled(value: Boolean) = update { it.copy(appLockEnabled = value) }
    override suspend fun setBiometricUnlockEnabled(value: Boolean) = update { it.copy(biometricUnlockEnabled = value) }
    override suspend fun setLockAfter(value: LockAfter) = update { it.copy(lockAfter = value) }
    override suspend fun setRediscoverRemindersEnabled(value: Boolean) = update { it.copy(rediscoverRemindersEnabled = value) }
    private fun update(block: (ProfileSettings) -> ProfileSettings): Result<Unit> { settings.value = block(settings.value); return Result.success(Unit) }
}

private class FakeProfileRepository : ProfileRepository {
    override fun observeProfile() = flowOf(ProfileSnapshot(Instant(0), 0, 0, 0))
}

private class FakeMediaStore : MediaStore {
    val deleted = mutableListOf<MediaStorageRef>()
    override fun extensionFor(type: MediaType) = "jpg"
    override fun allocateKey(type: MediaType) = MediaStorageRef("new.jpg")
    override fun resolveAbsolutePath(ref: MediaStorageRef) = ref.value
    override fun exists(ref: MediaStorageRef) = true
    override fun delete(ref: MediaStorageRef) { deleted += ref }
    override fun sizeBytes(ref: MediaStorageRef) = 0L
}
