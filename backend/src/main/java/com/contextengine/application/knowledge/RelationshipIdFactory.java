package com.contextengine.application.knowledge;

import com.contextengine.domain.valueobject.RelationshipId;
import java.util.Objects;
import java.util.UUID;

/**
 * Factory responsible for generating deterministic relationship identifiers.
 * Encapsulates the SHA-256 composite key hashing, truncation, and RFC-4122 layout encoding.
 * <p>
 * Bounded Context: Knowledge Graph
 * Related Subsystem: Knowledge Graph Engine (KG-ENG)
 * Reference: Functional Requirement FR-016 (Knowledge Graph Engine) Section 8 & 12
 * </p>
 */
public final class RelationshipIdFactory {

    private RelationshipIdFactory() {
        // Prevent instantiation
    }

    /**
     * Generates a RelationshipId deterministically from the composite key coordinates.
     * The returned UUID is a SHA-256-derived deterministic UUID representation.
     *
     * @param sourceUrn the source node URN
     * @param targetUrn the target node URN
     * @param type the relationship type
     * @return the resolved deterministic RelationshipId
     */
    public static RelationshipId create(String sourceUrn, String targetUrn, String type) {
        Objects.requireNonNull(sourceUrn, "Source URN must not be null");
        Objects.requireNonNull(targetUrn, "Target URN must not be null");
        Objects.requireNonNull(type, "Relationship type must not be null");

        String compositeKey = sourceUrn + "|" + targetUrn + "|" + type;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] sha256Bytes = digest.digest(compositeKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] uuidBytes = new byte[16];
            System.arraycopy(sha256Bytes, 0, uuidBytes, 0, 16);

            // Set version to 5 layout to satisfy RelationshipId UUID version validation
            uuidBytes[6] &= 0x0f;
            uuidBytes[6] |= 0x50;  // Version 5 layout
            uuidBytes[8] &= 0x3f;
            uuidBytes[8] |= 0x80;  // IETF Variant

            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (uuidBytes[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (uuidBytes[i] & 0xff);
            }

            return new RelationshipId(new UUID(msb, lsb));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
