package co.adityarajput.notifilter.views.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import co.adityarajput.notifilter.R
import co.adityarajput.notifilter.utils.PinLock

/**
 * Zaslon koji se prikazuje umjesto sadrzaja dok je aplikacija zakljucana.
 * Poziva [onUnlock] kad je unesen ispravan PIN.
 */
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun submit() {
        if (PinLock.verify(pin)) {
            error = false
            onUnlock()
        } else {
            error = true
            pin = ""
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.padding_large)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.lock_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.padding_large)))
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                        pin = it
                        error = false
                    }
                },
                singleLine = true,
                isError = error,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                supportingText = if (error) {
                    { Text(stringResource(R.string.wrong_pin)) }
                } else null,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.padding_medium)))
            Button(
                onClick = { submit() },
                enabled = pin.length == 4 || pin.length == 6,
            ) {
                Text(stringResource(R.string.unlock))
            }
        }
    }
}
