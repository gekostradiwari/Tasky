package com.android.tasky.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.android.tasky.utility.RetrofitInstance
import com.android.tasky.utility.RetrofitInterface
import com.android.tasky.utility.SessionManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionManager = SessionManager.getInstance(applicationContext)
        var type: String? = null;
        var email: String? = null;
        var password: String? = null;
        var sesso: String? = null;
        var id_dipartimento: Int? = null;
        var tokenDecrypted = sessionManager.getAuthToken()
        val api = RetrofitInstance.getRetrofitInstance().create(RetrofitInterface::class.java)
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception")
        }
        if (!tokenDecrypted.isNullOrBlank()) {
            lifecycleScope.launch(Dispatchers.IO + handler) {
                try {
                    val responseStatus = async { api.login(mapOf("token" to tokenDecrypted)) }
                    println("Start loading")
                    val response = responseStatus.await()
                    println("End loading")
                    if (response.isSuccessful) {
                        type = response.body()!!.data.type
                        sesso = response.body()!!.data.sesso
                        id_dipartimento = response.body()!!.data.dipartimento
                        println("1Mando l'utente alla homepage oppure ritorno il token attraverso intent")
                        val risultatoIntent = Intent()
                        risultatoIntent.putExtra("type", type)
                        risultatoIntent.putExtra("token", tokenDecrypted)
                        risultatoIntent.putExtra("sesso", sesso)
                        risultatoIntent.putExtra("id_dipartimento", id_dipartimento)
                        setResult(RESULT_OK, risultatoIntent)

                        finish()

                    }
                } catch (e: java.net.ConnectException) {
                    println("Impossibile contattare il server")
                } catch (e: java.io.IOException) {
                    println("Problema di connessione")
                } catch (e: Exception) {
                    println("Errore sconosciuto")
                }
            }
        }
        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color("#D06FCA".toColorInt()),
                                Color("#A56FD9".toColorInt()),
                                Color("#866FE5".toColorInt()),
                                Color("#7B6FE9".toColorInt()),
                                Color("#A56FD9".toColorInt()),
                                Color("#D06FCA".toColorInt()),
                                Color("#A56FD9".toColorInt()),
                                Color("#7B6FE9".toColorInt())
                            )

                        )
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                var tempEmail by remember { mutableStateOf("") }
                var tempPassword by remember { mutableStateOf("") }
                TextField(
                    value = tempEmail,
                    onValueChange = { it -> tempEmail = it },
                    placeholder = { Text("Email") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = tempPassword,
                    onValueChange = { it -> tempPassword = it },
                    placeholder = { Text("Password") }
                )
                Button(onClick = {
                    email = tempEmail
                    password = tempPassword
                    lifecycleScope.launch(Dispatchers.IO + handler) {
                        try {
                            val responseStatus =
                                async { api.login(mapOf("email" to email, "password" to password)) }
                            println("Start loading")
                            val response = responseStatus.await()
                            println("End loading")
                            if (response.isSuccessful) {
                                sessionManager.clearAuthToken()
                                sessionManager.saveAuthToken(response.body()!!.data.token)
                                type = response.body()!!.data.type
                                sesso = response.body()!!.data.sesso
                                id_dipartimento = response.body()!!.data.dipartimento
                                println("2Mando l'utente sulla pagina iniziale o intent a main activity")
                                val risultatoIntent = Intent()
                                risultatoIntent.putExtra("type", type)
                                risultatoIntent.putExtra("token", tokenDecrypted)
                                risultatoIntent.putExtra("sesso", sesso)
                                risultatoIntent.putExtra("id_dipartimento", id_dipartimento)
                                setResult(RESULT_OK, risultatoIntent)
                                finish()
                            } else {
                                println("2Email o password errati")
                            }
                        } catch (e: java.net.ConnectException) {
                            println("Impossibile contattare il server")
                        } catch (e: java.io.IOException) {
                            println("Problema di connessione")
                        } catch (e: Exception) {
                            println("Errore sconosciuto $e")
                        }
                    }

                }) {
                    Text(text = "Login")
                }
            }
        }
    }

}