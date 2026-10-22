package com.giwu.bible.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tag scheme CI publishes under, read back.
 *
 * Everything the update check decides rests on these two things: that the tag
 * parses, and that the comparison agrees with how the APK is stamped.
 */
class ReleaseVersionTest {

    @Test
    fun `parses the tag CI publishes`() {
        val version = ReleaseVersion.parse("v1.0.0-staging.42")

        assertEquals(listOf(1, 0, 0), version?.core)
        assertEquals("staging", version?.channel)
        assertEquals(42, version?.build)
        assertEquals("1.0.0 (42)", version?.display)
    }

    @Test
    fun `parses a plain version tag with no channel or build`() {
        val version = ReleaseVersion.parse("v2.1")

        assertEquals(listOf(2, 1), version?.core)
        assertNull(version?.channel)
        assertNull(version?.build)
        assertEquals("2.1", version?.display)
    }

    @Test
    fun `returns null for a tag in another scheme`() {
        assertNull(ReleaseVersion.parse("release-candidate"))
        assertNull(ReleaseVersion.parse(""))
    }

    @Test
    fun `a higher run number of the same version is newer`() {
        val version = requireNotNull(ReleaseVersion.parse("v1.0.0-staging.42"))

        assertTrue(version.isNewerThan(installedName = "1.0.0", installedCode = 41))
    }

    @Test
    fun `the same run number of the same version is not newer`() {
        val version = requireNotNull(ReleaseVersion.parse("v1.0.0-staging.42"))

        assertFalse(version.isNewerThan(installedName = "1.0.0", installedCode = 42))
    }

    @Test
    fun `a higher version wins over a higher installed run number`() {
        val version = requireNotNull(ReleaseVersion.parse("v1.1.0-staging.3"))

        assertTrue(version.isNewerThan(installedName = "1.0.0", installedCode = 90))
    }

    @Test
    fun `an older version is never offered as an update`() {
        val version = requireNotNull(ReleaseVersion.parse("v0.9.0-staging.500"))

        assertFalse(version.isNewerThan(installedName = "1.0.0", installedCode = 1))
    }

    @Test
    fun `a locally built APK sees every published build as newer`() {
        // A local build falls back to versionName 1.0.0 and versionCode 1.
        val version = requireNotNull(ReleaseVersion.parse("v1.0.0-staging.7"))

        assertTrue(version.isNewerThan(installedName = "1.0.0", installedCode = 1))
    }

    @Test
    fun `a shorter version string is padded rather than misread`() {
        val version = requireNotNull(ReleaseVersion.parse("v1.0-staging.5"))

        assertFalse(version.isNewerThan(installedName = "1.0.0", installedCode = 5))
        assertTrue(version.isNewerThan(installedName = "1.0.0", installedCode = 4))
    }
}
