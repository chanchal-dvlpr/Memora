/**
 * Knowledge-engine feature boundary for graph, retrieval, ranking, and context concerns.
 *
 * <p>This package will organize application-facing knowledge contracts and coordination using domain
 * and application abstractions. Technology-specific graph stores, search engines, and AI clients
 * belong in infrastructure. It must not contain transport endpoints, persistence adapters, or domain
 * model rules outside its assigned bounded context.</p>
 */
package com.contextengine.knowledge;
