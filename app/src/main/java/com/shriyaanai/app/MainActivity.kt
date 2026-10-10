package com.shriyaanai.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val scrollView = ScrollView(this)

    val layout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(32, 40, 32, 32)
        setBackgroundColor(Color.rgb(12, 18, 35))
    }

    val title = TextView(this).apply {
        text = "Shriyaan AI"
        textSize = 32f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
    }

    layout.addView(title)

    val subtitle = TextView(this).apply {
        text = "Your AI Content Creation Studio"
        textSize = 16f
        setTextColor(Color.LTGRAY)
        gravity = Gravity.CENTER
        setPadding(0, 8, 0, 28)
    }

    layout.addView(subtitle)

    val features = listOf(
        "AI Script Writer",
        "Video Ideas",
        "Thumbnail Ideas",
        "Voice & Caption Tools",
        "Social Media Planner"
    )

    for (feature in features) {
        val button = Button(this).apply {
            text = feature
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(38, 76, 145))

            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "$feature — Coming Soon!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 10, 0, 10)
        layout.addView(button, params)
    }

    val note = TextView(this).apply {
        text = "Create • Plan • Publish\n\nShriyaan AI — Made for Creators"
        textSize = 14f
        setTextColor(Color.LTGRAY)
        gravity = Gravity.CENTER
        setPadding(0, 24, 0, 0)
    }

    layout.addView(note)
    scrollView.addView(layout)
    setContentView(scrollView)
}

}
