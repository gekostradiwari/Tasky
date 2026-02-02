package com.android.tasky.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.room.util.TableInfo
import com.android.tasky.R
import com.android.tasky.dto.Dipendente
import com.android.tasky.dto.Progetto
import com.android.tasky.dto.Task
import com.android.tasky.ui.theme.computerSaysNo
import com.android.tasky.utility.RetrofitInstance
import com.android.tasky.utility.RetrofitInterface
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListViewer : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //Array di tags che servono per definire che oggetto elencare
        val intent = this.intent
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
        val adding = intent.getBooleanExtra("adding", false)
        val is_suspend = intent.getBooleanExtra("suspend", false)

        setContent {
            var Tasks_Completate by remember { mutableStateOf<List<Task>>(emptyList()) }
            var Tasks_In_Corso by remember { mutableStateOf<List<Task>>(emptyList()) }
            var Tasks_Sospese by remember { mutableStateOf<List<Task>>(emptyList()) }
            var Progetti_List by remember { mutableStateOf<List<Progetto>>(emptyList()) }
            var Dipendenti_List by remember { mutableStateOf<List<Dipendente>>(emptyList()) }
            var Task_list_By_project by remember { mutableStateOf<List<Task>>(emptyList()) }
            val context = LocalContext.current
            var isLoading by remember { mutableStateOf(false) }
            var showConnErrorDialog by remember { mutableStateOf(false) }
            var isCompletedDialog by remember {mutableStateOf(false)}
            var isCompleted by remember {mutableStateOf(false)}
            val configuration = LocalConfiguration.current
            val isLandScape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            var selectedTask by remember {mutableStateOf<Task?>(null)}
            var selectedProgetto by remember {mutableStateOf<Progetto?>(null)}
            var need_reload by remember {mutableStateOf(false)}

            val isShowingList = type == "task_in_corso" || type == "progetti" || type == "task_completati" || type == "task_sospesi" || type == "ProgettiByMGR" || type == "task_by_project"

            DisposableEffect(isShowingList) {
                val activity = context as? Activity
                if (isShowingList) {
                    // Se è una lista, lascia ruotare liberamente
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                } else {
                    // Altrimenti, blocca in verticale
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }

                onDispose { }
            }

            suspend fun loadData(){
                if(isLoading) return
                lifecycleScope.launch(Dispatchers.IO + handler) {
                    println("Sono nella loadData")
                    try {
                        withContext(Dispatchers.Main) {
                            isLoading = true
                        }
                        if(type.equals("task_in_corso")){
                            println(email)
                            println(token)
                            val responseStatus = async{
                                api.TaskLister(mapOf("token" to token, "email_dipendente" to email))
                            }
                            val response = responseStatus.await()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                            if(response.isSuccessful){
                                Tasks_In_Corso = emptyList()
                                Tasks_In_Corso = response.body()!!.data.items
                            }
                        }
                        else if(type.equals("task_completati")){
                            val responseStatus = async{
                                api.TaskListerCompleted(mapOf("token" to token, "email_dipendente" to email))
                            }
                            val response = responseStatus.await()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                            if(response.isSuccessful){
                                Tasks_Completate = emptyList()
                                Tasks_Completate = response.body()!!.data.items
                            }
                        }
                        else if(type.equals("task_sospesi")){ //Qui non bisogna chiudere con else ma continuare con gli altri casi se ci sono progetti dipendenti o altro
                            val responseStatus = async{
                                api.TaskListerSuspended(mapOf("token" to token, "email_dipendente" to email))
                            }
                            val response = responseStatus.await()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                            if(response.isSuccessful){
                                Tasks_Sospese = emptyList()
                                Tasks_Sospese = response.body()!!.data.items
                            }

                        }
                        else if(type.equals("dipendenti")){
                            val responseStatus = async{
                                api.dipendentiByDepartment(mapOf<String,Any>("token" to token!!, "id_dipartimento" to dipartimento))
                            }
                            val response = responseStatus.await()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                            if(response.isSuccessful){
                                Dipendenti_List = response.body()!!.items
                            }
                        }
                        else if(type.equals("progetti")){
                            val responseStatus = async{
                                api.getProjectByDepartment(mapOf<String,Any>("token" to token!!, "id_dipartimento" to dipartimento))
                            }
                            val response = responseStatus.await()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                            if(response.isSuccessful){
                                Progetti_List = response.body()!!.data.items
                            }
                        }
                        else if(type.equals("task_by_project")){
                            val responseStatus = async{
                                api.getTaskByProjectMGR(mapOf<String,Any>("token" to token!!, "id_progetto" to progetto, "id_dipartimento" to dipartimento))
                            }
                            val response = responseStatus.await()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                            if(response.isSuccessful){
                                Task_list_By_project = emptyList()
                                Task_list_By_project = response.body()!!.data.items
                            }

                        }
                        else if(type.equals("ProgettiByMGR")){
                            val responseStatus = async{
                                api.getProjectsByMGR(mapOf("email_manager" to email!!))
                            }
                            val response = responseStatus.await()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                            if(response.isSuccessful){
                                Progetti_List = response.body()!!.data.items
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
                        println("Errore sconosciuto $e")
                    } catch (e: java.net.SocketTimeoutException) {
                        withContext(Dispatchers.Main) {
                            showConnErrorDialog = true
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                        }
                    }
                }
            }
            val lifecycleOwner = LocalLifecycleOwner.current
            val scope = rememberCoroutineScope()
            DisposableEffect(lifecycleOwner){
                val observer = LifecycleEventObserver { _, event ->
                    // Se l'evento è ON_RESUME, ricarica i dati
                    if (event == Lifecycle.Event.ON_RESUME) {
                        scope.launch {
                            loadData()
                        }
                    }
                }

                // Aggiungi l'osservatore al ciclo di vita
                lifecycleOwner.lifecycle.addObserver(observer)

                // Rimuovi l'osservatore quando il Composable viene distrutto per evitare memory leak
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }

            }
            /*LaunchedEffect(Unit){
                lifecycleScope.launch(Dispatchers.IO + handler) {
                    try {
                        withContext(Dispatchers.Main) {
                            isLoading = true
                        }
                        if(type.equals("task_in_corso")){
                            println(email)
                            println(token)
                            val responseStatus = async{
                                api.TaskLister(mapOf("token" to token, "email_dipendente" to email))
                            }
                            val response = responseStatus.await()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                            if(response.isSuccessful){
                                Tasks_In_Corso = response.body()!!.data.items
                            }
                        }
                        else if(type.equals("task_completati")){
                            val responseStatus = async{
                                api.TaskListerCompleted(mapOf("token" to token, "email_dipendente" to email))
                            }
                            val response = responseStatus.await()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                            if(response.isSuccessful){
                                Tasks_Completate = response.body()!!.data.items
                            }
                        }
                        else if(type.equals("task_sospesi")){ //Qui non bisogna chiudere con else ma continuare con gli altri casi se ci sono progetti dipendenti o altro
                            val responseStatus = async{
                                api.TaskListerSuspended(mapOf("token" to token, "email_dipendente" to email))
                            }
                            val response = responseStatus.await()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                            if(response.isSuccessful){
                                Tasks_Sospese = response.body()!!.data.items
                            }

                        }
                        else if(type.equals("dipendenti")){
                            val responseStatus = async{
                                api.dipendentiByDepartment(mapOf<String,Any>("token" to token!!, "id_dipartimento" to dipartimento))
                            }
                            val response = responseStatus.await()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                            if(response.isSuccessful){
                                Dipendenti_List = response.body()!!.items
                            }
                        }
                        else if(type.equals("progetti")){
                            val responseStatus = async{
                                api.getProjectByDepartment(mapOf<String,Any>("token" to token!!, "id_dipartimento" to dipartimento))
                            }
                            val response = responseStatus.await()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                            if(response.isSuccessful){
                                Progetti_List = response.body()!!.data.items
                            }
                        }
                        else if(type.equals("task_by_project")){
                            val responseStatus = async{
                                api.getTaskByProjectMGR(mapOf<String,Any>("token" to token!!, "id_progetto" to progetto, "id_dipartimento" to dipartimento))
                            }
                            val response = responseStatus.await()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                            if(response.isSuccessful){
                                Task_list_By_project = response.body()!!.data.items
                            }

                        }
                        else if(type.equals("ProgettiByMGR")){
                            val responseStatus = async{
                                api.getProjectsByMGR(mapOf("email_manager" to email!!))
                            }
                            val response = responseStatus.await()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                            if(response.isSuccessful){
                                Progetti_List = response.body()!!.data.items
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
                        println("Errore sconosciuto $e")
                    }finally {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                        }
                    }
                }

            }*/
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
                            .height(
                                if(isLandScape){
                                    36.dp
                                }else {
                                    70.dp
                                }
                            ),
                        //horizontalArrangement = Arrangement.spacedBy(125.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { (context as? Activity)?.finish()}
                        ) {
                            Icon(Icons.Default.ArrowBack, "TurnBack")
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.taskyfinalnobackground),
                                contentDescription = "Logo Tasky",
                                modifier = Modifier.size(if (isLandScape) 48.dp else 78.dp)
                            )
                        }

                        // 3. Uno Spacer vuoto a destra con la stessa larghezza del pulsante
                        // per bilanciare la riga e mantenere il Box perfettamente al centro
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                },
                modifier = Modifier.fillMaxSize(),
                content = { paddingValues ->
                    if(isLandScape) {
                        val context = LocalContext.current
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ){
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                if (type.equals("task_in_corso")) {
                                    items(Tasks_In_Corso.sortedBy { it.nome_progetto }) { elemento ->
                                        TaskInCorso(
                                            elemento,
                                            tipo,
                                            token,
                                            { },
                                            is_suspend,
                                            { isLoading = it },
                                            { showConnErrorDialog = it },
                                            { isCompletedDialog = it },
                                            dipartimento,
                                            {selectedTask = it}
                                        )
                                    }

                                } else if (type.equals("task_completati")) {
                                    items(Tasks_Completate.sortedBy { it.nome_progetto }) { elemento ->
                                        TaskCompletata(
                                            elemento,
                                            tipo,
                                            token,
                                            { },
                                            { isLoading = it },
                                            { showConnErrorDialog = it },
                                            { isCompletedDialog = it },
                                            dipartimento,
                                            {selectedTask = it}
                                        )
                                    }

                                } else if (type.equals("task_sospesi")) { //Qui non bisogna chiudere con else ma continuare con gli altri casi se ci sono progetti dipendenti o altro
                                    items(Tasks_Sospese.sortedBy { it.nome_progetto }) { elemento ->
                                        TaskSospesa(
                                            elemento,
                                            tipo,
                                            token,
                                            { },
                                            { isLoading = it },
                                            { showConnErrorDialog = it },
                                            { isCompletedDialog = it },
                                            dipartimento,
                                            {selectedTask = it}
                                        )
                                    }
                                } else if (type.equals("dipendenti")) {
                                    items(Dipendenti_List.sortedBy { it.cognome }) { elemento ->
                                        DipendenteElement(elemento)
                                    }
                                } else if (type.equals("progetti")) {
                                    items(Progetti_List.sortedBy { it.nome }) { elemento ->
                                        ProgettoElement(
                                            elemento,
                                            email,
                                            type,
                                            token,
                                            tipo,
                                            dipartimento,
                                            {
                                                Progetti_List = Progetti_List - it
                                            },
                                            adding,
                                            is_suspend,
                                            { isLoading = it },
                                            { showConnErrorDialog = it },
                                            { isCompletedDialog = it },
                                            {selectedProgetto = it}
                                            )
                                    }
                                } else if (type.equals("ProgettiByMGR")) {
                                    items(Progetti_List.sortedBy { it.nome }) { elemento ->
                                        ProgettoElement(
                                            elemento,
                                            email,
                                            type,
                                            token,
                                            tipo,
                                            dipartimento,
                                            {
                                                Progetti_List = Progetti_List - it
                                            },
                                            adding,
                                            is_suspend,
                                            { isLoading = it },
                                            { showConnErrorDialog = it },
                                            { isCompletedDialog = it },
                                            {selectedProgetto = it}
                                            )
                                    }
                                } else if (type.equals("task_by_project") && is_suspend) {
                                    items(Task_list_By_project.sortedBy { it.nome }) { elemento ->
                                        if (elemento.stato.equals("InProgress")) {
                                            TaskInCorso(
                                                elemento,
                                                tipo,
                                                token,
                                                {
                                                    Task_list_By_project = Task_list_By_project - it
                                                },
                                                is_suspend,
                                                { isLoading = it },
                                                { showConnErrorDialog = it },
                                                { isCompletedDialog = it },
                                                dipartimento,
                                                {selectedTask = it}
                                            )
                                        }
                                    }
                                } else if (type.equals("task_by_project")) {
                                    items(Task_list_By_project.sortedBy {
                                        when (it.stato) {
                                            "InProgress" -> 1
                                            "Sospeso" -> 2
                                            "Completato" -> 3
                                            else -> 4
                                        }
                                    }) { elemento ->
                                        if (elemento.stato.equals("InProgress")) {
                                            TaskInCorso(
                                                elemento,
                                                tipo,
                                                token,
                                                {
                                                    Task_list_By_project = Task_list_By_project - it
                                                },
                                                is_suspend,
                                                { isLoading = it },
                                                { showConnErrorDialog = it },
                                                { isCompletedDialog = it },
                                                dipartimento,
                                                {selectedTask = it}
                                            )
                                        } else if (elemento.stato.equals("Completato")) {
                                            TaskCompletata(
                                                elemento,
                                                tipo,
                                                token,
                                                {
                                                    Task_list_By_project = Task_list_By_project - it
                                                },
                                                { isLoading = it },
                                                { showConnErrorDialog = it },
                                                { isCompletedDialog = it },
                                                dipartimento,
                                                {selectedTask = it}
                                            )
                                        } else {
                                            TaskSospesa(
                                                elemento,
                                                tipo,
                                                token,
                                                {
                                                    Task_list_By_project = Task_list_By_project - it
                                                },
                                                { isLoading = it },
                                                { showConnErrorDialog = it },
                                                { isCompletedDialog = it },
                                                dipartimento,
                                                {selectedTask = it}
                                            )
                                        }
                                    }
                                }
                            }
                            Column(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),

                            ){

                                if(selectedTask != null){
                                    infoTask(
                                        taskObj = selectedTask!!,
                                        type = type ?: "", // Il tipo di lista corrente
                                        tipo = tipo ?: "", // "dipendente" o "manager"
                                        paddingValues = PaddingValues(0.dp),
                                        onLoadingChange = { isLoading = it },
                                        token = token ?: "",
                                        onErrorConn = { showConnErrorDialog = it },
                                        isCompletedDialog = { },
                                        isConfirmedDialog = { /* gestisci se necessario */ },
                                        isConfirmed = false,
                                        isCompleted = false,
                                        isLandscape = {
                                            scope.launch {
                                                loadData()
                                            }
                                        }
                                    )

                                }
                                else if(selectedProgetto != null){
                                    infoProgetto(
                                        progetto = selectedProgetto!!,
                                        paddingValues = PaddingValues(0.dp),
                                        onErrorConn = { showConnErrorDialog = it },
                                    )

                                }
                                else{
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                                        Text("Seleziona un task o un progetto per vederne i dettagli", fontFamily = computerSaysNo)
                                    }
                                }

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
                                    icon = {
                                        Image(
                                            painter = painterResource(id = R.drawable.police_car_light_svgrepo_com),
                                            contentDescription = "Warning",
                                            modifier = Modifier.size(48.dp),
                                        )
                                    },
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
                                    icon = {
                                        Image(
                                            painter = painterResource(id = R.drawable.check_mark_button_svgrepo_com),
                                            contentDescription = "Warning",
                                            modifier = Modifier.size(48.dp),
                                        )
                                    },
                                    title = { Text("Success!") },
                                    text = { Text("Operazione avvenuta con successo!") },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                // Chiudi il dialog e permetti all'utente di riprovare
                                                isCompleted = true
                                                isCompletedDialog = false
                                                if (tipo.equals("dipendente")) {
                                                    val intent =
                                                        Intent(context, HomeDipendenteActivity::class.java)
                                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                                    startActivity(intent)
                                                    finish()
                                                } else {
                                                    val intent =
                                                        Intent(context, HomeManagerActivity::class.java)
                                                    intent.flags =
                                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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
                    }
                    else{
                    LazyColumn(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        if (type.equals("task_in_corso")) {
                            items(Tasks_In_Corso.sortedBy { it.nome_progetto }) { elemento ->
                                TaskInCorso(
                                    elemento,
                                    tipo,
                                    token,
                                    { },
                                    is_suspend,
                                    { isLoading = it },
                                    { showConnErrorDialog = it },
                                    { isCompletedDialog = it },
                                    dipartimento,
                                    {selectedTask = it}
                                )
                            }

                        } else if (type.equals("task_completati")) {
                            items(Tasks_Completate.sortedBy { it.nome_progetto }) { elemento ->
                                TaskCompletata(
                                    elemento,
                                    tipo,
                                    token,
                                    { },
                                    { isLoading = it },
                                    { showConnErrorDialog = it },
                                    { isCompletedDialog = it },
                                    dipartimento,
                                    {selectedTask = it}
                                )
                            }

                        } else if (type.equals("task_sospesi")) { //Qui non bisogna chiudere con else ma continuare con gli altri casi se ci sono progetti dipendenti o altro
                            items(Tasks_Sospese.sortedBy { it.nome_progetto }) { elemento ->
                                TaskSospesa(
                                    elemento,
                                    tipo,
                                    token,
                                    { },
                                    { isLoading = it },
                                    { showConnErrorDialog = it },
                                    { isCompletedDialog = it },
                                    dipartimento,
                                    {selectedTask = it}
                                )
                            }
                        } else if (type.equals("dipendenti")) {
                            items(Dipendenti_List.sortedBy { it.cognome }) { elemento ->
                                DipendenteElement(elemento)
                            }
                        } else if (type.equals("progetti")) {
                            items(Progetti_List.sortedBy { it.nome }) { elemento ->
                                ProgettoElement(
                                    elemento,
                                    email,
                                    type,
                                    token,
                                    tipo,
                                    dipartimento,
                                    {
                                        Progetti_List = Progetti_List - it
                                    },
                                    adding,
                                    is_suspend,
                                    { isLoading = it },
                                    { showConnErrorDialog = it },
                                    { isCompletedDialog = it },
                                    {selectedProgetto = it}
                                    )
                            }
                        } else if (type.equals("ProgettiByMGR")) {
                            items(Progetti_List.sortedBy { it.nome }) { elemento ->
                                ProgettoElement(
                                    elemento,
                                    email,
                                    type,
                                    token,
                                    tipo,
                                    dipartimento,
                                    {
                                        Progetti_List = Progetti_List - it
                                    },
                                    adding,
                                    is_suspend,
                                    { isLoading = it },
                                    { showConnErrorDialog = it },
                                    { isCompletedDialog = it },
                                    {selectedProgetto = it}
                                    )
                            }
                        } else if (type.equals("task_by_project") && is_suspend) {
                            items(Task_list_By_project.sortedBy { it.nome }) { elemento ->
                                if (elemento.stato.equals("InProgress")) {
                                    TaskInCorso(
                                        elemento,
                                        tipo,
                                        token,
                                        {
                                            Task_list_By_project = Task_list_By_project - it
                                        },
                                        is_suspend,
                                        { isLoading = it },
                                        { showConnErrorDialog = it },
                                        { isCompletedDialog = it },
                                        dipartimento,
                                        {selectedTask = it}
                                    )
                                }
                            }
                        } else if (type.equals("task_by_project")) {
                            items(Task_list_By_project.sortedBy {
                                when (it.stato) {
                                    "InProgress" -> 1
                                    "Sospeso" -> 2
                                    "Completato" -> 3
                                    else -> 4
                                }
                            }) { elemento ->
                                if (elemento.stato.equals("InProgress")) {
                                    TaskInCorso(
                                        elemento,
                                        tipo,
                                        token,
                                        {
                                            Task_list_By_project = Task_list_By_project - it
                                        },
                                        is_suspend,
                                        { isLoading = it },
                                        { showConnErrorDialog = it },
                                        { isCompletedDialog = it },
                                        dipartimento,
                                        {selectedTask = it}
                                    )
                                } else if (elemento.stato.equals("Completato")) {
                                    TaskCompletata(
                                        elemento,
                                        tipo,
                                        token,
                                        {
                                            Task_list_By_project = Task_list_By_project - it
                                        },
                                        { isLoading = it },
                                        { showConnErrorDialog = it },
                                        { isCompletedDialog = it },
                                        dipartimento,
                                        {selectedTask = it}
                                    )
                                } else {
                                    TaskSospesa(
                                        elemento,
                                        tipo,
                                        token,
                                        {
                                            Task_list_By_project = Task_list_By_project - it
                                        },
                                        { isLoading = it },
                                        { showConnErrorDialog = it },
                                        { isCompletedDialog = it },
                                        dipartimento,
                                        {selectedTask = it}
                                    )
                                }
                            }
                        }
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
                            icon = {
                                Image(
                                    painter = painterResource(id = R.drawable.police_car_light_svgrepo_com),
                                    contentDescription = "Warning",
                                    modifier = Modifier.size(48.dp),
                                )
                            },
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
                            icon = {
                                Image(
                                    painter = painterResource(id = R.drawable.check_mark_button_svgrepo_com),
                                    contentDescription = "Warning",
                                    modifier = Modifier.size(48.dp),
                                )
                            },
                            title = { Text("Success!") },
                            text = { Text("Operazione avvenuta con successo!") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        // Chiudi il dialog e permetti all'utente di riprovare
                                        isCompleted = true
                                        isCompletedDialog = false
                                        if (tipo.equals("dipendente")) {
                                            val intent =
                                                Intent(this, HomeDipendenteActivity::class.java)
                                            intent.flags =
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            val intent =
                                                Intent(this, HomeManagerActivity::class.java)
                                            intent.flags =
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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
                }
            )
        }
    }
}

