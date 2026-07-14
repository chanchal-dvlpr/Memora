package com.contextengine.application.scanner.hashing;

import com.contextengine.application.scanner.ScanCandidate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reconstructs a nested directory tree model from flat lists of scan candidates,
 * enabling bottom-up Merkle Tree building.
 */
public class MerkleTreeBuilder {

    private static class TempNode {
        final String relativePath;
        final String name;
        final MerkleNode.Type type;
        final Map<String, TempNode> children = new HashMap<>();
        ScanCandidate candidate = null;

        TempNode(String relativePath, String name, MerkleNode.Type type) {
            this.relativePath = relativePath;
            this.name = name;
            this.type = type;
        }
    }

    /**
     * Builds the MerkleTree from the candidates list and computes node hashes.
     *
     * @param rootPath the workspace root directory path
     * @param candidates list of files and directories in the workspace
     * @param fileHasher function or helper that returns the file content hash
     * @return the root MerkleNode representing the workspace root
     */
    public MerkleNode build(String rootPath, Collection<ScanCandidate> candidates, FileHasher fileHasher) {
        Objects.requireNonNull(rootPath, "RootPath must not be null");
        Objects.requireNonNull(candidates, "Candidates must not be null");
        Objects.requireNonNull(fileHasher, "FileHasher must not be null");

        TempNode root = new TempNode("", "", MerkleNode.Type.DIR);

        // 1. Populates mutable tree from candidates list
        for (ScanCandidate candidate : candidates) {
            String relPath = candidate.relativePath();
            if (relPath.isEmpty()) {
                continue;
            }

            String[] segments = relPath.split("/");
            TempNode current = root;
            StringBuilder currentPath = new StringBuilder();

            for (int i = 0; i < segments.length; i++) {
                String segment = segments[i];
                if (currentPath.length() > 0) {
                    currentPath.append("/");
                }
                currentPath.append(segment);

                boolean isLast = (i == segments.length - 1);
                MerkleNode.Type nodeType = isLast && !"DIR".equalsIgnoreCase(candidate.nodeType()) 
                        ? MerkleNode.Type.FILE 
                        : MerkleNode.Type.DIR;

                TempNode child = current.children.get(segment);
                if (child == null) {
                    child = new TempNode(currentPath.toString(), segment, nodeType);
                    current.children.put(segment, child);
                }

                if (isLast && nodeType == MerkleNode.Type.FILE) {
                    child.candidate = candidate;
                }
                current = child;
            }
        }

        // 2. Synthesize tree bottom-up
        return createMerkleNode(root, fileHasher);
    }

    private MerkleNode createMerkleNode(TempNode tempNode, FileHasher fileHasher) {
        if (tempNode.type == MerkleNode.Type.FILE) {
            String hash = fileHasher.hash(tempNode.candidate);
            return new MerkleNode(tempNode.relativePath, MerkleNode.Type.FILE, hash, null);
        } else {
            List<MerkleNode> childNodes = new ArrayList<>();
            for (TempNode childTemp : tempNode.children.values()) {
                childNodes.add(createMerkleNode(childTemp, fileHasher));
            }

            // Sort children lexicographically by relative path to ensure deterministic ordering
            childNodes.sort((a, b) -> a.relativePath().compareTo(b.relativePath()));

            // Directory hashing: concatenate child metadata and hashes
            StringBuilder sb = new StringBuilder();
            for (MerkleNode child : childNodes) {
                sb.append(child.type().name())
                  .append(":")
                  .append(child.relativePath())
                  .append(":")
                  .append(child.hash())
                  .append(";");
            }

            String dirHash = fileHasher.hashString(sb.toString());
            return new MerkleNode(tempNode.relativePath, MerkleNode.Type.DIR, dirHash, childNodes);
        }
    }

    /**
     * Interface to delegate file content and metadata hashing logic.
     */
    public interface FileHasher {
        String hash(ScanCandidate candidate);
        String hashString(String input);
    }
}
