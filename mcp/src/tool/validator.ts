import { ToolInputSchema, ToolExecutionResult } from '../types/tool';
import { ToolValidationError, ToolOutputValidationError } from '../errors';

export class ToolValidator {
  /**
   * Performs recursive schema parameter validation.
   */
  public static validateInput(schema: ToolInputSchema, params: Record<string, unknown>): void {
    if (!params || typeof params !== 'object') {
      throw new ToolValidationError('Parameters must be an object.');
    }

    // 1. Required field detection
    if (schema.required) {
      for (const req of schema.required) {
        if (!(req in params) || params[req] === undefined || params[req] === null) {
          throw new ToolValidationError(`Missing required parameter: "${req}".`);
        }
      }
    }

    // 2. Unknown property detection
    if (schema.properties) {
      const allowedKeys = new Set(Object.keys(schema.properties));
      for (const key of Object.keys(params)) {
        if (!allowedKeys.has(key)) {
          throw new ToolValidationError(`Unknown parameter detected: "${key}".`);
        }
      }

      // 3. Recursive validation of defined properties
      for (const [key, prop] of Object.entries(schema.properties)) {
        if (key in params) {
          const val = params[key];
          const rules = prop as Record<string, unknown>;

          if (rules.type) {
            // Primitive Validation
            if (rules.type === 'string') {
              if (typeof val !== 'string') {
                throw new ToolValidationError(`Parameter "${key}" must be of type string.`);
              }
              if (rules.minLength !== undefined && val.length < (rules.minLength as number)) {
                throw new ToolValidationError(`Parameter "${key}" length must be >= ${rules.minLength}.`);
              }
              if (rules.maxLength !== undefined && val.length > (rules.maxLength as number)) {
                throw new ToolValidationError(`Parameter "${key}" length must be <= ${rules.maxLength}.`);
              }
              if (rules.pattern) {
                const regex = new RegExp(rules.pattern as string);
                if (!regex.test(val)) {
                  throw new ToolValidationError(`Parameter "${key}" does not match pattern: ${rules.pattern}.`);
                }
              }
            } else if (rules.type === 'number') {
              if (typeof val !== 'number' || isNaN(val)) {
                throw new ToolValidationError(`Parameter "${key}" must be of type number.`);
              }
              if (rules.minimum !== undefined && val < (rules.minimum as number)) {
                throw new ToolValidationError(`Parameter "${key}" must be >= ${rules.minimum}.`);
              }
              if (rules.maximum !== undefined && val > (rules.maximum as number)) {
                throw new ToolValidationError(`Parameter "${key}" must be <= ${rules.maximum}.`);
              }
            } else if (rules.type === 'boolean') {
              if (typeof val !== 'boolean') {
                throw new ToolValidationError(`Parameter "${key}" must be of type boolean.`);
              }
            } else if (rules.type === 'array') {
              if (!Array.isArray(val)) {
                throw new ToolValidationError(`Parameter "${key}" must be of type array.`);
              }
              if (rules.items) {
                const itemRules = rules.items as Record<string, unknown>;
                for (let idx = 0; idx < val.length; idx++) {
                  const item = val[idx];
                  if (itemRules.type && typeof item !== itemRules.type) {
                    throw new ToolValidationError(`Array item at index ${idx} of "${key}" must be of type ${String(itemRules.type)}.`);
                  }
                }
              }
            } else if (rules.type === 'object') {
              if (typeof val !== 'object' || val === null || Array.isArray(val)) {
                throw new ToolValidationError(`Parameter "${key}" must be of type object.`);
              }
              // Recursively validate inner object
              this.validateInput(rules as ToolInputSchema, val as Record<string, unknown>);
            }
          }

          // Enum set validation
          if (Array.isArray(rules.enum)) {
            if (!rules.enum.includes(val)) {
              throw new ToolValidationError(`Parameter "${key}" must be one of: ${rules.enum.join(', ')}.`);
            }
          }
        }
      }
    }
  }

  /**
   * Validates tool output format and serialization safety.
   */
  public static validateOutput(result: ToolExecutionResult): void {
    if (!result) {
      throw new ToolOutputValidationError('Tool did not return any result.');
    }
    if (!Array.isArray(result.content)) {
      throw new ToolOutputValidationError('Tool result content must be an array.');
    }

    for (let idx = 0; idx < result.content.length; idx++) {
      const item = result.content[idx];
      if (!item || typeof item !== 'object') {
        throw new ToolOutputValidationError(`Content item at index ${idx} must be an object.`);
      }
      if (typeof item.type !== 'string' || !item.type) {
        throw new ToolOutputValidationError(`Content item at index ${idx} must contain a non-empty string type.`);
      }
      if (typeof item.text !== 'string') {
        throw new ToolOutputValidationError(`Content item at index ${idx} must contain string text.`);
      }

      // Check serialization compatibility
      try {
        JSON.stringify(item);
      } catch (err) {
        throw new ToolOutputValidationError(`Content item at index ${idx} is not JSON-serializable.`);
      }
    }
  }
}
