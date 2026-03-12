# Testing Strategy

For every new feature or code change in ViewModels and Screens, propose and implement comprehensive test coverage before marking work complete.

## Overview

We follow a layered testing approach:
- **Unit Tests** — ViewModels, repositories, interactors, business logic
- **UI Tests** — Compose screens, state rendering, user interactions
- **Integration Tests** — Real infrastructure implementations (storage, network, platform APIs)
- **E2E Tests** — Full user flows across real screens, real DI graph, and live dev environment

All code paths should be covered, including but not limited to: happy path, error conditions, edge cases, boundary conditions, and any other relevant scenarios.

## When to Write Each Type

| Test Type | When to Use | What Not to Test |
|-----------|-------------|------------------|
| **Unit Tests** | ViewModels, repositories, interactors, any business logic | Compose UI rendering, infrastructure implementations |
| **UI Tests** | Every screen with a ViewModel | ViewModel business logic, infrastructure |
| **Integration Tests** | Real implementations of services, data sources, platform APIs that unit tests mock | ViewModels, interactors, repositories, Compose UI |
| **E2E Tests** | Full user journeys across multiple screens with real data | Business logic correctness (unit-tests), single-screen rendering (compose-ui-tests), infrastructure encoding (integration-tests) |

## Detailed Rules & Templates

For comprehensive rules, patterns, and code templates, consult the skill files:

- **[prepare-unit-tests.md](../skills/prepare-unit-tests.md)** — ViewModel and business logic test rules, table test patterns, dispatcher setup, full templates
- **[prepare-ui-tests.md](../skills/prepare-ui-tests.md)** — Compose UI test rules, Koin setup, semantic finders, interaction patterns, full templates
- **[prepare-integration-tests.md](../skills/prepare-integration-tests.md)** — Infrastructure test rules, boundary stubs (MockSettings, MockEngine), test placement, full templates
- **[prepare-e2e-tests.md](../skills/prepare-e2e-tests.md)** — E2E flow test rules, real Koin graph setup, fixture lifecycle, waitUntil patterns, full templates

## Key Principles (Apply to All Tests)

- Tests must be deterministic — no timing dependencies or external state
- One assertion per logical concept
- Use descriptive backtick test names: `` `when X then Y` ``
- Add Given/When/Then comments to every test body
- Extract repeated setup into helper functions (DRY principle)
- Never run tests in agent mode — propose to validate, run, then proceed
- Never use reflection — use public APIs only

## Implementation Approach

When implementing a feature:
1. **Propose test cases upfront** — covering requirements for all applicable test types
2. **Write tests alongside implementation** — unit, UI, integration, and e2e tests as needed
3. **Propose validation** — ask the user to run tests and report results; never run tests automatically
4. **Document dependencies** — note any special setup or requirements

Always include testing as part of the implementation, not as an afterthought.

**For detailed planning workflow in Agent mode**, see **[test-planning-phase.md](test-planning-phase.md)** — defines the step-by-step process for planning and implementing tests (propose test cases → prepare test files with stubs → implement test groups incrementally).

## Tech Stack

| Test Type | Stack |
|-----------|-------|
| **Unit Tests** | `kotlin.test`, `kotlinx-coroutines-test`, `mockk` |
| **UI Tests** | `compose.uiTest`, `kotlin.test`, `mockk`, `koin-test` |
| **Integration Tests** | `kotlin.test`, `kotlinx-coroutines-test`, `mockk`, `multiplatform-settings-test`, `ktor-client-mock` |
| **E2E Tests** | `compose.uiTest`, `kotlin.test`, `koin` |

See individual skill files for detailed usage patterns and examples.