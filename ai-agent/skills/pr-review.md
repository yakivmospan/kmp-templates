---
name: pr-review
description: >
  Use this skill when reviewing pull requests, performing code reviews, or checking
  code quality. Triggers include: any request to review a PR, check code changes,
  verify code style, identify issues, assess architecture compliance, or validate
  that changes follow project standards. Also use when asked to create a review
  checklist or provide feedback on proposed changes.
---

# PR Review Skill

## Overview

PR reviews ensure that code changes maintain quality standards, follow architectural principles, and integrate seamlessly with the existing codebase. Every review should be thorough yet constructive, focusing on correctness, maintainability, and consistency.

The goal is to catch issues early, share knowledge, and maintain a high-quality codebase. Reviews should be systematic, covering all aspects from architecture to style, while respecting the author's effort and expertise.

---

## Review Process Workflow

### Local Review (Before Commit) - REPORT ONLY MODE

**IMPORTANT:** Local reviews are read-only. Never automatically fix, add, or modify code. Always follow this process:

1. **Identify changed files** — Use `git status` or `git diff --name-only` to see what's modified
2. **Review each changed file** — Read through changes line by line using the comprehensive checklist
3. **Generate status report** — Create a detailed report listing all findings by severity (Critical, Major, Minor, Suggestion)
4. **Present the report** — Show findings to the developer. DO NOT make any changes yet
5. **Wait for approval** — Developer decides which issues to address and in what order
6. **Create action plan if requested** — If developer wants to fix issues, create a prioritized plan for addressing them
7. **Address issues one by one** — Fix issues only after explicit approval for each one. Wait for confirmation before proceeding to the next

**Golden Rule:** Status first, plan second, action only with explicit approval for each step.

### PR Review (Before Merge)

1. **Understand the context** — Read the PR description, linked issues, and understand what problem is being solved
2. **Check the scope** — Verify the changes are focused and don't include unrelated modifications
3. **Run through checklists** — Use the comprehensive checklist below, ordered by criticality
4. **Classify issues** — Mark findings as Critical, Major, Minor, or Suggestion
5. **Verify tests** — Ensure adequate test coverage for all changes
6. **Provide feedback** — Be specific, actionable, and constructive

---

## Quick Local Review Checklist (Pre-Commit)

Use this checklist to inspect modified files only. Report findings, don't fix automatically:

- [ ] **Debug code present?** — Check for `println`, `TODO`, commented-out code, test data
- [ ] **Secrets in code?** — Look for API keys, passwords, or certificates
- [ ] **Import issues?** — Identify unused imports, wildcard imports (`*`)
- [ ] **Tests missing?** — New logic without corresponding test files
- [ ] **Tests status** — Note if tests pass or fail (run `./gradlew test`)
- [ ] **Build status** — Note if build succeeds or has errors (run `./gradlew build`)
- [ ] **Magic numbers found?** — Identify hard-coded values that need names
- [ ] **Error handling issues?** — Find catch blocks without logging or silent failures
- [ ] **Layer boundary violations?** — Check imports for wrong layer dependencies
- [ ] **Scope misuse?** — Look for `GlobalScope` usage
- [ ] **Exposed mutable state?** — Find public `MutableStateFlow` or `var` properties
- [ ] **Missing documentation?** — Identify complex logic without KDoc

**Quick commands for inspection:**
```bash
# See what you changed
git diff

# See changed files only
git status
git diff --name-only

# Review specific file changes
git diff path/to/file

# Check for common issues
git diff | grep -i "TODO\|println\|GlobalScope"

# Run tests to check status
./gradlew test

# Build to check status
./gradlew build
```

---

## Severity Classification

| Severity | When to Use | Examples |
|----------|-------------|----------|
| **🔴 Critical** | Blocks merge — breaks functionality, violates core architecture, introduces security issues, or missing essential tests | Security vulnerability, data loss risk, violates layer boundaries, exposes internal implementation, missing critical tests, breaks API contract |
| **🟠 Major** | Should be fixed before merge — significant quality issues, architectural inconsistencies, or poor practices | Code duplication, improper error handling, inefficient algorithms, missing important tests, hard-coded values that should be configurable |
| **🟡 Minor** | Nice to fix — style inconsistencies, naming improvements, or minor optimizations | Inconsistent naming, missing documentation, minor code style violations, redundant code |
| **🟢 Suggestion** | Optional improvements — alternative approaches, potential enhancements, or learning opportunities | Refactoring ideas, performance optimizations, alternative patterns, educational notes |

