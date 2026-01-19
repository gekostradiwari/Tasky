package com.android.tasky.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType.Companion.Date
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.android.tasky.R
import com.android.tasky.dto.Dipendente
import com.android.tasky.dto.Progetto
import com.android.tasky.dto.Task
import com.android.tasky.ui.theme.computerSaysNo
import com.android.tasky.utility.RetrofitInstance
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskSospender : ComponentActivity(){
    override fun onCreate(savedInstanceState : Bundle?){
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val intent = this.intent
        val jsonRicevuto = intent.getStringExtra("task")
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val taskAdapter = moshi.adapter(Task::class.java)
        var taskObj: Task? = if (jsonRicevuto != null) {
            taskAdapter.fromJson(jsonRicevuto)
        } else {
            null
        }
        val email = intent.getStringExtra("email")
        val type = intent.getStringExtra("type")
        val token = intent.getStringExtra("token")
        val tipo = intent.getStringExtra("tipo")
        val dipartimento = intent.getIntExtra("dipartimento",0)
        val progetto = intent.getIntExtra("progetto_id",0)
        val api = RetrofitInstance.api
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception")
        }
        setContent{
            val context = LocalContext.current
            var isLoading by remember { mutableStateOf(false) }
            var showConnErrorDialog by remember { mutableStateOf(false) }
            var isConfirmedDialog by remember {mutableStateOf(false)}
            var isCompletedDialog by remember {mutableStateOf(false)}
            var isConfirmed by remember {mutableStateOf(false)}
            var isCompleted by remember {mutableStateOf(false)}
            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(brush = Brush.horizontalGradient(
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

                            ))
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                            .height(70.dp),
                        horizontalArrangement = Arrangement.spacedBy(125.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { (context as? Activity)?.finish()}
                        ) {
                            Icon(Icons.Default.ArrowBack, "TurnBack")
                        }
                        Image(
                            painter = painterResource(id = R.drawable.taskyfinalnobackground),
                            contentDescription = "Logo Tasky",
                            modifier = Modifier
                                .size(74.dp)
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
                content = { paddingValues ->
                    TaskSospenderPreview(taskObj!!, type!!, tipo!!, paddingValues, {isLoading = it}, token!!,{showConnErrorDialog = it}, {isCompletedDialog = it})
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize() // Occupa tutto lo schermo
                                .background(Color.Black.copy(alpha = 0.4f)) // Sfondo scuro e semitrasparente
                                .clickable(enabled = false, onClick = {})
                                .padding(paddingValues), // Blocca i click sullo sfondo
                            contentAlignment = Alignment.Center // 2. Centra TUTTO il suo contenuto
                        ) {
                            // 3. Il CircularProgressIndicator ora verrà centrato da questo Box
                            CircularProgressIndicator(
                                color = Color.Magenta,
                                strokeWidth = 5.dp // Aumentato leggermente per maggiore visibilità
                            )
                        }
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
                    if (isCompletedDialog) {
                        AlertDialog(
                            onDismissRequest = { isCompletedDialog = false },
                            icon = { Image(
                                painter = painterResource(id = R.drawable.check_mark_button_svgrepo_com),
                                contentDescription = "Warning",
                                modifier = Modifier.size(48.dp),
                            ) },
                            title = { Text("Success!") },
                            text = { Text("L'operazione è avvenuta con successo!") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        // Chiudi il dialog e permetti all'utente di riprovare
                                        isCompleted = true
                                        isCompletedDialog = false
                                        val intent = Intent(this, HomeManagerActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        startActivity(intent)
                                        finish()
                                    }
                                ) {
                                    Text("OK")
                                }
                            },
                            containerColor = Color.White,
                            iconContentColor = MaterialTheme.colorScheme.error,
                            titleContentColor = Color.Black
                        )
                    }
                }
            )

        }

    }
}

