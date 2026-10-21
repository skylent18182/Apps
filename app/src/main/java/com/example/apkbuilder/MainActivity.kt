package com.example.apkbuilder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.apkbuilder.ui.MainApp
import com.example.apkbuilder.viewmodel.ApkBuilderViewModel
import com.example.ui.theme.ApkBuilderTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ApkBuilderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApkBuilderTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}
