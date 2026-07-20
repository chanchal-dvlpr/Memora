# Session Events Model

This document outlines the session event framework and pub/sub mechanisms.

## 1. Supported Event Types

The event model emits notifications for session lifecycle changes:

- **`SessionCreated`**: Fired when a new session is initialized.
- **`SessionOpened`**: Fired when a session transitions to active state.
- **`SessionUpdated`**: Fired when attributes or state are updated.
- **`SessionTouched`**: Fired when last activity timestamp is refreshed.
- **`SessionExpired`**: Fired when absolute or sliding timeouts expire a session.
- **`SessionClosed`**: Fired when a session is closed cleanly.
- **`SessionRemoved`**: Fired when a session is deleted from the registry.

## 2. Session EventEmitter

The `SessionEventEmitter` class enables event subscription:

- **`on(type, listener)`**: Subscribes a listener to a specific event type or `*` for all events. Returns an unsubscribe function.
- **`off(type, listener)`**: Unsubscribes a listener.
- **`emit(event)`**: Dispatches an event to subscribed listeners.