@Composable
fun TaskCompletata(task: Task, tipo: String?, token: String?, onDeleteRequest: (Task) -> Unit, onLoadingChange: (Boolean) -> Unit, onErrorChange: (Boolean) -> Unit, isCompletedDialog: (Boolean) -> Unit, dipartimento: Int, onSelect: (Task) -> Unit){
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val api = RetrofitInstance.api
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught $exception")
    }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandScape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { /* Non fare nulla per renderlo modale */ },
            icon = { Image(
                painter = painterResource(id = R.drawable.police_car_light_svgrepo_com),
                contentDescription = "Warning",
                modifier = Modifier.size(48.dp),
            ) },
            title = { Text("Attenzione") },
            text = { Text("Desideri davvero proseguire con l'eliminazione del seguente task?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        scope.launch(Dispatchers.IO + handler){
                            try{
                                withContext(Dispatchers.Main) {
                                    onLoadingChange(true)
                                }
                                val responseStatus = async{ api.deleteTask(mapOf<String, Any>("token" to token!!, "id" to task.id, "id_dipartimento" to dipartimento))}
                                val response = responseStatus.await()
                                withContext(Dispatchers.Main) {
                                    onLoadingChange(false)
                                }
                                if(response.isSuccessful){
                                    withContext(Dispatchers.Main) {
                                        onDeleteRequest(task)
                                        isCompletedDialog(true)
                                    }
                                }
                            } catch (e: java.net.ConnectException) {
                                withContext(Dispatchers.Main) {
                                    onErrorChange(true)
                                }
                            } catch (e: java.io.IOException) {
                                withContext(Dispatchers.Main) {
                                    onErrorChange(true)
                                }
                            } catch (e: Exception) {
                                println("Errore sconosciuto $e")
                            }finally {
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color("#66D161".toColorInt()),
                        Color("#B2FFB7".toColorInt()),
                    )
                ),
                shape = RoundedCornerShape(34)
            )
    ){
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Image(
                painter = painterResource(id = R.drawable.check_mark_button_svgrepo_com),
                contentDescription = "check_mark",
                modifier = Modifier
                    .size(48.dp)
            )
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    task.nome,
                    fontFamily = computerSaysNo,
                    fontWeight = FontWeight.W400,
                    fontSize = 40.sp,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 5.dp)
                        .width(200.dp)
                )
                if(task.nome_progetto != null) {
                    Text(
                        task.nome_progetto ?: "",
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 5.dp, top = 10.dp)
                            .width(200.dp)
                    )
                }
            }
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = {
                    if(tipo.equals("dipendente")) {
                        if(isLandScape){
                            onSelect(task)
                        }
                        else {
                            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                                .adapter(Task::class.java)
                            val gson = moshi.toJson(task)
                            val taskInfoIntent = Intent(context, InfoViewer::class.java)
                            taskInfoIntent.putExtra("task", gson)
                            taskInfoIntent.putExtra("type", "task_completata")
                            taskInfoIntent.putExtra("tipo", tipo)
                            taskInfoIntent.putExtra("infoType", "task")
                            taskInfoIntent.putExtra("token", token)
                            context.startActivity(taskInfoIntent)
                        }
                    }
                    else{
                        isExpanded = true
                    }
                }
            ) {
                Image(painter = painterResource(id= R.drawable.info_circle_svgrepo_com),"Info", modifier = Modifier.size(48.dp))
            }
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
                offset = DpOffset(x = (160).dp, y = (0).dp)
            ){
                DropdownMenuItem(
                    text = { Text("Info") },
                    onClick = {
                        if(isLandScape){
                            onSelect(task)
                            isExpanded = false
                        }else {
                            isExpanded = false
                            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                                .adapter(Task::class.java)
                            val gson = moshi.toJson(task)
                            val taskInfoIntent = Intent(context, InfoViewer::class.java)
                            taskInfoIntent.putExtra("task", gson)
                            taskInfoIntent.putExtra("type", "task_completata")
                            taskInfoIntent.putExtra("tipo", tipo)
                            taskInfoIntent.putExtra("infoType", "task")
                            taskInfoIntent.putExtra("token", token)
                            context.startActivity(taskInfoIntent)
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Elimina") },
                    onClick = {
                        showConfirmationDialog = true
                        isExpanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun TaskInCorso(task: Task, tipo: String?, token: String?, onDeleteRequest: (Task) -> Unit, isSuspend: Boolean, onLoadingChange: (Boolean) -> Unit, onErrorChange: (Boolean) -> Unit, isCompletedDialog: (Boolean) -> Unit, dipartimento: Int, onSelect: (Task) -> Unit){
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val api = RetrofitInstance.api
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught $exception")
    }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandScape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { /* Non fare nulla per renderlo modale */ },
            icon = { Image(
                painter = painterResource(id = R.drawable.police_car_light_svgrepo_com),
                contentDescription = "Warning",
                modifier = Modifier.size(48.dp),
            ) },
            title = { Text("Attenzione") },
            text = { Text("Desideri davvero proseguire con l'eliminazione del seguente task?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        scope.launch(Dispatchers.IO + handler){
                            try{
                                withContext(Dispatchers.Main) {
                                    onLoadingChange(true)
                                }
                                val responseStatus = async{ api.deleteTask(mapOf<String, Any>("token" to token!!, "id" to task.id, "id_dipartimento" to dipartimento))}
                                val response = responseStatus.await()
                                withContext(Dispatchers.Main) {
                                    onLoadingChange(false)
                                }
                                if(response.isSuccessful){
                                    withContext(Dispatchers.Main) {
                                        onDeleteRequest(task)
                                        isCompletedDialog(true)
                                    }
                                }
                            } catch (e: java.net.ConnectException) {
                                withContext(Dispatchers.Main) {
                                    onErrorChange(true)
                                }
                            } catch (e: java.io.IOException) {
                                withContext(Dispatchers.Main) {
                                    onErrorChange(true)
                                }
                            } catch (e: Exception) {
                                println("Errore sconosciuto $e")
                            } catch (e: java.net.SocketTimeoutException) {
                                withContext(Dispatchers.Main) {
                                    onErrorChange(true)
                                }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color("#FF07F0".toColorInt()),
                        Color("#D06FCA".toColorInt()),
                    )
                ),
                shape = RoundedCornerShape(34)
            )
    ){
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Image(
                painter = painterResource(id = R.drawable.gear_svgrepo_com),
                contentDescription = "check_mark",
                modifier = Modifier
                    .size(48.dp)
            )
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    task.nome,
                    fontFamily = computerSaysNo,
                    fontWeight = FontWeight.W400,
                    fontSize = 40.sp,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 5.dp)
                        .width(200.dp)
                )
                if(task.nome_progetto != null) {
                    Text(
                        task.nome_progetto ?: "",
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 5.dp, top = 10.dp)
                            .width(200.dp)
                    )
                }
            }
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = {
                    if(tipo.equals("dipendente")) {
                        if (isLandScape) {
                            onSelect(task)
                        }
                        else{
                        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                            .adapter(Task::class.java)
                        val gson = moshi.toJson(task)
                        val taskInfoIntent = Intent(context, InfoViewer::class.java)
                        taskInfoIntent.putExtra("task", gson)
                        taskInfoIntent.putExtra("type", "task_in_corso")
                        taskInfoIntent.putExtra("tipo", tipo)
                        taskInfoIntent.putExtra("infoType", "task")
                        taskInfoIntent.putExtra("token", token)
                        context.startActivity(taskInfoIntent)
                    }
                    }
                    else if(isSuspend){
                        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                            .adapter(Task::class.java)
                        val gson = moshi.toJson(task)
                        val taskSuspendIntent = Intent(context, TaskSospender::class.java)
                        taskSuspendIntent.putExtra("task", gson)
                        taskSuspendIntent.putExtra("type", "task_in_corso")
                        taskSuspendIntent.putExtra("tipo", tipo)
                        taskSuspendIntent.putExtra("infoType", "task")
                        taskSuspendIntent.putExtra("token", token)
                        context.startActivity(taskSuspendIntent)
                    }
                    else{
                        isExpanded = true
                    }
                }
            ) {
                Image(painter = painterResource(id= R.drawable.info_circle_svgrepo_com),"Info", modifier = Modifier.size(48.dp))
            }
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
                offset = DpOffset(x = (160).dp, y = (0).dp)
            ){
                DropdownMenuItem(
                    text = { Text("Info") },
                    onClick = {
                        if (isLandScape) {
                            onSelect(task)
                            isExpanded = false
                        }
                        else {
                            isExpanded = false
                            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                                .adapter(Task::class.java)
                            val gson = moshi.toJson(task)
                            val taskInfoIntent = Intent(context, InfoViewer::class.java)
                            taskInfoIntent.putExtra("task", gson)
                            taskInfoIntent.putExtra("type", "task_in_corso")
                            taskInfoIntent.putExtra("tipo", tipo)
                            taskInfoIntent.putExtra("infoType", "task")
                            taskInfoIntent.putExtra("token", token)
                            context.startActivity(taskInfoIntent)
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Elimina") },
                    onClick = {
                        showConfirmationDialog = true
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TaskSospesa(task: Task, tipo: String?, token: String?, onDeleteRequest: (Task) -> Unit, onLoadingChange: (Boolean) -> Unit, onErrorChange: (Boolean) -> Unit, isCompletedDialog: (Boolean) -> Unit, dipartimento: Int, onSelect: (Task) -> Unit){
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val api = RetrofitInstance.api
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught $exception")
    }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandScape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { /* Non fare nulla per renderlo modale */ },
            icon = { Image(
                painter = painterResource(id = R.drawable.police_car_light_svgrepo_com),
                contentDescription = "Warning",
                modifier = Modifier.size(48.dp),
            ) },
            title = { Text("Attenzione") },
            text = { Text("Desideri davvero proseguire con l'eliminazione del seguente task?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        scope.launch(Dispatchers.IO + handler){
                            try{
                                withContext(Dispatchers.Main) {
                                    onLoadingChange(true)
                                }
                                val responseStatus = async{ api.deleteTask(mapOf<String, Any>("token" to token!!, "id" to task.id, "id_dipartimento" to dipartimento))}
                                val response = responseStatus.await()
                                withContext(Dispatchers.Main) {
                                    onLoadingChange(false)
                                }
                                if(response.isSuccessful){
                                    withContext(Dispatchers.Main) {
                                        onDeleteRequest(task)
                                        isCompletedDialog(true)
                                    }
                                }
                            } catch (e: java.net.ConnectException) {
                                withContext(Dispatchers.Main) {
                                    onErrorChange(true)
                                }
                            } catch (e: java.io.IOException) {
                                withContext(Dispatchers.Main) {
                                    onErrorChange(true)
                                }
                            } catch (e: Exception) {
                                println("Errore sconosciuto $e")
                            } catch (e: java.net.SocketTimeoutException) {
                                withContext(Dispatchers.Main) {
                                    onErrorChange(true)
                                }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color("#FF850A".toColorInt()),
                        Color("#FBAB76".toColorInt()),
                    )
                ),
                shape = RoundedCornerShape(34)
            )
    ){
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Image(
                painter = painterResource(id = R.drawable.red_exclamation_mark_svgrepo_com),
                contentDescription = "check_mark",
                modifier = Modifier
                    .size(48.dp)
            )
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    task.nome,
                    fontFamily = computerSaysNo,
                    fontWeight = FontWeight.W400,
                    fontSize = 40.sp,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 5.dp)
                        .width(200.dp)
                )
                if(task.nome_progetto != null) {
                    Text(
                        task.nome_progetto ?: "",
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 5.dp, top = 10.dp)
                            .width(200.dp)
                    )
                }
            }
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = {
                    if(tipo.equals("dipendente")) {
                        if (isLandScape) {
                            onSelect(task)
                        }
                        else {
                            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                                .adapter(Task::class.java)
                            val gson = moshi.toJson(task)
                            val taskInfoIntent = Intent(context, InfoViewer::class.java)
                            taskInfoIntent.putExtra("task", gson)
                            taskInfoIntent.putExtra("type", "task_sospesa")
                            taskInfoIntent.putExtra("tipo", tipo)
                            taskInfoIntent.putExtra("infoType", "task")
                            taskInfoIntent.putExtra("token", token)
                            context.startActivity(taskInfoIntent)
                        }
                    }
                    else{
                        isExpanded = true
                    }
                }
            ) {
                Image(painter = painterResource(id= R.drawable.info_circle_svgrepo_com),"Info", modifier = Modifier.size(48.dp))
            }
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
                offset = DpOffset(x = (160).dp, y = (0).dp)
            ){
                DropdownMenuItem(
                    text = { Text("Info") },
                    onClick = {
                        if (isLandScape) {
                            onSelect(task)
                            isExpanded = false
                        }
                        else {
                            isExpanded = false
                            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                                .adapter(Task::class.java)
                            val gson = moshi.toJson(task)
                            val taskInfoIntent = Intent(context, InfoViewer::class.java)
                            taskInfoIntent.putExtra("task", gson)
                            taskInfoIntent.putExtra("type", "task_sospesa")
                            taskInfoIntent.putExtra("tipo", tipo)
                            taskInfoIntent.putExtra("infoType", "task")
                            taskInfoIntent.putExtra("token", token)
                            context.startActivity(taskInfoIntent)
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Elimina") },
                    onClick = {
                        showConfirmationDialog = true
                        isExpanded = false
                    }
                )
            }
        }
    }

}

