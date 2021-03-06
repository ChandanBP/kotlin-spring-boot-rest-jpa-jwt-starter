package osahner

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ActiveProfiles("test")
internal class AuthenticationTest(@Autowired private val restTemplate: TestRestTemplate) {
  val loginForm = hashMapOf("username" to "john.doe", "password" to "test1234")


  @Test
  @Order(1)
  fun `ping restricted`() {
    val result = restTemplate.getForEntity<String>("/api/v1/restricted")

    assertNotNull(result)
    assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
  }


  // Fixme RestTemplate with HttpStatus.UNAUTHORIZED result
  // https://github.com/spring-projects/spring-framework/issues/21321
  @Test
  @Order(2)
  fun `failed login with wrong password`() {
    val falseLoginForm = hashMapOf("username" to "john.doe", "password" to "wrongpassword")
    try {
      val result = restTemplate.postForEntity<Any>("/login", falseLoginForm)

      assertNotNull(result)
      assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    } catch (e: Exception) {
      print("Fixme RestTemplate with HttpStatus.UNAUTHORIZED result")
    }
  }

  @Test
  @Order(3)
  fun `failed login with wrong username`() {
    val falseLoginForm = hashMapOf("username" to "john.wrong", "password" to "wrongpassword")
    try {
      val result = restTemplate.postForEntity<Any>("/login", falseLoginForm)

      assertNotNull(result)
      assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    } catch (e: Exception) {
      print("Fixme RestTemplate with HttpStatus.UNAUTHORIZED result")
    }
  }

  @Test
  @Order(4)
  fun `failed login with faulty POST`() {
    val falseLoginForm = hashMapOf("username" to "john.doe", "password" to "wrongpassword", "bogus" to "bogus")
    try {
      val result = restTemplate.postForEntity<Any>("/login", falseLoginForm)

      assertNotNull(result)
      assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    } catch (e: Exception) {
      print("Fixme RestTemplate with HttpStatus.UNAUTHORIZED result")
    }
  }

  @Test
  @Order(5)
  fun `successfull login`() {
    val result = restTemplate.postForEntity<String>("/login", loginForm)

    assertNotNull(result)
    assertEquals(HttpStatus.OK, result.statusCode)
    val bearer = result.headers["authorization"]?.get(0).orEmpty()
    assertNotNull(bearer)
    assertThat(bearer).contains("Bearer")
  }


  @Test
  @Order(6)
  fun `ping restricted again`() {
    val expected = "Pong!"

    val login = restTemplate.postForEntity<String>("/login", loginForm)
    val bearer = login.headers["authorization"]?.get(0).orEmpty()
    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_JSON
    headers["Authorization"] = bearer
    val requestEntity = HttpEntity<String>(headers)

    val result = restTemplate.exchange<String>("/api/v1/restricted", HttpMethod.GET, requestEntity, String::class.java)
    assertNotNull(result)
    assertEquals(HttpStatus.OK, result.statusCode)
    assertEquals(expected, result.body)
  }

  @Test
  @Order(7)
  fun `ping restricted again with a faulty bearer`() {
    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_JSON
    headers["Authorization"] = "Bearer TOTALYWRONG"
    var requestEntity = HttpEntity<String>(headers)

    var result = restTemplate.exchange<String>("/api/v1/restricted", HttpMethod.GET, requestEntity, String::class.java)
    assertNotNull(result)
    assertEquals(HttpStatus.FORBIDDEN, result.statusCode)

    headers["Authorization"] = "TOTALYWRONG"
    requestEntity = HttpEntity(headers)

    result = restTemplate.exchange<String>("/api/v1/restricted", HttpMethod.GET, requestEntity, String::class.java)
    assertNotNull(result)
    assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
  }

}