---

## Comprehensive Review Checklist

### 1. Architecture & Design

| Check | What to Verify |
|-------|----------------|
| **Layer boundaries** | Code respects the layered architecture (App → Presentation → Core → Common). No layer accesses a higher layer. Core layer uses Dependency Inversion (interfaces, not concrete implementations) |
| **Package organization** | Files are in the correct package: `service` in App, `screens`/`components`/`viewmodels` in Presentation, `entity`/`repository`/`interactors`/`api`/`storage` in Core, utilities in Common |
| **Dependency direction** | ViewModels depend on Interactors/Repositories (interfaces), not concrete implementations. Repositories depend on Data Sources (interfaces). No circular dependencies |
| **Single Responsibility** | Each class has one clear purpose. No god classes or mixed concerns |
| **Dependency Injection** | Koin is used for DI. Use `single { }` for singletons, `factory { }` for transient instances, `viewModel { }` for ViewModels |
| **Interface segregation** | Interfaces are focused and minimal. Services expose only what clients need |
| **Multiplatform compatibility** | Common code doesn't use platform-specific APIs. Platform-specific code is in `jvmMain`, `androidMain`, or `iosMain` |

### 2. Code Quality & Logic

| Check | What to Verify |
|-------|----------------|
| **Correct logic** | Algorithm is correct, handles all cases, produces expected outputs |
| **Error handling** | All failure paths are handled. Errors are propagated appropriately. No silent failures |
| **Edge cases** | Null values, empty collections, boundary conditions, invalid inputs are handled |
| **Resource cleanup** | Resources (streams, connections, scopes) are properly closed or cancelled |
| **Concurrency safety** | Shared mutable state is protected. Coroutines use appropriate dispatchers and scopes |
| **State consistency** | State transitions are atomic. No race conditions or inconsistent intermediate states |
| **Cancellation handling** | Coroutines respect cancellation. Long-running operations are cancellable |
| **No magic numbers** | Hard-coded values are extracted to named constants with clear meaning |
| **Complexity** | Functions are focused and readable. Complex logic is broken down or documented |

### 3. Code Style & Conventions (Kotlin)

| Check | What to Verify |
|-------|----------------|
| **Naming conventions** | Classes: `PascalCase`, functions/properties: `camelCase`, constants: `UPPER_SNAKE_CASE`, test functions: backtick names with spaces |
| **Kotlin idioms** | Uses `data class`, `sealed class/interface`, expression bodies, default arguments, named parameters, scope functions appropriately |
| **Null safety** | Proper use of `?`, `?.`, `?:`, `!!` (with justification), and nullable types |
| **Immutability** | Prefer `val` over `var`. Use immutable collections (`List`, not `MutableList`) in public APIs |
| **Extension functions** | Used appropriately for utility functions, not to add behavior to external classes you don't own |
| **When expressions** | Exhaustive when used with sealed types. No unnecessary `else` branches |
| **Coroutines style** | `suspend` functions over callbacks, `Flow` for streams, structured concurrency |
| **Visibility modifiers** | Minimal visibility: `private` by default, `internal` for module-level, `public` only when necessary |
| **Formatting** | Consistent indentation (4 spaces), line length (120 chars), proper spacing |
| **Import organization** | No wildcard imports (`*`), unused imports removed, grouped logically |

### 4. Code Duplication & Reusability

| Check | What to Verify |
|-------|----------------|
| **DRY principle** | No duplicated logic. Repeated patterns are extracted to functions or classes |
| **Existing utilities** | Uses existing utility functions from Common layer instead of reimplementing |
| **Common code** | Shared logic between platforms is in `commonMain`, not duplicated per platform |
| **Helper functions** | Repeated test setup or assertions are extracted to helper functions |
| **Composition over inheritance** | Behavior is composed through interfaces and delegation, not deep hierarchies |

### 5. Compose UI Best Practices

| Check | What to Verify |
|-------|----------------|
| **State hoisting** | Stateless composables receive state and callbacks as parameters |
| **Recomposition safety** | Composables don't capture mutable state. Side effects use `LaunchedEffect`, `DisposableEffect`, etc. |
| **ViewModel access** | Only top-level screen composables access ViewModels via `koinViewModel()`. Child composables receive data/callbacks |
| **Preview annotations** | `@Preview` functions use the private overload with hardcoded state, not the public one |
| **Modifiers** | Applied in logical order: size → padding → semantics → interaction → drawing |
| **Semantic properties** | Interactive elements have proper content descriptions for accessibility |
| **Performance** | Expensive operations are not in the composition body. Use `remember`, `derivedStateOf`, or `LaunchedEffect` |
| **Test tags** | Only added when semantic finders are insufficient. Annotated with `@VisibleForTesting` |

