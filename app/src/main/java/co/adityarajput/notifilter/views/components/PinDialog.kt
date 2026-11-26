package co.adityarajput.notifilter.views.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import co.adityarajput.notifilter.R
import co.adityarajput.notifilter.utils.PinLock
import co.adityarajput.notifilter.utils.isValidPin

/**
 * Dijalog za postavljanje PIN-a (nova vrijednost + potvrda) ili,
 * kad je [removing] true, za uklanjanje postojeceg PIN-a (unos trenutnog).
 *
 * [onResult] vraca true ako je akcija uspjela (PIN postavljen/uklonjen).
 */
@Composable
fun PinDialog(removing: Boolean, onDismiss: () -> Unit, onResult: (Boolean) -> Unit) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }

    fun onlyDigits(value: String) = value.length <= 6 && value.all { it.isDigit() }

    fun confirm() {
        if (removing) {
            if (PinLock.verify(first)) {
                PinLock.clearPin()
                onResult(true)
            } else {
                error = R.string.wrong_pin
            }
            return
        }

        if (!first.isValidPin()) {
            error = R.string.pin_invalid
            return
        }
        if (first != second) {
            error = R.string.pin_mismatch
            return
        }
        PinLock.setPin(first)
        onResult(true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (removing) R.string.remove_pin_title else R.string.set_pin_title,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))) {
                OutlinedTextField(
                    value = first,
                    onValueChange = { if (onlyDigits(it)) { first = it; error = null } },
                    singleLine = true,
                    isError = error != null,
                    label = {
                        Text(
                            stringResource(
                                if (removing) R.string.pin_current_hint else R.string.pin_hint,
                            ),
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                if (!removing) {
                    OutlinedTextField(
                        value = second,
                        onValueChange = { if (onlyDigits(it)) { second = it; error = null } },
                        singleLine = true,
                        isError = error != null,
                        label = { Text(stringResource(R.string.pin_confirm_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                }
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = { confirm() }) {
                Text(stringResource(if (removing) R.string.remove else R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
