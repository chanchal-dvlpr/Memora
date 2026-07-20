# MCP Prompt Registry

The `PromptRegistry` is a thread-safe, runtime registry that acts as the single source of truth for prompt definitions and metadata.

---

## 1. Registry Operations

The registry supports the following lifecycle operations:

- **`registerPrompt(prompt, metadata)`**: Registers an executable prompt definition along with its discovery metadata. If a prompt with the same name already exists, it throws a `PromptRegistrationError`.
- **`unregisterPrompt(name)`**: Unregisters an existing prompt. Throws a `PromptNotFoundError` if not found.
- **`getPrompt(name)`**: Returns the executable definition of the prompt.
- **`getMetadata(name)`**: Returns the metadata schema associated with the prompt.
- **`hasPrompt(name)`**: Checks if a prompt with the given name exists.
- **`listPrompts()`**: Returns a read-only list of all registered prompt definitions, sorted alphabetically by name.

---

## 2. Immutability Guarantees

To ensure that the runtime configuration cannot be modified or corrupted post-registration:
1. **Freeze**: The registry recursively freezes both the `PromptDefinition` and `PromptMetadata` objects during registration using a recursive `deepFreeze` helper.
2. **Immutable Returns**: Operations like `listPrompts()` return Object-frozen arrays to prevent external code from mutating the registry's collections.

---

## 3. Expanded Discovery Metadata Schema

The registry stores a fully fledged prompt metadata layout returned via `prompts/list`:

- `name`: Unique prompt identifier.
- `displayName`: Client-facing readable label.
- `description` & `longDescription`: Descriptions of usage.
- `version`: SemVer string.
- `author`: Creation author details.
- `category` / `categories`: Logical groupings (e.g. system, code).
- `tags`: Tag lists for filtering.
- `examples`: Usage input parameter examples.
- `arguments`: Argument constraints and schemas.
- `visibility`: Access visibility constraints (public/internal).
- `experimentalFlag` & `deprecationFlag`: Stability state indicators.
