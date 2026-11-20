package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for the official Mail.tm API (https://api.mail.tm/).
 *
 * Follows API Platform / Hydra JSON-LD conventions:
 * - Collection endpoints return `hydra:member` and `hydra:totalItems`.
 * - PATCH /messages/{id} requires `application/merge-patch+json`.
 * - Authenticated endpoints accept `Authorization: Bearer <jwt>`.
 */
interface MailTmApi {

  @Headers("Accept: application/ld+json")
  @GET("domains")
  suspend fun getDomains(
    @Query("page") page: Int = 1,
  ): Response<HydraCollection<DomainDto>>

  @Headers(
    "Content-Type: application/json",
    "Accept: application/ld+json",
  )
  @POST("accounts")
  suspend fun createAccount(
    @Body request: CredentialsRequest,
  ): Response<AccountDto>

  @Headers(
    "Content-Type: application/json",
    "Accept: application/json",
  )
  @POST("token")
  suspend fun createToken(
    @Body request: CredentialsRequest,
  ): Response<TokenResponse>

  @Headers("Accept: application/ld+json")
  @GET("me")
  suspend fun getMe(
    @Header("Authorization") bearerToken: String,
  ): Response<AccountDto>

  @DELETE("accounts/{id}")
  suspend fun deleteAccount(
    @Header("Authorization") bearerToken: String,
    @Path("id") accountId: String,
  ): Response<Unit>

  @Headers("Accept: application/ld+json")
  @GET("messages")
  suspend fun getMessages(
    @Header("Authorization") bearerToken: String,
    @Query("page") page: Int = 1,
  ): Response<HydraCollection<MessageSummaryDto>>

  @Headers("Accept: application/ld+json")
  @GET("messages/{id}")
  suspend fun getMessageDetail(
    @Header("Authorization") bearerToken: String,
    @Path("id") messageId: String,
  ): Response<MessageDetailDto>

  @Headers(
    "Content-Type: application/merge-patch+json",
    "Accept: application/ld+json",
  )
  @PATCH("messages/{id}")
  suspend fun markMessageSeen(
    @Header("Authorization") bearerToken: String,
    @Path("id") messageId: String,
    @Body body: MarkSeenRequest = MarkSeenRequest(seen = true),
  ): Response<MessageDetailDto>

  @DELETE("messages/{id}")
  suspend fun deleteMessage(
    @Header("Authorization") bearerToken: String,
    @Path("id") messageId: String,
  ): Response<Unit>

  @Headers("Accept: application/ld+json")
  @GET("sources/{id}")
  suspend fun getMessageSource(
    @Header("Authorization") bearerToken: String,
    @Path("id") messageId: String,
  ): Response<SourceDto>
}