@Composable
fun ProgettoElement(progetto: Progetto, email: String?, type: String?, token: String?, tipo: String?, dipartimento: Int?, onDeleteRequest: (Progetto) -> Unit, adding: Boolean, is_suspend: Boolean, onLoadingChange: (Boolean) -> Unit, onErrorChange: (Boolean) -> Unit, isCompletedDialog: (Boolean) -> Unit, onSelectProgetto: (Progetto) -> Unit){
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val api = RetrofitInstance.api
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught $exception")
    }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandScape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { /* Non fare nulla per renderlo modale */ },
            icon = { Image(
                painter = painterResource(id = R.drawable.police_car_light_svgrepo_com),
                contentDescription = "Warning",
                modifier = Modifier.size(48.dp),
            ) },
            title = { Text("Attenzione") },
            text = { Text("Desideri davvero proseguire con l'eliminazione del seguente progetto?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        scope.launch(Dispatchers.IO + handler){
                            try{
                                withContext(Dispatchers.Main) {
                                    onLoadingChange(true)
                                }
                                val responseStatus = async{ api.deleteProject(mapOf<String, Any>("token" to token!!, "id_progetto" to progetto.id_progetto, "id_dipartimento" to dipartimento!!))}
                                val response = responseStatus.await()
                                withContext(Dispatchers.Main) {
                                    onLoadingChange(false)
                                }
                                if(response.isSuccessful){
                                    withContext(Dispatchers.Main){
                                        onDeleteRequest(progetto)
                                        isCompletedDialog(true)
                                    }
                                }
                            } catch (e: java.net.ConnectException) {
                                withContext(Dispatchers.Main) {
                                    onErrorChange(true)
                                }
                            } catch (e: java.io.IOException) {
                                withContext(Dispatchers.Main) {
                                    onErrorChange(true)
                                }
                            } catch (e: Exception) {
                                println("Errore sconosciuto $e")
                            }catch (e: java.net.SocketTimeoutException) {
                                withContext(Dispatchers.Main) {
                                    onErrorChange(true)
                                }
                            }finally {
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color("#A56FD9".toColorInt()),
                        Color("#D06FCA".toColorInt()),
                    )
                ),
                shape = RoundedCornerShape(34)
            )
    ){
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Image(
                painter = painterResource(id = R.drawable.briefcase_svgrepo_com),
                contentDescription = "check_mark",
                modifier = Modifier
                    .size(48.dp)
            )
            Text(progetto.nome,
                fontFamily = computerSaysNo,
                fontWeight = FontWeight.W400,
                fontSize = 40.sp,
                modifier = Modifier
                    .padding(start = 16.dp, end = 5.dp)
                    .width(200.dp)
            )
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = {
                    if(adding){
                        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                            .adapter(Progetto::class.java)
                        val gson = moshi.toJson(progetto)
                        val addTaskIntent = Intent(context, Adder::class.java)
                        addTaskIntent.putExtra("type", "taskAdder")
                        addTaskIntent.putExtra("token", token)
                        addTaskIntent.putExtra("email", email)
                        addTaskIntent.putExtra("id_dipartimento", dipartimento)
                        addTaskIntent.putExtra("id_progetto", progetto.id_progetto)
                        addTaskIntent.putExtra("nome_progetto", progetto.nome)
                        addTaskIntent.putExtra("progetto", gson)

                        context.startActivity(addTaskIntent)

                    }
                    else if(is_suspend){
                        val projectInfoIntent = Intent(context, ListViewer::class.java)
                        projectInfoIntent.putExtra("email", email)
                        projectInfoIntent.putExtra("type", "task_by_project")
                        projectInfoIntent.putExtra("token", token)
                        projectInfoIntent.putExtra("tipo", tipo)
                        projectInfoIntent.putExtra("progetto_id", progetto.id_progetto)
                        projectInfoIntent.putExtra("dipartimento", dipartimento)
                        projectInfoIntent.putExtra("suspend",true)
                        context.startActivity(projectInfoIntent)
                    }
                    else {
                        isExpanded = true
                    }
                }
            ) {
                Image(painter = painterResource(id= R.drawable.info_circle_svgrepo_com),"Info", modifier = Modifier.size(48.dp))
            }
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
                offset = DpOffset(x = (160).dp, y = (0).dp)
            ) {
                if (type.equals("ProgettiByMGR") && progetto.Dipartimento_id_dipartimento != dipartimento) {
                    DropdownMenuItem(
                        text = { Text("Info") },
                        onClick = {
                            if (isLandScape) {
                                onSelectProgetto(progetto)
                                isExpanded = false
                            }
                            else {
                                isExpanded = false
                                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                                    .adapter(Progetto::class.java)
                                val gson = moshi.toJson(progetto)
                                val projectInfoIntent = Intent(context, InfoViewer::class.java)
                                projectInfoIntent.putExtra("progetto", gson)
                                projectInfoIntent.putExtra("infoType", "progetto")
                                context.startActivity(projectInfoIntent)
                            }
                        }
                    )

                }
                else{
                DropdownMenuItem(
                    text = { Text("Info") },
                    onClick = {
                        if (isLandScape) {
                            onSelectProgetto(progetto)
                            isExpanded = false
                        }
                        else {
                            isExpanded = false
                            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                                .adapter(Progetto::class.java)
                            val gson = moshi.toJson(progetto)
                            val projectInfoIntent = Intent(context, InfoViewer::class.java)
                            projectInfoIntent.putExtra("progetto", gson)
                            projectInfoIntent.putExtra("infoType", "progetto")
                            context.startActivity(projectInfoIntent)
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Tasks") },
                    onClick = {
                        isExpanded = false
                        val projectInfoIntent = Intent(context, ListViewer::class.java)
                        projectInfoIntent.putExtra("email", email)
                        projectInfoIntent.putExtra("type", "task_by_project")
                        projectInfoIntent.putExtra("token", token)
                        projectInfoIntent.putExtra("tipo", tipo)
                        projectInfoIntent.putExtra("progetto_id", progetto.id_progetto)
                        projectInfoIntent.putExtra("dipartimento", dipartimento)
                        context.startActivity(projectInfoIntent)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Elimina") },
                    onClick = {
                        showConfirmationDialog = true
                        isExpanded = false
                    }
                )
            }
            }
        }
    }
}

