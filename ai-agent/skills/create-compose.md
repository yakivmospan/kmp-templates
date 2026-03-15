---
name: create-compose
description: >
  Use this skill when creating a new Compose Multiplatform screen or component, or when
  updating an existing one. Triggers include: any request to add, create, build, or update
  a Compose screen, a Compose component, a UI layout, or a Composable function. Also use
  when asked about Compose architecture, state hoisting, recomposition optimisation, or
  stability annotations. Do NOT use for writing UI tests — use the compose-ui-tests skill
  instead. Do NOT use for ViewModel logic in isolation — use the unit-tests skill instead.
---

# Create or Update Compose Screen / Component

## Rules

| Rule | Detail |
|------|--------|
| **Two-overload pattern** | Every screen, and any component that accepts state or an `onEvent` lambda, exposes two overloads: a **public** ViewModel-connected one and a **private** stateless one used for Previews. |
| **Previews** | Screens: one `@PreviewLightDark` per distinct UiState variant + one at `fontScale = 2f`. Reusable components: at least one preview. Private screen-local sub-components: only if complex enough to warrant it — the parent screen preview covers simple cases. |
| **Hoist state** | State lives at the lowest common ancestor that needs it. Stop hoisting when only one composable needs the state. |
| **Pass plain values to children** | Children receive plain values, not entire state objects or `State<T>` wrappers. |
| **`onEvent` for screens** | Screens and screen-specific components expose `onEvent: (Event) -> Unit`. Never pass ViewModel references into child composables. Reusable components expose typed lambdas (`onConfirm: () -> Unit`, etc.) instead. |
| **Single sealed Event type** | All screen interactions go through one `sealed class` via `onEvent`. Never substitute multiple callback lambdas for a ViewModel interaction boundary. |
| **`sealed class` for UiState and Event** | Prefer `sealed class` — the compiler infers stability from subclasses automatically. If `sealed interface` is used it must be annotated `@Immutable`. |
| **`ImmutableList<T>` for collections** | `List<T>` is treated as unstable by the compiler. Use `ImmutableList<T>` (Kotlinx Immutable Collections) for all UI state collections. |
| **`data class` with `val` only** | All UiState and UI model types use `val` properties only. |
| **`@Immutable` vs `@Stable`** | `@Immutable`: all properties are `val`, no mutations after construction. `@Stable`: `equals()` is reliable; may mutate but notifies Compose. Never apply `@Immutable` to a mutable type. |
| **Simple lambdas by default** | Plain inline lambdas are correct in most cases. Stabilise with `remember` only when profiling shows unnecessary recomposition. In `LazyColumn`, pass `viewModel::onEvent` with id in the event — avoid `remember(item.id) { { ... } }` per item. |
| **`derivedStateOf`** | Use inside `remember { }` only when a UI-local value changes less frequently than its upstream state (e.g. scroll position → button visibility). Never for ViewModel data transformations. |
| **`remember` for expensive work** | Wrap expensive computations in `remember { }` with correct keys. Never place sorting, filtering, or mapping directly inside `items {}` — it runs on every scroll frame. |
| **Lazy layout keys** | Always pass a stable `key` to `items()`. Without it, list reorders recompose every item instead of just the moved one. |
| **Lambda modifiers for frame-rate state** | When state changes every frame (scroll offset, animation), use the lambda modifier variant: `Modifier.offset { }` not `Modifier.offset(y = )`, `Modifier.drawBehind { }` not `Modifier.background()`. Skips composition and layout phases entirely. |
| **No backwards writes** | Never write to a `State` object after reading it in the same composition body — causes an infinite recomposition loop. Write only in event lambdas or `LaunchedEffect`. |
| **Side effects** | Use `CollectSideEffects(viewModel.sideEffects) { }` in the public overload. Never pass `SharedFlow` down the tree. Never use a raw `LaunchedEffect` for side-effect collection. Concrete handlers go in `private suspend fun` under a `// Side effects` comment. |
| **`CollectSideEffects` signature** | `@Composable fun <T> CollectSideEffects(flow: SharedFlow<T>, handler: suspend (T) -> Unit)` — from `ui/utils/SideEffects.kt`. |
| **Modifier convention** | `modifier: Modifier = Modifier` after required parameters, before optional styling. |
| **No hard-coded dimensions** | Use `MaterialTheme.spacing` or design system tokens. |
| **Content descriptions** | Every interactive or meaningful element has a `contentDescription` describing its **purpose** (not type, not appearance). Decorative elements: `contentDescription = null`. Always use `stringResource` — no hardcoded strings. List item descriptions must be unique per item. |
| **Touch target size** | Minimum 48dp × 48dp for every interactive element. Use `Modifier.minimumInteractiveComponentSize()` or padding to reach the minimum when the visual is smaller. |
| **Text sizes in `sp`** | Always `sp` (or `MaterialTheme.typography`) for text — never `dp`. Layouts must survive font scale 200%: use `wrapContentHeight()`, not fixed heights, on text containers. |
| **Semantics: merge related elements** | Wrap logically related composables (icon + title + subtitle) with `Modifier.semantics(mergeDescendants = true) {}` so TalkBack announces them as one unit. |
| **Semantics: headings** | Mark section headings with `Modifier.semantics { heading() }`. |
| **Semantics: custom interactive state** | Custom toggles/switches must expose `role` and `stateDescription` via `Modifier.semantics { role = Role.Switch; stateDescription = "..." }`. |
| **Live regions** | Dynamic content that updates without user interaction (status messages, async results) needs `Modifier.semantics { liveRegion = LiveRegionMode.Polite }`. |
| **Keyboard / Switch Access focus** | All interactive elements must be focusable and reachable in logical composition order. Dialogs trap focus while open. Restore focus to a sensible element after a dialog closes via `FocusRequester.requestFocus()` in a `LaunchedEffect`. |
| **Colour alone** | Never convey information by colour alone — always pair with icon, label, or shape. |
| **Colour contrast — EAA mandatory** | Normal text (< 18sp, or < 14sp bold): ≥ 4.5:1. Large text (≥ 18sp, or ≥ 14sp bold): ≥ 3:1. UI component boundaries and meaningful graphics: ≥ 3:1. Check all interactive states; disabled components are exempt. |
| **Flashing — EAA mandatory** | Nothing may flash more than 3 times per second (WCAG 2.3.1). |
| **Session timeouts — EAA mandatory** | Warn before session expiry; give ≥ 20 seconds to extend. Auto-advancing content must be pausable (WCAG 2.2.1). |
| **Form errors — EAA mandatory** | Errors identified in text (not colour alone) with a correction suggestion. Error state announced via `Modifier.semantics { error("...") }` or a live region (WCAG 3.3.1 / 3.3.3). |
| **Gesture alternatives — EAA mandatory** | Every multi-point or path-based gesture (swipe, pinch, drag) must have a single-pointer alternative (WCAG 2.5.1). |

