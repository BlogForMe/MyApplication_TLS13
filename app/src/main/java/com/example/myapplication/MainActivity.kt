package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.myapplication.edge.FixedActivity
import com.example.myapplication.edge.ProblematicActivity
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        this,
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(context: Context, name: String, modifier: Modifier = Modifier) {
    Column {
        Button(onClick = {
            OkHttpSelfSignedDemo.makeRequest(context)
        }) {
            Text(
                text = "Hello TLSV1.3 OKhttp $name!",
                modifier = modifier
            )
        }
        Button(onClick = {
            OkHttpCustomCAAsync.makeAsyncRequest(context)
        }) {

            Text(
                text = "Hello OKHTTP CA $name!",
                modifier = modifier
            )
        }
        Button(onClick = {
//            val url  = "https://tls13.1d.pw/"
            val url = "https://realmjon.mynetgear.com:8443/hello"
            Executors.newCachedThreadPool().execute {
                HttpsUrlConnection.reqeustConnection(context, url)
            }
        }) {
            Text(
                text = "Https Url Connection",
                modifier = modifier
            )
        }
        Button (onClick = {
            context.startActivity(Intent(context,ProblematicActivity::class.java))
        }) {
            Text(
                text = "problem edge activity",
                modifier = modifier
            )
        }
        Button (onClick = {
            context.startActivity(Intent(context,FixedActivity::class.java))
        }) {
            Text(
                text = "fixed edge activity",
                modifier = modifier
            )
        }
    }

}