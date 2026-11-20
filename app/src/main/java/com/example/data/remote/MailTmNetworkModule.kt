package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object MailTmNetworkModule {

  fun createMoshi(): Moshi =
    Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()

  fun createOkHttpClient(): OkHttpClient {
    val logging = HttpLoggingInterceptor().apply {
      level = if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor.Level.BASIC
      } else {
        HttpLoggingInterceptor.Level.NONE
      }
    }
    return OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(15, TimeUnit.SECONDS)
      .addInterceptor(logging)
      .build()
  }

  fun createApi(
    baseUrl: String = BuildConfig.MAIL_TM_BASE_URL,
    client: OkHttpClient = createOkHttpClient(),
    moshi: Moshi = createMoshi(),
  ): MailTmApi =
    Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(client)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(MailTmApi::class.java)
}
