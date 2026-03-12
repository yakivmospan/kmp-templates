---
name: compose-ui-tests
description: >
  Use this skill when writing UI tests for Compose Multiplatform screens.
  Triggers include: any request to add, write, or improve UI tests for Compose screens,
  test composables, verify UI state rendering, test user interactions in Compose,
  or test navigation behavior triggered from the UI.
  Do NOT use for pure ViewModel unit tests, repository tests, or non-Compose UI frameworks.
---

# Compose Multiplatform UI Tests

## Overview

UI tests verify that the **rendered UI matches expected state** and that **user interactions reach the ViewModel**. They complement ViewModel unit tests — ViewModel tests own business logic, UI tests own rendering contracts and event wiring.

Each screen exposes two overloads:
- **Public** — resolves ViewModel via `koinViewModel()`, used in production and in **all tests**
- **Private** — accepts `state`, `searchQuery`, `onEvent` directly, used only for Previews

Always test the **public overload**. The ViewModel is mocked via Koin so state is fully controlled. This means every assertion proves both that the UI renders correctly **and** that the value genuinely came from the ViewModel.

---

## Key Principles & Structure Rules

| Rule | Detail |
|------|--------|
| **Cover all code paths** | Happy path, error conditions, and edge cases |
| **Always test the public overload** | Mount `ExampleScreen()` with no arguments — Koin provides the mocked ViewModel |
| **Always mock the ViewModel** | Use `mockk(relaxed = true)` — never use a real ViewModel with real repositories |
| **Re-register Koin fresh per test** | Call `startKoin` inside `launchScreen`, `stopKoin` in `@AfterTest` — never share Koin state between tests |
| **Use `viewModel { }` DSL** | Not `single { }` — the public overload resolves via `koinViewModel()` which requires the ViewModel scope |
| **Verify events on the ViewModel** | Use `verify { mockViewModel.onEvent(...) }` — not an emitted events list |
| **Verify no-op interactions** | For interactions that must NOT fire an event (e.g. clicking an already-active element), use `verify(exactly = 0) { mockViewModel.onEvent(...) }` |
| **State assertions prove ViewModel wiring** | Because state comes from the mocked ViewModel, a passing assertion proves the wire is connected |
| **No `Dispatchers.setMain`** | Compose test framework manages its own dispatcher via `runComposeUiTest` |
| **`waitForIdle()` after interactions** | Always call after triggering clicks or input changes before asserting — no import needed, it's a method on `ComposeUiTest` |
| **`assertDoesNotExist()` needs no import** | It's a method on `SemanticsNodeInteraction`, not a top-level function |
| **Prefer semantic finders** | Use `onNodeWithText`, `onNodeWithContentDescription` before resorting to `onNodeWithTag` |
| **Add test tags sparingly** | Only add `Modifier.testTag(...)` to ambiguous composables; annotate with `@VisibleForTesting` |
| **Scroll before asserting off-screen nodes** | For nodes that may be below the fold in a scrollable screen, call `performScrollTo()` followed by `waitForIdle()` before asserting visibility or enabled state. `assertDoesNotExist()` does not require scrolling — it checks the semantic tree directly |
| **Querying nodes with the same text** | `onNodeWithText` is the default — use it when the text is unique in the tree. It crashes if the same text appears more than once, which itself catches unintended duplicates. Switch to `onAllNodesWithText(...)` only when the text is known to appear multiple times. **Use `onFirst()` only when you intentionally target a single node and the assertion is valid for any one instance** (e.g. a node that is unique in the tree). When a label is known to appear more than once, always assert **all** instances by iterating with index — this applies to every operation including visibility, enabled state, clicks, and scrolls. Using `onFirst()` and ignoring the rest is incorrect and gives false confidence regardless of the assertion type. To assert ALL instances are absent, use `onAllNodesWithText(...).fetchSemanticsNodes()` and assert the result is empty. For the index iteration pattern, define a reusable `assertAllNodesWithText` helper in the test class (see Template). |
| **One assertion per concept** | Each `@Test` verifies one logical UI contract |
| **Table tests** | Use when the same assertion must hold across multiple inputs. Two patterns are valid — choose based on diagnostic value: **(A) Individual `@Test` functions + private helper** — when each input is a semantically distinct state (e.g. `Loading`, `Error`) and a failing test name alone should identify the problem. **(B) Single `@Test` with a `for` loop** — when inputs are a flat homogeneous list (e.g. all field labels, all field values) and the assertion is structurally identical for each item; the item value itself provides sufficient failure diagnostics. Never use Pattern B when inputs produce structurally different assertions. |
| **Avoid code duplication** | Extract repetitive test logic into private helper functions. Examples: common setup for multiple test scenarios, repeated mock configurations, or shared assertion logic. Helper functions should have clear names and documentation. Always prefer DRY (Don't Repeat Yourself) — if the same setup or assertion sequence appears in 3+ tests, create a helper function. |
| **Given/When/Then comments** | In every test body |
| **When/then naming** | No camelCase, Kotlin backtick names, keep them concise |
| **Ask before assuming** | If the screen's contract introduces a pattern, interaction, or UI structure not covered by this skill, ask for clarification before writing tests. Wrong tests that pass are worse than no tests |

---

## Tech Stack

We are using `compose.uiTest`, `kotlin.test`, `mockk`, and `koin-test`.

---

## Template
```kotlin
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ExampleScreenTest {

    // -------------------------------------------------------------------------
    // Infrastructure
    // -------------------------------------------------------------------------

    private val mockViewModel: ExampleViewModel = mockk(relaxed = true)

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @BeforeTest
    fun setup() {
        // Set default fallback state for all ViewModel properties
        every { mockViewModel.state } returns MutableStateFlow(ExampleUiState.Loading)
        every { mockViewModel.extraProperty } returns MutableStateFlow("")
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        clearAllMocks()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun launchScreen(
        state: ExampleUiState,
        extraProperty: String = "",
        block: ComposeUiTest.() -> Unit
    ) = runComposeUiTest {
        every { mockViewModel.state } returns MutableStateFlow(state)
        every { mockViewModel.extraProperty } returns MutableStateFlow(extraProperty)
        startKoin {
            modules(module { viewModel { mockViewModel } })
        }
        setContent { ExampleScreen() }
        block()
    }

    /**
     * Scrolls to and asserts [assertion] on every node matching [text].
     * Use when a label appears more than once in the tree (e.g. "Name", "App Icon")
     * and every instance must satisfy the same condition.
     */
    private fun ComposeUiTest.assertAllNodesWithText(
        text: String,
        assertion: SemanticsNodeInteraction.() -> Unit
    ) {
        val count = onAllNodesWithText(text).fetchSemanticsNodes().size
        for (i in 0 until count) {
            onAllNodesWithText(text)[i].performScrollTo()
            waitForIdle()
            onAllNodesWithText(text)[i].assertion()
        }
    }

    // -------------------------------------------------------------------------
    // State rendering — one section per UiState variant
    // -------------------------------------------------------------------------

    @Test
    fun `when ViewModel state is X then correct content is displayed`() =
        launchScreen(ExampleUiState.X) {
            // Given — ViewModel provides X state

            // When
            waitForIdle()

            // Then
            onNodeWithTag(TAG_EXAMPLE).assertIsDisplayed()
        }

    // -------------------------------------------------------------------------
    // Extra ViewModel properties — one section per property
    // -------------------------------------------------------------------------

    @Test
    fun `when ViewModel property is Y then UI reacts accordingly`() =
        launchScreen(state = ExampleUiState.Loaded(...), extraProperty = "value") {
            // Given — ViewModel provides non-empty extraProperty

            // When
            waitForIdle()

            // Then
            onNodeWithText("Element").assertDoesNotExist()
        }

    // -------------------------------------------------------------------------
    // Conditional visibility — table test pattern for repeated assertions
    // -------------------------------------------------------------------------

    @Test
    fun `when ViewModel state is loading then action button is disabled`() =
        assertActionButtonDisabled(ExampleUiState.Loading)

    @Test
    fun `when ViewModel state is error then action button is disabled`() =
        assertActionButtonDisabled(ExampleUiState.Error(cause = null))

    private fun assertActionButtonDisabled(state: ExampleUiState) =
        launchScreen(state) {
            // Given — ViewModel provides a state where the button should be disabled

            // When
            waitForIdle()

            // Then
            onNodeWithContentDescription("Action").assertIsNotEnabled()
        }

    // -------------------------------------------------------------------------
    // Interactions — one test per interactive element
    // -------------------------------------------------------------------------

    @Test
    fun `when user clicks X then ViewModel onEvent is called with X event`() =
        launchScreen(ExampleUiState.Loaded(...)) {
            // Given

            // When
            waitForIdle()
            onNodeWithText("Element").performClick()
            waitForIdle()

            // Then
            verify { mockViewModel.onEvent(ExampleEvent.X) }
        }

    @Test
    fun `when user clicks already active element then ViewModel onEvent is not called`() =
        launchScreen(ExampleUiState.Loaded(...)) {
            // Given — element is already in the active state

            // When
            waitForIdle()
            onNodeWithText("Active Element").performClick()
            waitForIdle()

            // Then
            verify(exactly = 0) { mockViewModel.onEvent(ExampleEvent.X) }
        }
}
```

---

## What to Test (Focus Areas)

**State rendering** — each `UiState` variant renders the correct content. Cover every distinct state and every distinct data condition that produces a different rendered output:
- Loading → spinner visible, content not visible
- Loaded (empty) → empty state message visible, list not visible
- Loaded (with data) → items visible with correct labels and values
- Error → error title visible, error cause surfaced in the UI
- Selection state → when a UI element can be active/inactive (e.g. a selected tab, a toggled chip), assert `assertIsSelected()` / `assertIsNotSelected()` — not just visibility

**Extra ViewModel properties** — one test per property proving the UI effect is driven by the ViewModel value, not hardcoded. Cover every property that independently affects the UI:
- Non-empty value → UI reacts (e.g. certain elements hidden, others appear)
- Empty/default value → UI shows its default shape

**Conditional visibility** — elements that appear, disappear, or change enabled state based on current state or ViewModel properties. Cover every element whose presence or enabled state is conditional:
- Enabled/disabled correctly per `UiState`
- Visible/hidden correctly per `UiState` or extra ViewModel property combination
- Where multiple states produce the same assertion, use the table test pattern to avoid duplication

**User interactions** — verify the ViewModel received the correct event. Cover every interactive element with at least a positive case, and a negative case where the interaction should be a no-op:
- Item click → `verify { mockViewModel.onEvent(OpenDetail(id)) }`
- Button click → `verify { mockViewModel.onEvent(ExpectedEvent) }`
- Text input → `verify { mockViewModel.onEvent(SearchQueryChanged(query)) }`
- No-op interaction → when an action should have no effect (e.g. re-selecting the already-active element), verify the event was never fired: `verify(exactly = 0) { mockViewModel.onEvent(ExpectedEvent) }`

**Empty states** — distinct messages for semantically different empty conditions (e.g. "no data exists" vs "no data matches current filter"). Each distinct empty condition should have its own test.

---

## What NOT to Test in UI Tests

- ViewModel business logic (belongs in ViewModel unit tests)
- Repository data transformation (belongs in repository/interactor tests)
- Navigation stack correctness end-to-end (belongs in integration tests)
- Exact pixel layout or colors (fragile; prefer screenshot tests if needed)