---

## Two-Overload Pattern

```kotlin
// Public — ViewModel-connected.
@Composable
fun ExampleScreen(
    modifier: Modifier = Modifier,
    viewModel: ExampleViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectSideEffects(viewModel.sideEffects) { effect ->
        when (effect) {
            is ExampleSideEffect.NavigateTo -> { /* handle */ }
        }
    }
    ExampleScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}

// Private — stateless, Preview-friendly.
@Composable
private fun ExampleScreen(
    state: ExampleUiState,
    onEvent: (ExampleEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ExampleUiState.Loading -> LoadingContent(modifier)
        is ExampleUiState.Loaded  -> LoadedContent(state, onEvent, modifier)
        is ExampleUiState.Error   -> ErrorContent(state, onEvent, modifier)
    }
}
```

### Non-obvious semantics APIs

```kotlin
// Merge related elements into one TalkBack announcement.
Row(modifier = Modifier.semantics(mergeDescendants = true) {}) { ... }

// Mark a heading so TalkBack users can jump between sections.
Text(modifier = Modifier.semantics { heading() }, ...)

// Expose state on a custom toggle.
Box(modifier = Modifier.semantics { role = Role.Switch; stateDescription = "Enabled" })

// Announce dynamic content updates automatically.
Text(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, ...)

// Announce form field errors.
OutlinedTextField(modifier = Modifier.semantics { error("Enter a valid email") }, ...)
```

---

## Template

