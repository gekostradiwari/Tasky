package com.android.tasky.ui.screens

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.android.tasky.R
import com.android.tasky.ui.theme.computerSaysNo
import kotlinx.coroutines.launch
import kotlin.math.round

class HomeDipendenteActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var showDialog by remember { mutableStateOf(false) }
            if(showDialog){
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    icon =
                        {Image(
                            painter = painterResource(id = R.drawable.police_car_light_svgrepo_com),
                            contentDescription = "Warning",
                            modifier = Modifier.size(48.dp),
                        )},
                    title = {Text("Uscire da Tasky?")},
                    text = {Text("Sei sicuro di voler uscire da Tasky?")},
                    confirmButton = {Button(
                      onClick = {
                          (context as? Activity)?.finishAffinity()
                      },
                    ){
                        Text("Conferma")
                    }},
                    dismissButton = {Button(
                        onClick = { showDialog = false }
                    ){
                        Text("Annulla", color = Color.Red)
                    }}
                )
            }
            HomeDipendenteActivityPreview()
            BackHandler {
                showDialog = true
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun HomeDipendenteActivityPreview() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(200.dp)) {
                Spacer(Modifier.height(16.dp))
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.logout_ico),
                            contentDescription = "Log Out"
                        )
                    },
                    label = { Text("Log Out") },
                    selected = true,
                    onClick = { scope.launch { /* Mandare l'utente sull'activity di logout*/ } }
                )
            }
        }
    ) {
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
                    IconButton(onClick = { scope.launch { drawerState.open() } }
                    ) {
                        Icon(Icons.Default.Menu, "Menu")
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    Text("Benvenuto!",
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier.padding(0.dp, 20.dp, 0.dp, 0.dp)
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(358.dp)
                            .height(92.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color("#FF07F0".toColorInt()),
                                        Color("#D06FCA".toColorInt()).copy(0.5f),
                                    ),
                                ),
                                shape = RoundedCornerShape(34)
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Nome Dipendente",
                                textAlign = TextAlign.Center,
                                fontFamily = computerSaysNo,
                                fontWeight = FontWeight.W400,
                                fontSize = 40.sp,
                                modifier = Modifier.width(250.dp)

                            )
                            Image(
                                painter = painterResource(id = R.drawable.man_technologist_light_skin_tone_svgrepo_com),
                                contentDescription = "Dipendente",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(358.dp)
                            .height(161.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color("#7B6FE9".toColorInt()).copy(0.2f),
                                        Color("#866FE5".toColorInt())
                                    ),
                                ),
                                shape = RoundedCornerShape(34
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Dipartimento", textAlign = TextAlign.Center)
                            Text("Nome Dipartimento")
                            Image(
                                painter = painterResource(id = R.drawable.office_building_svgrepo_com),
                                contentDescription = "Dipartimento",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(358.dp)
                            .height(161.dp)
                            .background(Color.Green, shape = RoundedCornerShape(34))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(Modifier.height(30.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Text("Tasks in corso!")
                                Text("Qui va l'icona")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.Bottom
                            ){
                                IconButton(onClick = { /*TODO*/ }) {
                                    Icon(Icons.Default.ArrowForward,"Vai")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(160.dp)
                                .height(161.dp)
                                .background(Color.Blue, shape = RoundedCornerShape(34))
                        ){
                            Column (
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                                Text("Tasks")
                                Text("Completati")
                                Text("Qui va l'icona")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.Bottom
                                ){
                                    IconButton(onClick = { /*TODO*/ }) {
                                        Icon(Icons.Default.ArrowForward,"Vai")
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.width(30.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(160.dp)
                                .height(161.dp)
                                .background(Color.Red, shape = RoundedCornerShape(34))
                        ){
                            Column (
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                                Text("Tasks")
                                Text("Sospesi")
                                Text("Qui va l'icona")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.Bottom
                                ){
                                    IconButton(onClick = { /*TODO*/ }) {
                                        Icon(Icons.Default.ArrowForward,"Vai")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}