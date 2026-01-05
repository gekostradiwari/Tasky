package com.android.tasky.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
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
import androidx.lifecycle.lifecycleScope
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
            LaunchedEffect(Unit){
                lifecycleScope.launch(Dispatchers.IO + handler) {
                    try {
                        isLoading = true;
                        if(type.equals("task_in_corso")){
                            println(email)
                            println(token)
                            val responseStatus = async{
                                api.TaskLister(mapOf("token" to token, "email_dipendente" to email))
                            }
                            val response = responseStatus.await()
                            isLoading = false
                            if(response.isSuccessful){
                                Tasks_In_Corso = response.body()!!.data.items
                            }
                        }
                        else if(type.equals("task_completati")){
                            val responseStatus = async{
                                api.TaskListerCompleted(mapOf("token" to token, "email_dipendente" to email))
                            }
                            val response = responseStatus.await()
                            isLoading = false
                            if(response.isSuccessful){
                                Tasks_Completate = response.body()!!.data.items
                            }
                        }
                        else if(type.equals("task_sospesi")){ //Qui non bisogna chiudere con else ma continuare con gli altri casi se ci sono progetti dipendenti o altro
                            val responseStatus = async{
                                api.TaskListerSuspended(mapOf("token" to token, "email_dipendente" to email))
                            }
                            val response = responseStatus.await()
                            isLoading = false
                            if(response.isSuccessful){
                                Tasks_Sospese = response.body()!!.data.items
                            }

                        }
                        else if(type.equals("dipendenti")){
                            val responseStatus = async{
                                api.dipendentiByDepartment(mapOf<String,Any>("token" to token!!, "id_dipartimento" to dipartimento))
                            }
                            val response = responseStatus.await()
                            isLoading = false
                            if(response.isSuccessful){
                                Dipendenti_List = response.body()!!.items
                            }
                        }
                        else if(type.equals("progetti")){
                            val responseStatus = async{
                                api.getProjectByDepartment(mapOf<String,Any>("token" to token!!, "id_dipartimento" to dipartimento))
                            }
                            val response = responseStatus.await()
                            isLoading = false
                            if(response.isSuccessful){
                                Progetti_List = response.body()!!.data.items
                            }
                        }
                        else if(type.equals("task_by_project")){
                            val responseStatus = async{
                                api.getTaskByProjectMGR(mapOf<String,Any>("token" to token!!, "id_progetto" to progetto, "id_dipartimento" to dipartimento))
                            }
                            val response = responseStatus.await()
                            isLoading = false
                            if(response.isSuccessful){
                                Task_list_By_project = response.body()!!.data.items
                            }

                        }
                        else if(type.equals("ProgettiByMGR")){
                            val responseStatus = async{
                                api.getProjectsByMGR(mapOf("email_manager" to email!!))
                            }
                            val response = responseStatus.await()
                            isLoading = false
                            if(response.isSuccessful){
                                Progetti_List = response.body()!!.data.items
                            }
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
            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .height(70.dp)
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
                            ),
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
                    LazyColumn(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        if(type.equals("task_in_corso")){
                            items(Tasks_In_Corso){elemento ->
                                TaskInCorso(elemento, tipo, token, { }, is_suspend)
                            }

                        }
                        else if(type.equals("task_completati")){
                            items(Tasks_Completate){elemento ->
                                TaskCompletata(elemento, tipo, token, { })
                            }

                        }
                        else if(type.equals("task_sospesi")){ //Qui non bisogna chiudere con else ma continuare con gli altri casi se ci sono progetti dipendenti o altro
                            items(Tasks_Sospese){elemento ->
                                TaskSospesa(elemento, tipo, token, { })
                            }
                        }
                        else if(type.equals("dipendenti")){
                            items(Dipendenti_List){elemento ->
                                DipendenteElement(elemento)
                            }
                        }
                        else if(type.equals("progetti")) {
                            items(Progetti_List) { elemento ->
                                ProgettoElement(elemento, email, type, token, tipo, dipartimento, {
                                    lifecycleScope.launch(Dispatchers.IO + handler){
                                        try{
                                            isLoading = true
                                            val responseStatus = async{ api.deleteProject(mapOf<String, Any>("token" to token!!, "id_progetto" to elemento.id_progetto, "id_dipartimento" to dipartimento))}
                                            val response = responseStatus.await()
                                            isLoading = false
                                            if(response.isSuccessful){
                                                Progetti_List = Progetti_List - elemento
                                            }
                                        } catch (e: java.net.ConnectException) {
                                            println("Impossibile contattare il server")
                                        } catch (e: java.io.IOException) {
                                            println("Problema di connessione")
                                        } catch (e: Exception) {
                                            println("Errore sconosciuto $e")
                                        }
                                    }
                                }, adding, is_suspend)
                            }
                        }
                        else if(type.equals("ProgettiByMGR")) {
                            items(Progetti_List) { elemento ->
                                ProgettoElement(elemento, email, type, token, tipo, dipartimento, {
                                    lifecycleScope.launch(Dispatchers.IO + handler){
                                        try{
                                            isLoading = true
                                            val responseStatus = async{ api.deleteProject(mapOf<String, Any>("token" to token!!, "id_progetto" to elemento.id_progetto, "id_dipartimento" to dipartimento))}
                                            val response = responseStatus.await()
                                            isLoading = false
                                            if(response.isSuccessful){
                                                Progetti_List = Progetti_List - elemento
                                            }
                                        } catch (e: java.net.ConnectException) {
                                            println("Impossibile contattare il server")
                                        } catch (e: java.io.IOException) {
                                            println("Problema di connessione")
                                        } catch (e: Exception) {
                                            println("Errore sconosciuto $e")
                                        }
                                    }
                                }, adding, is_suspend)
                            }
                        }
                        else if(type.equals("task_by_project") && is_suspend){
                            items(Task_list_By_project){
                                elemento ->
                                if(elemento.stato.equals("InProgress")){
                                    TaskInCorso(elemento, tipo, token, {
                                        lifecycleScope.launch(Dispatchers.IO + handler){
                                            try{
                                                isLoading = true
                                                val responseStatus = async{ api.deleteTask(mapOf<String, Any>("token" to token!!, "id" to elemento.id, "id_dipartimento" to dipartimento))}
                                                val response = responseStatus.await()
                                                isLoading = false
                                                if(response.isSuccessful){
                                                    Task_list_By_project = Task_list_By_project - elemento
                                                }
                                            } catch (e: java.net.ConnectException) {
                                                println("Impossibile contattare il server")
                                            } catch (e: java.io.IOException) {
                                                println("Problema di connessione")
                                            } catch (e: Exception) {
                                                println("Errore sconosciuto $e")
                                            }
                                        }
                                    }, is_suspend)
                                }
                            }
                        }
                        else if(type.equals("task_by_project")){
                            items(Task_list_By_project){elemento ->
                                if(elemento.stato.equals("InProgress")){
                                    TaskInCorso(elemento, tipo, token, {
                                        lifecycleScope.launch(Dispatchers.IO + handler){
                                            try{
                                                isLoading = true
                                                val responseStatus = async{ api.deleteTask(mapOf<String, Any>("token" to token!!, "id" to elemento.id, "id_dipartimento" to dipartimento))}
                                                val response = responseStatus.await()
                                                isLoading = false
                                                if(response.isSuccessful){
                                                    Task_list_By_project = Task_list_By_project - elemento
                                                }
                                            } catch (e: java.net.ConnectException) {
                                                println("Impossibile contattare il server")
                                            } catch (e: java.io.IOException) {
                                                println("Problema di connessione")
                                            } catch (e: Exception) {
                                                println("Errore sconosciuto $e")
                                            }
                                        }
                                    },is_suspend)
                                }
                                else if(elemento.stato.equals("Completato")){
                                    TaskCompletata(elemento, tipo, token, {
                                        lifecycleScope.launch(Dispatchers.IO + handler){
                                        try{
                                            isLoading = true
                                            val responseStatus = async{ api.deleteTask(mapOf<String, Any>("token" to token!!, "id" to elemento.id, "id_dipartimento" to dipartimento))}
                                            val response = responseStatus.await()
                                            isLoading = false
                                            if(response.isSuccessful){
                                                Task_list_By_project = Task_list_By_project - elemento
                                            }
                                        } catch (e: java.net.ConnectException) {
                                            println("Impossibile contattare il server")
                                        } catch (e: java.io.IOException) {
                                            println("Problema di connessione")
                                        } catch (e: Exception) {
                                            println("Errore sconosciuto $e")
                                        }
                                    }})
                                }
                                else{
                                    TaskSospesa(elemento, tipo, token, {
                                        lifecycleScope.launch(Dispatchers.IO + handler){
                                        try{
                                            isLoading = true
                                            val responseStatus = async{ api.deleteTask(mapOf<String, Any>("token" to token!!, "id" to elemento.id, "id_dipartimento" to dipartimento))}
                                            val response = responseStatus.await()
                                            isLoading = false
                                            if(response.isSuccessful){
                                                Task_list_By_project = Task_list_By_project - elemento
                                            }
                                        } catch (e: java.net.ConnectException) {
                                            println("Impossibile contattare il server")
                                        } catch (e: java.io.IOException) {
                                            println("Problema di connessione")
                                        } catch (e: Exception) {
                                            println("Errore sconosciuto $e")
                                        }
                                    }})
                                }
                            }
                        }
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

@Composable
fun TaskCompletata(task: Task, tipo: String?, token: String?, onDeleteRequest: () -> Unit){
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
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
            Text(
                task.nome,
                fontFamily = computerSaysNo,
                fontWeight = FontWeight.W400,
                fontSize = 40.sp,
                modifier = Modifier
                    .padding(start = 16.dp, end = 5.dp)
                    .width(240.dp)
                )
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = {
                    if(tipo.equals("dipendente")) {
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
            ){
                DropdownMenuItem(
                    text = { Text("Info") },
                    onClick = {
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
                )
                DropdownMenuItem(
                    text = { Text("Elimina") },
                    onClick = {
                        onDeleteRequest()
                        isExpanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun TaskInCorso(task: Task, tipo: String?, token: String?, onDeleteRequest: () -> Unit, isSuspend: Boolean){
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
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
            Text(task.nome,
                fontFamily = computerSaysNo,
                fontWeight = FontWeight.W400,
                fontSize = 40.sp,
                modifier = Modifier
                    .padding(start = 16.dp, end = 5.dp)
                    .width(240.dp)
            )
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = {
                    if(tipo.equals("dipendente")) {
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
            ){
                DropdownMenuItem(
                    text = { Text("Info") },
                    onClick = {
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
                )
                DropdownMenuItem(
                    text = { Text("Elimina") },
                    onClick = {
                        onDeleteRequest()
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TaskSospesa(task: Task, tipo: String?, token: String?, onDeleteRequest: () -> Unit){
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
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
            Text(task.nome,
                fontFamily = computerSaysNo,
                fontWeight = FontWeight.W400,
                fontSize = 40.sp,
                modifier = Modifier
                    .padding(start = 16.dp, end = 5.dp)
                    .width(240.dp)
            )
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = {
                    if(tipo.equals("dipendente")) {
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
            ){
                DropdownMenuItem(
                    text = { Text("Info") },
                    onClick = {
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
                )
                DropdownMenuItem(
                    text = { Text("Elimina") },
                    onClick = {
                        onDeleteRequest()
                        isExpanded = false
                    }
                )
            }
        }
    }

}

@Composable
fun ProgettoElement(progetto: Progetto, email: String?, type: String?, token: String?, tipo: String?, dipartimento: Int?, onDeleteRequest: () -> Unit, adding: Boolean, is_suspend: Boolean){
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
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
            Text(progetto.nome)
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = {
                    if(adding){
                        val addTaskIntent = Intent(context, Adder::class.java)
                        addTaskIntent.putExtra("type", "taskAdder")
                        addTaskIntent.putExtra("token", token)
                        addTaskIntent.putExtra("email", email)
                        addTaskIntent.putExtra("id_dipartimento", dipartimento)
                        addTaskIntent.putExtra("id_progetto", progetto.id_progetto)
                        addTaskIntent.putExtra("nome_progetto", progetto.nome)
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
            ) {
                if (type.equals("ProgettiByMGR") && progetto.Dipartimento_id_dipartimento != dipartimento) {
                    DropdownMenuItem(
                        text = { Text("Info") },
                        onClick = {
                            isExpanded = false
                            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                                .adapter(Progetto::class.java)
                            val gson = moshi.toJson(progetto)
                            val projectInfoIntent = Intent(context, InfoViewer::class.java)
                            projectInfoIntent.putExtra("progetto", gson)
                            projectInfoIntent.putExtra("infoType", "progetto")
                            context.startActivity(projectInfoIntent)
                        }
                    )

                }
                else{
                DropdownMenuItem(
                    text = { Text("Info") },
                    onClick = {
                        isExpanded = false
                        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                            .adapter(Progetto::class.java)
                        val gson = moshi.toJson(progetto)
                        val projectInfoIntent = Intent(context, InfoViewer::class.java)
                        projectInfoIntent.putExtra("progetto", gson)
                        projectInfoIntent.putExtra("infoType", "progetto")
                        context.startActivity(projectInfoIntent)
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
                        onDeleteRequest()
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
                Text("${d.nome} ${d.cognome}")
                Text(d.email)
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
                offset = DpOffset(x = (-42).dp, y = (-10).dp),
                modifier = Modifier.background(Color.White, RoundedCornerShape(16.dp))
            ) {
                // 6. Contenuto personalizzato del popup
                Box(
                    modifier = Modifier.padding(16.dp)
                        .background(Color.Transparent, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tel: ${d.numero_telefono}", // Assumendo che 'd' abbia un campo 'telefono'
                        fontFamily = computerSaysNo,
                        fontSize = 20.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }

}
