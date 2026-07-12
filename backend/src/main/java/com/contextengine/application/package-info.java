/**
 * Use-case orchestration and application boundary contracts.
 *
 * <p>This package coordinates domain behavior through commands, queries, use cases, ports, DTOs,
 * and mappings. It may depend on the domain and neutral shared concepts, while adapters implement
 * its ports from outer layers. HTTP concerns, Spring controllers, database implementations, and
 * infrastructure-specific logic must not be placed here.</p>
 */
package com.contextengine.application;
