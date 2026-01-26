package com.android.tasky.ui.screens

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.android.tasky.R
import com.android.tasky.ui.theme.computerSaysNo
import com.android.tasky.utility.RetrofitInstance
import com.android.tasky.utility.RetrofitInterface
import com.android.tasky.utility.SessionManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.transition.Slide
import android.view.Gravity

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionManager = SessionManager.getInstance(applicationContext)
        var type: String? = null;
        var email: String? = null;
        var password: String? = null;
        var sesso: String? = null;
        var id_dipartimento: Int? = null;
        var nome_dipartimento: String? = null;
        var nome: String? = null;
        var tokenDecrypted = sessionManager.getAuthToken()
        val api = RetrofitInstance.api
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception")
        }
        setContent {
            val context = LocalContext.current
            var showDialog by remember { mutableStateOf(false) }
            var isLoading by remember { mutableStateOf(false) }
            var showLoginErrorDialog by remember { mutableStateOf(false) }
            var tempEmail by remember { mutableStateOf("") }
            var tempPassword by remember { mutableStateOf("") }
            var isEmailValid by remember { mutableStateOf(true) }
            var showConnErrorDialog by remember { mutableStateOf(false) }
            LaunchedEffect(Unit){
                if (!tokenDecrypted.isNullOrBlank()) {
                    lifecycleScope.launch(Dispatchers.IO + handler) {
                        try {
                            val responseStatus = async { api.login(mapOf("token" to tokenDecrypted)) }
                            withContext(Dispatchers.Main) {
                                isLoading = true
                            }
                            val response = responseStatus.await()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                            if (response.isSuccessful) {
                                withContext(Dispatchers.Main) {
                                    type = response.body()!!.data.type
                                    sesso = response.body()!!.data.sesso
                                    id_dipartimento = response.body()!!.data.id_dipartimento
                                    nome_dipartimento = response.body()!!.data.nome_dipartimento
                                    nome = response.body()!!.data.name
                                    email = response.body()!!.data.email
                                    println("1Mando l'utente alla homepage oppure ritorno il token attraverso intent")
                                    val risultatoIntent = Intent()
                                    risultatoIntent.putExtra("type", type)
                                    risultatoIntent.putExtra("token", tokenDecrypted)
                                    risultatoIntent.putExtra("sesso", sesso)
                                    risultatoIntent.putExtra("id_dipartimento", id_dipartimento)
                                    risultatoIntent.putExtra("nome_dipartimento", nome_dipartimento)
                                    risultatoIntent.putExtra("nome", nome)
                                    risultatoIntent.putExtra("email", email)
                                    setResult(RESULT_OK, risultatoIntent)

                                    finish()
                                }

                            }
                        } catch (e: java.net.ConnectException) {
                            withContext(Dispatchers.Main) {
                                showConnErrorDialog = true
                            }

                        } catch (e: java.io.IOException) {
                            withContext(Dispatchers.Main) {
                                showConnErrorDialog = true
                            }
                        } catch (e: Exception) {
                            println("Errore sconosciuto1")
                        }
                        catch (e: java.net.SocketTimeoutException) {
                            withContext(Dispatchers.Main) {
                                showConnErrorDialog = true
                            }
                        }
                        finally {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                        }
                    }
                }
            }
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    icon =
                        {
                            Image(
                                painter = painterResource(id = R.drawable.police_car_light_svgrepo_com),
                                contentDescription = "Warning",
                                modifier = Modifier.size(48.dp),
                            )
                        },
                    title = { Text("Uscire da Tasky?") },
                    text = { Text("Sei sicuro di voler uscire da Tasky?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                (context as? Activity)?.finishAffinity()
                            },
                        ) {
                            Text("Conferma")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = { showDialog = false }
                        ) {
                            Text("Annulla", color = Color.Red)
                        }
                    }
                )
            }
            BackHandler {
                showDialog = true
            }
            if (showLoginErrorDialog) {
                AlertDialog(
                    onDismissRequest = { /* Non fare nulla per renderlo modale */ },
                    icon = { Image(
                        painter = painterResource(id = R.drawable.police_car_light_svgrepo_com),
                        contentDescription = "Warning",
                        modifier = Modifier.size(48.dp),
                    ) },
                    title = { Text("Login Fallito") },
                    text = { Text("Email o password sono errati. Controlla i dati e riprova.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                // Chiudi il dialog e permetti all'utente di riprovare
                                showLoginErrorDialog = false
                            }
                        ) {
                            Text("Riprova")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                // Chiudi il dialog e pulisci i campi
                                tempEmail = ""
                                tempPassword = ""
                                showLoginErrorDialog = false
                            }
                        ) {
                            Text("Annulla", color = Color.Red)
                        }
                    },
                    containerColor = Color.White,
                    iconContentColor = MaterialTheme.colorScheme.error,
                    titleContentColor = Color.Black
                )
            }
            if (showConnErrorDialog) {
                AlertDialog(
                    onDismissRequest = { /* Non fare nulla per renderlo modale */ },
                    icon = { Image(
                        painter = painterResource(id = R.drawable.police_car_light_svgrepo_com),
                        contentDescription = "Warning",
                        modifier = Modifier.size(48.dp),
                    ) },
                    title = { Text("Problema di connessione") },
                    text = { Text("Problema di connessione si prega di attendere e riprovare.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                // Chiudi il dialog e permetti all'utente di riprovare
                                showConnErrorDialog = false
                            }
                        ) {
                            Text("Riprova")
                        }
                    },
                    containerColor = Color.White,
                    iconContentColor = MaterialTheme.colorScheme.error,
                    titleContentColor = Color.Black
                )
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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

                    Image(
                        painter = painterResource(id = R.drawable.taskyfinalnobackground),
                        contentDescription = "Logo Tasky",
                        modifier = Modifier.size(180.dp)
                    )

                    Text(
                        text = "Entra in App!",
                        textAlign = TextAlign.Start,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W500,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(200.dp)
                            .padding(start = 18.dp, bottom = 50.dp),
                        // --- INIZIO MODIFICA PER L'OMBRA ---
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.5f), // Colore dell'ombra (nero con trasparenza)
                                offset = Offset(
                                    x = 4f,
                                    y = 4f
                                ),     // Spostamento (x, y) dell'ombra rispetto al testo
                                blurRadius = 8f                        // Quanto deve essere sfocata l'ombra
                            )
                        )
                    )

                    TextField(
                        value = tempEmail,
                        onValueChange = {
                            it -> tempEmail = it
                            isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches()
                                        },
                        placeholder = { Text("Email") },
                        singleLine = true,
                        isError = !isEmailValid,
                        supportingText = {
                            if(!isEmailValid){
                                Text(
                                    text = "Formato email non valido",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = tempPassword,
                        onValueChange = { it -> tempPassword = it },
                        placeholder = { Text("Password") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    )
                    Spacer(Modifier.padding(bottom = 20.dp))
                    Button(onClick = {
                        email = tempEmail
                        password = tempPassword
                        lifecycleScope.launch(Dispatchers.IO + handler) {
                            try {
                                withContext(Dispatchers.Main) {
                                    isLoading = true
                                }
                                val responseStatus =
                                    async {
                                        api.login(
                                            mapOf(
                                                "email" to email,
                                                "password" to password
                                            )
                                        )
                                    }
                                val response = responseStatus.await()
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                }
                                if (response.isSuccessful) {
                                    withContext(Dispatchers.Main) {
                                        sessionManager.clearAuthToken()
                                        sessionManager.saveAuthToken(response.body()!!.data.token)
                                        type = response.body()!!.data.type
                                        sesso = response.body()!!.data.sesso
                                        id_dipartimento = response.body()!!.data.id_dipartimento
                                        nome_dipartimento = response.body()!!.data.nome_dipartimento
                                        nome = response.body()!!.data.name
                                        println("2Mando l'utente sulla pagina iniziale o intent a main activity")
                                        val risultatoIntent = Intent()
                                        risultatoIntent.putExtra("type", type)
                                        risultatoIntent.putExtra(
                                            "token",
                                            response.body()!!.data.token
                                        )
                                        risultatoIntent.putExtra("sesso", sesso)
                                        risultatoIntent.putExtra("id_dipartimento", id_dipartimento)
                                        risultatoIntent.putExtra(
                                            "nome_dipartimento",
                                            nome_dipartimento
                                        )
                                        risultatoIntent.putExtra("nome", nome)
                                        risultatoIntent.putExtra("email", email)
                                        setResult(RESULT_OK, risultatoIntent)
                                        finish()
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        showLoginErrorDialog = true
                                    }
                                    println("2Email o password errati")
                                }
                            } catch (e: java.net.ConnectException) {
                                withContext(Dispatchers.Main) {
                                    showConnErrorDialog = true
                                }
                            } catch (e: java.io.IOException) {
                                withContext(Dispatchers.Main) {
                                    showConnErrorDialog = true
                                }
                            } catch (e: Exception) {
                                println("Errore sconosciuto2 $e")
                            }catch (e: java.net.SocketTimeoutException) {
                                withContext(Dispatchers.Main) {
                                    showConnErrorDialog = true
                                }
                            }finally {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                }
                            }
                        }

                    },
                        enabled = tempEmail.isNotBlank() && tempPassword.isNotBlank() && isEmailValid
                        ) {
                        Text(text = "Login")
                    }
                }
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize() // Occupa tutto lo schermo
                            .background(Color.Black.copy(alpha = 0.4f)) // Sfondo scuro e semitrasparente
                            .clickable(enabled = false, onClick = {}), // Blocca i click sullo sfondo
                        contentAlignment = Alignment.Center // 2. Centra TUTTO il suo contenuto
                    ) {
                        // 3. Il CircularProgressIndicator ora verrà centrato da questo Box
                        CircularProgressIndicator(
                            color = Color.Magenta,
                            strokeWidth = 5.dp // Aumentato leggermente per maggiore visibilità
                        )
                    }
                }
            }
        }
    }

}


@Composable
@Preview
fun LoginPreview() {

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
        }) {
            Text(text = "Login")
        }
    }

}