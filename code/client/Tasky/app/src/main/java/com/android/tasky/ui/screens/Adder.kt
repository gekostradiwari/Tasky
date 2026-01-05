package com.android.tasky.ui.screens

import android.app.Activity
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
        setContent {
            val context = LocalContext.current
            var isLoading by remember { mutableStateOf(false) }
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
                            painter = painterResource(id = R.drawable.tasky_logo),
                            contentDescription = "Logo Tasky",
                            modifier = Modifier
                                .size(48.dp)
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
                content = { paddingValues ->
                    if(type.equals("taskAdder")){
                    TaskAdder(paddingValues, {isLoading = it}, token, email, id_dipartimento, nome_progetto, id_progetto)
                    }
                    else if(type.equals("projectAdder")){
                        projectAdder(paddingValues, {isLoading = it}, token, id_dipartimento)
                    }
                    else{
                        Text(text = "Errore")
                    }
                    if (isLoading) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Black.copy(alpha = 0.3f), // Grigio trasparente
                            onClick = { /* Non fare nulla, serve a intercettare i click */ }
                        ) {
                            // Vuoto, serve solo per il colore
                        }

                        // Il cerchio sopra lo sfondo scuro
                        CircularProgressIndicator(
                            color = Color.Magenta, // Colore del cerchio
                            strokeWidth = 4.dp
                        )
                    }
                }
            )
        }

    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAdder(paddingValues: PaddingValues, onLoadingChange: (Boolean) -> Unit, token: String?, email: String?, id_dipartimento: Int, nome_progetto: String?, id_progetto: Int) {
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
    val api = RetrofitInstance.api
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught $exception")
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO + handler) {
            try {
                onLoadingChange(true)
                val responseStatus = async {
                    api.dipendentiByDepartment(
                        mapOf<String, Any>(
                            "token" to token!!,
                            "id_dipartimento" to id_dipartimento,
                        )
                    )
                }
                val response = responseStatus.await()
                onLoadingChange(false)
                if (response.isSuccessful) {
                    Dipendenti_List = response.body()!!.items
                }
            } catch (e: java.net.ConnectException) {
                println("Impossibile contattare il server")
            } catch (e: java.io.IOException) {
                println("Problema di connessione")
            } catch (e: Exception) {
                println("Errore sconosciuto $e")
            }
        }
    }


    if (statoCorrente == 1) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(380.dp)
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
                            onValueChange = {nome -> nomeTask = nome},
                            placeholder = { Text("Inserisci un nome...") },
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
                    .width(390.dp)
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
                        textAlign = TextAlign.Center,
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
                            shape = RoundedCornerShape(34)
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
                        },
                        placeholder = { Text("Inserisci una descrizione...") },
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
                    .width(380.dp)
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
            Button(
                onClick = {
                    statoCorrente = 2
                }, //Qui bisogna cambiare lo stato per passare allo stato 2
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
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
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
                                    showDialog = false
                                    val selectedDateMillis = datePickerState.selectedDateMillis
                                    if (selectedDateMillis != null) {
                                        selectedDate = Date(selectedDateMillis)
                                        selectedDateString = dateFormatter.format(selectedDate)
                                    }
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
                                    showDialogFine = false
                                    val selectedDateMillis = datePickerStateFine.selectedDateMillis
                                    if (selectedDateMillis != null) {
                                        selectedDateFine = Date(selectedDateMillis)
                                        selectedDateStringFine =
                                            dateFormatter.format(selectedDateFine)
                                    }
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
            Button(
                onClick = {
                    statoCorrente = 3
                }, //Qui bisogna cambiare lo stato per passare allo stato 2
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
        modifier = Modifier.fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
        Button(
            onClick = {
                scope.launch(Dispatchers.IO + handler) {
                    try {
                        onLoadingChange(true)
                        val responseStatus = async {
                            api.addTask(
                                mapOf<String, Any>(
                                    "token" to token!!,
                                    "id_dipartimento" to id_dipartimento,
                                    "id_progetto" to id_progetto,
                                    "id_dipartimento" to id_dipartimento,
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
                        onLoadingChange(false)
                        if (response.isSuccessful) {
                            println("Task Aggiunta con successo!!")
                        }
                    } catch (e: java.net.ConnectException) {
                        println("Impossibile contattare il server")
                    } catch (e: java.io.IOException) {
                        println("Problema di connessione")
                    } catch (e: Exception) {
                        println("Errore sconosciuto $e")
                    }
                }
            }, //Qui bisogna cambiare lo stato per passare allo stato 2
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
fun projectAdder(paddingValues: PaddingValues, onLoadingChange: (Boolean) -> Unit, token: String?, id_dipartimento: Int) {
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

    if (statoCorrente == 1) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                            .height(58.dp)
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
                        },
                        placeholder = { Text("Inserisci budget, ex: 100.50") },
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
                        },
                        placeholder = { Text("Inserisci una descrizione...") },
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
                        },
                        placeholder = { Text("Inserisci il nome del progetto") },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.W400,
                            fontSize = 20.sp,
                        )
                        //trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                    )

                }

            }
            Button(
                onClick = {
                    statoCorrente = 2
                }, //Qui bisogna cambiare lo stato per passare allo stato 2
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
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
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
                                    showDialog = false
                                    val selectedDateMillis = datePickerState.selectedDateMillis
                                    if (selectedDateMillis != null) {
                                        selectedDate = Date(selectedDateMillis)
                                        selectedDateString = dateFormatter.format(selectedDate)
                                    }
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
                                    showDialogFine = false
                                    val selectedDateMillis = datePickerStateFine.selectedDateMillis
                                    if (selectedDateMillis != null) {
                                        selectedDateFine = Date(selectedDateMillis)
                                        selectedDateStringFine =
                                            dateFormatter.format(selectedDateFine)
                                    }
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
            Button(
                onClick = {
                    statoCorrente = 3
                }, //Qui bisogna cambiare lo stato per passare allo stato 2
                modifier = Modifier
                    .width(130.dp)
                    .height(48.dp),
            ) {
                Text(text = "Prosegui")
            }

        }
    } else if (statoCorrente == 3){
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                    "Budget: ", //Inserire il budget del progetto
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
                            value = "Completato", //Qui ci va sempre Task.stato,
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
                            "Nome: Nome Progetto", //Inserire il nome del progetto
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
                    "Data inizio: ", //Inserire il nome del dipendente
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
                    "Data fine: ", //Inserire il nome del dipendente
                    textAlign = TextAlign.Center,
                    fontFamily = computerSaysNo,
                    fontWeight = FontWeight.W400,
                    fontSize = 40.sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 15.dp)
                )
            }
            Button(
                onClick = {
                    scope.launch(Dispatchers.IO + handler) {
                        try {
                            onLoadingChange(true)
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
                            onLoadingChange(false)
                            if (response.isSuccessful) {
                                println("Progetto aggiunto con successo!!")
                            }
                        } catch (e: java.net.ConnectException) {
                            println("Impossibile contattare il server")
                        } catch (e: java.io.IOException) {
                            println("Problema di connessione")
                        } catch (e: Exception) {
                            println("Errore sconosciuto $e")
                        }
                    }

                }, //Qui bisogna cambiare lo stato per passare allo stato 2
                modifier = Modifier
                    .width(130.dp)
                    .height(48.dp),
            ) {
                Text(text = "Conferma!")
            }

        }
}

}

