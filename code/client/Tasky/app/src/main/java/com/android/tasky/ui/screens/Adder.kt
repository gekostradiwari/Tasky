package com.android.tasky.ui.screens

import android.app.Activity
import android.content.Intent
import android.icu.util.Calendar
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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


class Adder : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val type = intent.getStringExtra("type")
        val token = intent.getStringExtra("token")
        val email = intent.getStringExtra("email")
        val id_dipartimento = intent.getIntExtra("id_dipartimento", 0)
        val nome_dipartimento = intent.getStringExtra("nome_dipartimento")
        val nome_progetto = intent.getStringExtra("nome_progetto")
        val id_progetto = intent.getIntExtra("id_progetto", 0)
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val jsonRicevutoProject = intent.getStringExtra("progetto")
        val progettoAdapter = moshi.adapter(Progetto::class.java)
        var progettoObj: Progetto? = if (jsonRicevutoProject != null) {
            progettoAdapter.fromJson(jsonRicevutoProject)
        } else {
            null
        }
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
                        modifier = Modifier
                            .fillMaxWidth()
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
                            )
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
                    if(type.equals("taskAdder")){
                    TaskAdder(paddingValues, {isLoading = it}, token, email, id_dipartimento, nome_progetto, id_progetto, progettoObj!!,{showConnErrorDialog = it}, {isCompletedDialog = it})
                    }
                    else if(type.equals("projectAdder")){
                        projectAdder(paddingValues, {isLoading = it}, token, id_dipartimento,{showConnErrorDialog = it}, {isCompletedDialog = it})
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAdder(paddingValues: PaddingValues, onLoadingChange: (Boolean) -> Unit, token: String?, email: String?, id_dipartimento: Int, nome_progetto: String?, id_progetto: Int, progettoObj: Progetto, onErrorConn: (Boolean) -> Unit, isCompletedDialog: (Boolean) -> Unit) {
    val datePickerState = rememberDatePickerState()
    var showDialog by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var selectedDateString by remember { mutableStateOf("") }
    var selectedDate: Date
    val datePickerStateFine = rememberDatePickerState()
    var showDialogFine by remember { mutableStateOf(false) }
    var selectedDateStringFine by remember { mutableStateOf("") }
    var selectedDateFine: Date
    var nomeTask by remember { mutableStateOf("") }
    var statoCorrente by remember { mutableIntStateOf(1) }
    var Dipendenti_List by remember { mutableStateOf<List<Dipendente>>(emptyList()) }
    var dipendente: Dipendente? = null
    var dipendenteSelezionato by remember { mutableStateOf(dipendente) }
    var testoDescrizione by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    //Poi va inserita la lista dei progetti per selezionare su quale progetto si vuole aggiungere la task
    var progetto: String? = null
    var progettoSelezionato by remember { mutableStateOf(progetto) }
    var isExpandedProgetto by remember { mutableStateOf(false) }
    var isNomeTaskValid by remember { mutableStateOf(true) }
    var isDescrizioneValid by remember { mutableStateOf(true) }
    var isDataInizioValid by remember { mutableStateOf(true) }
    var isDataFineValid by remember { mutableStateOf(true) }
    val dataInizioProgetto = remember { progettoObj?.dataInizio?.toDateOrNull() }
    val dataFineProgetto = remember { progettoObj?.dataFine?.toDateOrNull() }
    val api = RetrofitInstance.api
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught $exception")
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showDataAllertDialog by remember {mutableStateOf(false)}
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO + handler) {
            try {
                withContext(Dispatchers.Main) {
                    onLoadingChange(true)
                }
                val responseStatus = async {
                    api.dipendentiByDepartment(
                        mapOf<String, Any>(
                            "token" to token!!,
                            "id_dipartimento" to id_dipartimento,
                        )
                    )
                }
                val response = responseStatus.await()
                withContext(Dispatchers.Main) {
                    onLoadingChange(false)
                }
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Dipendenti_List = response.body()!!.items
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
            }
            catch (e: java.net.SocketTimeoutException) {
                withContext(Dispatchers.Main) {
                    onErrorConn(true)
                }
            }
            catch (e: Exception) {
                println("Errore sconosciuto $e")
            }finally {
                withContext(Dispatchers.Main) {
                    onLoadingChange(false)
                }
            }
        }
    }
    fun validateAll(): Boolean {
        isNomeTaskValid = nomeTask.isNotBlank() && nomeTask.length <= 20
        isDescrizioneValid = testoDescrizione.length <= 500

        val dataInizioTask = selectedDateString.toDateOrNull()
        val dataFineTask = selectedDateStringFine.toDateOrNull()

        isDataInizioValid = dataInizioTask != null && !dataInizioTask.isBeforeToday() &&
                (dataInizioProgetto == null || !dataInizioTask.before(dataInizioProgetto))

        isDataFineValid = dataFineTask != null && dataInizioTask != null &&
                !dataFineTask.before(dataInizioTask) &&
                (dataFineProgetto == null || !dataFineTask.after(dataFineProgetto))

        return isNomeTaskValid && isDescrizioneValid && isDataInizioValid && isDataFineValid && dipendenteSelezionato != null
    }

    if(showDataAllertDialog){
        AlertDialog(
            onDismissRequest = { /* Non fare nulla per renderlo modale */ },
            icon = { Image(
                painter = painterResource(id = R.drawable.police_car_light_svgrepo_com),
                contentDescription = "Attenzione",
                modifier = Modifier.size(48.dp),
            ) },
            title = { Text("Attenzione!!") },
            text = { Text("Inserire una data valida, assicurati che essa sia " +
                    "compresa tra la data di inizio e fine del progetto!") },
            confirmButton = {
                Button(
                    onClick = {
                        showDataAllertDialog = false
                    }
                ) {
                    Text("Conferma")
                }
            },
            containerColor = Color.White,
            iconContentColor = MaterialTheme.colorScheme.error,
            titleContentColor = Color.Black
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
            text = { Text("Desideri davvero proseguire con l'aggiunta del seguente task?") },
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
                                    api.addTask(
                                        mapOf<String, Any>(
                                            "token" to token!!,
                                            "id_dipartimento" to id_dipartimento,
                                            "id_progetto" to id_progetto,
                                            "nome" to nomeTask,
                                            "stato" to "InProgress",
                                            "descrizione" to testoDescrizione,
                                            "data_inizio" to selectedDateString,
                                            "data_fine" to selectedDateStringFine,
                                            "email_dipendente" to dipendenteSelezionato!!.email,
                                            "email_manager" to email!!
                                        )
                                    )
                                }
                                val response = responseStatus.await()
                                withContext(Dispatchers.Main) {
                                    onLoadingChange(false)
                                }
                                if (response.isSuccessful) {
                                    withContext(Dispatchers.Main){
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
                            }catch (e: java.net.SocketTimeoutException) {
                                withContext(Dispatchers.Main) {
                                    onErrorConn(true)
                                }
                            } finally{
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



    if (statoCorrente == 1) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Spacer(Modifier.padding(bottom = 20.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(400.dp)
                    .height(113.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color("#A56FD9".toColorInt()),
                                Color("#D06FCA".toColorInt()),
                            ),
                        ),
                        shape = RoundedCornerShape(34)
                    )
            ){
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Text(
                        "Nome Task: ",
                        textAlign = TextAlign.Center,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(150.dp)
                            .padding(start = 10.dp)
                    )
                        OutlinedTextField(
                            modifier = Modifier
                                .width(180.dp)
                                .background(Color.White, shape = RoundedCornerShape(34)),
                            shape = RoundedCornerShape(34),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                            ),
                            value = nomeTask,
                            readOnly = false,
                            onValueChange = {nome -> nomeTask = nome
                                            isNomeTaskValid = nome.length <= 20},
                            isError = !isNomeTaskValid,
                            //supportingText = {if(!isNomeTaskValid) Text("Max 20 caratteri")},
                            placeholder = { Text("Max 20 caratteri...") },
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
                    .width(400.dp)
                    .height(113.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color("#A56FD9".toColorInt()),
                                Color("#7B6FE9".toColorInt()),
                            ),
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
                        "Dipendente: ",
                        textAlign = TextAlign.Start,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(150.dp)
                            .padding(start = 10.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = isExpanded,
                        onExpandedChange = { isExpanded = !isExpanded },
                        modifier = Modifier
                            .width(250.dp)
                            .padding(16.dp)
                    ) {
                        val shapeDynamic = if (isExpanded) {
                            RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                        } else {
                            RoundedCornerShape(34.dp)
                        }
                        OutlinedTextField(
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .shadow(elevation = 8.dp, shape = shapeDynamic)
                                .background(Color.White, shape = shapeDynamic),
                            shape = shapeDynamic,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                            ),
                            value = dipendenteSelezionato?.nome ?: "Seleziona un dipendente",
                            readOnly = true,
                            onValueChange = {},
                            textStyle = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.W400,
                                fontSize = 20.sp,
                            )
                            //trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        )
                        ExposedDropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false },
                            shape = RoundedCornerShape(
                                topStart = 0.dp,
                                topEnd = 0.dp,
                                bottomEnd = 34.dp,
                                bottomStart = 34.dp
                            ),
                            modifier = Modifier
                                .exposedDropdownSize(true)
                                .background(Color.White)
                                .border(
                                    width = 1.dp,
                                    color = Color.LightGray,
                                    shape = RoundedCornerShape(
                                        topStart = 0.dp,
                                        topEnd = 0.dp,
                                        bottomEnd = 34.dp,
                                        bottomStart = 34.dp
                                    )
                                )

                        ) {
                            Dipendenti_List.forEach { dipendenteLista ->
                                DropdownMenuItem(

                                    // 3. Mostra la proprietà 'nomeCompleto' nel testo
                                    text = { Text("${dipendenteLista.nome} ${dipendenteLista.cognome}") },

                                    onClick = {
                                        // 4. Passa l'INTERO OGGETTO 'dipendente'
                                        dipendente = dipendenteLista
                                        dipendenteSelezionato = dipendente
                                        isExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(400.dp)
                    .height(250.dp)

            ) {
                Box(
                    modifier = Modifier
                        .width(400.dp)
                        .height(200.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color("#FF12F0".toColorInt()),
                                    Color("#D06FCA".toColorInt()),
                                ),
                            ),
                            shape = RoundedCornerShape(24)
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
                        value = testoDescrizione, //Qui ci va sempre Task.stato,
                        onValueChange = { newText ->
                            testoDescrizione = newText
                            isDescrizioneValid = newText.length <= 500
                        },
                        isError = !isDescrizioneValid,
                        //supportingText = {if(!isDescrizioneValid) Text("Max 500 caratteri")},
                        placeholder = { Text("Max 500 caratteri") },
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
                    .width(400.dp)
                    .height(113.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color("#EB2D93".toColorInt()),
                                Color("#FF12F0".toColorInt()),
                            ),
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
                        "Progetto: ",
                        textAlign = TextAlign.Center,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(150.dp)
                            .padding(start = 10.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = isExpandedProgetto,
                        onExpandedChange = { isExpandedProgetto = !isExpandedProgetto },
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
                            value = nome_progetto ?: "Seleziona un progetto",
                            readOnly = true,
                            onValueChange = {},
                            textStyle = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.W400,
                                fontSize = 20.sp,
                            )
                            //trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        )
                        ExposedDropdownMenu(
                            expanded = isExpandedProgetto,
                            onDismissRequest = { isExpandedProgetto = false },
                        ) {
                            DropdownMenuItem(

                                // 3. Mostra la proprietà 'nomeProgetto' nel testo
                                text = { Text(nome_progetto!!) },

                                onClick = {
                                    // 4. Passa l'INTERO OGGETTO 'progetto'
                                    progetto = nome_progetto
                                    progettoSelezionato = progetto
                                    isExpandedProgetto = false
                                }
                            )

                        }
                    }
                }

            }
            Spacer(Modifier.padding(bottom = 20.dp))
            Button(
                onClick = {
                    statoCorrente = 2
                }, //Qui bisogna cambiare lo stato per passare allo stato 2
                enabled = isNomeTaskValid && isDescrizioneValid && dipendenteSelezionato != null,
                modifier = Modifier
                    .width(130.dp)
                    .height(48.dp),
            ) {
                Text(text = "Prosegui")
            }

        }
    } else if (statoCorrente == 2) {
        val interactionSource = remember { MutableInteractionSource() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Release) {
                    showDialog = true
                }
            }

        }
        val interactionSourceFine = remember { MutableInteractionSource() }
        LaunchedEffect(interactionSourceFine) {
            interactionSourceFine.interactions.collect { interaction ->
                if (interaction is PressInteraction.Release) {
                    showDialogFine = true
                }
            }

        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Spacer(Modifier.padding(top = 20.dp))
            Box(
                contentAlignment = Alignment.TopStart,
                modifier = Modifier
                    .width(370.dp)
                    .height(250.dp)

            ) {
                Box(
                    contentAlignment = Alignment.TopStart,
                    modifier = Modifier
                        .width(327.dp)
                        .height(155.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color("#A56FD9".toColorInt()),
                                    Color("#D06FCA".toColorInt()),
                                ),
                            ),
                            shape = RoundedCornerShape(20)
                        )
                ) {
                    Text(
                        "Data Inizio: ",
                        textAlign = TextAlign.Center,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(200.dp)
                            .padding(start = 10.dp, top = 10.dp)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(327.dp)
                        .height(180.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color("#DCC4F3".toColorInt()),
                                    Color("#DCC4F3".toColorInt())
                                ),
                            ),
                            shape = RoundedCornerShape(20)
                        )
                        .align(Alignment.BottomEnd)
                ) {
                    Text(
                        "Inserire data inizio: ",
                        textAlign = TextAlign.Start,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(300.dp)
                            .padding(start = 20.dp, top = 20.dp)
                            .align(Alignment.TopStart)
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .width(250.dp)
                            .height(60.dp)
                            .background(Color.Transparent),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color("#6750A4".toColorInt()),
                            unfocusedBorderColor = Color("#6750A4".toColorInt()),
                            disabledBorderColor = Color("#6750A4".toColorInt()),
                        ),
                        value = selectedDateString, //Qui ci va sempre Task.stato,
                        readOnly = true,
                        onValueChange = {},
                        isError = !isDataInizioValid,
                        //supportingText = {if(!isDataInizioValid) Text("Data inizio non valida")},
                        label = { Text("Date") },
                        placeholder = { Text("Inserisci una data...") },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.W400,
                            fontSize = 20.sp,
                        ),
                        interactionSource = interactionSource,
                        //trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                    )

                }
                if (showDialog) {
                    DatePickerDialog(
                        onDismissRequest = { showDialog = false },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val dataSelezionata = Date(datePickerState.selectedDateMillis!!)
                                    val oggi = Calendar.getInstance().apply { clearTime() }.time
                                    val selectedDateMillis = datePickerState.selectedDateMillis
                                    if (selectedDateMillis != null && !dataSelezionata.before(oggi) && (dataInizioProgetto == null || !dataSelezionata.before(dataInizioProgetto))) {
                                        isDataInizioValid = true
                                        selectedDate = Date(selectedDateMillis)
                                        selectedDateString = dateFormatter.format(selectedDate)
                                    }
                                    else{
                                        isDataInizioValid = false
                                        showDataAllertDialog = true
                                    }
                                    showDialog = false
                                }

                            ) {
                                Text("Ok")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showDialog = false }) {
                                Text("Annulla")
                            }

                        }

                    ) {
                        DatePicker(state = datePickerState)
                    }
                }


            }
            Spacer(Modifier.padding(top = 60.dp))
            Box(
                contentAlignment = Alignment.TopStart,
                modifier = Modifier
                    .width(370.dp)
                    .height(250.dp)

            ) {
                Box(
                    contentAlignment = Alignment.TopStart,
                    modifier = Modifier
                        .width(327.dp)
                        .height(155.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color("#A56FD9".toColorInt()),
                                    Color("#D06FCA".toColorInt()),
                                ),
                            ),
                            shape = RoundedCornerShape(20)
                        )
                ) {
                    Text(
                        "Data fine: ",
                        textAlign = TextAlign.Center,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(200.dp)
                            .padding(start = 5.dp, top = 10.dp)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(327.dp)
                        .height(180.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color("#DCC4F3".toColorInt()),
                                    Color("#DCC4F3".toColorInt())
                                ),
                            ),
                            shape = RoundedCornerShape(20)
                        )
                        .align(Alignment.BottomEnd)
                ) {
                    Text(
                        "Inserire data fine: ",
                        textAlign = TextAlign.Start,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(300.dp)
                            .padding(start = 20.dp, top = 20.dp)
                            .align(Alignment.TopStart)
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .width(250.dp)
                            .height(60.dp)
                            .background(Color.Transparent),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color("#6750A4".toColorInt()),
                            unfocusedBorderColor = Color("#6750A4".toColorInt()),
                            disabledBorderColor = Color("#6750A4".toColorInt()),
                        ),
                        value = selectedDateStringFine, //Qui ci va sempre Task.stato,
                        readOnly = true,
                        onValueChange = {},
                        isError = !isDataFineValid,
                        //supportingText = {if(!isDataFineValid) Text("Data di fine non valida")},
                        label = { Text("Date") },
                        placeholder = { Text("Inserisci una data...") },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.W400,
                            fontSize = 20.sp,
                        ),
                        interactionSource = interactionSourceFine,
                        //trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                    )

                }
                if (showDialogFine) {
                    DatePickerDialog(
                        onDismissRequest = { showDialogFine = false },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val dataInizioTask = selectedDateString.toDateOrNull()
                                    val dataFineSelezionata = Date(datePickerStateFine.selectedDateMillis!!)
                                    val selectedDateMillis = datePickerStateFine.selectedDateMillis
                                    if (selectedDateMillis != null &&dataInizioTask != null && !dataFineSelezionata.before(dataInizioTask) &&
                                        (dataFineProgetto == null || !dataFineSelezionata.after(dataFineProgetto))) {
                                        isDataFineValid = true
                                        selectedDateFine = Date(selectedDateMillis)
                                        selectedDateStringFine =
                                            dateFormatter.format(selectedDateFine)
                                    }
                                    else {
                                        isDataFineValid = false
                                        showDataAllertDialog = true
                                    }
                                    showDialogFine = false
                                }

                            ) {
                                Text("Ok")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showDialogFine = false }) {
                                Text("Annulla")
                            }

                        }

                    ) {
                        DatePicker(state = datePickerStateFine)
                    }
                }


            }
            Spacer(Modifier.padding(bottom = 20.dp))
            Button(
                onClick = {
                    statoCorrente = 3
                }, //Qui bisogna cambiare lo stato per passare allo stato 2
                enabled = isDataInizioValid && isDataFineValid && selectedDateString.isNotBlank() && selectedDateStringFine.isNotBlank(),
                modifier = Modifier
                    .width(130.dp)
                    .height(48.dp),
            ) {
                Text(text = "Prosegui")
            }

        }
    }
    else if(statoCorrente == 3){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.padding(top = 20.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(350.dp)
                .height(97.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color("#A56FD9".toColorInt()),
                            Color("#7B6FE9".toColorInt()),
                        ),
                    ),
                    shape = RoundedCornerShape(34)
                )
        ) {
            Text(
                "Dipendente: ${dipendenteSelezionato?.nome} ${dipendenteSelezionato?.cognome}", //Inserire il nome del dipendente
                textAlign = TextAlign.Center,
                fontFamily = computerSaysNo,
                fontWeight = FontWeight.W400,
                fontSize = 40.sp,
                modifier = Modifier
                    .width(300.dp)
                    .padding(bottom = 20.dp)
            )
        }
        Spacer(Modifier.padding(top = 20.dp))
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .width(380.dp)
                .height(340.dp)

        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .width(350.dp)
                    .height(150.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color("#FF12F0".toColorInt()),
                                Color("#D06FCA".toColorInt()),
                            ),
                        ),
                        shape = RoundedCornerShape(24)
                    )
            ) {
                Text(
                    "Descrizione: ", //Inserire il nome del dipendente
                    textAlign = TextAlign.Start,
                    fontFamily = computerSaysNo,
                    fontWeight = FontWeight.W400,
                    fontSize = 40.sp,
                    modifier = Modifier
                        .width(170.dp)
                        .padding(start = 10.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .width(380.dp)
                    .height(230.dp)
            ) {
                Spacer(Modifier.padding(start = 190.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .width(180.dp)
                        .height(200.dp)
                ) {
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
                        value = testoDescrizione, //Qui ci va sempre Task.stato,
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
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .width(350.dp)
                    .height(114.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color("#EB2D93".toColorInt()),
                                Color("#FF12F0".toColorInt()),
                            ),
                        ),
                        shape = RoundedCornerShape(34)
                    )
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Text(
                        "Progetto: ", //Inserire il nome del dipendente
                        textAlign = TextAlign.Start,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(130.dp)
                            .padding(start = 10.dp)
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .width(200.dp)
                            .height(60.dp)
                            .shadow(elevation = 20.dp, shape = RoundedCornerShape(12))
                            .background(Color.White, shape = RoundedCornerShape(34)),
                        shape = RoundedCornerShape(34),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                        ),
                        value = nome_progetto!!, //Qui ci va il nome del progetto,
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
        Spacer(Modifier.padding(top = 20.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(350.dp)
                .height(67.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color("#A56FD9".toColorInt()),
                            Color("#D06FCA".toColorInt()),
                        ),
                    ),
                    shape = RoundedCornerShape(54)
                )
        ) {
            Text(
                "Data inizio: ${selectedDateString}", //Inserire il nome del dipendente
                textAlign = TextAlign.Center,
                fontFamily = computerSaysNo,
                fontWeight = FontWeight.W400,
                fontSize = 40.sp,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 15.dp)
            )
        }
        Spacer(Modifier.padding(top = 20.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(350.dp)
                .height(67.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color("#A56FD9".toColorInt()),
                            Color("#D06FCA".toColorInt()),
                        ),
                    ),
                    shape = RoundedCornerShape(54)
                )
        ) {
            Text(
                "Data fine: ${selectedDateStringFine}", //Inserire il nome del dipendente
                textAlign = TextAlign.Center,
                fontFamily = computerSaysNo,
                fontWeight = FontWeight.W400,
                fontSize = 40.sp,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 15.dp)
            )
        }
        Spacer(Modifier.padding(bottom = 20.dp))
        Button(
            onClick = {
                if (validateAll()){
                    showConfirmationDialog = true
            }
            }, //Qui bisogna cambiare lo stato per passare allo stato 2
            enabled = nomeTask.isNotBlank() && isDataInizioValid && isDataFineValid && dipendenteSelezionato != null,
            modifier = Modifier
                .width(130.dp)
                .height(48.dp),
        ) {
            Text(text = "Conferma!")
        }

    }
}
    //qua si deve chiudere il secondo if-else
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun projectAdder(paddingValues: PaddingValues, onLoadingChange: (Boolean) -> Unit, token: String?, id_dipartimento: Int, onErrorConn: (Boolean) -> Unit, isCompletedDialog: (Boolean) -> Unit) {
    var statoCorrente by remember { mutableIntStateOf(1) }
    var budget by remember { mutableStateOf("") }
    var descrizione by remember { mutableStateOf("") }
    var nomeProgetto by remember { mutableStateOf("") }
    val datePickerState = rememberDatePickerState()
    var showDialog by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var selectedDateString by remember { mutableStateOf("") }
    var selectedDate: Date
    val datePickerStateFine = rememberDatePickerState()
    var showDialogFine by remember { mutableStateOf(false) }
    var selectedDateStringFine by remember { mutableStateOf("") }
    var selectedDateFine: Date
    val api = RetrofitInstance.api
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught $exception")
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isNomeProgettoValid by remember { mutableStateOf(true) }
    var isBudgetValid by remember { mutableStateOf(true) }
    var isDescrizioneValid by remember { mutableStateOf(true) }
    var isDataInizioValid by remember { mutableStateOf(true) }
    var isDataFineValid by remember { mutableStateOf(true) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    val budgetRegex = remember { Regex("^(?!0\\d)\\d+(\\.\\d{1,2})?$") }
    fun validateAll(): Boolean {
        // Riesegue tutti i controlli (utile se l'utente preme "Aggiungi" senza toccare un campo)
        isNomeProgettoValid = nomeProgetto.isNotBlank() && nomeProgetto.length <= 20
        isDescrizioneValid = descrizione.length <= 500
        isBudgetValid = budget.matches(budgetRegex) && (budget.toDoubleOrNull() ?: 0.0) > 0.0

        // La validazione delle date avviene già quando vengono selezionate, ma possiamo ricontrollare
        val dataInizio = selectedDateString.toDateOrNull()
        val dataFine = selectedDateStringFine.toDateOrNull()
        isDataInizioValid = dataInizio != null && !dataInizio.isBeforeToday()
        isDataFineValid = dataFine != null && dataInizio != null && !dataFine.before(dataInizio)

        return isNomeProgettoValid && isBudgetValid && isDescrizioneValid && isDataInizioValid && isDataFineValid
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
            text = { Text("Desideri davvero proseguire con l'aggiunta del seguente progetto?") },
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
                                    api.addProject(
                                        mapOf<String, Any>(
                                            "token" to token!!,
                                            "nome" to nomeProgetto,
                                            "descrizione" to descrizione,
                                            "budget" to budget,
                                            "data_inizio" to selectedDateString,
                                            "data_fine" to selectedDateStringFine,
                                            "id_dipartimento" to id_dipartimento,
                                        )
                                    )
                                }
                                val response = responseStatus.await()
                                withContext(Dispatchers.Main) {
                                    onLoadingChange(false)
                                }
                                if (response.isSuccessful) {
                                    withContext(Dispatchers.Main){
                                        isCompletedDialog(true)
                                    }
                                }
                            } catch (e: java.net.ConnectException) {
                                withContext(Dispatchers.Main){
                                    onErrorConn(true)
                                }
                            } catch (e: java.io.IOException) {
                                withContext(Dispatchers.Main){
                                    onErrorConn(true)
                                }
                            } catch (e: Exception) {
                                println("Errore sconosciuto $e")
                            } catch (e: java.net.SocketTimeoutException) {
                                withContext(Dispatchers.Main) {
                                    onErrorConn(true)
                                }
                            } finally{
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

    if (statoCorrente == 1) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.padding(top = 20.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(370.dp)
                    .height(103.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color("#96FF13".toColorInt()),
                                Color("#97C65C".toColorInt()),
                            ),
                        ),
                        shape = RoundedCornerShape(34)
                    )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Text(
                        "Budget: ", //Inserire il nome del dipendente
                        textAlign = TextAlign.Start,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(110.dp)
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .width(190.dp)
                            .height(60.dp)
                            .shadow(elevation = 20.dp, shape = RoundedCornerShape(34))
                            .background(Color.White, shape = RoundedCornerShape(34)),
                        shape = RoundedCornerShape(34),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                        ),
                        value = budget, //Qui ci va sempre Task.stato,
                        onValueChange = { newText ->
                            budget = newText
                            isBudgetValid = newText.matches(budgetRegex) && (newText.toDoubleOrNull() ?: 0.0) > 0.0
                        },
                        isError = !isBudgetValid,
                        //supportingText = {if(!isBudgetValid) Text("Formato non valido o importo nullo", color = MaterialTheme.colorScheme.error)},
                        placeholder = { Text("es: 100.50") },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.W400,
                            fontSize = 20.sp,
                        )
                        //trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                    )

                }

            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(370.dp)
                    .height(250.dp)

            ) {
                Box(
                    modifier = Modifier
                        .width(370.dp)
                        .height(200.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color("#FF12F0".toColorInt()),
                                    Color("#D06FCA".toColorInt()),
                                ),
                            ),
                            shape = RoundedCornerShape(24)
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
                        value = descrizione, //Qui ci va sempre Task.stato,
                        onValueChange = { newText ->
                            descrizione = newText
                            isDescrizioneValid = newText.length <= 500
                        },
                        isError = !isDescrizioneValid,
                        //supportingText = {if(!isDescrizioneValid) Text("Max 500 caratteri!", color = MaterialTheme.colorScheme.error)},
                        placeholder = { Text("Max 500 caratteri!") },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.W400,
                            fontSize = 20.sp,
                        )
                        //trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                    )
                }
            }
            Spacer(Modifier.padding(top = 20.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(370.dp)
                    .height(103.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color("#A56FD9".toColorInt()),
                                Color("#7B6FE9".toColorInt()),
                            ),
                        ),
                        shape = RoundedCornerShape(34)
                    )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Text(
                        "Nome: ", //Inserire il nome del dipendente
                        textAlign = TextAlign.Start,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(110.dp)
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .width(190.dp)
                            .height(58.dp)
                            .shadow(elevation = 20.dp, shape = RoundedCornerShape(34))
                            .background(Color.White, shape = RoundedCornerShape(34)),
                        shape = RoundedCornerShape(34),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                        ),
                        value = nomeProgetto, //Qui ci va sempre Task.stato,
                        onValueChange = { newText ->
                            nomeProgetto = newText
                            isNomeProgettoValid = newText.isNotBlank() && newText.length <= 20
                        },
                        isError = !isNomeProgettoValid,
                        //supportingText = {if(!isNomeProgettoValid) Text("Max 20 caratteri!", color = MaterialTheme.colorScheme.error)},
                        placeholder = { Text("Max 20 caratteri") },
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
            Button(
                onClick = {
                    statoCorrente = 2
                },
                enabled = isNomeProgettoValid && isDescrizioneValid && isBudgetValid, //Qui bisogna cambiare lo stato per passare allo stato 2
                modifier = Modifier
                    .width(130.dp)
                    .height(48.dp),
            ) {
                Text(text = "Prosegui")
            }

        }
    } else if (statoCorrente == 2) {
        val interactionSource = remember { MutableInteractionSource() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Release) {
                    showDialog = true
                }
            }

        }
        val interactionSourceFine = remember { MutableInteractionSource() }
        LaunchedEffect(interactionSourceFine) {
            interactionSourceFine.interactions.collect { interaction ->
                if (interaction is PressInteraction.Release) {
                    showDialogFine = true
                }
            }

        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Spacer(Modifier.padding(top = 20.dp))
            Box(
                contentAlignment = Alignment.TopStart,
                modifier = Modifier
                    .width(370.dp)
                    .height(250.dp)

            ) {
                Box(
                    contentAlignment = Alignment.TopStart,
                    modifier = Modifier
                        .width(327.dp)
                        .height(155.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color("#FF07F0".toColorInt()),
                                    Color("#D06FCA".toColorInt()),
                                ),
                            ),
                            shape = RoundedCornerShape(20)
                        )
                ) {
                    Text(
                        "Data Inizio: ",
                        textAlign = TextAlign.Center,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(200.dp)
                            .padding(start = 10.dp, top = 10.dp)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(327.dp)
                        .height(180.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color("#DCC4F3".toColorInt()),
                                    Color("#DCC4F3".toColorInt())
                                ),
                            ),
                            shape = RoundedCornerShape(20)
                        )
                        .align(Alignment.BottomEnd)
                ) {
                    Text(
                        "Inserire data inizio: ",
                        textAlign = TextAlign.Start,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(300.dp)
                            .padding(start = 20.dp, top = 20.dp)
                            .align(Alignment.TopStart)
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .width(250.dp)
                            .height(60.dp)
                            .background(Color.Transparent),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color("#6750A4".toColorInt()),
                            unfocusedBorderColor = Color("#6750A4".toColorInt()),
                            disabledBorderColor = Color("#6750A4".toColorInt()),
                        ),
                        value = selectedDateString, //Qui ci va sempre Task.stato,
                        readOnly = true,
                        onValueChange = {},
                        isError = !isDataInizioValid,
                        //supportingText = {if(!isDataInizioValid) Text("Selezionare una data di inizio valida!", color = MaterialTheme.colorScheme.error)},
                        label = { Text("Date") },
                        placeholder = { Text("Inserisci una data...") },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.W400,
                            fontSize = 20.sp,
                        ),
                        interactionSource = interactionSource,
                        //trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                    )

                }
                if (showDialog) {
                    DatePickerDialog(
                        onDismissRequest = { showDialog = false },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val dataSelezionata = Date(datePickerState.selectedDateMillis!!)
                                    val selectedDateMillis = datePickerState.selectedDateMillis
                                    if(!dataSelezionata.isBeforeToday() && selectedDateMillis != null){
                                        isDataInizioValid = true
                                        selectedDate = Date(selectedDateMillis)
                                        selectedDateString = dateFormatter.format(selectedDate)
                                    }else {
                                        isDataInizioValid = false
                                    }
                                    showDialog = false
                                }

                            ) {
                                Text("Ok")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showDialog = false }) {
                                Text("Annulla")
                            }

                        }

                    ) {
                        DatePicker(state = datePickerState)
                    }
                }


            }
            Spacer(Modifier.padding(top = 60.dp))
            Box(
                contentAlignment = Alignment.TopStart,
                modifier = Modifier
                    .width(370.dp)
                    .height(250.dp)

            ) {
                Box(
                    contentAlignment = Alignment.TopStart,
                    modifier = Modifier
                        .width(327.dp)
                        .height(155.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color("#FF07F0".toColorInt()),
                                    Color("#D06FCA".toColorInt()),
                                ),
                            ),
                            shape = RoundedCornerShape(20)
                        )
                ) {
                    Text(
                        "Data fine: ",
                        textAlign = TextAlign.Center,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(200.dp)
                            .padding(start = 5.dp, top = 10.dp)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(327.dp)
                        .height(180.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color("#DCC4F3".toColorInt()),
                                    Color("#DCC4F3".toColorInt())
                                ),
                            ),
                            shape = RoundedCornerShape(20)
                        )
                        .align(Alignment.BottomEnd)
                ) {
                    Text(
                        "Inserire data fine: ",
                        textAlign = TextAlign.Start,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(300.dp)
                            .padding(start = 20.dp, top = 20.dp)
                            .align(Alignment.TopStart)
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .width(250.dp)
                            .height(60.dp)
                            .background(Color.Transparent),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color("#6750A4".toColorInt()),
                            unfocusedBorderColor = Color("#6750A4".toColorInt()),
                            disabledBorderColor = Color("#6750A4".toColorInt()),
                        ),
                        value = selectedDateStringFine, //Qui ci va sempre Task.stato,
                        readOnly = true,
                        onValueChange = {},
                        isError = !isDataFineValid,
                        //supportingText = {if(!isDataFineValid) Text("Selezionare una data di fine valida!", color = MaterialTheme.colorScheme.error)},
                        label = { Text("Date") },
                        placeholder = { Text("Inserisci una data...") },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.W400,
                            fontSize = 20.sp,
                        ),
                        interactionSource = interactionSourceFine,
                        //trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                    )

                }
                if (showDialogFine) {
                    DatePickerDialog(
                        onDismissRequest = { showDialogFine = false },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val selectedDateMillis = datePickerStateFine.selectedDateMillis
                                    val dataInizio = selectedDateString.toDateOrNull()
                                    val dataFineSelezionata = Date(datePickerStateFine.selectedDateMillis!!)
                                    if(dataInizio != null && !dataFineSelezionata.before(dataInizio) && selectedDateMillis != null) {
                                        isDataFineValid = true
                                        selectedDateFine = Date(selectedDateMillis)
                                        selectedDateStringFine =
                                            dateFormatter.format(selectedDateFine)
                                    }
                                    else {
                                        isDataFineValid = false
                                    }
                                    showDialogFine = false
                                }

                            ) {
                                Text("Ok")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showDialogFine = false }) {
                                Text("Annulla")
                            }

                        }

                    ) {
                        DatePicker(state = datePickerStateFine)
                    }
                }


            }
            Spacer(Modifier.padding(bottom = 20.dp))
            Button(
                onClick = {
                    statoCorrente = 3
                },
                enabled = isDataInizioValid && isDataFineValid && !selectedDateString.isNullOrBlank()  && !selectedDateStringFine.isNullOrBlank(),//Qui bisogna cambiare lo stato per passare allo stato 2
                modifier = Modifier
                    .width(130.dp)
                    .height(48.dp),
            ) {
                Text(text = "Prosegui")
            }

        }
    } else if (statoCorrente == 3){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.padding(top = 20.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(350.dp)
                    .height(97.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color("#96FF13".toColorInt()),
                                Color("#97C65C".toColorInt()),
                            ),
                        ),
                        shape = RoundedCornerShape(34)
                    )
            ) {
                Text(
                    "Budget: ${budget}", //Inserire il budget del progetto
                    textAlign = TextAlign.Center,
                    fontFamily = computerSaysNo,
                    fontWeight = FontWeight.W400,
                    fontSize = 40.sp,
                    modifier = Modifier
                        .width(300.dp)
                        .padding(bottom = 20.dp)
                )
            }
            Spacer(Modifier.padding(top = 20.dp))
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .width(380.dp)
                    .height(340.dp)

            ) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .width(350.dp)
                        .height(150.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color("#FF12F0".toColorInt()),
                                    Color("#D06FCA".toColorInt()),
                                ),
                            ),
                            shape = RoundedCornerShape(24)
                        )
                ) {
                    Text(
                        "Descrizione: ", //Inserire il nome del dipendente
                        textAlign = TextAlign.Start,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(170.dp)
                            .padding(start = 10.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .width(380.dp)
                        .height(230.dp)
                ) {
                    Spacer(Modifier.padding(start = 190.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .width(180.dp)
                            .height(200.dp)
                    ) {
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
                            value = "${descrizione}", //Qui ci va sempre Task.stato,
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
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .width(350.dp)
                        .height(114.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color("#A56FD9".toColorInt()),
                                    Color("#7B6FE9".toColorInt()),
                                ),
                            ),
                            shape = RoundedCornerShape(34)
                        )
                        .align(Alignment.BottomCenter)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Text(
                            "Nome: ${nomeProgetto}", //Inserire il nome del progetto
                            textAlign = TextAlign.Start,
                            fontFamily = computerSaysNo,
                            fontWeight = FontWeight.W400,
                            fontSize = 40.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp)
                        )
                    }

                }

            }
            Spacer(Modifier.padding(top = 20.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(350.dp)
                    .height(67.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color("#FF07F0".toColorInt()),
                                Color("#D06FCA".toColorInt()),
                            ),
                        ),
                        shape = RoundedCornerShape(54)
                    )
            ) {
                Text(
                    "Data inizio: ${selectedDateString}", //Inserire il nome del dipendente
                    textAlign = TextAlign.Center,
                    fontFamily = computerSaysNo,
                    fontWeight = FontWeight.W400,
                    fontSize = 40.sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 15.dp)
                )
            }
            Spacer(Modifier.padding(top = 20.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(350.dp)
                    .height(67.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color("#FF07F0".toColorInt()),
                                Color("#D06FCA".toColorInt()),
                            ),
                        ),
                        shape = RoundedCornerShape(54)
                    )
            ) {
                Text(
                    "Data fine: ${selectedDateStringFine}", //Inserire il nome del dipendente
                    textAlign = TextAlign.Center,
                    fontFamily = computerSaysNo,
                    fontWeight = FontWeight.W400,
                    fontSize = 40.sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 15.dp)
                )
            }
            Spacer(Modifier.padding(bottom = 20.dp))
            Button(
                onClick = {
                    if(validateAll()) {
                        showConfirmationDialog = true
                    }

                },
                enabled = nomeProgetto.isNotBlank() && budget.isNotBlank() && isDataInizioValid && isDataFineValid,//Qui bisogna cambiare lo stato per passare allo stato 2
                modifier = Modifier
                    .width(130.dp)
                    .height(48.dp),
            ) {
                Text(text = "Conferma!")
            }

        }
}

}
// Funzione per convertire una stringa "dd/MM/yyyy" in un oggetto Date
fun String.toDateOrNull(): Date? {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return try {
        formatter.parse(this)
    } catch (e: Exception) {
        null
    }
}

// Funzione per formattare una data
fun Date.toFormattedString(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(this)
}

// Funzione per verificare se una data è precedente a oggi
fun Date.isBeforeToday(): Boolean {
    val today = Calendar.getInstance().apply { clearTime() }.time
    return this.before(today)
}

// Estensione per pulire l'orario da un Calendar, utile per confronti di sole date
fun Calendar.clearTime() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}


