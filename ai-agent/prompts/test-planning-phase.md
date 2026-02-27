# Test Planning Phase

When I start a new session in Agent mode - never do coding except I asked you directly. Never run tests or propose to run tests
until asked to do so.

Carefully analyze the task and come up with a high-level implementation plan. The plan should consist of 5-10 granular independent
steps, where one step may contain one test or one group of tests - ask me what to choose.

Each plan should start from:

1. Propose test cases upfront, covering any given requirements, grouped by the section structure defined in the relevant skill
   template (e.g. for compose-ui-tests: state rendering, extra ViewModel properties, conditional visibility, interactions, empty
   states; for unit-tests: happy path, error conditions, edge cases). If multiple skills apply, group by each skill's sections
   separately.
2. Prepare test files:
    - Write all required setup from given templates (Before, After, reusable test data, fakes and mocks), if any provided
    - Write all empty test stubs to the file matching the skill template structure — no implementation yet, bodies remain empty
3. Other steps follow, filling in test bodies group by group — one group per step, in the order they appear in the template.

When the plan is ready - make a pause here and let me review it.

Start implementation only after I approve the plan.

If anything is unclear — requirements, screen contract, data shape, edge cases, or anything else — ask before implementing.
The relevant skill (looked up by name or path, e.g. `compose-ui-tests` or `../skills/prepare-ui-tests.md`) may provide
additional context on patterns, but clarification scope is not limited to the skill — ask about anything that could affect
the tests.

Ask me questions with optional answers following the pattern: "Option A -, Option B -, what option would you like to choose?"

Implementation phase: start working according to the finalized plan. Only do 1 step at a time. After each step stop and wait for
my feedback before proceeding to the next step.

When asking for approval, treat these as positive answers too: "+", "yes", "appr", "go", "next", "continue". Only complete all or
multiple steps if I say so directly. If you don't understand my input - always ask again.

## Reference Files
- [unit-tests](../skills/prepare-unit-tests.md)  — template and rules for ViewModel tests
- [compose-ui-tests](../skills/prepare-ui-tests.md) — template and rules for Screen UI tests