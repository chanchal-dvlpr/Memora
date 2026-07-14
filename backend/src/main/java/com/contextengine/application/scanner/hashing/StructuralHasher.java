package com.contextengine.application.scanner.hashing;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.application.scanner.ScanCandidate;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates hashing workspace files, caching unmodified file fingerprints,
 * and building deterministic bottom-up directory Merkle Tree hashes.
 */
public class StructuralHasher implements MerkleTreeBuilder.FileHasher {

    // Cache key: relativePath + ":" + size + ":" + lastModifiedMillis
    private static final Map<String, String> fileHashCache = new ConcurrentHashMap<>();

    private final FilesystemPort filesystemPort;

    /**
     * Constructs a StructuralHasher.
     *
     * @param filesystemPort physical filesystem port
     */
    public StructuralHasher(FilesystemPort filesystemPort) {
        this.filesystemPort = Objects.requireNonNull(filesystemPort, "FilesystemPort must not be null");
    }

    /**
     * Executes bottom-up Merkle Tree synthesis and returns the structural hash results.
     *
     * @param rootPath canonical root workspace path
     * @param candidates collection of scanned files/directories in workspace
     * @return StructuralHashResult containing top-level workspace hash and stats
     */
    public StructuralHashResult calculate(String rootPath, Collection<ScanCandidate> candidates) {
        Objects.requireNonNull(rootPath, "RootPath must not be null");
        Objects.requireNonNull(candidates, "Candidates must not be null");

        MerkleTreeBuilder builder = new MerkleTreeBuilder();
        MerkleNode rootNode = builder.build(rootPath, candidates, this);

        // Compute metrics
        long[] counts = countNodes(rootNode);
        long dirCount = counts[0];
        long fileCount = counts[1];
        long totalNodes = dirCount + fileCount;

        return new StructuralHashResult(
            rootNode.hash(),
            totalNodes,
            dirCount,
            fileCount,
            "SHA-256",
            Instant.now()
        );
    }

    private long[] countNodes(MerkleNode node) {
        long dirCount = 0;
        long fileCount = 0;
        if (node.type() == MerkleNode.Type.DIR) {
            dirCount++;
        } else {
            fileCount++;
        }

        for (MerkleNode child : node.children()) {
            long[] childCounts = countNodes(child);
            dirCount += childCounts[0];
            fileCount += childCounts[1];
        }

        return new long[]{dirCount, fileCount};
    }

    @Override
    public String hash(ScanCandidate candidate) {
        Objects.requireNonNull(candidate, "Candidate must not be null");

        String cacheKey = candidate.relativePath() + ":" + candidate.size() + ":" + candidate.lastModified().toEpochMilli();
        String cachedHash = fileHashCache.get(cacheKey);
        if (cachedHash != null) {
            return cachedHash;
        }

        String fileHash = computeFileSha256(candidate.absolutePath());
        fileHashCache.put(cacheKey, fileHash);
        return fileHash;
    }

    @Override
    public String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private String computeFileSha256(String absolutePath) {
        try {
            File file = new File(absolutePath);
            if (!file.exists() || !file.isFile() || !file.canRead()) {
                return hashString(""); // Fallback for missing/unreadable files
            }

            // Compute hash of file contents bytes
            byte[] fileBytes = Files.readAllBytes(Paths.get(absolutePath));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);
            return bytesToHex(hash);
        } catch (Exception e) {
            // Fallback to hashing file path name on exception
            return hashString(absolutePath);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
