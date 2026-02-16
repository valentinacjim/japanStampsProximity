package com.mapclover.stampquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mapclover.stampquest.ui.StampQuestApp
import com.mapclover.stampquest.ui.theme.StampQuestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // se ejecuta cuando la app arranca
        setContent {
            StampQuestTheme {
                StampQuestApp()
            }
        }

    }
}