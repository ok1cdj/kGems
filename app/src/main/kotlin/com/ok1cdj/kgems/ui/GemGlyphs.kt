/*
 * kGems — match-3 puzzle for the Mudita Kompakt
 * Copyright (C) 2026 Ondrej Kolonicny (OK1CDJ)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.ok1cdj.kgems.ui

import android.graphics.Canvas as NativeCanvas
import android.graphics.Paint as NativePaint
import android.graphics.Path as NativePath
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate

/**
 * The seven gem glyphs, drawn as pure 1-bit vector shapes.
 *
 * Every type is distinguished along three independent axes at once — silhouette,
 * internal pattern and fill density — so that the loss of any single axis on an
 * e-ink panel does not make two types look alike. Three types deliberately share
 * the circle silhouette and differ only in their interior; seven visually
 * distinct outlines would be harder to tell apart at a glance, not easier.
 *
 *   0  circle, empty                 lightest
 *   1  circle, cross
 *   2  circle, diagonal hatch        dense
 *   3  triangle, nested triangle
 *   4  diamond, cross
 *   5  brilliant, facet lines
 *   6  rounded square, 3x3 grid      heaviest
 *
 * Geometry is authored in a normalised 0..1 square and scaled to the cell at
 * draw time. Everything is drawn with anti-aliasing off — grey edge pixels are
 * the main cause of ghosting on this panel.
 */
object GemGlyphs {

    const val TYPE_COUNT = 7

    /** Geometry was authored against a 60 px cell; all widths scale from this. */
    private const val REFERENCE_CELL_PX = 60f

    /** Tuning knobs — outline is the primary carrier of legibility, keep it heavy. */
    private const val OUTLINE_WIDTH_UNITS = 3f
    private const val DETAIL_WIDTH_UNITS = 3f
    private const val HATCH_WIDTH_UNITS = 3f
    private const val HATCH_SPACING_UNITS = 6f

    private class Glyph(
        /** Closed outlines and interior detail lines, all stroked. */
        val strokes: List<NativePath>,
        /** Non-null for hatched types: the region the hatch is clipped to. */
        val hatchClip: NativePath? = null,
    )

    private var cacheSize = -1f
    private var cache: List<Glyph> = emptyList()

    private val outlinePaint = NativePaint().apply {
        isAntiAlias = false
        style = NativePaint.Style.STROKE
        strokeJoin = NativePaint.Join.ROUND
        strokeCap = NativePaint.Cap.BUTT
    }

    private val hatchPaint = NativePaint().apply {
        isAntiAlias = false
        style = NativePaint.Style.STROKE
        strokeCap = NativePaint.Cap.BUTT
    }

    private val fillPaint = NativePaint().apply {
        isAntiAlias = false
        style = NativePaint.Style.FILL
    }

    /**
     * Draws one gem into a square cell.
     *
     * @param inverted selection state — the cell is flooded with [ink] and the
     *   glyph is drawn in [paper]. Inversion is the clearest possible selection
     *   cue on e-ink and costs nothing extra to render.
     */
    fun DrawScope.drawGem(
        type: Int,
        topLeft: Offset,
        cellSize: Float,
        ink: Color = Color.Black,
        paper: Color = Color.White,
        inverted: Boolean = false,
    ) {
        require(type in 0 until TYPE_COUNT) { "unknown gem type $type" }

        val glyphs = glyphsFor(cellSize)
        val glyph = glyphs[type]
        val scale = cellSize / REFERENCE_CELL_PX

        val foreground = if (inverted) paper.toArgb() else ink.toArgb()
        val background = if (inverted) ink.toArgb() else paper.toArgb()

        outlinePaint.color = foreground
        outlinePaint.strokeWidth = OUTLINE_WIDTH_UNITS * scale
        hatchPaint.color = foreground
        hatchPaint.strokeWidth = HATCH_WIDTH_UNITS * scale
        fillPaint.color = background

        translate(topLeft.x, topLeft.y) {
            drawContext.canvas.nativeCanvas.apply {
                drawRect(0f, 0f, cellSize, cellSize, fillPaint)
                glyph.hatchClip?.let { drawHatch(this, it, cellSize, scale) }
                glyph.strokes.forEach { drawPath(it, outlinePaint) }
            }
        }
    }

