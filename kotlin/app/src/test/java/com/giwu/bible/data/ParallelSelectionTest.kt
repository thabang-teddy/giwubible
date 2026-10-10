package com.giwu.bible.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The comparison-panel selection, where an empty list means "every
 * translation" rather than "none".
 */
class ParallelSelectionTest {

    private val all = listOf("t_asv", "t_bbe", "t_web")

    @Test
    fun `unticking one from the implicit all leaves the rest`() {
        assertEquals(
            listOf("t_asv", "t_web"),
            AppPrefs.nextParallelSelection(emptyList(), "t_bbe", all),
        )
    }

    @Test
    fun `unticking from an explicit selection removes it`() {
        assertEquals(
            listOf("t_asv"),
            AppPrefs.nextParallelSelection(listOf("t_asv", "t_web"), "t_web", all),
        )
    }

    @Test
    fun `ticking the last missing one collapses back to all`() {
        assertEquals(
            emptyList<String>(),
            AppPrefs.nextParallelSelection(listOf("t_asv", "t_bbe"), "t_web", all),
        )
    }

    @Test
    fun `ticking a further one keeps an explicit selection`() {
        assertEquals(
            listOf("t_asv", "t_bbe"),
            AppPrefs.nextParallelSelection(listOf("t_asv"), "t_bbe", all),
        )
    }

    @Test
    fun `unticking the last one returns to the implicit all`() {
        // There is no "compare against nothing" state: emptying the selection
        // is how the panel goes back to showing every downloaded translation.
        assertEquals(
            emptyList<String>(),
            AppPrefs.nextParallelSelection(listOf("t_asv"), "t_asv", all),
        )
    }
}
