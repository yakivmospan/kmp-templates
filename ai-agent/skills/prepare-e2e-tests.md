---
name: e2e-tests
description: >
  Use this skill when writing end-to-end tests that verify complete user-facing flows
  through real Compose screens backed by the real DI graph and live dev environment.
  Triggers include: any request to add, write, or improve e2e tests, test multi-screen
  navigation flows, verify real data loading across the full stack, or test complete
  user journeys from app launch through UI interaction.
  Do NOT use for isolated ViewModel logic — use unit-tests.
  Do NOT use for single-screen rendering or event wiring — use compose-ui-tests.
  Do NOT use for infrastructure implementation verification — use integration-tests.
---

# E2E Tests

## Overview

E2E tests verify **complete user-facing flows** by navigating through real Compose screens
backed by the real DI graph and the live dev environment. Nothing in the stack is mocked.

E2E tests exist because the other three skills all mock at least one boundary:
- `unit-tests` mocks all dependencies
- `integration-tests` mocks network transport or OS boundaries
- `compose-ui-tests` mocks the ViewModel via Koin

E2E tests are the only place where the full stack — UI, ViewModel, Interactor, Repository,
storage, network — executes together against real data.

---

## Key Principles & Structure Rules

| Rule | Detail |
|------|--------|
| **No mocks anywhere** | The entire stack is real — no MockEngine, no MockSettings, no mockk |
| **Real Koin graph** | Koin is initialized automatically by `TestApp()` composable via `App(diModules)` constructor when `setContent { TestApp() }` is called. Override environment config via `e2eConfigModule` |
| **Read-only against all data** | No create, update, or delete against any environment — live or dev. Tests only verify, never mutate |
| **Per-test lifecycle by default** | `stopKoin()` in `@AfterTest` only — Koin starts automatically when TestApp is rendered. No `@BeforeTest` needed for Koin setup |
| **Per-class lifecycle for dependent suites** | Use `@BeforeClass` / `@AfterClass` only when tests in the class form an ordered sequence where later tests depend on state established by earlier ones. Document this explicitly with a comment |
| **Scenario-shaped tests** | Each test describes a full user journey, not a single interaction (e.g. "user opens kiosk list, taps a kiosk, sees detail") |
| **UI state assertions** | Assert any UI element that confirms the flow reached the expected state — screens, dialogs, bottom sheets, banners, content descriptions, tags |
| **`waitForNodeWithTag(tag)` for real I/O** | Use the shared helper extension from `com.yumpu.kiosksmanager.utils` — never assume instant load — always wait for content to appear before asserting |
| **`waitForNodeWithContentDescriptionEnabled()`** | When UI elements are disabled during loading (e.g. buttons disabled while data loads), use this helper to wait for both existence AND enabled state before interaction |
| **Robot for system-level dialogs** | When tests trigger OS-level dialogs (e.g. macOS Keychain, file picker), use `java.awt.Robot` to simulate keyboard/mouse input. Initialize Robot in `@BeforeTest`, press keys with delays to ensure focus, and use multiple attempts if needed |
| **`runComposeUiTest`** | Same test runner as compose-ui-tests |
| **No `Dispatchers.setMain`** | Compose test framework manages its own dispatcher |
| **Given/When/Then comments** | In every test body |
| **When/then naming** | No camelCase, Kotlin backtick names, keep them concise |
| **Never run tests in agent mode** | Propose to validate, run, then proceed |
| **One flow per test** | Each `@Test` covers one user journey from entry point to final assertion |

---

## Test Placement

```
composeApp/src/
  jvmTest/kotlin/
    com/yumpu/kiosksmanager/
      e2e/                        ← all e2e tests live here
        flows/                    ← one file per feature flow
        config/                   ← e2eConfigModule and TestApp composable
      utils/                      ← shared test helpers (waitForNodeWithTag, etc.)
```

---

## Tech Stack

We are using `compose.uiTest`, `kotlin.test`, and `koin` (no mockk in e2e tests).

---

## Fixture Lifecycle Reference

### Default: per-test (use this unless there is an explicit dependency between tests)

```kotlin
@BeforeTest
fun setup() {
  // Any additional setup.
}

@AfterTest
fun tearDown() {
    stopKoin() // Make sure each test start with fresh TestApp
}
```

### Exception: per-class dependent suite

Always document why with a comment.

```kotlin
companion object {
    // Per-class lifecycle: these tests form a dependent suite.
    // Step 2 relies on the navigation state established in Step 1.
    @BeforeClass
    fun setupFixture() {
       // Any additional setup.
    }

    @AfterClass
    fun tearDownFixture() {
        stopKoin()
    }
}
```

