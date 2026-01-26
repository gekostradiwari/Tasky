package com.android.tasky

import android.app.ActivityOptions
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Label
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.android.tasky.ui.screens.HomeDipendenteActivity
import com.android.tasky.ui.screens.HomeManagerActivity
import com.android.tasky.ui.screens.LoginActivity
import com.android.tasky.ui.theme.TaskyTheme
import com.android.tasky.utility.RetrofitInstance
import com.android.tasky.utility.RetrofitInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.internal.wait

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        var token:String? = ""
        var type:String? = ""
        var nome:String? = ""
        var sesso:String? = ""
        var dipartimento:Int? = null
        var nome_dipartimento:String? = ""
        var email:String? = ""

        var launcher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
            type = result.data?.getStringExtra("type")
            token = result.data?.getStringExtra("token")
            sesso = result.data?.getStringExtra("sesso")
            dipartimento = result.data?.getIntExtra("id_dipartimento",0)
                nome_dipartimento = result.data?.getStringExtra("nome_dipartimento")
                nome = result.data?.getStringExtra("nome")
                email = result.data?.getStringExtra("email")

                if(type.equals("Dipendente")){
                    val DipendenteIntent = Intent(this, HomeDipendenteActivity::class.java)
                    DipendenteIntent.putExtra("token",token)
                    DipendenteIntent.putExtra("sesso",sesso)
                    DipendenteIntent.putExtra("id_dipartimento",dipartimento)
                    DipendenteIntent.putExtra("nome_dipartimento",nome_dipartimento)
                    DipendenteIntent.putExtra("nome",nome)
                    DipendenteIntent.putExtra("email",email)
                    DipendenteIntent.putExtra("tipo", "dipendente")
                    this.startActivity(DipendenteIntent)
                }
                else if(type.equals("Manager")){
                    val ManagerIntent = Intent(this, HomeManagerActivity::class.java)
                    ManagerIntent.putExtra("token",token)
                    ManagerIntent.putExtra("sesso",sesso)
                    ManagerIntent.putExtra("id_dipartimento",dipartimento)
                    ManagerIntent.putExtra("nome_dipartimento",nome_dipartimento)
                    ManagerIntent.putExtra("nome",nome)
                    ManagerIntent.putExtra("email",email)
                    ManagerIntent.putExtra("tipo", "manager")
                    this.startActivity(ManagerIntent)

                }
                else{
                    println("Errore generico")
                }
            }else {
                finish()
            }
        }
        val LoginIntent = Intent(this, LoginActivity::class.java)
        LoginIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        launcher.launch(LoginIntent)
        /*
        In base al tipo di utente, lanciare l'activity parametrizzata con il parametro token
        ricevuto dall'activity di login.
        * */

    }
}



@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TaskyTheme {
        Greeting("Android")
    }
}
