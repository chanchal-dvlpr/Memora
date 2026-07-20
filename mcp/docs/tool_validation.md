# Strict Input & Output Validation Rules

This document specifies the validation engine rules implemented in the custom `ToolValidator` class.

## Parameter Input Validation

All tool calls are validated against the schema defined in `ToolInputSchema` with support for:
- **Required parameters**: Validates presence.
- **Unknown properties detection**: Rejects arguments that are not defined in the schema to ensure strict type boundaries.
- **Primitive Type Validations**: Validates `string`, `number`, `boolean`, `array`, and `object`.
- **Enum restriction**: Limits parameter values to a static array list.
- **String boundaries**: Support for `minLength`, `maxLength`, and regex `pattern` validations.
- **Numeric boundaries**: Support for `minimum` and `maximum` range checks.
- **Recursive objects**: Validates nested schema properties.

## Output Structure Validation

Every response from tool execution is validated to enforce security and serialization compliance:
- **Structure verification**: Asserts type and schema compatibility of returned objects.
- **Formatting constraints**: Checks that `content` array elements match text layouts.
- **JSON Serialization test**: Tests that objects can be successfully serialized without circular loops or invalid types.
