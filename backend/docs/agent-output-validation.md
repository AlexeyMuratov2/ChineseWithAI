# Agent Output Validation

## How it works

The final output validation pipeline has two layers:

1. `DefaultOutputValidator` always validates the JSON shape described by `output_contract_json`.
2. `OutputValidationStrategy` components add agent-specific validation when they automatically match the current `AgentProfile` and `OutputContract`.

The runtime flow is:

1. `FinalOutputValidationService` parses the model output into JSON.
2. Built-in contract validation runs against `profile.outputContract()`.
3. `OutputValidationStrategyCatalog.resolve(...)` selects every strategy whose `supports(...)` method returns `true`.
4. Matching strategies run in `order()` order and add extra `OutputValidationIssue` items.

## Main extension points

### `OutputContract`

Use these helpers when you need to match a validator to a contract:

- `hasRequiredField(...)`
- `hasAllRequiredFields(...)`
- `matchesRequiredFieldsExactly(...)`
- `rawJson()`

`rawJson()` is available for future cases where a validator needs details from the original `output_contract_json`, not only `requiredFields`.

### `OutputValidationStrategy`

To add agent-specific validation:

1. Create a Spring `@Component` that implements `OutputValidationStrategy`.
2. Make `supports(OutputValidationStrategyRequest request)` as narrow as possible.
3. Use `request.profile().profileKey()` and `request.profile().outputContract()` to decide whether the strategy should apply.
4. Return structured `OutputValidationIssue` values from `validate(...)`.

Example pattern:

```java
@Component
public class MyAgentOutputValidationStrategy implements OutputValidationStrategy {

    @Override
    public boolean supports(OutputValidationStrategyRequest request) {
        return request.profile().profileKey().startsWith("my-agent:")
                && request.profile().outputContract().hasAllRequiredFields(Map.of(
                        "summary", OutputFieldType.STRING,
                        "items", OutputFieldType.ARRAY));
    }

    @Override
    public List<OutputValidationIssue> validate(OutputValidationStrategyRequest request) {
        // Add agent-specific rules here.
        return List.of();
    }
}
```

## Guidelines for future changes

- Keep `supports(...)` deterministic and cheap.
- Match on both profile and contract when possible, so validator selection stays explicit even if profiles evolve.
- Prefer contract helpers for common cases. Drop to `rawJson()` only when the contract shape carries extra semantics.
- Use `order()` only when multiple strategies are intentionally composed.
- For tests and fixtures, prefer `OutputContract.ofRequiredFields(...)` instead of hand-writing raw JSON.

## Legacy note

The database column `output_validation_strategy_key` still exists for historical compatibility, but runtime strategy selection now happens automatically through `supports(...)`.
