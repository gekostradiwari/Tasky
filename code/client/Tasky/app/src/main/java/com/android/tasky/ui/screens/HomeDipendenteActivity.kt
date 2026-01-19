package com.android.tasky.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.lerp
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
import com.android.tasky.MainActivity
import com.android.tasky.R
import com.android.tasky.ui.theme.computerSaysNo
import com.android.tasky.utility.SessionManager
import kotlinx.coroutines.launch
import kotlin.math.round

class HomeDipendenteActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val intent = this.intent
        val token = intent.getStringExtra("token")
        val sesso = intent.getStringExtra("sesso")
        val id_dipartimento = intent.getIntExtra("id_dipartimento",0)
        println(id_dipartimento)
        val nome_dipartimento = intent.getStringExtra("nome_dipartimento")
        val nome = intent.getStringExtra("nome")
        val email = intent.getStringExtra("email")
        println("Start HomeDipendenteActivity $email")
        val tipo = intent.getStringExtra("tipo")
        val sessionManager = SessionManager.getInstance(applicationContext)
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
            HomeDipendenteActivityPreview(token, sesso, id_dipartimento, nome_dipartimento, nome, sessionManager, email, tipo)
            BackHandler {
                showDialog = true
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDipendenteActivityPreview(token:String?, sesso:String?, id_dipartimento:Int?, nome_dipartimento:String?, nome:String?, sessionManager:SessionManager, email:String?, tipo:String?) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val interactionSourceCompletati = remember { MutableInteractionSource() }
    val isPressedCompletati by interactionSourceCompletati.collectIsPressedAsState()
    val scaleCompletati by animateFloatAsState(if (isPressedCompletati) 0.95f else 1f)

    val interactionSourceSospesi = remember { MutableInteractionSource() }
    val isPressedSospesi by interactionSourceSospesi.collectIsPressedAsState()
    val scaleSospesi by animateFloatAsState(if (isPressedSospesi) 0.95f else 1f)

    val interactionSourceInCorso = remember { MutableInteractionSource() }
    val isPressedInCorso by interactionSourceInCorso.collectIsPressedAsState()
    val scaleInCorso by animateFloatAsState(if (isPressedInCorso) 0.95f else 1f)


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
                    onClick = {
                        sessionManager.clearAuthToken()
                        val intent = Intent(context, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                        (context as? Activity)?.finish()

                    }
                )
            }
        }
    ) {
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
                    IconButton(onClick = { scope.launch { drawerState.open() } }
                    ) {
                        Icon(Icons.Default.Menu, "Menu")
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
                                        lerp(Color("#D06FCA".toColorInt()), Color.White, 0.5f)
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
                            Text("$nome",
                                textAlign = TextAlign.Center,
                                fontFamily = computerSaysNo,
                                fontWeight = FontWeight.W400,
                                fontSize = 40.sp,
                                modifier = Modifier.width(250.dp)

                            )
                            if(sesso.equals("M")){
                                Image(
                                    painter = painterResource(id = R.drawable.man_technologist_light_skin_tone_svgrepo_com),
                                    contentDescription = "Manager M",
                                    modifier = Modifier
                                        .size(48.dp)
                                )

                            }
                            else{
                                Image(
                                    painter = painterResource(id = R.drawable.woman_technologist_light_skin_tone_svgrepo_com),
                                    contentDescription = "Manager F",
                                    modifier = Modifier
                                        .size(48.dp)
                                )

                            }
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
                                        lerp(Color("#7B6FE9".toColorInt()), Color.White, 0.7f),
                                        //Color("#7B6FE9".toColorInt()).copy(0.2f),
                                        Color("#866FE5".toColorInt())
                                    ),
                                ),
                                shape = RoundedCornerShape(34
                                )
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ){
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxHeight()
                            ){
                                Text("Dipartimento",
                                    textAlign = TextAlign.Center,
                                    fontFamily = computerSaysNo,
                                    fontWeight = FontWeight.W400,
                                    fontSize = 40.sp,
                                    modifier = Modifier
                                        .width(170.dp)
                                )
                                Text("$nome_dipartimento",
                                    textAlign = TextAlign.Center,
                                    fontFamily = computerSaysNo,
                                    fontWeight = FontWeight.W400,
                                    fontSize = 40.sp,
                                    modifier = Modifier
                                        .width(270.dp)
                                )

                            }
                            Image(
                                painter = painterResource(id = R.drawable.office_building_svgrepo_com),
                                contentDescription = "Department",
                                modifier = Modifier
                                    .size(48.dp)
                            )

                        }
                        /*Column(
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
                        }*/
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .scale(scaleInCorso)
                            .shadow(18.dp, RoundedCornerShape(34))
                            .width(358.dp)
                            .height(161.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color("#A56FD9".toColorInt()),
                                        Color("#D06FCA".toColorInt())
                                    ),
                                ),
                                shape = RoundedCornerShape(34
                                )
                            )
                            .clickable(
                                interactionSource = interactionSourceInCorso,
                                indication = null,
                                onClick = {
                                    val taskIntent = Intent(context, ListViewer::class.java)
                                    println("Emaila HomeDipendente $email")
                                    taskIntent.putExtra("token",token)
                                    taskIntent.putExtra("email", email)
                                    taskIntent.putExtra("type", "task_in_corso")
                                    taskIntent.putExtra("tipo", tipo)
                                    context.startActivity(taskIntent)
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Spacer(Modifier.padding(start = 50.dp))
                            Text("Tasks in corso",
                                textAlign = TextAlign.Center,
                                fontFamily = computerSaysNo,
                                fontWeight = FontWeight.W400,
                                fontSize = 40.sp,
                                modifier = Modifier
                                    .width(190.dp)
                            )
                            Image(
                                painter = painterResource(id = R.drawable.gear_svgrepo_com),
                                contentDescription = "List tasks",
                                modifier = Modifier
                                    .size(48.dp)
                            )
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
                                .scale(scaleCompletati)
                                .shadow(8.dp, RoundedCornerShape(34))
                                .width(160.dp)
                                .height(161.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color("#66D161".toColorInt()),
                                            Color("#B2FFB7".toColorInt())
                                        ),
                                    ),
                                    shape = RoundedCornerShape(34
                                    )
                                )
                                .clickable(
                                    interactionSource = interactionSourceCompletati,
                                    indication = null,
                                    onClick = {
                                        val taskIntent = Intent(context, ListViewer::class.java)
                                        taskIntent.putExtra("token",token)
                                        taskIntent.putExtra("email", email)
                                        taskIntent.putExtra("type", "task_completati")
                                        taskIntent.putExtra("tipo", tipo)
                                        context.startActivity(taskIntent)
                                    }
                                )
                        ){
                            Column (
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                                Text("Tasks completati",
                                    textAlign = TextAlign.Center,
                                    fontFamily = computerSaysNo,
                                    fontWeight = FontWeight.W400,
                                    fontSize = 40.sp,
                                    modifier = Modifier
                                        .width(130.dp)
                                )
                                Spacer(Modifier.height(15.dp))
                                Image(
                                    painter = painterResource(id = R.drawable.check_mark_button_svgrepo_com),
                                    contentDescription = "Mark Check",
                                    modifier = Modifier
                                        .size(32.dp)

                                )
                            }
                        }
                        Spacer(Modifier.width(30.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .scale(scaleSospesi)
                                .shadow(8.dp, RoundedCornerShape(34))
                                .width(160.dp)
                                .height(161.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color("#FF850A".toColorInt()),
                                            Color("#FBAB76".toColorInt())
                                        ),
                                    ),
                                    shape = RoundedCornerShape(34
                                    )
                                )
                                .clickable(
                                    interactionSource = interactionSourceSospesi,
                                    indication = null,
                                    onClick = {
                                        val taskIntent = Intent(context, ListViewer::class.java)
                                        taskIntent.putExtra("token",token)
                                        taskIntent.putExtra("email", email)
                                        taskIntent.putExtra("type", "task_sospesi")
                                        taskIntent.putExtra("tipo", tipo)
                                        context.startActivity(taskIntent)
                                    }
                                )
                        ){
                            Column (
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                                Text("Tasks sospesi",
                                    textAlign = TextAlign.Center,
                                    fontFamily = computerSaysNo,
                                    fontWeight = FontWeight.W400,
                                    fontSize = 40.sp,
                                    modifier = Modifier
                                        .width(130.dp)
                                )
                                Spacer(Modifier.height(15.dp))
                                Image(
                                    painter = painterResource(id = R.drawable.red_exclamation_mark_svgrepo_com),
                                    contentDescription = "Exclamation Mark",
                                    modifier = Modifier
                                        .size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
