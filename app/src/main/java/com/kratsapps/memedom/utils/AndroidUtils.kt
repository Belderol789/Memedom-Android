package com.kratsapps.memedom.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Point
import android.graphics.Shader
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.ShapeDrawable.ShaderFactory
import android.graphics.drawable.shapes.RectShape
import android.view.Display
import android.view.View


class AndroidUtils {

    fun animateView(view: View, toVisibility: Int, toAlpha: Float, duration: Int) {
        val show = toVisibility == View.VISIBLE
        if (show) {
            view.alpha = 0f
        }
        view.visibility = View.VISIBLE
        view.animate()
            .setDuration(duration.toLong())
            .alpha((if (show) toAlpha else 0) as Float)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = toVisibility
                }
            })
    }

    fun changeViewGradient(view: View) {

        val topColor = Color.parseColor("#ED4465")
        val bottomColor = Color.parseColor("#FEEABB")

        val sf: ShaderFactory = object : ShaderFactory() {
            override fun resize(width: Int, height: Int): Shader {
                return LinearGradient(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    intArrayOf(topColor, bottomColor),
                    floatArrayOf(0f, 0.5f, .55f, 1f),
                    Shader.TileMode.REPEAT
                )
            }
        }

        val p = PaintDrawable()
        p.shape = RectShape()
        p.shaderFactory = sf
    }

    fun getScreenWidthAndHeight(context: Activity, dimensions: (Int, Int) -> Unit) {
        var display: Display?

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            display = context.display
        } else {
            display = context.windowManager.defaultDisplay
        }
        val size = Point()
        display?.getRealSize(size)
        var width = size.x
        var height = size.y
        dimensions(width, height)
    }


}