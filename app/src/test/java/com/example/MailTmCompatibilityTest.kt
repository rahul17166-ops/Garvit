package com.example

import com.example.data.remote.DomainDto
import com.example.data.remote.HydraCollection
import com.example.data.remote.MailTmNetworkModule
import com.example.data.remote.MessageDetailDto
import com.example.data.remote.MessageSummaryDto
import com.example.data.remote.SourceDto
import com.example.data.remote.TokenResponse
import com.example.data.repository.MailFormatters
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies Mail.tm OpenAPI / Hydra JSON-LD contract parsing using Moshi KSP codegen
 * and validates formatting utilities used across CI and local builds.
 */
class MailTmCompatibilityTest {

  private val moshi = MailTmNetworkModule.createMoshi()

  @Test
  fun `parses MailTm Hydra domains collection accurately`() {
    val json = """
      {
        "@context": "/contexts/Domain",
        "@id": "/domains",
        "@type": "hydra:Collection",
        "hydra:member": [
          {
            "id": "66a1b2c3d4e5",
            "domain": "mailvault-test.tm",
            "isActive": true,
            "isPrivate": false,
            "createdAt": "2026-01-15T08:00:00+00:00",
            "updatedAt": "2026-01-15T08:00:00+00:00"
          }
        ],
        "hydra:totalItems": 1
      }
    """.trimIndent()

    val type = Types.newParameterizedType(HydraCollection::class.java, DomainDto::class.java)
    val adapter = moshi.adapter<HydraCollection<DomainDto>>(type)
    val parsed = adapter.fromJson(json)

    assertNotNull(parsed)
    assertEquals(1, parsed!!.totalItems)
    assertEquals(1, parsed.members.size)
    assertEquals("mailvault-test.tm", parsed.members.first().domain)
    assertTrue(parsed.members.first().isActive)
    assertFalse(parsed.members.first().isPrivate)
  }

  @Test
  fun `parses MailTm token response accurately`() {
    val json = """{"id":"acc_123","token":"eyJhbGciOiJIUzI1NiJ9.payload.sig"}"""
    val parsed = moshi.adapter(TokenResponse::class.java).fromJson(json)

    assertNotNull(parsed)
    assertEquals("acc_123", parsed!!.id)
    assertTrue(parsed.token.startsWith("eyJ"))
  }

  @Test
  fun `parses MailTm Hydra messages collection accurately`() {
    val json = """
      {
        "@type": "hydra:Collection",
        "hydra:member": [
          {
            "id": "msg_99",
            "accountId": "acc_123",
            "msgid": "<abc@example.org>",
            "from": { "address": "security@example.org", "name": "Security Bot" },
            "to": [{ "address": "cipher.a1b2c3@mailvault-test.tm", "name": "" }],
            "subject": "Verification Code 482910",
            "intro": "Your one-time code is 482910.",
            "seen": false,
            "isDeleted": false,
            "hasAttachments": true,
            "size": 2048,
            "downloadUrl": "/messages/msg_99/download",
            "createdAt": "2026-11-20T12:30:00+00:00",
            "updatedAt": "2026-11-20T12:30:00+00:00"
          }
        ],
        "hydra:totalItems": 1
      }
    """.trimIndent()

    val type = Types.newParameterizedType(HydraCollection::class.java, MessageSummaryDto::class.java)
    val parsed = moshi.adapter<HydraCollection<MessageSummaryDto>>(type).fromJson(json)

    assertNotNull(parsed)
    assertEquals(1, parsed!!.members.size)
    val msg = parsed.members.first()
    assertEquals("msg_99", msg.id)
    assertEquals("Security Bot", msg.from.name)
    assertEquals("Verification Code 482910", msg.subject)
    assertTrue(msg.hasAttachments)
    assertFalse(msg.seen)
  }

  @Test
  fun `parses MailTm message detail and strips HTML when plain text is absent`() {
    val json = """
      {
        "id": "msg_100",
        "subject": "HTML Only",
        "from": { "address": "no-reply@service.io", "name": "Service" },
        "to": [{ "address": "vault@mailvault-test.tm", "name": "" }],
        "html": ["<html><body><p>Hello&nbsp;<strong>World</strong></p><br/><p>Code: 7711</p></body></html>"],
        "size": 512
      }
    """.trimIndent()

    val detail = moshi.adapter(MessageDetailDto::class.java).fromJson(json)
    assertNotNull(detail)

    val readable = MailFormatters.extractReadableBody(detail!!)
    assertTrue(readable.contains("Hello World"))
    assertTrue(readable.contains("Code: 7711"))
    assertFalse(readable.contains("<p>"))
  }

  @Test
  fun `parses raw MIME source payload accurately`() {
    val json = """
      {
        "id": "msg_100",
        "downloadUrl": "/sources/msg_100/download",
        "data": "MIME-Version: 1.0\r\nSubject: Test\r\n\r\nBody"
      }
    """.trimIndent()
    val source = moshi.adapter(SourceDto::class.java).fromJson(json)
    assertNotNull(source)
    assertTrue(source!!.data.contains("MIME-Version: 1.0"))
  }

  @Test
  fun `formats byte sizes across units`() {
    assertEquals("512 B", MailFormatters.formatBytes(512L))
    assertEquals("2.0 KB", MailFormatters.formatBytes(2048L))
    assertEquals("1.00 MB", MailFormatters.formatBytes(1024L * 1024L))
  }

  @Test
  fun `alphanumeric username and password constraints match MailTm API spec`() {
    val allowedUser = Regex("^[a-z0-9]+$")
    val allowedPass = Regex("^[A-Za-z0-9]{16}$")
    assertTrue(allowedUser.matches("cipher9x2k7m"))
    assertTrue(allowedPass.matches("AbCdEfGh23456789"))
  }
}
