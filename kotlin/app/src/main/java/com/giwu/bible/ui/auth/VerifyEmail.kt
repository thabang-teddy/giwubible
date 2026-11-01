package com.giwu.bible.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.giwu.bible.repo.AuthStore
import com.giwu.bible.ui.theme.giwu
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** What came of asking for another verification link. */
sealed interface ResendState {
    data object Idle : ResendState
    data object Sending : ResendState
    data object Sent : ResendState
    data object AlreadyVerified : ResendState
    data class Failed(val message: String) : ResendState
}

/**
 * Asking for a verification link, and what came of it.
 *
 * The link itself always opens the web app — signing a URL is the server's
 * business — so nothing here can confirm an address. What it can do is get a
 * fresh link sent and notice when the reader has already followed one
 * somewhere else, which [AuthStore.resendVerificationEmail] reports.
 */
@Stable
class EmailResend internal constructor(
    private val auth: AuthStore,
    private val scope: CoroutineScope,
) {
    var state: ResendState by mutableStateOf(ResendState.Idle)
        private set

    val isSending: Boolean get() = state is ResendState.Sending

    /** True once the server has said the address is confirmed after all. */
    val isSettled: Boolean get() = state is ResendState.AlreadyVerified

    fun send() {
        if (isSending || isSettled) return
        state = ResendState.Sending
        scope.launch {
            state = try {
                if (auth.resendVerificationEmail()) {
                    ResendState.AlreadyVerified
                } else {
                    ResendState.Sent
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ResendState.Failed(e.message ?: "Could not send the link. Please try again.")
            }
        }
    }
}

@Composable
fun rememberEmailResend(auth: AuthStore): EmailResend {
    val scope = rememberCoroutineScope()
    return remember(auth, scope) { EmailResend(auth, scope) }
}

/** The status line under a resend button: what just happened, if anything. */
@Composable
private fun ResendStatus(state: ResendState) {
    when (state) {
        is ResendState.Sent -> {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Sent. It can take a minute to arrive.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.giwu.muted,
            )
        }

        is ResendState.Failed -> {
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        else -> Unit
    }
}

/** The label a resend button carries in each state. */
private fun resendLabel(resend: EmailResend): String = when {
    resend.isSettled -> "Already confirmed"
    resend.isSending -> "Sending…"
    else -> "Send the link again"
}

/** Explains why a bookmark was refused, and offers another link. */
@Composable
fun VerifyEmailDialog(
    auth: AuthStore,
    email: String,
    onDismiss: () -> Unit,
) {
    val resend = rememberEmailResend(auth)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm your email") },
        text = {
            Column {
                Text(
                    text = if (resend.isSettled) {
                        "Your email is already confirmed — try that bookmark again."
                    } else {
                        "Bookmarks are saved to your Giwu account, so we need to know " +
                            "the address is yours. Open the link we sent to $email."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                ResendStatus(resend.state)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Reading needs no account, so nothing else is held up by this.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.giwu.muted,
                )
            }
        },
        confirmButton = {
            if (resend.isSettled) {
                TextButton(onClick = onDismiss) { Text("Done") }
            } else {
                TextButton(onClick = resend::send, enabled = !resend.isSending) {
                    Text(resendLabel(resend))
                }
            }
        },
        dismissButton = {
            if (!resend.isSettled) {
                TextButton(onClick = onDismiss) { Text("Not now") }
            }
        },
    )
}

/**
 * The notice that replaces the sign-up form once an account exists.
 *
 * Carrying on into the reader is the primary action: the account is created
 * and usable, and only bookmarks wait on the link.
 */
@Composable
fun RegisteredNotice(
    auth: AuthStore,
    email: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resend = rememberEmailResend(auth)

    Column(modifier = modifier) {
        Text(
            text = "Confirm your email",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your account is ready. Open the link we sent to $email and your " +
                "bookmarks will follow you to any device.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Reading needs no account at all, so you can start now and confirm later.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.giwu.muted,
        )

        ResendStatus(resend.state)

        Spacer(Modifier.height(20.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Start reading")
        }
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = resend::send,
            // Nothing left to send once the server has confirmed the
            // address, so the button stops being an action.
            enabled = !resend.isSending && !resend.isSettled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(resendLabel(resend))
        }
    }
}
