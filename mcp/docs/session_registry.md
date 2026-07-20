# Session Registry

This document describes the `SessionRegistry` component, responsible for managing the active session inventory.

## 1. Responsibilities

- Maintains an in-memory dictionary of session entities keyed by unique `SessionId`.
- Prevents duplicate session registrations by throwing `DuplicateSessionError`.
- Enforces session view immutability using deep freezes on returned structures.
- Provides deterministic listing order based on creation timestamps and session ID string comparisons.

## 2. API Reference

- **`createSession(id, metadata, attributes, context, state)`**: Creates a new session record and returns an immutable view.
- **`getSession(id)`**: Looks up a session by ID.
- **`updateSession(id, updates)`**: Performs partial updates on attributes, context, or state.
- **`removeSession(id)`**: Unregisters a session.
- **`hasSession(id)`**: Checks for existence.
- **`listSessions()`**: Returns a sorted, immutable list of all registered sessions.
