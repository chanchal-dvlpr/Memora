/* eslint-disable @typescript-eslint/no-explicit-any */
import { PromptArgument, PromptArgumentType, PromptInvocationResult } from '../types/prompt';
import { PromptValidationError, PromptOutputValidationError } from '../errors';

export class PromptValidator {
  /**
   * Performs validation on the prompt's invocation arguments.
   * Modifies the args object to populate default values and parse strings where necessary.
   */
  public static validateInput(
    argumentsDef: PromptArgument[] | undefined,
    args: Record<string, any>
  ): void {
    if (!args || typeof args !== 'object') {
      throw new PromptValidationError('Arguments must be an object.');
    }

    const defs = argumentsDef || [];
    const allowedNames = new Set(defs.map((d) => d.name));

    // 1. Detect unknown parameters
    for (const key of Object.keys(args)) {
      if (!allowedNames.has(key)) {
        throw new PromptValidationError(`Unknown prompt argument detected: "${key}".`);
      }
    }

    // 2. Inject default values and run constraints
    for (const def of defs) {
      let val = args[def.name];

      // Default value injection
      if ((val === undefined || val === null || val === '') && def.defaultValue !== undefined) {
        args[def.name] = def.defaultValue;
        val = def.defaultValue;
      }

      // Check required
      if (def.required) {
        if (val === undefined || val === null || val === '') {
          throw new PromptValidationError(`Missing required prompt argument: "${def.name}".`);
        }
      }

      // Run advanced validation on present values
      if (val !== undefined && val !== null && val !== '') {
        args[def.name] = this.validateValue(def.name, val, def);
      }
    }
  }

  /**
   * Validates a single value against its schema definition, returning the normalized/parsed value.
   */
  private static validateValue(name: string, val: any, def: PromptArgument): any {
    let target = val;

    // Type Conversion / Parsing
    if (def.type) {
      if (def.type === PromptArgumentType.NUMBER) {
        if (typeof target === 'string') {
          const num = Number(target);
          if (isNaN(num)) {
            throw new PromptValidationError(`Argument "${name}" must be a number representation, received: "${val}".`);
          }
          target = num;
        } else if (typeof target !== 'number') {
          throw new PromptValidationError(`Argument "${name}" must be a number.`);
        }
      } else if (def.type === PromptArgumentType.BOOLEAN) {
        if (typeof target === 'string') {
          if (target !== 'true' && target !== 'false') {
            throw new PromptValidationError(`Argument "${name}" must be a boolean representation, received: "${val}".`);
          }
          target = target === 'true';
        } else if (typeof target !== 'boolean') {
          throw new PromptValidationError(`Argument "${name}" must be a boolean.`);
        }
      } else if (def.type === PromptArgumentType.ARRAY) {
        if (typeof target === 'string') {
          try {
            target = JSON.parse(target);
          } catch {
            throw new PromptValidationError(`Argument "${name}" must be a valid JSON array representation.`);
          }
        }
        if (!Array.isArray(target)) {
          throw new PromptValidationError(`Argument "${name}" must be an array.`);
        }
      } else if (def.type === PromptArgumentType.OBJECT) {
        if (typeof target === 'string') {
          try {
            target = JSON.parse(target);
          } catch {
            throw new PromptValidationError(`Argument "${name}" must be a valid JSON object representation.`);
          }
        }
        if (typeof target !== 'object' || target === null || Array.isArray(target)) {
          throw new PromptValidationError(`Argument "${name}" must be an object.`);
        }
      } else if (def.type === PromptArgumentType.STRING) {
        if (typeof target !== 'string') {
          throw new PromptValidationError(`Argument "${name}" must be a string.`);
        }
      }
    }

    // Constraints Validation
    
    // Enum match
    if (def.enum) {
      if (!def.enum.includes(String(target))) {
        throw new PromptValidationError(`Argument "${name}" must be one of: ${def.enum.join(', ')}.`);
      }
    }

    // Regex pattern
    if (def.pattern && typeof target === 'string') {
      const regex = new RegExp(def.pattern);
      if (!regex.test(target)) {
        throw new PromptValidationError(`Argument "${name}" does not match pattern: ${def.pattern}.`);
      }
    }

    // Numeric min/max
    if (typeof target === 'number') {
      if (def.minimum !== undefined && target < def.minimum) {
        throw new PromptValidationError(`Argument "${name}" must be >= ${def.minimum}.`);
      }
      if (def.maximum !== undefined && target > def.maximum) {
        throw new PromptValidationError(`Argument "${name}" must be <= ${def.maximum}.`);
      }
    }

    // String length
    if (typeof target === 'string') {
      if (def.minLength !== undefined && target.length < def.minLength) {
        throw new PromptValidationError(`Argument "${name}" length must be >= ${def.minLength}.`);
      }
      if (def.maxLength !== undefined && target.length > def.maxLength) {
        throw new PromptValidationError(`Argument "${name}" length must be <= ${def.maxLength}.`);
      }
    }

    // Array items
    if (Array.isArray(target) && def.items) {
      for (let idx = 0; idx < target.length; idx++) {
        const item = target[idx];
        if (typeof item !== def.items.type) {
          throw new PromptValidationError(`Array item at index ${idx} of "${name}" must be of type ${def.items.type}.`);
        }
      }
    }

    // Nested object properties
    if (typeof target === 'object' && target !== null && !Array.isArray(target) && def.properties) {
      const allowedKeys = new Set(Object.keys(def.properties));
      for (const k of Object.keys(target)) {
        if (!allowedKeys.has(k)) {
          throw new PromptValidationError(`Unknown property "${k}" in nested object "${name}".`);
        }
      }

      for (const [propName, propDef] of Object.entries(def.properties)) {
        const propVal = target[propName];
        if (propDef.required && (propVal === undefined || propVal === null)) {
          throw new PromptValidationError(`Missing required property "${propName}" in nested object "${name}".`);
        }
        if (propVal !== undefined && propVal !== null) {
          target[propName] = this.validateValue(`${name}.${propName}`, propVal, propDef);
        }
      }
    }

    return target;
  }

