package com.android.tasky.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.android.tasky.R
import com.android.tasky.ui.theme.computerSaysNo

class InfoViewer : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun infoTask(/*Qua bisogna passare la task di cui si vuole visualizzare le informazioni*/) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(358.dp)
                .height(92.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color("#66D161".toColorInt()),
                            Color("#B2FFB7".toColorInt()),
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
                var statoTask =
                    "Completato" // Quando si avrà la task mettere direttamente task.stato in mutableStateOf
                var statoAttuale by remember { mutableStateOf(statoTask) }
                var isExpanded by remember { mutableStateOf(false) }
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
                    /*ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sospesa") },
                            onClick = {
                                statoAttuale = "Sospesa"
                                //Impostare lo stato della task uguale a quello di stato attuale
                                isExpanded = false
                            },
                        )
                    }*/
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
                            colors = listOf(
                                Color("#66D161".toColorInt()),
                                Color("#B2FFB7".toColorInt()),
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
                            colors = listOf(
                                Color("#66D161".toColorInt()),
                                Color("#B2FFB7".toColorInt()),
                            ),
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
                    value = "Data Inizio", //Qui ci va sempre Task.stato,
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
                            colors = listOf(
                                Color("#66D161".toColorInt()),
                                Color("#B2FFB7".toColorInt()),
                            ),
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
                    value = "Data Fine", //Qui ci va sempre Task.stato,
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
fun infoProgetto(/*Qui deve essere passato il progetto di cui si vogliono visualizzare le informazioni*/){
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
    ){
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
            Text("Budget: 100€", //qui va inserito il budget che è stato istanziato per il progetto
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
        }
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
            Text("Nome: Revolution", //qui va inserito il nome del progetto
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
            Text("Data inizio: 10/10/10", //qui va inserito il nome del progetto
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
            Text("Data fine: 10/10/10", //qui va inserito il nome del progetto
                textAlign = TextAlign.Center,
                fontFamily = computerSaysNo,
                fontWeight = FontWeight.W400,
                fontSize = 40.sp,
                modifier = Modifier.fillMaxSize().padding(top=16.dp)
            )

        }
    }
}
