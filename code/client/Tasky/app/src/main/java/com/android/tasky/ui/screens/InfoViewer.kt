package com.android.tasky.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.android.tasky.R
import com.android.tasky.dto.Progetto
import com.android.tasky.dto.Task
import com.android.tasky.ui.theme.computerSaysNo
import com.android.tasky.utility.RetrofitInstance
import com.android.tasky.utility.RetrofitInterface
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InfoViewer : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val jsonRicevuto = intent.getStringExtra("task")
        val token = intent.getStringExtra("token")
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val taskAdapter = moshi.adapter(Task::class.java)
        var taskObj: Task? = if (jsonRicevuto != null) {
            taskAdapter.fromJson(jsonRicevuto)
        } else {
            null
        }
        val jsonRicevutoProject = intent.getStringExtra("progetto")
        val progettoAdapter = moshi.adapter(Progetto::class.java)
        var progettoObj: Progetto? = if (jsonRicevutoProject != null) {
            progettoAdapter.fromJson(jsonRicevutoProject)
        } else {
            null
        }
        val infoType = intent.getStringExtra("infoType")
        val type = intent.getStringExtra("type")
        val tipo = intent.getStringExtra("tipo")
        setContent {
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
                        modifier = Modifier.height(70.dp)
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

                            )),
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
                    if(infoType.equals("task")){
                        infoTask(taskObj!!, type!!, tipo!!, paddingValues, {isLoading = it}, token!!, {showConnErrorDialog = it}, {isCompletedDialog = it}, {isConfirmedDialog = it}, isConfirmed, isCompleted)
                    }
                    else if(infoType.equals("progetto")) {
                        infoProgetto(progettoObj!!, paddingValues, {showConnErrorDialog = it})
                    }
                    else{
                        Text(text = "Errore")
                    }
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
                    if (isConfirmedDialog) {
                        AlertDialog(
                            onDismissRequest = { /* Non fare nulla per renderlo modale */ },
                            icon = { Image(
                                painter = painterResource(id = R.drawable.police_car_light_svgrepo_com),
                                contentDescription = "Warning",
                                modifier = Modifier.size(48.dp),
                            ) },
                            title = { Text("Attenzione") },
                            text = { Text("Desideri davvero proseguire con il cambio di stato del seguente task?") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        // Chiudi il dialog e permetti all'utente di riprovare
                                        isConfirmed = true
                                        isConfirmedDialog = false
                                    }
                                ) {
                                    Text("Conferma")
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = {
                                        // Chiudi il dialog e pulisci i campi
                                        isConfirmedDialog = false
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
                    if (isCompletedDialog) {
                        AlertDialog(
                            onDismissRequest = { isCompletedDialog = false },
                            icon = { Image(
                                painter = painterResource(id = R.drawable.check_mark_button_svgrepo_com),
                                contentDescription = "Warning",
                                modifier = Modifier.size(48.dp),
                            ) },
                            title = { Text("Success!") },
                            text = { Text("Lo stato della task è stata cambiata con successo!") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        // Chiudi il dialog e permetti all'utente di riprovare
                                        isCompleted = true
                                        isCompletedDialog = false
                                        if(tipo.equals("dipendente")) {
                                            val intent = Intent(this, HomeDipendenteActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                            startActivity(intent)
                                            finish()
                                        }
                                        else{
                                            val intent = Intent(this, HomeManagerActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                            startActivity(intent)
                                            finish()
                                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun infoTask(taskObj: Task, type:String, tipo:String, paddingValues: PaddingValues, onLoadingChange: (Boolean) -> Unit, token: String, onErrorConn: (Boolean) -> Unit, isCompletedDialog: (Boolean) -> Unit, isConfirmedDialog: (Boolean) -> Unit, isConfirmed: Boolean, isCompleted: Boolean) {
    var statoAttuale by remember { mutableStateOf(taskObj.stato) }
    var isExpanded by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var statoDaImpostare by remember { mutableStateOf("")}
    val api = RetrofitInstance.api
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught $exception")
    }
    val scope = rememberCoroutineScope()
    val gradientColors = if(statoAttuale.equals("Completato")){
        listOf(
            Color("#66D161".toColorInt()),
            Color("#B2FFB7".toColorInt()),
        )
    } else if(statoAttuale.equals("InProgress")){
        listOf(
            Color("#FF07F0".toColorInt()),
            Color("#D06FCA".toColorInt()),
        )
    }else{
        listOf(
            Color("#FF850A".toColorInt()),
            Color("#FBAB76".toColorInt()),
        )
    }
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { /* Non fare nulla per renderlo modale */ },
            icon = { Image(
                painter = painterResource(id = R.drawable.police_car_light_svgrepo_com),
                contentDescription = "Warning",
                modifier = Modifier.size(48.dp),
            ) },
            title = { Text("Attenzione") },
            text = { Text("Desideri davvero proseguire con il cambio di stato del seguente task?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        scope.launch(Dispatchers.IO + handler) {
                            try {
                                withContext(Dispatchers.Main) {
                                    onLoadingChange(true)
                                }

                                val responseStatus = async {
                                    if(tipo.equals("dipendente")) {
                                        api.updateTask(
                                            mapOf<String, Any>(
                                                "token" to token,
                                                "id" to taskObj.id,
                                                "stato" to statoDaImpostare
                                            )
                                        )
                                    }
                                    else{
                                            api.updateTaskMGR(
                                                mapOf<String, Any>(
                                                    "token" to token,
                                                    "id" to taskObj.id,
                                                    "nome" to taskObj.nome,
                                                    "stato" to statoDaImpostare,
                                                    "descrizione" to taskObj.descrizione,
                                                    "data_inizio" to taskObj.data_inizio,
                                                    "data_fine" to taskObj.data_fine,
                                                    "email_dipendente" to taskObj.Dipendente_email,
                                                    "email_manager" to taskObj.Manager_email
                                                )
                                            )
                                    }
                                }
                                val response = responseStatus.await()
                                withContext(Dispatchers.Main) {
                                    onLoadingChange(false)
                                }
                                if (response.isSuccessful) {
                                    withContext(Dispatchers.Main) {
                                        if(statoDaImpostare.equals("Completato")){
                                            statoAttuale = "Completato"
                                        }
                                        else if(statoDaImpostare.equals("Sospeso")){
                                            statoAttuale = "Sospeso"
                                        }
                                        else if(statoDaImpostare.equals("InProgress")){
                                            statoAttuale = "In Corso"
                                        }
                                        else{
                                            statoAttuale = ""
                                        }
                                        taskObj.stato = statoDaImpostare
                                        isCompletedDialog(true)
                                    }
                                }
                            } catch (e: java.net.ConnectException) {
                                withContext(Dispatchers.Main) {
                                    onErrorConn(true)
                                }
                            } catch (e: java.io.IOException) {
                                withContext(Dispatchers.Main) {
                                    onErrorConn(true)
                                }
                            } catch (e: Exception) {
                                println("Errore sconosciuto $e")
                            } finally {
                                withContext(Dispatchers.Main) {
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
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Spacer(Modifier.padding(top = 10.dp))
            Text("${taskObj.Dipendente_email}", //qui va inserito il nome del progetto
                textAlign = TextAlign.Center,
                fontFamily = computerSaysNo,
                fontWeight = FontWeight.W400,
                fontSize = 30.sp,
                modifier = Modifier.width(353.dp)
            )

        Spacer(Modifier.padding(bottom = 5.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(358.dp)
                .height(92.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = gradientColors,
                    ),
                    shape = RoundedCornerShape(34)
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
            ) {

                Text(
                    "Stato:",
                    textAlign = TextAlign.End,
                    fontFamily = computerSaysNo,
                    fontWeight = FontWeight.W400,
                    fontSize = 40.sp,
                    modifier = Modifier.width(120.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .width(250.dp)
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .background(Color.White, shape = RoundedCornerShape(34)),
                        shape = RoundedCornerShape(34),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                        ),
                        value = statoAttuale, //Qui ci va sempre Task.stato,
                        readOnly = true,
                        onValueChange = {},
                        textStyle = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.W400,
                            fontSize = 20.sp,
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false },
                    ) {
                        if(tipo.equals("dipendente") && (taskObj.stato.equals("Completato") || taskObj.stato.equals("Sospeso"))){

                        }
                        else if(tipo.equals("dipendente") && taskObj.stato.equals("InProgress")){
                            DropdownMenuItem(
                                text = { Text("Completato") },
                                onClick = {
                                    statoDaImpostare = "Completato"
                                    showConfirmationDialog =true
                                    isExpanded = false
                                },
                            )
                        }
                        else{
                            DropdownMenuItem(
                                text = { Text("Completato") },
                                onClick = {
                                    statoDaImpostare = "Completato"
                                    showConfirmationDialog =true
                                    isExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("In corso") },
                                onClick = {
                                    statoDaImpostare = "InProgress"
                                    showConfirmationDialog =true
                                    isExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Sospeso") },
                                onClick = {
                                    statoDaImpostare = "Sospeso"
                                    showConfirmationDialog =true
                                    isExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(358.dp)
                .height(250.dp)

        ) {
            Box(
                modifier = Modifier
                    .width(358.dp)
                    .height(200.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = gradientColors,
                        ),
                        shape = RoundedCornerShape(12)
                    ),
                contentAlignment = Alignment.CenterStart,

                ) {
                Text(
                    "Descrizione:",
                    textAlign = TextAlign.Start,
                    fontFamily = computerSaysNo,
                    fontWeight = FontWeight.W400,
                    fontSize = 40.sp,
                    modifier = Modifier
                        .width(160.dp)
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                Spacer(Modifier.padding(start = 150.dp))
                OutlinedTextField(
                    modifier = Modifier
                        .width(180.dp)
                        .height(200.dp)
                        .shadow(elevation = 20.dp, shape = RoundedCornerShape(12))
                        .background(Color.White, shape = RoundedCornerShape(12)),
                    shape = RoundedCornerShape(34),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                    ),
                    value = taskObj.descrizione, //Qui ci va sempre Task.stato,
                    readOnly = true,
                    onValueChange = {},
                    textStyle = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.W400,
                        fontSize = 20.sp,
                    )
                    //trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                )
            }
        }
        Spacer(Modifier.padding(bottom = 20.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(358.dp)
                .height(132.dp)

        ) {

            Box(
                modifier = Modifier
                    .width(358.dp)
                    .height(132.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = gradientColors
                        ),
                        shape = RoundedCornerShape(34)
                    ),
                contentAlignment = Alignment.CenterStart,

                ) {
                Text(
                    "Data Inizio:",
                    textAlign = TextAlign.Start,
                    fontFamily = computerSaysNo,
                    fontWeight = FontWeight.W400,
                    fontSize = 40.sp,
                    modifier = Modifier
                        .width(160.dp)
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp),
                )
            }
            Image(
                painter = painterResource(id = R.drawable.tear_off_calendar_svgrepo_com),
                contentDescription = "Calendar",
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.TopStart)
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Spacer(Modifier.padding(start = 140.dp))
                OutlinedTextField(
                    modifier = Modifier
                        .width(170.dp)
                        .height(58.dp)
                        .background(Color.White, shape = RoundedCornerShape(34)),
                    shape = RoundedCornerShape(34),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                    ),
                    value = "${taskObj.data_inizio}", //Qui ci va sempre Task.stato,
                    readOnly = true,
                    onValueChange = {},
                    textStyle = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.W400,
                        fontSize = 20.sp,
                    )
                    //trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                )

            }


        }
        Spacer(Modifier.padding(top = 25.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(358.dp)
                .height(132.dp)

        ) {

            Box(
                modifier = Modifier
                    .width(358.dp)
                    .height(132.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = gradientColors
                        ),
                        shape = RoundedCornerShape(34)
                    ),
                contentAlignment = Alignment.CenterStart,

                ) {
                Text(
                    "Data Fine:",
                    textAlign = TextAlign.Start,
                    fontFamily = computerSaysNo,
                    fontWeight = FontWeight.W400,
                    fontSize = 40.sp,
                    modifier = Modifier
                        .width(160.dp)
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp),
                )
            }
            Image(
                painter = painterResource(id = R.drawable.tear_off_calendar_svgrepo_com),
                contentDescription = "Calendar",
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.TopStart)
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Spacer(Modifier.padding(start = 140.dp))
                OutlinedTextField(
                    modifier = Modifier
                        .width(170.dp)
                        .height(58.dp)
                        .background(Color.White, shape = RoundedCornerShape(34)),
                    shape = RoundedCornerShape(34),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                    ),
                    value = "${taskObj.data_fine}", //Qui ci va sempre Task.stato,
                    readOnly = true,
                    onValueChange = {},
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
}

@Composable
fun infoProgetto(progetto: Progetto, paddingValues: PaddingValues, onErrorConn: (Boolean) -> Unit){
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ){
        Spacer(Modifier.padding(top = 10.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(358.dp)
                .height(97.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color("#96FF13".toColorInt()),
                            Color("#97C65C".toColorInt()),
                        ),
                    ),
                    shape = RoundedCornerShape(34)
                )
        ){
            Text("Budget: ${progetto.budgetIstanziato}€", //qui va inserito il budget che è stato istanziato per il progetto
                textAlign = TextAlign.Center,
                fontFamily = computerSaysNo,
                fontWeight = FontWeight.W400,
                fontSize = 40.sp,
                modifier = Modifier.width(250.dp)
            )

        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(358.dp)
                .height(250.dp)
        ){
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(358.dp)
                    .height(250.dp)

            ) {
                Box(
                    modifier = Modifier
                        .width(358.dp)
                        .height(200.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color("#FF12F0".toColorInt()),
                                    Color("#D06FCA".toColorInt()),
                                ),
                            ),
                            shape = RoundedCornerShape(12)
                        ),
                    contentAlignment = Alignment.CenterStart,

                    ) {
                    Text(
                        "Descrizione:",
                        textAlign = TextAlign.Start,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(160.dp)
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxHeight()
                ) {
                    Spacer(Modifier.padding(start = 150.dp))
                    OutlinedTextField(
                        modifier = Modifier
                            .width(180.dp)
                            .height(200.dp)
                            .shadow(elevation = 20.dp, shape = RoundedCornerShape(12))
                            .background(Color.White, shape = RoundedCornerShape(12)),
                        shape = RoundedCornerShape(34),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                        ),
                        value = progetto.descrizione, //Qui ci va sempre Task.stato,
                        readOnly = true,
                        onValueChange = {},
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
        Spacer(Modifier.padding(bottom = 20.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(358.dp)
                .height(97.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color("#A56FD9".toColorInt()),
                            Color("#7B6FE9".toColorInt()),
                        ),
                    ),
                    shape = RoundedCornerShape(34)
                )
        ){
            Text("Nome: ${progetto.nome}", //qui va inserito il nome del progetto
                textAlign = TextAlign.Center,
                fontFamily = computerSaysNo,
                fontWeight = FontWeight.W400,
                fontSize = 40.sp,
                modifier = Modifier.width(250.dp)
            )

        }
        Spacer(Modifier.padding(top = 20.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(358.dp)
                .height(67.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color("#FF07F0".toColorInt()),
                            Color("#D06FCA".toColorInt()),
                        ),
                    ),
                    shape = RoundedCornerShape(70)
                )
        ){
            Text("Data inizio: ${progetto.dataInizio}", //qui va inserito il nome del progetto
                textAlign = TextAlign.Center,
                fontFamily = computerSaysNo,
                fontWeight = FontWeight.W400,
                fontSize = 40.sp,
                modifier = Modifier.fillMaxSize().padding(top=16.dp)
            )

        }
        Spacer(Modifier.padding(top = 20.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(358.dp)
                .height(67.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color("#FF07F0".toColorInt()),
                            Color("#D06FCA".toColorInt()),
                        ),
                    ),
                    shape = RoundedCornerShape(70)
                )
        ){
            Text("Data fine: ${progetto.dataFine}", //qui va inserito il nome del progetto
                textAlign = TextAlign.Center,
                fontFamily = computerSaysNo,
                fontWeight = FontWeight.W400,
                fontSize = 40.sp,
                modifier = Modifier.fillMaxSize().padding(top=16.dp)
            )

        }
    }
}
