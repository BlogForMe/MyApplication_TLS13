package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.keep.TestClass
import com.example.myapplication.ui.theme.MyApplicationTheme

class KeepTestActivty : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use TestClass - this makes it reachable, so normally it would be kept
//        val test = TestClass()
//        Log.d("KeepDemo", "Using TestClass: ${test.publicMethod()}")
        // Remove direct usage, use only reflection
        try {
            val clazz = Class.forName("com.example.myapplication.keep.TestClass")
            val instance = clazz.getDeclaredConstructor().newInstance()
            // This will break if TestClass gets obfuscated!
        } catch (e: Exception) {
            Log.e("Test", "Reflection failed: ${e.message}")
        }
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KeepTestScreen()
                }
            }
        }
    }
}

@Composable
fun KeepTestScreen() {
    var result by remember { mutableStateOf("Click buttons to test") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "-keep Rule Test Demo",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Test Steps:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = """
                    1. Build release: ./gradlew assembleRelease
                    2. Check mapping.txt for obfuscation results
                    3. Modify proguard-rules.pro and rebuild
                    4. Compare mapping.txt differences
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Button(
            onClick = {
//                val test = TestClass()
//                result = "TestClass: ${test.publicMethod()}\nPrivate: ${test.getPrivateData()}"
//                Log.d("KeepDemo", "Button clicked - TestClass used")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Use TestClass")
        }

        Button(
            onClick = {
                try {
                    // Try to access TestClass via reflection
                    val clazz = Class.forName("com.example.keepdemo.test.TestClass")
                    val instance = clazz.getDeclaredConstructor().newInstance()
                    val method = clazz.getMethod("publicMethod")
                    val reflectionResult = method.invoke(instance)
                    result = "Reflection success: $reflectionResult"
                    Log.d("KeepDemo", "Reflection success")
                } catch (e: Exception) {
                    result = "Reflection failed: ${e.message}"
                    Log.e("KeepDemo", "Reflection failed", e)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test Reflection")
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Result:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = result,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Expected mapping.txt results:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = """
                    Without -keep rules:
                    TestClass -> a: (obfuscated)
                    UnusedClass -> (removed/obfuscated)
                    
                    With -keep rules:
                    TestClass -> TestClass: (preserved)
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}