### 6. Testing Coverage & Quality

| Check | What to Verify |
|-------|----------------|
| **Test presence** | Every new/modified class with logic has tests. At minimum: happy path, one error case, one edge case |
| **Test completeness** | Tests cover all public API surface. For classes with multiple entry points, every behavior is tested through each entry point |
| **Full assertion** | Tests verify complete output objects (full `UiState`, full entity), not individual fields |
| **Proper mocking** | External dependencies are mocked. Tests are isolated and don't depend on real services |
| **Test structure** | Tests follow project templates (Given/When/Then, proper lifecycle, helpers for duplication) |
| **Test names** | Descriptive backtick names following "when X then Y" pattern |
| **UI tests** | Test the public overload with mocked ViewModel via Koin. Verify state rendering and event wiring |
| **Unit tests** | Use `TestDispatcher`, proper setup/teardown, verify flows and state emissions completely |
| **No flaky tests** | Tests are deterministic. No `Thread.sleep`, no time-based waits, proper use of test coroutines |

### 7. Documentation & Clarity

| Check | What to Verify |
|-------|----------------|
| **Code comments** | Complex logic has explanatory comments. Non-obvious decisions are documented |
| **KDoc** | Public APIs have KDoc comments explaining purpose, parameters, return values, and exceptions |
| **PR description** | Clear description of changes, why they were made, and any trade-offs |
| **TODO comments** | Any TODOs have context, owner, and are tracked (or should be removed if done) |
| **Commit messages** | Descriptive and follow conventions (if project has them) |

### 8. Security & Data Safety

| Check | What to Verify |
|-------|----------------|
| **No secrets in code** | No API keys, passwords, certificates, or tokens hardcoded. Use configuration or secure storage |
| **Input validation** | User input is validated before use. SQL injection, path traversal, and injection attacks are prevented |
| **Sensitive data** | Sensitive information is not logged. Encryption is used where appropriate |
| **Permission checks** | Operations requiring permissions check them before execution |
| **Secure communication** | Network calls use HTTPS. Certificate pinning if required |

### 9. Performance & Efficiency

| Check | What to Verify |
|-------|----------------|
| **Algorithm efficiency** | No unnecessary O(n²) when O(n) exists. Collections operations are efficient |
| **Memory management** | No memory leaks. Large objects are released when no longer needed |
| **Database queries** | Efficient queries. Indexes are used. No N+1 query problems |
| **Network efficiency** | Batching when possible. Appropriate caching. No redundant requests |
| **UI performance** | No blocking operations on main thread. Heavy computations are offloaded |
| **Flow operators** | Proper use of `flowOn`, `buffer`, `conflate` for flow performance |

### 10. Dependencies & Build

| Check | What to Verify |
|-------|----------------|
| **Dependency necessity** | New dependencies are justified. Existing dependencies can't solve the problem |
| **Version compatibility** | Dependencies are compatible with Kotlin Multiplatform and Compose Multiplatform |
| **Version consistency** | Versions are managed in `libs.versions.toml`, not duplicated in build files |
| **Build configuration** | Gradle configuration is correct. No deprecated APIs. Build succeeds |
| **ProGuard rules** | If obfuscation-sensitive code is added, ProGuard rules are updated |

---

## Anti-Patterns to Watch For

