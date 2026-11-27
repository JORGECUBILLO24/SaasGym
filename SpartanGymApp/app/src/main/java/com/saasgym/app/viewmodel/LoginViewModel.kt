package com.saasgym.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.saasgym.app.network.LoginRequest
import com.saasgym.app.network.PasswordRecoveryRequest
import com.saasgym.app.network.RetrofitClient
import com.saasgym.app.network.SessionStore
import com.saasgym.app.network.SesionGuardada
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var loginSuccess by mutableStateOf(false)
    var recoveryMessage by mutableStateOf<String?>(null)

    var userRole by mutableStateOf("")
    var userEmail by mutableStateOf("")
    var userId by mutableStateOf("")

    fun iniciarSesion(email: String, pass: String, gimnasioSlug: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            loginSuccess = false

            try {
                RetrofitClient.setGimnasioSlug(gimnasioSlug)
                val response = RetrofitClient.apiService.login(
                    LoginRequest(
                        email = email.trim(),
                        password = pass,
                        gimnasioSlug = gimnasioSlug.trim().lowercase()
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    val role = authResponse.rol.orEmpty().uppercase().removePrefix("ROLE_")
                    if (role !in setOf("SOCIO", "ENTRENADOR")) {
                        RetrofitClient.setAuthToken(null)
                        errorMessage = "Esta cuenta debe ingresar desde el panel web. La app móvil es para socios y entrenadores."
                        return@launch
                    }

                    userEmail = authResponse.email ?: email.trim()
                    userRole = authResponse.rol.orEmpty()
                    userId = authResponse.id.orEmpty()
                    RetrofitClient.setAuthToken(authResponse.token)
                    SessionStore.guardar(
                        getApplication(),
                        SesionGuardada(
                            token = authResponse.token.orEmpty(),
                            email = userEmail,
                            role = userRole,
                            userId = userId
                        )
                    )

                    loginSuccess = true
                } else {
                    errorMessage = "Credenciales incorrectas."
                }
            } catch (e: Exception) {
                errorMessage = "No se pudo conectar con la API: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun solicitarRecuperacion(email: String, gimnasioSlug: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            recoveryMessage = null

            try {
                RetrofitClient.setGimnasioSlug(gimnasioSlug)
                val response = RetrofitClient.apiService.solicitarRestablecimiento(
                    PasswordRecoveryRequest(email = email.trim())
                )

                if (response.isSuccessful) {
                    recoveryMessage = "Hemos enviado un correo de recuperacion."
                } else {
                    errorMessage = "No se pudo solicitar la recuperacion."
                }
            } catch (e: Exception) {
                errorMessage = "No se pudo conectar con la API: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun limpiarEstado() {
        loginSuccess = false
        errorMessage = null
        recoveryMessage = null
    }
}