```kotlin
// ── UiState & Events ──────────────────────────────────────────────────────────

sealed class ExampleUiState {
    data object Loading : ExampleUiState()
    data class Loaded(val items: ImmutableList<ItemUi>) : ExampleUiState()
    data class Error(val cause: Throwable? = null) : ExampleUiState()
}

sealed class ExampleEvent {
    data class ItemClicked(val id: String) : ExampleEvent()
    data object RetryClicked : ExampleEvent()
}

// ── Public overload ───────────────────────────────────────────────────────────

@Composable
fun ExampleScreen(
    modifier: Modifier = Modifier,
    viewModel: ExampleViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectSideEffects(viewModel.sideEffects) { /* handle side effects */ }
    ExampleScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}

// ── Private overload ──────────────────────────────────────────────────────────

@Composable
private fun ExampleScreen(
    state: ExampleUiState,
    onEvent: (ExampleEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ExampleUiState.Loading -> LoadingContent(modifier)
        is ExampleUiState.Loaded  -> LoadedContent(state, onEvent, modifier)
        is ExampleUiState.Error   -> ErrorContent(state, onEvent, modifier)
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LoadedContent(
    state: ExampleUiState.Loaded,
    onEvent: (ExampleEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(state.items, key = { it.id }) { item ->
            ItemRow(item = item, onEvent = onEvent)
        }
    }
}

@Composable
private fun ItemRow(
    item: ItemUi,
    onEvent: (ExampleEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEvent(ExampleEvent.ItemClicked(item.id)) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Text(item.title)
    }
}

@Composable
private fun ErrorContent(
    state: ExampleUiState.Error,
    onEvent: (ExampleEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onRetry = remember { { onEvent(ExampleEvent.RetryClicked) } }
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Something went wrong")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun ExampleScreenLoadingPreview() {
    AppTheme { ExampleScreen(state = ExampleUiState.Loading, onEvent = {}) }
}

@PreviewLightDark
@Composable
private fun ExampleScreenLoadedPreview() {
    AppTheme {
        ExampleScreen(
            state = ExampleUiState.Loaded(
                items = persistentListOf(
                    ItemUi(id = "1", title = "Item One"),
                    ItemUi(id = "2", title = "Item Two"),
                )
            ),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ExampleScreenErrorPreview() {
    AppTheme { ExampleScreen(state = ExampleUiState.Error(), onEvent = {}) }
}

@Preview(fontScale = 2f, name = "Large font")
@Composable
private fun ExampleScreenLargeFontPreview() {
    AppTheme {
        ExampleScreen(
            state = ExampleUiState.Loaded(items = persistentListOf(ItemUi("1", "Item One"))),
            onEvent = {},
        )
    }
}
```

---

## Checklist

**Architecture**
- [ ] Public overload is ViewModel-connected; private overload is stateless and Preview-friendly
- [ ] Every screen and every component accepting state or `onEvent` has both overloads
- [ ] Side effects use `CollectSideEffects`; no raw `LaunchedEffect` for side-effect collection

**State & events**
- [ ] `onEvent: (Event) -> Unit` everywhere; no ViewModel references below the public overload
- [ ] Children receive plain values, not state objects or `State<T>`
- [ ] State hoisted to lowest common ancestor only

**Stability**
- [ ] UiState and Event are `sealed class`; if `sealed interface` is used it is annotated `@Immutable`
- [ ] All UI model `data class` types use `val` only
- [ ] Collections are `ImmutableList<T>`

**Performance**
- [ ] Simple lambdas by default; `remember` only where profiling justifies it
- [ ] `LazyColumn` passes `viewModel::onEvent`; no per-item `remember` allocations
- [ ] `derivedStateOf` used only for UI-local derived state, not ViewModel data
- [ ] Expensive computations and list transformations wrapped in `remember`; none inside `items {}`
- [ ] Every `LazyColumn` / `LazyRow` `items()` call has a stable `key`
- [ ] Frame-rate state (animation, scroll offset) read via lambda modifiers
- [ ] No backwards writes

**Previews**
- [ ] Screens: one `@PreviewLightDark` per UiState variant + one at `fontScale = 2f`
- [ ] Reusable components: at least one preview
- [ ] Private sub-components: preview only if complexity warrants it

**Accessibility**
- [ ] Every interactive/meaningful element has a `contentDescription` (purpose, not type); decorative elements have `null`
- [ ] All `contentDescription` strings use `stringResource`; list descriptions are unique per item
- [ ] Every interactive element meets 48dp × 48dp minimum touch target
- [ ] All text in `sp` or `MaterialTheme.typography`; text containers use `wrapContentHeight` not fixed heights
- [ ] Logically related elements use `semantics(mergeDescendants = true)`
- [ ] Section headings marked with `semantics { heading() }`
- [ ] Custom interactive elements expose `role` and `stateDescription` via semantics
- [ ] Dynamic content updates use `liveRegion = LiveRegionMode.Polite`
- [ ] All interactive elements keyboard/Switch Access focusable; dialogs trap and restore focus
- [ ] Information never conveyed by colour alone
- [ ] Normal text contrast ≥ 4.5:1; large text ≥ 3:1; UI boundaries and meaningful graphics ≥ 3:1 (all states; disabled exempt)
- [ ] Nothing flashes > 3 times/second
- [ ] Session timeouts give ≥ 20s to extend; auto-advancing content is pausable
- [ ] Form errors identified in text with correction suggestion; announced via `semantics { error(...) }`
- [ ] Every swipe/pinch/drag gesture has a single-pointer alternative

**Misc**
- [ ] No hard-coded dimensions — design tokens or `Dp` constants used