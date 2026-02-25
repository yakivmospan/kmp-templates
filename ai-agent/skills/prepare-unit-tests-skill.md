# Prepare Unit Tests Skill

## Test Coverage Requirements

### Unit Tests

- Test individual functions and methods in isolation
- Cover all code paths including happy path and error conditions
- Mock external dependencies (databases, APIs, file systems)
- Verify edge cases (empty inputs, null values, boundary conditions)

### Edge Cases and Error Scenarios

- Test with invalid, malformed, or unexpected inputs
- Verify graceful handling of network failures and timeouts
- Test concurrent access and race conditions where applicable
- Confirm proper behavior when resources are unavailable
- Assure correct error propagation and silent failure handling

## Test Quality Standards

- Tests should be deterministic and not rely on timing or external state
- Use descriptive test names that explain what is being tested, follow when - then naming convention
- Do not make names them too long, no camel case - Kotlin allows white spaces in fun `func name`
- Keep tests focused - one assertion per logical concept
- Ensure tests are maintainable and easy to understand
- Add tests for bug fixes to prevent regression
- When testing states - verify against full state, not its parts (for example ViewModel UI state.)
- Add Given/When/What comments to corresponding test sections

## Implementation Approach

1. Use following template `Example Template`, if required
2. Ensure tests are properly grouped and groups are separated with good visible comments
3. Do table tests where possible to avoid test duplications
4. Do table tests for scenarios where you need to test multiple error or success inputs
5. Do not use deprecated classes until absolute necessary
6. Never run tests or propose to run tests in agent mode until asked to do so. Instead propose me to validate, run them and then
   proceed on fixing or jumping to next steps.
7. Never use reflection, there should be a public available use case that updates private local variables.
8. Never use matchers for real (not mocked) objects. Verify full object parameters instead
9. If there is a private implementation in file, and you need it for testing - propose to make it internal(@VisibleForTesting)

```
@OptIn(ExperimentalCoroutinesApi::class)
class ExampleTemplateTest {
    // -------------------------------------------------------------------------
    // Infrastructure
    // -------------------------------------------------------------------------

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // Mocked dependencies/constats/variables goes here 
    
    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Initialize mocked dependencies/variables here
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        // Any additional clean ups.
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    
    // If possoble and suitable create default mocks to avoid duplication
    private fun successResult(output: ..) = {}}
    private fun failedResult(error: Throwable) = {}
    private fun mockDefaults(
    ) {
        every { .. } returns successResult(..)
        every { .. } returns successResult(..)
    }
    
    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------
    @Test
    fun `when all commands succeed then returns correct value`() = runTest {
        // Given
        // When
        // Then
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------
    @Test
    fun `when command fails then it throws exception`() = runTest {
        // Given
        // When
        // Then
    }
    
    // -------------------------------------------------------------------------
    // Other groups follow the same logic.
    // -------------------------------------------------------------------------
```

## Tech stack

We are using kotlin.test, kotlinx-coroutines-test and mockk.
