package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Mail.tm JSON-LD / Hydra collection envelope returned by GET /domains and GET /messages.
 */
@JsonClass(generateAdapter = true)
data class HydraCollection<T>(
  @Json(name = "@context") val context: String? = null,
  @Json(name = "@id") val hydraId: String? = null,
  @Json(name = "@type") val type: String? = null,
  @Json(name = "hydra:member") val members: List<T> = emptyList(),
  @Json(name = "hydra:totalItems") val totalItems: Int = 0,
)

@JsonClass(generateAdapter = true)
data class DomainDto(
  val id: String,
  val domain: String,
  val isActive: Boolean = true,
  val isPrivate: Boolean = false,
  val createdAt: String = "",
  val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class CredentialsRequest(
  val address: String,
  val password: String,
)

@JsonClass(generateAdapter = true)
data class AccountDto(
  val id: String,
  val address: String,
  val quota: Long = 40_000_000L,
  val used: Long = 0L,
  val isDisabled: Boolean = false,
  val isDeleted: Boolean = false,
  val createdAt: String = "",
  val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class TokenResponse(
  val id: String,
  val token: String,
)

@JsonClass(generateAdapter = true)
data class EmailAddressDto(
  val address: String = "",
  val name: String = "",
)

@JsonClass(generateAdapter = true)
data class MessageSummaryDto(
  val id: String,
  val accountId: String = "",
  val msgid: String = "",
  val from: EmailAddressDto = EmailAddressDto(),
  val to: List<EmailAddressDto> = emptyList(),
  val subject: String = "",
  val intro: String = "",
  val seen: Boolean = false,
  val isDeleted: Boolean = false,
  val hasAttachments: Boolean = false,
  val size: Long = 0L,
  val downloadUrl: String = "",
  val createdAt: String = "",
  val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class AttachmentDto(
  val id: String = "",
  val filename: String = "",
  val contentType: String = "",
  val disposition: String = "",
  val size: Long = 0L,
  val downloadUrl: String = "",
)

@JsonClass(generateAdapter = true)
data class MessageDetailDto(
  val id: String,
  val accountId: String = "",
  val msgid: String = "",
  val from: EmailAddressDto = EmailAddressDto(),
  val to: List<EmailAddressDto> = emptyList(),
  val cc: List<String> = emptyList(),
  val bcc: List<String> = emptyList(),
  val subject: String = "",
  val intro: String = "",
  val seen: Boolean = false,
  val isDeleted: Boolean = false,
  val hasAttachments: Boolean = false,
  val attachments: List<AttachmentDto> = emptyList(),
  val text: String? = null,
  val html: List<String> = emptyList(),
  val size: Long = 0L,
  val downloadUrl: String = "",
  val retentionDate: String? = null,
  val createdAt: String = "",
  val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class MarkSeenRequest(
  val seen: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class SourceDto(
  val id: String,
  val downloadUrl: String = "",
  val data: String = "",
)
