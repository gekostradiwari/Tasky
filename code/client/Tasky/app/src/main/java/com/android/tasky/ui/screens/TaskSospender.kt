package com.android.tasky.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType.Companion.Date
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.android.tasky.dto.Dipendente
import com.android.tasky.dto.Progetto
import com.android.tasky.ui.theme.computerSaysNo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskSospender : ComponentActivity(){
    override fun onCreate(savedInstanceState : Bundle?){
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

    }
}

@Composable
@Preview
fun TaskSospenderPreview(){
    var testoDescrizione by remember { mutableStateOf("") }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(370.dp)
                .height(400.dp)

        ) {
            Box(
                modifier = Modifier
                    .width(370.dp)
                    .height(200.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color("#FF850A".toColorInt()),
                                Color("#FBAB76".toColorInt()),
                            ),
                        ),
                        shape = RoundedCornerShape(24)
                    ),
                contentAlignment = Alignment.TopStart,

                ) {
                Text(
                    "Motivazione:",
                    textAlign = TextAlign.Start,
                    fontFamily = computerSaysNo,
                    fontWeight = FontWeight.W400,
                    fontSize = 40.sp,
                    modifier = Modifier
                        .width(360.dp)
                        .align(Alignment.Center)
                        .padding(start = 38.dp, bottom = 130.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                Spacer(Modifier.padding(start = 150.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .width(180.dp)
                            .height(250.dp)
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
                        placeholder = { Text("Inserisci una motivazione...") },
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
        Spacer(Modifier.padding(top = 40.dp))
        Button(
            onClick = { /*TODO*/ },
            colors = ButtonColors(
                containerColor = Color("#FF0A0A".toColorInt()),
                contentColor = Color.Black,
                disabledContainerColor = Color("#FF0A0A".toColorInt()),
                disabledContentColor = Color("#FF0A0A".toColorInt())
            ),
            modifier = Modifier
                .width(270.dp)
                .height(63.dp)
        ){
            Text(
                "Sospendi",
                textAlign = TextAlign.Start,
                fontFamily = computerSaysNo,
                fontWeight = FontWeight.W400,
                fontSize = 40.sp,
            )
        }
    }

}

