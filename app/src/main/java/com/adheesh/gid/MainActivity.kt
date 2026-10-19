package com.adheesh.gid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.adheesh.gid.app.GIDNavHost
import com.adheesh.gid.core.designsystem.GIDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge: the system draws behind the status and nav bars.
        // Our Scaffold handles the insets via innerPadding.
        enableEdgeToEdge()

        setContent {
            GIDTheme {
                GIDNavHost()
            }
        }
    }
}
