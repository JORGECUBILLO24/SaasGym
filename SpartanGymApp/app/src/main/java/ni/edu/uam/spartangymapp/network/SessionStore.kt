package ni.edu.uam.spartangymapp.network

import android.content.Context
import android.util.Base64
import org.json.JSONObject

data class SesionGuardada(
    val token: String,
    val email: String,
    val role: String,
    val userId: String
)

object SessionStore {
    private const val PREFERENCES = "gym_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_EMAIL = "email"
    private const val KEY_ROLE = "role"
    private const val KEY_USER_ID = "user_id"

    fun guardar(context: Context, sesion: SesionGuardada) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putString(KEY_TOKEN, sesion.token)
            .putString(KEY_EMAIL, sesion.email)
            .putString(KEY_ROLE, sesion.role)
            .putString(KEY_USER_ID, sesion.userId)
            .apply()
    }

    fun cargar(context: Context): SesionGuardada? {
        val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)?.trim().orEmpty()
        if (token.isBlank() || tokenExpirado(token)) {
            limpiar(context)
            return null
        }
        return SesionGuardada(
            token = token,
            email = prefs.getString(KEY_EMAIL, null).orEmpty(),
            role = prefs.getString(KEY_ROLE, null).orEmpty(),
            userId = prefs.getString(KEY_USER_ID, null).orEmpty()
        )
    }

    fun limpiar(context: Context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun tokenExpirado(token: String): Boolean {
        return try {
            val partes = token.split('.')
            if (partes.size != 3) return true
            val payload = String(
                Base64.decode(partes[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                Charsets.UTF_8
            )
            val exp = JSONObject(payload).optLong("exp", 0L)
            exp > 0L && exp <= System.currentTimeMillis() / 1000L
        } catch (_: Exception) {
            true
        }
    }
}
