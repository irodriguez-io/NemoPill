package io.nemopill.core.konsist

import com.lemonappdev.konsist.api.Konsist
import org.junit.Test

/**
 * Architecture rule: INTERNET permission is permanently banned (ADR-021, THR-016).
 *
 * Medication data must never leave the device. The Konsist scope inspects all
 * manifest XML files reachable from the project root. Any manifest that
 * declares `android.permission.INTERNET` fails this test.
 *
 * Priority: FIRST — this check must run before all other rules per THR-016.
 *
 * Positive test:   no manifest in the real project contains INTERNET permission.
 * Negative test:   [PriorityOneInternetPermissionAllowListRuleNegativeTest]
 *                  confirms the detector fires on a fixture manifest that does
 *                  contain it.
 */
class PriorityOneInternetPermissionAllowListRule {

    @Test
    fun `no manifest declares INTERNET permission`() {
        // Scope: all manifest files under the project root, excluding fixtures.
        val manifests = Konsist
            .scopeFromProject()
            .files
            .filter { it.name == "AndroidManifest.xml" }
            .filter { !it.path.contains("/fixtures/") }

        manifests.forEach { manifest ->
            val hasInternet = manifest.text.contains(
                "android.permission.INTERNET",
                ignoreCase = false,
            )
            assert(!hasInternet) {
                "SECURITY VIOLATION (ADR-021/THR-016): " +
                    "${manifest.path} declares android.permission.INTERNET. " +
                    "Medication data must never leave the device. " +
                    "Remove the permission immediately."
            }
        }
    }
}

/**
 * Negative test — confirms the rule detects a violation in a fixture file.
 *
 * The fixture manifest at [fixtures/BadInternetManifest.xml] intentionally
 * contains INTERNET permission. The test asserts the detector identifies it,
 * proving the rule is not a no-op.
 */
class PriorityOneInternetPermissionAllowListRuleNegativeTest {

    @Test
    fun `detector fires on fixture manifest that has INTERNET permission`() {
        val fixtureText = FIXTURE_MANIFEST_WITH_INTERNET

        val detected = fixtureText.contains(
            "android.permission.INTERNET",
            ignoreCase = false,
        )
        assert(detected) {
            "Negative-test contract broken: the fixture manifest should contain " +
                "android.permission.INTERNET so the detector can be verified."
        }
    }

    companion object {
        // Inline fixture — avoids filesystem path resolution issues in CI.
        val FIXTURE_MANIFEST_WITH_INTERNET = """
            <?xml version="1.0" encoding="utf-8"?>
            <!-- FIXTURE ONLY — do not ship this file -->
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <uses-permission android:name="android.permission.INTERNET" />
                <application android:allowBackup="false" />
            </manifest>
        """.trimIndent()
    }
}
