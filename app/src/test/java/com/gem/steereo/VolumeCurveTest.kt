package com.gem.steereo

import com.gem.steereo.playback.volumeCurve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeCurveTest {

    private val eps = 1e-6f

    @Test
    fun zero_mapsToZero() {
        assertEquals(0f, volumeCurve(0f), eps)
    }

    @Test
    fun one_mapsToOne() {
        assertEquals(1f, volumeCurve(1f), eps)
    }

    @Test
    fun half_mapsToQuarter() {
        assertEquals(0.25f, volumeCurve(0.5f), eps)
    }

    @Test
    fun curve_isSquare() {
        assertEquals(0.01f, volumeCurve(0.1f), eps)
        assertEquals(0.5625f, volumeCurve(0.75f), eps)
    }

    @Test
    fun belowZero_clampedToZero() {
        assertEquals(0f, volumeCurve(-0.5f), eps)
        assertEquals(0f, volumeCurve(-10f), eps)
    }

    @Test
    fun aboveOne_clampedToOne() {
        assertEquals(1f, volumeCurve(1.5f), eps)
        assertEquals(1f, volumeCurve(100f), eps)
    }

    @Test
    fun curve_isMonotonicAndInRange() {
        var prev = -1f
        for (i in 0..100) {
            val out = volumeCurve(i / 100f)
            assertTrue("out of range at $i: $out", out in 0f..1f)
            assertTrue("not monotonic at $i", out >= prev)
            prev = out
        }
    }
}
