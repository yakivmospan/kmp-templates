---
name: integration-tests
description: >
  Use this skill when writing integration tests for real service implementations, data sources,
  and device/platform APIs — the classes that unit tests mock at the boundary.
  Triggers include: any request to add, write, or improve integration tests for ConsoleService,
  storage data sources, network clients, file system access, or any other infrastructure
  implementation. Also use when asked about test placement (commonTest vs platform source sets)
  or boundary verification strategy.
  Do NOT use for ViewModels, Interactors, or Repositories — those are covered by unit-tests.
  Do NOT use for Compose UI rendering — that is covered by compose-ui-tests.
---

# Integration Tests

## Overview

Integration tests verify the **real implementations of infrastructure** — services, data sources,
and platform APIs that unit tests mock at the boundary.

Integration tests exist because unit tests mock infrastructure. That mock is only valid if the
real implementation is verified somewhere. Integration tests are that place.

---

## Key Principles & Structure Rules

| Rule | Detail |
|------|--------|
| **Test real implementations only** | No mocks of the class under test — the whole point is to verify the real implementation behaves correctly |
| **Stub only external I/O boundaries** | Stub network transport (`MockEngine`), OS process calls (in `commonTest`), or hardware APIs — not the class itself |
| **Use `MockSettings` for storage tests** | `multiplatform-settings-test` provides a real in-memory Settings implementation — exercises real serialization and encoding |
| **Use `MockEngine` for network tests** | Real Ktor pipeline with fake transport — serialization, headers, content negotiation, and error handling all execute |
| **Stub platform-specific services at the interface** | In `commonTest`, stub platform-specific interfaces. Move tests requiring real platform execution to the appropriate source set (e.g. `desktopTest`, `androidTest`) |
| **Placement by platform requirement** | `commonTest` for implementations that compile to all targets; platform source set for implementations that require native APIs or a specific runtime |
| **Full output assertion** | Assert the complete result object — not individual fields |
| **One concept per test** | Each `@Test` verifies one logical contract of the implementation |
| **Table tests** | Same patterns as unit-tests skill: Pattern A (individual `@Test` + private helper) for semantically distinct cases; Pattern B (`for` loop) for homogeneous flat inputs |
| **Avoid code duplication** | Extract shared setup and assertion helpers into private functions. If the same setup appears in 3+ tests, create a helper |
| **Given/When/Then comments** | In every test body |
| **When/then naming** | No camelCase, Kotlin backtick names, keep them concise |
| **Never run tests** | In agent mode — propose to validate, run, then proceed |
| **Set and reset dispatcher** | Always `Dispatchers.setMain` in `@BeforeTest`, `Dispatchers.resetMain` in `@AfterTest` when coroutines are involved |
| **Coroutine scope** | Use `TestScope(testDispatcher)` — never `GlobalScope` or `MainScope` |
| **Test multiple configurations** | Use `runWithInstances()` helper with default parameter list to test different instance configurations (e.g., debug on/off) — ensures options don't affect core behavior |

---

## Test Placement

```
composeApp/src/
  commonTest/kotlin/          ← implementations that compile to all targets
  <platform>Test/kotlin/      ← implementations requiring platform APIs (e.g. desktopTest, androidTest)
```

---

## Tech Stack

We are using `kotlin.test`, `kotlinx-coroutines-test`, `mockk`, `multiplatform-settings-test`, and `ktor-client-mock`.

---

## Boundary Stub Reference

| Boundary | Tool | Notes |
|----------|------|-------|
| Key-value storage | `MockSettings` (multiplatform-settings-test) | Real impl, in-memory — exercises real encode/decode |
| HTTP / network | Ktor `MockEngine` | Real Ktor pipeline, fake transport — do not mock the client itself |
| Platform-specific service | `mockk<ServiceInterface>()` in `commonTest`; real impl in platform source set | Stub the interface, never the class under test |
| File system | `mockk<FileService>()` in `commonTest`; temp dir in platform source set | |

---