    private fun drawHatch(canvas: NativeCanvas, clip: NativePath, cellSize: Float, scale: Float) {
        val spacing = HATCH_SPACING_UNITS * scale
        val saved = canvas.save()
        canvas.clipPath(clip)
        var x = 0f
        while (x <= cellSize * 2f) {
            canvas.drawLine(x, 0f, x - cellSize, cellSize, hatchPaint)
            x += spacing
        }
        canvas.restoreToCount(saved)
    }

    private fun glyphsFor(cellSize: Float): List<Glyph> {
        if (cellSize != cacheSize) {
            cache = buildGlyphs(cellSize)
            cacheSize = cellSize
        }
        return cache
    }

    // ---- geometry -----------------------------------------------------------

    private fun buildGlyphs(s: Float): List<Glyph> = listOf(
        circleEmpty(s),
        circleCross(s),
        circleHatched(s),
        triangleNested(s),
        diamondCross(s),
        brilliant(s),
        squareGrid(s),
    )

    /** Inset of the outline from the cell edge, in normalised units. */
    private const val R = 25f / 60f

    private fun circlePath(s: Float): NativePath = NativePath().apply {
        addCircle(0.5f * s, 0.5f * s, R * s, NativePath.Direction.CW)
    }

    private fun circleEmpty(s: Float) = Glyph(listOf(circlePath(s)))

    private fun circleCross(s: Float) = Glyph(
        listOf(
            circlePath(s),
            line(s, 0.5f, 0.15f, 0.5f, 0.85f),
            line(s, 0.15f, 0.5f, 0.85f, 0.5f),
        )
    )

    private fun circleHatched(s: Float) = Glyph(
        strokes = listOf(circlePath(s)),
        hatchClip = circlePath(s),
    )

    private fun triangleNested(s: Float) = Glyph(
        listOf(
            polygon(s, 0.500f, 0.083f, 0.917f, 0.867f, 0.083f, 0.867f),
            polygon(s, 0.500f, 0.367f, 0.717f, 0.750f, 0.283f, 0.750f),
        )
    )

    private fun diamondCross(s: Float) = Glyph(
        listOf(
            polygon(s, 0.500f, 0.067f, 0.933f, 0.500f, 0.500f, 0.933f, 0.067f, 0.500f),
            line(s, 0.5f, 0.233f, 0.5f, 0.767f),
            line(s, 0.233f, 0.5f, 0.767f, 0.5f),
        )
    )

    private fun brilliant(s: Float) = Glyph(
        listOf(
            polygon(s, 0.150f, 0.233f, 0.850f, 0.233f, 0.500f, 0.917f),
            line(s, 0.267f, 0.467f, 0.733f, 0.467f),
            line(s, 0.383f, 0.233f, 0.500f, 0.917f),
            line(s, 0.617f, 0.233f, 0.500f, 0.917f),
        )
    )

    private fun squareGrid(s: Float): Glyph {
        val lo = 0.083f
        val hi = 0.917f
        val outline = NativePath().apply {
            addRoundRect(
                lo * s, lo * s, hi * s, hi * s,
                0.117f * s, 0.117f * s,
                NativePath.Direction.CW,
            )
        }
        return Glyph(
            listOf(
                outline,
                line(s, 0.367f, lo, 0.367f, hi),
                line(s, 0.633f, lo, 0.633f, hi),
                line(s, lo, 0.367f, hi, 0.367f),
                line(s, lo, 0.633f, hi, 0.633f),
            )
        )
    }

    private fun line(s: Float, x1: Float, y1: Float, x2: Float, y2: Float) =
        NativePath().apply {
            moveTo(x1 * s, y1 * s)
            lineTo(x2 * s, y2 * s)
        }

    private fun polygon(s: Float, vararg xy: Float): NativePath {
        require(xy.size >= 6 && xy.size % 2 == 0) { "need at least three points" }
        return NativePath().apply {
            moveTo(xy[0] * s, xy[1] * s)
            var i = 2
            while (i < xy.size) {
                lineTo(xy[i] * s, xy[i + 1] * s)
                i += 2
            }
            close()
        }
    }
}
