package co.adityarajput.notifilter.utils

import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.adityarajput.notifilter.services.Preferences
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Jednostavno zakljucavanje aplikacije PIN-om.
 *
 * PIN se ne sprema u citljivom obliku - cuva se samo salted SHA-256 hash
 * (hash + nasumicna sol) u SharedPreferences. Zakljucavanje je ukljuceno
 * cim [Preferences.pinHash] nije null.
 */
object PinLock {
    /** Tvornicki (hard-kodirani) PIN koji se postavlja pri prvoj instalaciji. */
    const val DEFAULT_PIN = "092013"

    /**
     * Je li aplikacija trenutno zakljucana (ceka unos PIN-a).
     * Compose promatra ovo stanje i sam ponovno iscrta zaslon.
     */
    var locked by mutableStateOf(false)

    /** Je li zakljucavanje PIN-om uopce postavljeno. */
    val isEnabled: Boolean
        get() = Preferences.pinHash != null

    /** Postavlja (ili mijenja) PIN. */
    fun setPin(pin: String) {
        val salt = newSalt()
        Preferences.pinSalt = salt
        Preferences.pinHash = hash(pin, salt)
    }

    /** Uklanja PIN i otkljucava aplikaciju. */
    fun clearPin() {
        Preferences.pinHash = null
        Preferences.pinSalt = null
        locked = false
    }

    /** Vraca true ako uneseni PIN odgovara spremljenom. */
    fun verify(pin: String): Boolean {
        val salt = Preferences.pinSalt ?: return false
        val expected = Preferences.pinHash ?: return false
        return constantTimeEquals(expected, hash(pin, salt))
    }

    /**
     * Pri prvom pokretanju postavlja tvornicki PIN [DEFAULT_PIN].
     * Izvrsava se samo jednom - ako korisnik kasnije promijeni ili ukloni
     * PIN, zadani se vise ne vraca.
     */
    fun seedDefaultPinIfNeeded() {
        if (Preferences.defaultPinSeeded) return
        if (!isEnabled) setPin(DEFAULT_PIN)
        Preferences.defaultPinSeeded = true
    }

    /**
     * Zakljucava aplikaciju ako je PIN postavljen.
     * Poziva se pri pokretanju i svakom odlasku u pozadinu.
     */
    fun lockIfEnabled() {
        if (isEnabled) locked = true
    }

    private fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256").apply {
            update(Base64.decode(salt, Base64.NO_WRAP))
        }.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }
}

/** Dopusteni PIN je 4 ili 6 znamenki. */
fun String.isValidPin(): Boolean = (length == 4 || length == 6) && all { it.isDigit() }