## Template: Storage Implementation

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ExampleStorageTest {

    // -------------------------------------------------------------------------
    // Infrastructure
    // -------------------------------------------------------------------------

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // Real in-memory Settings — no mocking
    private val settings = MockSettings()

    // Class under test — real implementation
    private val storage = ExampleStorage(settings)

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    fun `when entity is saved then it can be retrieved`() = testScope.runTest {
        // Given
        val entity = ExampleEntity(id = "e1", name = "Test", status = Status.ACTIVE)

        // When
        storage.save(entity)
        val result = storage.get("e1")

        // Then
        assertEquals(entity, result)
    }

    @Test
    fun `when multiple entities are saved then all are returned`() = testScope.runTest {
        // Given
        val entities = listOf(
            ExampleEntity("e1", "First", Status.ACTIVE),
            ExampleEntity("e2", "Second", Status.INACTIVE),
        )

        // When
        entities.forEach { storage.save(it) }
        val result = storage.getAll()

        // Then
        assertEquals(entities.toSet(), result.toSet())
    }

    // -------------------------------------------------------------------------
    // Error path
    // -------------------------------------------------------------------------

    @Test
    fun `when entity does not exist then get returns null`() = testScope.runTest {
        // Given — storage is empty

        // When
        val result = storage.get("missing")

        // Then
        assertNull(result)
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    fun `when entity is saved twice then only latest version is returned`() = testScope.runTest {
        // Given
        val original = ExampleEntity("e1", "Original", Status.ACTIVE)
        val updated = ExampleEntity("e1", "Updated", Status.INACTIVE)

        // When
        storage.save(original)
        storage.save(updated)
        val result = storage.get("e1")

        // Then
        assertEquals(updated, result)
    }

    @Test
    fun `when entity is deleted then it can no longer be retrieved`() = testScope.runTest {
        // Given
        val entity = ExampleEntity("e1", "Test", Status.ACTIVE)
        storage.save(entity)

        // When
        storage.delete("e1")
        val result = storage.get("e1")

        // Then
        assertNull(result)
    }
}
```

---

## Template: Network Client Implementation

```kotlin
class ExampleApiClientTest {

    // -------------------------------------------------------------------------
    // Infrastructure — real Ktor pipeline, fake transport
    // -------------------------------------------------------------------------

    private val mockEngine = MockEngine { request ->
        when {
            request.url.encodedPath.contains("/items") -> respond(
                content = """[{"id":"i1","name":"Item","active":true}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
            else -> respondError(HttpStatusCode.NotFound)
        }
    }

    private val httpClient = HttpClient(mockEngine) {
        install(ContentNegotiation) { json() }
    }

    // Class under test — real implementation
    private val apiClient = ExampleApiClient(httpClient)

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    fun `when server returns items then they are deserialized correctly`() = runTest {
        // Given — MockEngine responds with valid JSON

        // When
        val result = apiClient.fetchItems()

        // Then
        assertEquals(1, result.size)
        assertEquals(ExampleItem(id = "i1", name = "Item", active = true), result.first())
    }

    // -------------------------------------------------------------------------
    // Error path
    // -------------------------------------------------------------------------

    @Test
    fun `when server returns 404 then expected exception is thrown`() = runTest {
        // Given — MockEngine returns 404 for unknown paths

        // When / Then
        assertFailsWith<ItemNotFoundException> { apiClient.fetchItem("unknown") }
    }

    @Test
    fun `when server returns malformed JSON then parsing exception is thrown`() = runTest {
        // Given
        val brokenEngine = MockEngine {
            respond(content = "not json", status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = ExampleApiClient(HttpClient(brokenEngine) {
            install(ContentNegotiation) { json() }
        })

        // When / Then
        assertFailsWith<SerializationException> { client.fetchItems() }
    }
}
```

---

## Template: Platform Service Real Execution (platform source set only)

Place in the appropriate platform source set (e.g. `desktopTest`, `androidTest`) — never in
`commonTest`. Tests the real implementation against actual platform APIs.

```kotlin
// <platform>Test — ExamplePlatformServiceTest.kt
class ExamplePlatformServiceTest {

    // Class under test — real platform implementation
    private val service = RealPlatformServiceImpl()

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    fun `when valid input is provided then service returns success`() = runTest {
        // Given
        val input = ServiceInput(value = "valid")

        // When
        val result = service.execute(input)

        // Then
        assertIs<ServiceResult.Success>(result)
    }

    // -------------------------------------------------------------------------
    // Error path
    // -------------------------------------------------------------------------

    @Test
    fun `when invalid input is provided then service returns failure`() = runTest {
        // Given
        val input = ServiceInput(value = "")

        // When
        val result = service.execute(input)

        // Then
        assertIs<ServiceResult.Failure>(result)
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    fun `when input produces multi-part output then all parts are captured`() = runTest {
        // Given
        val input = ServiceInput(value = "multi")

        // When
        val result = service.execute(input)

        // Then
        assertIs<ServiceResult.Success>(result)
        assertTrue(result.parts.size > 1)
    }
}
```

---

## What to Test

**Storage encode/decode fidelity** — every field of the entity survives a save → retrieve
round-trip with correct types and values; overwrite, delete, list all behave correctly

**Network client correctness** — successful response deserialization; HTTP error codes map to
the correct exceptions or result types; malformed responses are handled gracefully; request
headers and parameters are constructed correctly

**Platform service execution** — success and failure results for valid and invalid inputs;
edge case inputs (empty, boundary values); any output structure the caller depends on

**Error propagation** — infrastructure errors surface as the correct domain exception or result
type, not leaked implementation details

**Edge cases** — empty responses, missing keys, wrong formats, duplicate writes, boundary values

---

## What NOT to Test in Integration Tests

- ViewModels, Interactors, Repositories — those belong in unit tests with mocked dependencies
- UI rendering and event wiring — that belongs in compose-ui-tests
- Business logic correctness — that belongs in unit tests; integration tests only verify
  the infrastructure implementation, not what the caller does with the result
- Navigation or screen flows — keep integration tests at the infrastructure layer