---

## Template: Standard Flow (per-test lifecycle)

```kotlin
package com.yumpu.kiosksmanager.e2e.flows

import androidx.compose.ui.test.*
import com.yumpu.kiosksmanager.e2e.config.TestApp
import com.yumpu.kiosksmanager.utils.waitForNodeWithTag
import org.koin.core.context.stopKoin
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class <FeatureName>FlowTest {

    // -------------------------------------------------------------------------
    // Lifecycle — per-test (default)
    // -------------------------------------------------------------------------

    @BeforeTest
    fun setup() {
        // Optional: any additional setup beyond Koin (uncommon)
    }

    @AfterTest
    fun tearDown() {
        stopKoin() // Make sure each test starts with fresh TestApp
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    // Create semantic helper methods that wrap waitForNodeWithTag
    // Example pattern: private fun ComposeUiTest.waitFor<Element>() = waitForNodeWithTag(TAG_<ELEMENT>)

    companion object {
        // Optional: define test constants for IDs, timeouts, etc.
    }

    // -------------------------------------------------------------------------
    // Test scenarios
    // -------------------------------------------------------------------------

    @Test
    fun `when <user action> then <expected outcome>`() = runComposeUiTest {
        // Given — launch app
        setContent { TestApp() }

        // When — perform user action(s)
        // Use helper methods to wait for elements
        // Use performClick(), performScrollTo(), performTextInput(), etc.

        // Then — assert expected UI state
        // Use onNodeWithTag().assertIsDisplayed()
        // Use onNodeWithText(), onNodeWithContentDescription() as needed
    }

    @Test
    fun `when <multi-step scenario> then <final state>`() = runComposeUiTest {
        // Given — initial state
        setContent { TestApp() }
        // Wait for initial screen

        // When — step 1
        // Interact with UI

        // When — step 2
        // Interact with UI

        // Then — verify final state
        // Assert expected elements visible
    }
}
```

---

## Template: Dependent Suite (per-class lifecycle)

```kotlin
package com.yumpu.kiosksmanager.e2e.flows

import androidx.compose.ui.test.*
import com.yumpu.kiosksmanager.e2e.config.TestApp
import com.yumpu.kiosksmanager.utils.waitForNodeWithTag
import org.koin.core.context.stopKoin
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class <FeatureName>DependentSuiteTest {

    // -------------------------------------------------------------------------
    // Lifecycle — per-class
    // IMPORTANT: Document WHY tests are dependent
    // Example: "Step 2 depends on navigation state from Step 1"
    // -------------------------------------------------------------------------

    companion object {
        @AfterClass
        fun tearDownFixture() {
            stopKoin()
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    // Create semantic helper methods that wrap waitForNodeWithTag
    // Example pattern: private fun ComposeUiTest.waitFor<Element>() = waitForNodeWithTag(TAG_<ELEMENT>)

    // -------------------------------------------------------------------------
    // Ordered steps — name tests with step numbers
    // -------------------------------------------------------------------------

    @Test
    fun `step 1 - when <initial action> then <intermediate state>`() = runComposeUiTest {
        // Given — app launch
        setContent { TestApp() }

        // When — perform first action
        // Wait and interact

        // Then — assert intermediate state
        // This state will be preserved for next test
    }

    @Test
    fun `step 2 - when <continuing action> then <final state>`() = runComposeUiTest {
        // Given — assumes state from step 1 exists
        setContent { TestApp() }
        // Wait to confirm we're in expected starting state

        // When — perform next action
        // Continue the flow from step 1

        // Then — assert final state
        // Verify the complete journey
    }
}
```

---

## What to Test

**Screen-to-screen navigation** — tapping an item navigates to the correct screen; back
navigation returns to the correct previous screen; deep navigation chains complete correctly

**Real data loading** — list screens load and display real items from the dev environment;
detail screens show correct data for the selected item; no empty state shown when data exists

**Full flow correctness** — multi-step user journeys complete without errors or unexpected
states (e.g. launch → list → detail → back → list still intact)

**UI state after real I/O** — dialogs, bottom sheets, banners, or error states that appear
as a result of real network or storage responses

**Error surface** — when the environment is unreachable or returns an error, the correct
error UI appears (not a crash or blank screen)

---

## What NOT to Test in E2E Tests

- Business logic correctness — owned by `unit-tests`
- Individual screen rendering per UiState variant — owned by `compose-ui-tests`
- Infrastructure encoding/decoding or HTTP error mapping — owned by `integration-tests`
- Any write, update, or delete operation against any environment
- Navigation stack internals — assert visible UI, not back-stack state