| Anti-Pattern | Description | Severity |
|--------------|-------------|----------|
| **🚫 Magic Numbers & Strings** | Hard-coded values without context (e.g., `if (count > 3)`, `delay(5000)`, `status == "active"`). Extract to named constants | 🟡 Minor |
| **🚫 God Classes** | Single class handling multiple unrelated domains (e.g., one class doing kiosks, certificates, builds, users, logging). Note: ViewModels are mediators and can orchestrate multiple interactors — that's their job | 🟠 Major |
| **🚫 Primitive Obsession** | Using `String`/`Int` instead of domain types (e.g., `String` for IDs/platforms instead of `KioskId`, `Platform` sealed class). Easy to mix up parameters | 🟡 Minor |
| **🚫 Stringly-Typed Code** | Using strings for types that should be enums/sealed classes (e.g., `platform: String` instead of `platform: Platform`). Loses type safety and compiler checks | 🟠 Major |
| **🚫 Callback Hell** | Deep nesting from callbacks. Use suspend functions and coroutines instead | 🟠 Major |
| **🚫 Leaking Implementation Details** | Exposing mutable state publicly (e.g., `val kiosks = MutableStateFlow`). Use `private val _state` + public `StateFlow` via `asStateFlow()` | 🔴 Critical |
| **🚫 Boolean Blindness** | Multiple boolean parameters making call sites unclear (e.g., `update(kiosk, true, false, true)`). Use named parameters or domain types | 🟡 Minor |
| **🚫 Null as a Special Case** | Using `null` to represent business states (e.g., `kiosk: Kiosk?` where null means loading/error/not found). Use sealed interfaces for explicit states | 🟠 Major |
| **🚫 Shotgun Surgery** | Single logical change requires modifications in many unrelated places. Indicates poor encapsulation | 🟠 Major |
| **🚫 Flag Arguments** | Boolean parameters that completely change function behavior (e.g., `process(kiosk, isAndroid = true)` with entirely different logic paths). Split into separate functions | 🟡 Minor |
| **🚫 Train Wreck** | Long call chains (e.g., `kiosk.config.security.certificates.first().data.value`). Use delegation or extension functions | 🟡 Minor |
| **🚫 Copy-Paste Programming** | Duplicating code blocks with minor variations instead of extracting common logic. Violates DRY principle | 🟠 Major |
| **🚫 Inappropriate Intimacy** | Classes accessing each other's internals (e.g., `repository._cache.clear()`). Use proper public APIs | 🟠 Major |
| **🚫 Solution Looking for a Problem** | Over-engineering with unnecessary abstractions (e.g., creating `StringProvider` interface just to hold a string). Keep it simple | 🟡 Minor |
| **🚫 Mutable State in Composables** | Business state and logic in composables instead of ViewModels. Use ViewModels for state management | 🔴 Critical |
| **🚫 Error Swallowing** | Catching exceptions without logging or propagating (e.g., empty catch blocks, `println` instead of proper logging) | 🔴 Critical |
| **🚫 Cargo Cult Programming** | Using patterns without understanding why (e.g., excessive chaining of scope functions). Keep code simple and clear | 🟡 Minor |
| **🚫 Anemic Domain Model** | Entities with no behavior, all logic in services. Consider adding behavior to entities (Note: This is organizational preference) | 🟡 Minor |
| **🚫 Feature Envy** | Method uses data from another class more than its own. Move logic to the class that owns the data | 🟡 Minor |
| **🚫 Temporal Coupling** | Methods must be called in specific order but API doesn't enforce it (e.g., must call `prepare()` before `upload()`). Make invalid states unrepresentable | 🟠 Major |
| **🚫 Clever Code** | Overly concise or "clever" one-liners that are hard to read. Prefer clarity over brevity | 🟡 Minor |
| **🚫 Premature Optimization** | Complex optimizations without proven performance issues. Start simple, optimize when profiling shows a problem | 🟡 Minor |
| **🚫 Not Invented Here** | Reimplementing standard library or dependency functionality (e.g., custom `isEmpty()`, date formatting). Use existing libraries | 🟠 Major |
| **🚫 Side Effects in Pure Functions** | Hidden side effects in calculation functions (e.g., logging to database inside `calculateTotal()`). Separate pure calculations from side effects | 🟠 Major |

---

## Common Issues & How to Address Them