@Composable
fun DipendenteElement(d: Dipendente){
    var isPopupVisible by remember { mutableStateOf(false) }
    var popupPosition by remember { mutableStateOf(IntOffset.Zero) }
    val onGloballyPositioned = { coordinates: LayoutCoordinates ->
        popupPosition = coordinates.localToWindow(Offset.Zero).round()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color("#7B6FE9".toColorInt()),
                        Color("#7B6FE9".toColorInt()).copy(0.8f),
                    )
                ),
                shape = RoundedCornerShape(34)
            )
    ){
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            if(d.sesso.equals("M")) {
                Image(
                    painter = painterResource(id = R.drawable.man_technologist_light_skin_tone_svgrepo_com),
                    contentDescription = "check_mark",
                    modifier = Modifier
                        .size(48.dp)
                )
            }
            else{
                Image(
                    painter = painterResource(id = R.drawable.woman_technologist_light_skin_tone_svgrepo_com),
                    contentDescription = "check_mark",
                    modifier = Modifier
                        .size(48.dp)
                )

            }
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
            ) {
                Text("${d.nome} ${d.cognome}",
                    fontFamily = computerSaysNo,
                    fontWeight = FontWeight.W400,
                    fontSize = 40.sp,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 5.dp)
                        .width(300.dp)
                )
                Text("${d.email}",
                    fontFamily = computerSaysNo,
                    fontWeight = FontWeight.W400,
                    fontSize = 25.sp,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 5.dp)
                        .width(300.dp)
                )
            }
            IconButton(
                modifier = Modifier
                    .size(48.dp)
                    .onGloballyPositioned(onGloballyPositioned),
                onClick = {
                    isPopupVisible = true
                }
            ) {
                Image(painter = painterResource(id= R.drawable.info_circle_svgrepo_com),"Info", modifier = Modifier.size(48.dp))
            }
            DropdownMenu(
                expanded = isPopupVisible,
                onDismissRequest = { isPopupVisible = false },
                // 5. Usa l'offset per posizionare il popup vicino al bottone
                offset = DpOffset(x = (160).dp, y = (0).dp),
                modifier = Modifier.background(Color.White, RoundedCornerShape(16.dp))
            ) {
                // 6. Contenuto personalizzato del popup
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(Color.Transparent, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tel: ${d.numero_telefono}", // Assumendo che 'd' abbia un campo 'telefono'
                        fontFamily = computerSaysNo,
                        fontSize = 40.sp,
                        color = Color.Black,
                        modifier = Modifier.background(Color.White,RoundedCornerShape(16.dp))
                    )
                }
            }
        }
    }

}
