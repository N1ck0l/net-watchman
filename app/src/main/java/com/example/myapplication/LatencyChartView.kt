package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class LatencyChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var dataPoints: List<Long> = emptyList()
    
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50") // Verde Material
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#224CAF50") // Verde transparente
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 24f
    }

    fun setData(latencies: List<Long>) {
        // Filtra apenas latências válidas (maiores que 0) e pega as últimas 20
        this.dataPoints = latencies.filter { it > 0 }.take(20).reversed()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.size < 2) return

        val padding = 40f
        val width = width.toFloat() - (padding * 2)
        val height = height.toFloat() - (padding * 2)
        
        val maxLatency = dataPoints.maxOrNull()?.toFloat() ?: 100f
        val minLatency = 0f
        
        val xStep = width / (dataPoints.size - 1)
        val yScale = height / (maxLatency - minLatency)

        val path = Path()
        val fillPath = Path()

        // Desenha linhas de grade horizontais (0, 50%, 100%)
        canvas.drawLine(padding, padding, width + padding, padding, gridPaint)
        canvas.drawLine(padding, padding + height / 2, width + padding, padding + height / 2, gridPaint)
        canvas.drawLine(padding, padding + height, width + padding, padding + height, gridPaint)

        // Labels
        canvas.drawText("${maxLatency.toInt()}ms", 5f, padding + 20f, textPaint)
        canvas.drawText("0ms", 5f, padding + height, textPaint)

        dataPoints.forEachIndexed { index, latency ->
            val x = padding + (index * xStep)
            val y = padding + height - ((latency - minLatency) * yScale)

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, padding + height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            
            if (index == dataPoints.size - 1) {
                fillPath.lineTo(x, padding + height)
                fillPath.close()
            }
        }

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)
    }
}
