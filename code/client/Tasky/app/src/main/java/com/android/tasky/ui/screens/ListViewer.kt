package com.android.tasky.ui.screens

import android.app.Activity
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.android.tasky.R
import com.android.tasky.dto.Task
import com.android.tasky.ui.theme.computerSaysNo
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch

class ListViewer : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //Array di tags che servono per definire che oggetto elencare
        val tags = arrayOf("Tasks_in_corso", "Tasks_completate", "Tasks_sospese", "Progetti", "Dipendenti")
        setContent {
            var TasksCompletate = listOf<String>()
            //Fare query la richiesta della lista delle tasks completate
            //Implementare l'animazione di caricamento circolare


            /*var ListTaskCompletate by remember {
                mutableStateOf(TasksCompletate)
            }*/

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
                        IconButton(onClick = { /*( as? Activity)?.finish()*/}
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
                        items(TasksCompletate){elemento ->
                            //chiamare la funzione composable corrispondente

                        }

                    }
                }
            )
        }
    }
}

@Composable
@Preview
fun TaskCompletata(/*task: Task*/){
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
            Text("nome task")
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = {/* Prendi la task e mandala all'activity per mostrare le info della task*/}
            ) {
                Image(painter = painterResource(id= R.drawable.info_circle_svgrepo_com),"Info", modifier = Modifier.size(48.dp))
            }
        }
    }

}


@Composable
fun TaskInCorso(/*task: Task*/){
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
            Text("nome task")
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = {/* Prendi la task e mandala all'activity per mostrare le info della task*/}
            ) {
                Image(painter = painterResource(id= R.drawable.info_circle_svgrepo_com),"Info", modifier = Modifier.size(48.dp))
            }
        }
    }

}

@Composable
fun TaskSospesa(/*task: Task*/){
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
            Text("nome task")
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = {/* Prendi la task e mandala all'activity per mostrare le info della task*/}
            ) {
                Image(painter = painterResource(id= R.drawable.info_circle_svgrepo_com),"Info", modifier = Modifier.size(48.dp))
            }
        }
    }

}

@Composable
fun Progetto(/*task: Task*/){
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
            Text("nome task")
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = {/* Prendi la task e mandala all'activity per mostrare le info della task*/}
            ) {
                Image(painter = painterResource(id= R.drawable.info_circle_svgrepo_com),"Info", modifier = Modifier.size(48.dp))
            }
        }
    }

}

@Composable
fun Dipendente(/*task: Task*/){
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
            if(true) {
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
                modifier = Modifier.width(260.dp).fillMaxHeight()

            ) {
                Text("Nome Dipendente")
                Text("EMail Dipendente")
            }
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = {/* Prendi la task e mandala all'activity per mostrare le info della task*/}
            ) {
                Image(painter = painterResource(id= R.drawable.info_circle_svgrepo_com),"Info", modifier = Modifier.size(48.dp))
            }
        }
    }

}
