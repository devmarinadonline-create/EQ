package com.n3p1x69.eq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.n3p1x69.eq.ui.EQScreen
import com.n3p1x69.eq.ui.EQTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as EQApp
        setContent {
            EQTheme {
                val vm: EQViewModel = viewModel(factory = EQViewModelFactory(app.engine))
                EQScreen(vm)
            }
        }
    }
}
