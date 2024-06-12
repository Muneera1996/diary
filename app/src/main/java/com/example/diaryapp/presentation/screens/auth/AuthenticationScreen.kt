package com.example.diaryapp.presentation.screens.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.diaryapp.utils.Constant.CLIENT_ID
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.stevdzasan.messagebar.ContentWithMessageBar
import com.stevdzasan.messagebar.MessageBarState
import com.stevdzasan.onetap.OneTapSignInState
import com.stevdzasan.onetap.OneTapSignInWithGoogle

@ExperimentalMaterial3Api
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AuthenticationScreen(
    authenticated: Boolean,
    loadingState: Boolean,
    oneTapState: OneTapSignInState,
    messageBarState: MessageBarState,
    onButtonClicked: () -> Unit,
    onSuccessfulFirebaseSignIn: (String) -> Unit,
    onFailedFirebaseSignIn: (Exception) -> Unit,
    onDialogDismissed: (String) -> Unit,
    navigateToHome: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
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
                        .setServerClientId(CLIENT_ID)
                        .setFilterByAuthorizedAccounts(false)
                        .build()
                )
                .setAutoSelectEnabled(true)
                .build()


        }

        // Register the callback for One Tap sign-in result
        val oneTapResultLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                    val tokenId = credential.googleIdToken
                    Log.d("OneTapSignIn", "ID Token: $tokenId")
                    val fbCredentials = GoogleAuthProvider.getCredential(tokenId, null)
                    FirebaseAuth.getInstance().signInWithCredential(fbCredentials)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                if (tokenId != null) {
                                    onSuccessfulFirebaseSignIn(tokenId)
                                }
                            } else {
                                task.exception?.let { it -> onFailedFirebaseSignIn(it) }
                            }
                        }
                } catch (e: ApiException) {
                    Log.e("OneTapSignIn", "Error: ${e.localizedMessage}")
                    onFailedFirebaseSignIn(e)
                }
            } else {
                Log.e("OneTapSignIn", "Sign-in canceled")
            }
        }
        ContentWithMessageBar(messageBarState = messageBarState) {
            AuthenticationContent(
                loadingState = loadingState,
                onButtonClicked = {
                    oneTapClient.beginSignIn(signInRequest)
                        .addOnSuccessListener { result ->
                            try {
                                oneTapResultLauncher.launch(
                                    IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                                )
                            } catch (e: Exception) {
                                Log.e("OneTapSignIn", "Error: ${e.localizedMessage}")
                                onFailedFirebaseSignIn(e)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("OneTapSignIn", "Error: ${e.localizedMessage}")
                            onFailedFirebaseSignIn(e)
                        }

                }
            )
        }
    }




//    OneTapSignInWithGoogle(
//        state = oneTapState,
//        clientId = CLIENT_ID,
//        onTokenIdReceived = { tokenId ->
//            Log.e("OneTap", tokenId)
//            val credential = GoogleAuthProvider.getCredential(tokenId, null)
//            FirebaseAuth.getInstance().signInWithCredential(credential)
//                .addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        onSuccessfulFirebaseSignIn(tokenId)
//                    } else {
//                        task.exception?.let { it -> onFailedFirebaseSignIn(it) }
//                    }
//                }
//        },
//        onDialogDismissed = { message ->
//            Log.e("OneTap", message)
//
//            onDialogDismissed(message)
//        }
//    )

    LaunchedEffect(key1 = authenticated) {
        if (authenticated) {
            navigateToHome()
        }
    }
}