@Composable
fun TaskSospenderPreview(taskObj: Task, type:String, tipo:String, paddingValues: PaddingValues, onLoadingChange: (Boolean) -> Unit, token: String, onErrorConn: (Boolean) -> Unit, isCompletedDialog: (Boolean) -> Unit){
    val api = RetrofitInstance.api
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught $exception")
    }
    val scope = rememberCoroutineScope()
    var testoDescrizione by remember { mutableStateOf("") }
    var isDescrizioneValid by remember { mutableStateOf(true) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { /* Non fare nulla per renderlo modale */ },
            icon = { Image(
                painter = painterResource(id = R.drawable.police_car_light_svgrepo_com),
                contentDescription = "Warning",
                modifier = Modifier.size(48.dp),
            ) },
            title = { Text("Attenzione") },
            text = { Text("Desideri davvero proseguire con la sospensione del seguente task?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        scope.launch(Dispatchers.IO + handler) {
                            try{
                                withContext(Dispatchers.Main) {
                                    onLoadingChange(true)
                                }
                                val responseStatus = async{ api.updateTaskMGR(mapOf<String,Any>("token" to token, "id" to taskObj.id, "nome" to taskObj.nome ,"stato" to "Sospeso", "descrizione" to testoDescrizione,
                                    "data_inizio" to taskObj.data_inizio, "data_fine" to taskObj.data_fine, "email_dipendente" to taskObj.Dipendente_email, "email_manager" to taskObj.Manager_email))}
                                val response = responseStatus.await()
                                withContext(Dispatchers.Main) {
                                    onLoadingChange(false)
                                }
                                if(response.isSuccessful){
                                    withContext(Dispatchers.Main) {
                                        taskObj.stato = "Sospeso"
                                        isCompletedDialog(true)
                                    }
                                }
                            }catch (e: java.net.ConnectException) {
                                withContext(Dispatchers.Main) {
                                    onErrorConn(true)
                                }
                            } catch (e: java.io.IOException) {
                                withContext(Dispatchers.Main) {
                                    onErrorConn(true)
                                }
                            } catch (e: Exception) {
                                println("Errore sconosciuto $e")
                            }catch (e: java.net.SocketTimeoutException) {
                                withContext(Dispatchers.Main) {
                                    onErrorConn(true)
                                }
                            } finally {
                                withContext(Dispatchers.Main){
                                    onLoadingChange(false)
                                }
                            }
                        }
                    }
                ) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        // Chiudi il dialog e pulisci i campi
                        showConfirmationDialog = false
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(370.dp)
                .height(400.dp)

        ) {
            Box(
                modifier = Modifier
                    .width(370.dp)
                    .height(200.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color("#FF850A".toColorInt()),
                                Color("#FBAB76".toColorInt()),
                            ),
                        ),
                        shape = RoundedCornerShape(24)
                    ),
                contentAlignment = Alignment.TopStart,

                ) {
                Text(
                    "Motivazione:",
                    textAlign = TextAlign.Start,
                    fontFamily = computerSaysNo,
                    fontWeight = FontWeight.W400,
                    fontSize = 40.sp,
                    modifier = Modifier
                        .width(360.dp)
                        .align(Alignment.Center)
                        .padding(start = 38.dp, bottom = 130.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                Spacer(Modifier.padding(start = 150.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .width(180.dp)
                            .height(250.dp)
                            .shadow(elevation = 20.dp, shape = RoundedCornerShape(12))
                            .background(Color.White, shape = RoundedCornerShape(12)),
                        shape = RoundedCornerShape(34),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                        ),
                        value = testoDescrizione, //Qui ci va sempre Task.stato,
                        onValueChange = { newText ->
                            testoDescrizione = newText
                            isDescrizioneValid = newText.length <= 500
                        },
                        isError = !isDescrizioneValid,
                        placeholder = { Text("Max 500 caratteri...") },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.W400,
                            fontSize = 20.sp,
                        )
                        //trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                    )
                }
            }
        }
        Spacer(Modifier.padding(top = 40.dp))
        Button(
            onClick = {
                showConfirmationDialog = true
            },
            colors = ButtonColors(
                containerColor = Color("#FF0A0A".toColorInt()),
                contentColor = Color.Black,
                disabledContainerColor = Color("#FF0A0A".toColorInt()),
                disabledContentColor = Color("#FF0A0A".toColorInt())
            ),
            modifier = Modifier
                .width(270.dp)
                .height(63.dp)
        ){
            Text(
                "Sospendi",
                textAlign = TextAlign.Start,
                fontFamily = computerSaysNo,
                fontWeight = FontWeight.W400,
                fontSize = 40.sp,
            )
        }
    }

}

