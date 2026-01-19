package com.android.tasky.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.android.tasky.R
import com.android.tasky.ui.theme.computerSaysNo

class DepartmentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val intent = this.intent
        val token = intent.getStringExtra("token")
        val dipartimento = intent.getIntExtra("id_dipartimento",0)
        val email = intent.getStringExtra("email")
        val nome_dipartimento = intent.getStringExtra("nome_dipartimento")
        setContent {
            val context = LocalContext.current

            val interactionSourceMembri = remember { MutableInteractionSource() }
            val isPressedMembri by interactionSourceMembri.collectIsPressedAsState()
            val scaleMembri by animateFloatAsState(if (isPressedMembri) 0.95f else 1f)

            val interactionSourceProgetti = remember { MutableInteractionSource() }
            val isPressedProgetti by interactionSourceProgetti.collectIsPressedAsState()
            val scaleProgetti by animateFloatAsState(if (isPressedProgetti) 0.95f else 1f)

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
                        IconButton(onClick = { ( context as? Activity)?.finish() }
                        ) {
                            Icon(Icons.Default.ArrowBack, "TurnBack")
                        }
                        Image(
                            painter = painterResource(id = R.drawable.taskyfinalnobackground),
                            contentDescription = "Logo Tasky",
                            modifier = Modifier
                                .size(74.dp),
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
                content = { paddingValues ->
                    Column(
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize(),
                    ) {
                        Text(
                            "Dipartimento",
                            fontFamily = computerSaysNo,
                            fontWeight = FontWeight.W400,
                            fontSize = 40.sp,
                            modifier = Modifier.padding(0.dp, 20.dp, 0.dp, 0.dp)
                        )
                        Text(
                            "$nome_dipartimento",
                            fontFamily = computerSaysNo,
                            fontWeight = FontWeight.W400,
                            fontSize = 40.sp,
                            modifier = Modifier.padding(0.dp, 20.dp, 0.dp, 0.dp)
                        )
                        Spacer(Modifier.padding(top = 100.dp))
                        Button(
                            interactionSource = interactionSourceMembri,
                            onClick = {
                                val listMembriIntent = Intent(context, ListViewer::class.java)
                                listMembriIntent.putExtra("token", token)
                                listMembriIntent.putExtra("dipartimento", dipartimento)
                                listMembriIntent.putExtra("email", email)
                                listMembriIntent.putExtra("type", "dipendenti")
                                listMembriIntent.putExtra("tipo", "manager")
                                context.startActivity(listMembriIntent)
                            },
                            modifier = Modifier
                                .shadow(18.dp, RoundedCornerShape(34))
                                .width(327.dp)
                                .height(155.dp)
                                .scale(scaleMembri),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp),


                            ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color("#7B6FE9".toColorInt()),
                                                lerp(Color("#7B6FE9".toColorInt()), Color.White, 0.6f),
                                            )
                                        ),
                                        shape = RoundedCornerShape(34)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Visualizza Membri",
                                            fontFamily = computerSaysNo,
                                            fontWeight = FontWeight.W400,
                                            fontSize = 40.sp,
                                            color = Color.Black,
                                            modifier = Modifier.padding(0.dp, 20.dp, 0.dp, 0.dp)
                                        )

                                    }
                                    Spacer(Modifier.padding(top = 10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.man_technologist_light_skin_tone_svgrepo_com),
                                            contentDescription = "Manager M",
                                            modifier = Modifier
                                                .size(48.dp)
                                        )
                                        Spacer(Modifier.padding(start = 10.dp))
                                        Image(
                                            painter = painterResource(id = R.drawable.woman_technologist_light_skin_tone_svgrepo_com),
                                            contentDescription = "Manager M",
                                            modifier = Modifier
                                                .size(48.dp)
                                        )
                                    }
                                }
                            }

                        }
                        Spacer(Modifier.padding(top = 30.dp))

                        Button(
                            interactionSource = interactionSourceProgetti,
                            onClick = {
                                val listProgettiIntent = Intent(context, ListViewer::class.java)
                                listProgettiIntent.putExtra("token", token)
                                listProgettiIntent.putExtra("dipartimento", dipartimento)
                                listProgettiIntent.putExtra("email", email)
                                listProgettiIntent.putExtra("type", "progetti")
                                listProgettiIntent.putExtra("tipo", "manager")
                                context.startActivity(listProgettiIntent)
                            },
                            modifier = Modifier
                                .scale(scaleProgetti)
                                .shadow(18.dp, RoundedCornerShape(34))
                                .width(327.dp)
                                .height(155.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp),


                            ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color("#7B6FE9".toColorInt()),
                                                lerp(Color("#7B6FE9".toColorInt()), Color.White, 0.6f),
                                            )
                                        ),
                                        shape = RoundedCornerShape(34)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp)

                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Visualizza Progetti",
                                            fontFamily = computerSaysNo,
                                            fontWeight = FontWeight.W400,
                                            fontSize = 40.sp,
                                            color = Color.Black,
                                            modifier = Modifier.padding(0.dp, 20.dp, 0.dp, 0.dp)
                                        )

                                    }
                                    Spacer(Modifier.padding(top = 10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.briefcase_svgrepo_com),
                                            contentDescription = "Manager M",
                                            modifier = Modifier
                                                .size(48.dp)
                                        )
                                        Spacer(Modifier.padding(start = 10.dp))
                                        Image(
                                            painter = painterResource(id = R.drawable.triangular_ruler_svgrepo_com),
                                            contentDescription = "Manager M",
                                            modifier = Modifier
                                                .size(48.dp)
                                        )
                                    }
                                }
                            }

                        }

                    }
                }
            )
        }
    }


}