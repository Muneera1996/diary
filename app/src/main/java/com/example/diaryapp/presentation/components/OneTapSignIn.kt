package com.example.diaryapp.presentation.components

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.diaryapp.utils.Constant
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import androidx.activity.compose.rememberLauncherForActivityResult
import android.app.Activity.RESULT_OK

@Composable
fun SignInScreen(context: Context) {
    val oneTapClient = remember { Identity.getSignInClient(context) }
    val signInRequest = remember {
        BeginSignInRequest.builder()
            .setPasswordRequestOptions(
                BeginSignInRequest.PasswordRequestOptions.builder()
                    .setSupported(true)
                    .build()
            )
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(Constant.CLIENT_ID)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()


    }

    var signInStatus by remember { mutableStateOf("Not Signed In") }

    // Register the callback for One Tap sign-in result
    val oneTapResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken
                signInStatus = "Signed in as: ${credential.displayName}"
                Log.d("OneTapSignIn", "ID Token: $idToken")
            } catch (e: ApiException) {
                signInStatus = "Sign-in failed: ${e.localizedMessage}"
                Log.e("OneTapSignIn", "Error: ${e.localizedMessage}")
            }
        } else {
            signInStatus = "Sign-in canceled"
        }
    }

    oneTapClient.beginSignIn(signInRequest)
        .addOnSuccessListener { result ->
            try {
                oneTapResultLauncher.launch(
                    IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                )
            } catch (e: Exception) {
                signInStatus = "Error launching One Tap: ${e.localizedMessage}"
                Log.e("OneTapSignIn", "Error: ${e.localizedMessage}")
            }
        }
        .addOnFailureListener { e ->
            signInStatus = "Sign-in failed: ${e.localizedMessage}"
            Log.e("OneTapSignIn", "Error: ${e.localizedMessage}")
        }

}