  /**
   * Validates the generated prompt message outputs.
   */
  public static validateOutput(result: PromptInvocationResult): void {
    if (!result) {
      throw new PromptOutputValidationError('Prompt invocation did not return a result.');
    }

    if (!Array.isArray(result.messages)) {
      throw new PromptOutputValidationError('Prompt messages must be an array.');
    }

    if (result.messages.length === 0) {
      throw new PromptOutputValidationError('Prompt messages array must be non-empty.');
    }

    const allowedRoles = new Set(['user', 'assistant', 'system']);
    let seenNonSystem = false;

    for (let idx = 0; idx < result.messages.length; idx++) {
      const msg = result.messages[idx];
      if (!msg || typeof msg !== 'object') {
        throw new PromptOutputValidationError(`Message at index ${idx} must be an object.`);
      }

      if (!allowedRoles.has(msg.role)) {
        throw new PromptOutputValidationError(`Message at index ${idx} has an invalid role: "${msg.role}".`);
      }

      // Message ordering validation: system messages must come before any user/assistant messages
      if (msg.role === 'system') {
        if (seenNonSystem) {
          throw new PromptOutputValidationError(`System message at index ${idx} appeared after a user or assistant message.`);
        }
      } else {
        seenNonSystem = true;
      }

      const content = msg.content;
      if (!content || typeof content !== 'object') {
        throw new PromptOutputValidationError(`Message at index ${idx} must contain a valid content object.`);
      }

      if (content.type === 'text') {
        if (typeof content.text !== 'string') {
          throw new PromptOutputValidationError(`Message at index ${idx} content text must be a string.`);
        }
      } else if (content.type === 'image') {
        if (typeof content.data !== 'string' || typeof content.mimeType !== 'string') {
          throw new PromptOutputValidationError(`Message at index ${idx} content image must contain base64 data and mimeType.`);
        }
      } else if (content.type === 'resource') {
        if (!content.resource || typeof content.resource !== 'object' || typeof content.resource.uri !== 'string') {
          throw new PromptOutputValidationError(`Message at index ${idx} content resource must contain a valid resource object with a uri.`);
        }
      } else {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        throw new PromptOutputValidationError(`Message at index ${idx} has an unknown content type: "${(content as any).type}".`);
      }

      // Check serialization compatibility & circular references
      try {
        JSON.stringify(msg);
      } catch (err) {
        throw new PromptOutputValidationError(`Message at index ${idx} contains circular references or is not JSON-serializable.`);
      }
    }
  }
}