| Issue | Problem | Solution | Severity |
|-------|---------|----------|----------|
| **Layer Boundary Violation** | Core/Presentation depending on higher layers (e.g., Core importing Presentation screens, Interactor calling UI code) | Keep layer dependencies one-way: App → Presentation → Core → Common. Move navigation/UI logic to Presentation layer | 🔴 Critical |
| **Code Duplication** | Same validation/logic repeated across multiple functions with minor variations | Extract common logic to shared functions. Use composition or higher-order functions | 🟠 Major |
| **Incomplete Error Handling** | Catching exceptions and returning default values silently (e.g., `catch { emptyList() }`) | Return `Result<T>`, throw specific exceptions, or log errors. Callers must distinguish success from failure | 🔴 Critical |
| **Untested Code** | New ViewModel/Interactor/Repository without corresponding test file | Add unit tests covering happy path, error cases, and edge cases. Follow prepare-unit-tests skill | 🔴 Critical |
| **ViewModel in Child Composable** | Child composables calling `koinViewModel()` instead of receiving state/events as parameters | Use two-overload pattern: public overload resolves ViewModel and hoists state/events, private overload receives them. See prepare-ui-tests skill | 🟠 Major |
| **Missing Test Coverage** | Tests only cover happy path, missing Loading/Error/Empty states | Add tests for all states. UI tests must verify Loading, Success, Error, Empty. Unit tests must verify all state transitions | 🟠 Major |
| **Non-exhaustive When** | Using `else` branch with sealed types, losing compiler exhaustiveness checks | Remove `else` branch. Let compiler force updates when sealed type is extended | 🟡 Minor |
| **Improper Scope Usage** | Using `GlobalScope` instead of proper lifecycle-aware scopes | Use `viewModelScope` for ViewModels, `CoroutineScope(SupervisorJob())` for repositories with proper cleanup | 🔴 Critical |
| **Hard-coded Values** | Magic numbers without names (e.g., `if (count > 100)`, `delay(5000)`) | Extract to named constants: `private const val MAX_COUNT = 100` | 🟡 Minor |
| **Incomplete Test Assertions** | Testing individual fields instead of complete state objects | Use `assertEquals(expectedState, actualState)` to verify entire object, not individual properties | 🟠 Major |
| **Non-specific Exception Types** | Throwing generic `Exception` or `RuntimeException` | Create domain-specific exception types (e.g., `KioskNotFoundException`, `InvalidPlatformException`) | 🟡 Minor |
| **Missing Documentation** | Complex algorithms without KDoc or comments explaining the logic | Add KDoc with algorithm explanation. Extract magic numbers to named constants with clear names | 🟡 Minor |
| **Exposed Mutable State** | Public `MutableStateFlow` or `var` properties allowing external mutation | Use `private val _state = MutableStateFlow()` + `val state = _state.asStateFlow()` pattern | 🔴 Critical |
| **Business Logic in Composables** | `LaunchedEffect` calling APIs, `remember { mutableStateOf() }` for business data | Move all business logic and state management to ViewModels. Composables only render state and emit events | 🔴 Critical |
| **Not Using Result Type** | Functions that can fail returning nullable or throwing without clear contract | Use `Result<T>` for operations that can fail. Makes error handling explicit and composable | 🟠 Major |
| **Unnecessary Nullability** | Using nullable types when value is always present or has a clear default | Use non-null types with defaults. Reserve nullability for genuinely optional values | 🟡 Minor |
| **Missing Koin Registration** | New ViewModel/Repository/Service not registered in DI module | Add to appropriate Koin module: `viewModel { }` for ViewModels, `single { }` for singletons, `factory { }` for transient | 🔴 Critical |
| **Incorrect Test Mocking** | Testing with real dependencies instead of mocks, or using real ViewModel in UI tests | Mock all external dependencies. UI tests must mock ViewModel via Koin. Unit tests mock all injected dependencies | 🟠 Major |
| **Platform Code in Common** | Platform-specific APIs (File, Network) used in commonMain | Use expect/actual declarations or interface abstractions. Keep common code truly multiplatform | 🔴 Critical |
| **Callback-based Async** | Using callbacks instead of suspend functions and coroutines | Convert to suspend functions. Use `Flow` for streams of values | 🟠 Major |

---

## Review Execution Template

### Local Review Report (Pre-Commit)

When performing a local review, use this format to report findings:

```markdown
## Local Review Report

### Files Changed
- [List of modified files from git status]

### Critical Issues (🔴) - Must Fix
- [ ] [Issue description] — [File:Line] — [Why it's critical]

### Major Issues (🟠) - Should Fix
- [ ] [Issue description] — [File:Line] — [Why it's important]

### Minor Issues (🟡) - Nice to Fix
- [ ] [Issue description] — [File:Line] — [Improvement suggestion]

### Suggestions (🟢) - Optional
- [ ] [Optional improvement] — [File:Line] — [Rationale]

### Build & Test Status
- Build: [✅ Success / ❌ Failed]
- Tests: [✅ All Passing / ⚠️ Some Failing / ❌ Multiple Failures]
- Test Coverage: [✅ Adequate / ⚠️ Incomplete / ❌ Missing]

### Summary
- Total Issues: [X Critical, Y Major, Z Minor]
- Commit Ready: [✅ Yes / ❌ No - fix critical/major issues first]

### Next Steps
1. [First recommended action]
2. [Second recommended action]
...

**Note:** This is a report only. No changes have been made. Review findings and decide which issues to address.
```

### PR Review Report (Before Merge)

When performing a PR review, use this format:

```markdown
## PR Review: [PR Title]

### Summary
[Brief overview of what the PR does]

### Critical Issues (🔴)
- [ ] [Issue description] — [File:Line] — [Why it's critical]

### Major Issues (🟠)
- [ ] [Issue description] — [File:Line] — [Why it's important]

### Minor Issues (🟡)
- [ ] [Issue description] — [File:Line] — [Improvement suggestion]

### Suggestions (🟢)
- [ ] [Optional improvement] — [File:Line] — [Rationale]

### Positive Observations
- ✅ [What was done well]
- ✅ [Good practices followed]

### Testing
- [ ] Unit tests present: [Yes/No] — Coverage: [Good/Needs work]
- [ ] UI tests present: [Yes/No] — Coverage: [Good/Needs work]
- [ ] Edge cases covered: [Yes/No]
- [ ] Error handling tested: [Yes/No]

### Verdict
- ✅ **Approved** — Ready to merge
- ⚠️ **Approved with suggestions** — Can merge, but consider the suggestions
- 🔄 **Changes requested** — Address critical/major issues before merge
- ❌ **Blocked** — Critical issues must be resolved
```

---

## Quick Reference: Priority Order

Review in this order to catch the most critical issues first:

1. **Correctness** — Does it work? Does it break anything?
2. **Security** — Any security vulnerabilities or data safety issues?
3. **Architecture** — Does it follow layer boundaries and dependency rules?
4. **Testing** — Are critical paths tested?
5. **Error handling** — Are failures handled gracefully?
6. **Logic quality** — Is the implementation sound and handles edge cases?
7. **Code duplication** — Is logic DRY and reusable?
8. **Style & conventions** — Does it follow Kotlin and project style?
9. **Documentation** — Is complex logic explained?
10. **Performance** — Are there obvious inefficiencies?

---

## Red Flags (Immediate Attention Required)

- ❌ Security vulnerability (secrets in code, SQL injection, XSS)
- ❌ Data loss risk (improper error handling, missing transaction boundaries)
- ❌ Layer boundary violation (Core depending on App/Presentation)
- ❌ Missing critical tests (new business logic without unit tests)
- ❌ Breaking changes without migration path
- ❌ Memory leaks (uncancelled coroutines, unclosed resources)
- ❌ Silent failures (caught exceptions without logging or propagation)
- ❌ Exposed internal implementation (public APIs revealing Core internals)

---

## Review Etiquette

- **Be specific** — Point to exact lines, provide concrete examples
- **Be constructive** — Suggest solutions, not just problems
- **Be respectful** — Assume good intent, acknowledge effort
- **Be educational** — Share knowledge, link to documentation
- **Be pragmatic** — Distinguish between blockers and nice-to-haves
- **Be consistent** — Apply standards uniformly across the codebase
- **Praise good work** — Highlight well-written code and smart solutions

---

## When to Ask for Clarification

If any of these situations arise, ask the PR author before requesting changes:

- The change introduces a pattern not seen elsewhere in the codebase
- The architectural decision has multiple valid approaches
- The business logic behavior is unclear or ambiguous
- The trade-offs between options are not obvious
- The requirement itself seems incomplete or contradictory
- Tests are missing but the reason might be intentional (e.g., pure data class)

**Wrong tests that pass are worse than no tests.** Always verify your understanding before mandating test changes.

---

## Final Checklist

### For Local Reviews (Report Mode)

Before presenting the review report, verify:

- [ ] All changed files have been inspected
- [ ] Issues are categorized by severity (Critical, Major, Minor, Suggestion)
- [ ] Each issue includes file location and clear explanation
- [ ] Build and test status are checked and reported
- [ ] Next steps are clearly outlined
- [ ] Report makes it clear: no changes have been made automatically

### For PR Reviews (Before Approving)

Before approving, verify:

- [ ] No critical issues remain unresolved
- [ ] Architecture principles are respected
- [ ] Tests exist and cover key scenarios
- [ ] Code style is consistent with the project
- [ ] Error handling is present and appropriate
- [ ] No code duplication or DRY violations
- [ ] Documentation exists for complex logic
- [ ] No security or performance red flags
- [ ] Build will succeed (if uncertain, ask for build confirmation)
- [ ] PR scope is focused and doesn't mix unrelated changes

---

## Remember

**For Local Reviews:** Always report findings first. Never automatically fix issues. Wait for explicit approval to address each issue. The developer drives the process — you provide insights.

**For PR Reviews:** A good review protects the codebase while empowering the team. Focus on what matters most: correctness, maintainability, and alignment with project standards. Be thorough but not pedantic. Ship quality code.





