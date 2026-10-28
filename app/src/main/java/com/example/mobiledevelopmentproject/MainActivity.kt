package com.example.mobiledevelopmentproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.mobiledevelopmentproject.ui.theme.MobileDevelopmentProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.google.firebase.auth.FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword("test@test.be", "test123456")
            .addOnSuccessListener { android.util.Log.d("FirebaseTest", "OK: ${it.user?.uid}") }
            .addOnFailureListener { android.util.Log.e("FirebaseTest", "FOUT", it) }
        enableEdgeToEdge()
        setContent {
            MobileDevelopmentProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MobileDevelopmentProjectTheme {
        Greeting("Android")
    }
}