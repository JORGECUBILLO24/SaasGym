package com.saasgym.app.network

import android.content.Context
import com.saasgym.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private var authToken: String? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun setAuthToken(token: String?) {
        authToken = token
        if (token.isNullOrBlank()) {
            appContext?.let(SessionStore::limpiar)
        }
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val token = authToken
                val builder: Request.Builder = chain.request().newBuilder()
                    // Pide el formato de error RFC 9457; ver mensajeDeError().
                    .header("Accept", ACCEPT_ERRORES)
                if (!token.isNullOrBlank()) {
                    builder.addHeader("Authorization", "Bearer $token")
                }
                val response = chain.proceed(builder.build())
                if (response.code == 401 && !token.isNullOrBlank()) {
                    authToken = null
                    appContext?.let(SessionStore::limpiar)
                }
                response
            })
            .build()
    }

    val apiService: SaasGymApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create()) // Convierte de JSON a Kotlin automatico
            .build()
            .create(SaasGymApi::class.java)
    }
}
