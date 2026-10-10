package com.example

import com.example.engine.FunscriptAlgorithm
import com.example.model.BeatEvent
import com.example.model.GeneratorConfig
import com.example.model.StrokeStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testCropStrategy() {
        val (highResult, highOob) = FunscriptAlgorithm.resolveOutOfBounds(120f, 50f, 50f, StrokeStrategy.CROP)
        assertEquals(100f, highResult, 0.001f)
        assertTrue(highOob)

        val (lowResult, lowOob) = FunscriptAlgorithm.resolveOutOfBounds(-20f, 50f, 50f, StrokeStrategy.CROP)
        assertEquals(0f, lowResult, 0.001f)
        assertTrue(lowOob)
    }

    @Test
    fun testBounceStrategy() {
        // Exceeding 100 by 15 -> bounce back to 100 - 15 = 85
        val (highResult, highOob) = FunscriptAlgorithm.resolveOutOfBounds(115f, 50f, 50f, StrokeStrategy.BOUNCE)
        assertEquals(85f, highResult, 0.001f)
        assertTrue(highOob)

        // Exceeding 0 below by 10 -> bounce back to 0 + 10 = 10
        val (lowResult, lowOob) = FunscriptAlgorithm.resolveOutOfBounds(-10f, 50f, 50f, StrokeStrategy.BOUNCE)
        assertEquals(10f, lowResult, 0.001f)
        assertTrue(lowOob)
    }

    @Test
    fun testFoldStrategy() {
        val (result, isOob) = FunscriptAlgorithm.resolveOutOfBounds(120f, 60f, 50f, StrokeStrategy.FOLD)
        assertTrue(isOob)
        assertTrue("Fold result must stay within bounds", result in 0f..100f)
    }

    @Test
    fun testScriptGenerationAndExport() {
        val beats = listOf(
            BeatEvent(timestampMs = 500L, bpm = 120f, energy = 0.5f, pitchHz = 220f),
            BeatEvent(timestampMs = 1000L, bpm = 120f, energy = 0.9f, pitchHz = 440f),
            BeatEvent(timestampMs = 1500L, bpm = 120f, energy = 0.3f, pitchHz = 110f)
        )

        val config = GeneratorConfig(strategy = StrokeStrategy.BOUNCE)
        val (script, stats) = FunscriptAlgorithm.generateScript(beats, config)

        assertTrue(script.actions.isNotEmpty())
        assertEquals(script.actions.size, stats.totalActions)

        val json = FunscriptAlgorithm.exportToJson(script, title = "test_video.mp4")
        assertTrue(json.contains("\"version\": \"1.0\""))
        assertTrue(json.contains("\"actions\": ["))
        assertTrue(json.contains("\"the-handy-sync\""))
    }

    @Test
    fun testTheHandyHardwareClamping() {
        val beats = listOf(
            BeatEvent(timestampMs = 500L, bpm = 120f, energy = 1.0f, pitchHz = 1000f),
            BeatEvent(timestampMs = 1000L, bpm = 120f, energy = 1.0f, pitchHz = 50f)
        )
        // Clamp between 10% and 90%
        val config = GeneratorConfig(
            strategy = StrokeStrategy.CROP,
            handyMinLimit = 10,
            handyMaxLimit = 90
        )
        val (script, _) = FunscriptAlgorithm.generateScript(beats, config)
        for (action in script.actions) {
            assertTrue("Action pos must be >= 10", action.pos >= 10)
            assertTrue("Action pos must be <= 90", action.pos <= 90)
        }
    }
}
