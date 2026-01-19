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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.tasky.R
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor
import androidx.core.graphics.toColorInt
import com.android.tasky.MainActivity
import com.android.tasky.ui.theme.computerSaysNo
import com.android.tasky.utility.SessionManager

class HomeManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val intent = this.intent
        val token = intent.getStringExtra("token")
        val sesso = intent.getStringExtra("sesso")
        val id_dipartimento = intent.getIntExtra("id_dipartimento",0)
        val nome_dipartimento = intent.getStringExtra("nome_dipartimento")
        val nome = intent.getStringExtra("nome")
        val email = intent.getStringExtra("email")
        val tipo = intent.getStringExtra("tipo")
        val sessionManager = SessionManager.getInstance(applicationContext)
        setContent{
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
            HomeManagerActivityPreview(token, sesso, id_dipartimento, nome_dipartimento, nome, sessionManager, email)
            BackHandler {
                showDialog = true
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeManagerActivityPreview(token:String?, sesso:String?, id_dipartimento:Int?, nome_dipartimento:String?, nome:String?, sessionManager:SessionManager, email:String?) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val interactionSourceDipartimento = remember { MutableInteractionSource() }
    val isPressedDipartimento by interactionSourceDipartimento.collectIsPressedAsState()
    val scaleDipartimento by animateFloatAsState(if (isPressedDipartimento) 0.95f else 1f)

    val interactionSourceAddProgetto = remember { MutableInteractionSource() }
    val isPressedAddProgetto by interactionSourceAddProgetto.collectIsPressedAsState()
    val scaleAddProgetto by animateFloatAsState(if (isPressedAddProgetto) 0.95f else 1f)

    val interactionSourceProgetti = remember { MutableInteractionSource() }
    val isPressedProgetti by interactionSourceProgetti.collectIsPressedAsState()
    val scaleProgetti by animateFloatAsState(if (isPressedProgetti) 0.95f else 1f)

    val interactionSourceAddTask = remember { MutableInteractionSource() }
    val isPressedAddTask by interactionSourceAddTask.collectIsPressedAsState()
    val scaleAddTask by animateFloatAsState(if (isPressedAddTask) 0.95f else 1f)

    val interactionSourceSuspendTask = remember { MutableInteractionSource() }
    val isPressedSuspendTask by interactionSourceSuspendTask.collectIsPressedAsState()
    val scaleSuspendTask by animateFloatAsState(if (isPressedSuspendTask) 0.95f else 1f)


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
                        )
                        )
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
                    Text(
                        "Benvenuto!",
                        textAlign = TextAlign.Start,
                        fontFamily = computerSaysNo,
                        fontWeight = FontWeight.W400,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .width(160.dp)
                            .padding(start = 18.dp, top = 10.dp),
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
                                    )
                                ),
                                shape = RoundedCornerShape(34)
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$nome",
                                textAlign = TextAlign.Center,
                                fontFamily = computerSaysNo,
                                fontWeight = FontWeight.W400,
                                fontSize = 40.sp,
                                modifier = Modifier
                                    .width(265.dp),
                            )
                            if(sesso.equals("M")){
                                Image(
                                    painter = painterResource(id = R.drawable.man_in_tuxedo_light_skin_tone_svgrepo_com),
                                    contentDescription = "Manager M",
                                    modifier = Modifier
                                        .size(48.dp)
                                )

                            }
                            else{
                                Image(
                                    painter = painterResource(id = R.drawable.woman_in_tuxedo_light_skin_tone_svgrepo_com),
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
                            .scale(scaleDipartimento)
                            .shadow(18.dp, RoundedCornerShape(34))
                            .width(358.dp)
                            .height(160.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        lerp(Color("#7B6FE9".toColorInt()), Color.White, 0.7f),
                                        Color("#866FE5".toColorInt()),
                                    )
                                ),
                                shape = RoundedCornerShape(34)
                            )
                            .clickable(
                                interactionSource = interactionSourceDipartimento,
                                indication = null,
                                onClick = {
                                    val departmentIntent = Intent(context, DepartmentActivity::class.java)
                                    departmentIntent.putExtra("token", token)
                                    departmentIntent.putExtra("email", email)
                                    departmentIntent.putExtra("id_dipartimento", id_dipartimento)
                                    departmentIntent.putExtra("nome_dipartimento", nome_dipartimento)
                                    context.startActivity(departmentIntent)
                                }
                            )
                    ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ){
                                    Text("Dipartimento\n$nome_dipartimento",
                                        textAlign = TextAlign.Center,
                                        fontFamily = computerSaysNo,
                                        fontWeight = FontWeight.W400,
                                        fontSize = 40.sp,
                                        modifier = Modifier
                                            .width(270.dp)
                                            .padding(start = 27.dp)
                                            )
                                Image(
                                    painter = painterResource(id = R.drawable.office_building_svgrepo_com),
                                    contentDescription = "Department",
                                    modifier = Modifier
                                        .size(48.dp)
                                )

                            }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .scale(scaleAddProgetto)
                            .shadow(18.dp, RoundedCornerShape(34))
                            .width(358.dp)
                            .height(120.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color("#97C65C".toColorInt()),
                                        lerp(Color("#96FF13".toColorInt()), Color.White, 0.5f),
                                    )
                                ),
                                shape = RoundedCornerShape(34)
                            )
                            .clickable(
                                interactionSource = interactionSourceAddProgetto,
                                indication = null,
                                onClick = {
                                    val addProjectIntent = Intent(context, Adder::class.java)
                                    addProjectIntent.putExtra("type", "projectAdder")
                                    addProjectIntent.putExtra("token", token)
                                    addProjectIntent.putExtra("id_dipartimento", id_dipartimento)
                                    context.startActivity(addProjectIntent)
                                }

                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Spacer(Modifier.padding(start = 65.dp))
                            Text("Aggiungi\nProgetto",
                                textAlign = TextAlign.Center,
                                fontFamily = computerSaysNo,
                                fontWeight = FontWeight.W400,
                                fontSize = 40.sp,
                                modifier = Modifier
                                    .width(230.dp)
                            )
                            Image(
                                painter = painterResource(id = R.drawable.triangular_ruler_svgrepo_com),
                                contentDescription = "Add progetto",
                                modifier = Modifier
                                    .size(48.dp)
                            )
                            Spacer(Modifier.padding(end = 30.dp))
                        }
                        /*Column(

                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(Modifier.padding(top = 10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Text("Aggiungi Progetto",
                                    textAlign = TextAlign.Center,
                                    fontFamily = computerSaysNo,
                                    fontWeight = FontWeight.W400,
                                    fontSize = 40.sp,
                                    modifier = Modifier
                                        .width(130.dp)
                                )
                                Image(
                                    painter = painterResource(id = R.drawable.triangular_ruler_svgrepo_com),
                                    contentDescription = "Add progetto",
                                    modifier = Modifier
                                        .size(48.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.Bottom
                            ){
                                IconButton(
                                    onClick = { /*TODO*/ },


                                    ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.next_arrow_forward_svgrepo_com),
                                        contentDescription = "ArrowForward",
                                        modifier = Modifier
                                            .size(48.dp)
                                    )
                                }
                                Spacer(Modifier.padding(end = 18.dp))
                            }
                        }*/
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .scale(scaleProgetti)
                            .shadow(18.dp, RoundedCornerShape(34))
                            .width(358.dp)
                            .height(100.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color("#A56FD9".toColorInt()),
                                        lerp(Color("#D06FCA".toColorInt()), Color.White, 0.5f),
                                    )
                                ),
                                shape = RoundedCornerShape(34)
                            )
                            .clickable(
                                interactionSource = interactionSourceProgetti,
                                indication = null,
                                onClick = {
                                    val listProgettiByMGRIntent = Intent(context, ListViewer::class.java)
                                    listProgettiByMGRIntent.putExtra("token", token)
                                    listProgettiByMGRIntent.putExtra("dipartimento", id_dipartimento)
                                    listProgettiByMGRIntent.putExtra("email", email)
                                    listProgettiByMGRIntent.putExtra("type", "ProgettiByMGR")
                                    listProgettiByMGRIntent.putExtra("tipo", "manager")
                                    context.startActivity(listProgettiByMGRIntent)
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Spacer(Modifier.padding(start = 65.dp))
                            Text("Progetti",
                                textAlign = TextAlign.Center,
                                fontFamily = computerSaysNo,
                                fontWeight = FontWeight.W400,
                                fontSize = 40.sp,
                                modifier = Modifier
                                    .width(225.dp)
                            )
                            Image(
                                painter = painterResource(id = R.drawable.briefcase_svgrepo_com),
                                contentDescription = "List progetto",
                                modifier = Modifier
                                    .size(48.dp)
                            )
                            Spacer(Modifier.padding(end = 30.dp))


                        }
                        /*Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(
                                modifier = Modifier.width(180.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Text("Progetti",
                                    textAlign = TextAlign.Center,
                                    fontFamily = computerSaysNo,
                                    fontWeight = FontWeight.W400,
                                    fontSize = 40.sp,
                                    modifier = Modifier
                                        .width(130.dp)
                                )
                                Image(
                                    painter = painterResource(id = R.drawable.briefcase_svgrepo_com),
                                    contentDescription = "List progetto",
                                    modifier = Modifier
                                        .size(48.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.Bottom
                            ){
                                IconButton(
                                    onClick = { /*TODO*/ },


                                    ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.next_arrow_forward_svgrepo_com),
                                        contentDescription = "ArrowForward",
                                        modifier = Modifier
                                            .size(48.dp)
                                    )
                                }
                                Spacer(Modifier.padding(end = 15.dp))
                            }
                        }*/
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .scale(scaleAddTask)
                                .shadow(18.dp, RoundedCornerShape(34))
                                .width(160.dp)
                                .height(161.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color("#66D161".toColorInt()),
                                            Color("#B2FFB7".toColorInt()),
                                        )
                                    ),
                                    shape = RoundedCornerShape(34)
                                )
                                .clickable(
                                    interactionSource = interactionSourceAddTask,
                                    indication = null,
                                    onClick = {
                                        val listProgettiIntent = Intent(context, ListViewer::class.java)
                                        listProgettiIntent.putExtra("token", token)
                                        listProgettiIntent.putExtra("dipartimento", id_dipartimento)
                                        listProgettiIntent.putExtra("email", email)
                                        listProgettiIntent.putExtra("type", "progetti")
                                        listProgettiIntent.putExtra("tipo", "manager")
                                        listProgettiIntent.putExtra("adding",true)
                                        context.startActivity(listProgettiIntent)
                                    }

                                )
                        ){
                            Column (
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                                Text("Aggiungi task",
                                    textAlign = TextAlign.Center,
                                    fontFamily = computerSaysNo,
                                    fontWeight = FontWeight.W400,
                                    fontSize = 40.sp,
                                    modifier = Modifier
                                        .width(130.dp)
                                )
                                Spacer(Modifier.height(15.dp))
                                Image(
                                    painter = painterResource(id = R.drawable.puzzle_piece_svgrepo_com),
                                    contentDescription = "Puzzle",
                                    modifier = Modifier
                                        .size(32.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(30.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .scale(scaleSuspendTask)
                                .shadow(18.dp, RoundedCornerShape(34))
                                .width(160.dp)
                                .height(161.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color("#FF850A".toColorInt()),
                                            Color("#FBAB76".toColorInt()),
                                        )
                                    ),
                                    shape = RoundedCornerShape(34)
                                )
                                .clickable(
                                    interactionSource = interactionSourceSuspendTask,
                                    indication = null,
                                    onClick ={
                                        val listProgettiIntent = Intent(context, ListViewer::class.java)
                                        listProgettiIntent.putExtra("token", token)
                                        listProgettiIntent.putExtra("dipartimento", id_dipartimento)
                                        listProgettiIntent.putExtra("email", email)
                                        listProgettiIntent.putExtra("type", "progetti")
                                        listProgettiIntent.putExtra("tipo", "manager")
                                        listProgettiIntent.putExtra("suspend",true)
                                        context.startActivity(listProgettiIntent)
                                    }
                                )
                        ){
                            Column (
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                                Text("Sospendi task",
                                    textAlign = TextAlign.Center,
                                    fontFamily = computerSaysNo,
                                    fontWeight = FontWeight.W400,
                                    fontSize = 40.sp,
                                    modifier = Modifier
                                        .width(130.dp)
                                )
                                Spacer(Modifier.height(15.dp))
                                Image(
                                    painter = painterResource(id = R.drawable.exclamation_question_mark_svgrepo_com),
                                    contentDescription = "task suspend",
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
