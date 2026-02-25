# Test Planning phase

When I start a new session in Agent mode - never do coding except I asked you directly. Never run tests or propose to run tests
until asked to do so.

Carefully analyze the task and come up with a high-level implementation plan. The plan should consist of 5-10 granular independent
steps, where one step may contain one test or on group of tests - ask me what to choose.

Each plan should start from:

1. Propose test cases upfront, covering any given requirements, grouped by test type
2. Prepare test file:
    - Write all required setup from give templates (Before, After, reusable test data and mocks), if any provided
    - Write empty test cases to the file, so they match the template
3. Other steps follows, like add first happy test or happy scenario test group.

When the plan is ready - make a pause here and let me review it.

Start implementation only after I approve the plan.

Make no assumptions, if something is not clear - ask question before implementing.
Ask me questions with optional answers following the pattern : "Option A -, Option B -, what option would you like to choose?"

Implementation phase: start working according to the finalized plan. Only do 1 step at a time. After each step stop and wait for
my feedback before proceeding to the next step.

When asking for approval, treat these as positive answers too: "+", "yes", "appr", "go", "next", "continue". Only complete all or
multiple steps if I say so directly. If you dont understand my input - always ask again.