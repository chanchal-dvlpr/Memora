/**
 * Application composition and framework configuration.
 *
 * <p>This package owns configuration properties and bean wiring that assemble the application at
 * runtime. It may depend on Spring and on application-layer ports, but it must not contain domain
 * decisions, use-case implementations, or technical adapter behavior. Framework-specific settings
 * and composition roots belong here; business rules, controllers, and persistence mappings do not.</p>
 */
package com.contextengine.configuration;
