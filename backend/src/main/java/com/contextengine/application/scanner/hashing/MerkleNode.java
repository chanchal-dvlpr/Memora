package com.contextengine.application.scanner.hashing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a node in the hierarchical Merkle Tree.
 */
public class MerkleNode {

    /**
     * Node classification type.
     */
    public enum Type {
        FILE,
        DIR
    }

    private final String relativePath;
    private final Type type;
    private final String hash;
    private final List<MerkleNode> children;

    /**
     * Constructs a MerkleNode.
     *
     * @param relativePath relative file path
     * @param type node classification type (FILE or DIR)
     * @param hash SHA-256 hash representation of this node
     * @param children nested children nodes
     */
    public MerkleNode(String relativePath, Type type, String hash, List<MerkleNode> children) {
        this.relativePath = Objects.requireNonNull(relativePath, "RelativePath must not be null");
        this.type = Objects.requireNonNull(type, "Type must not be null");
        this.hash = Objects.requireNonNull(hash, "Hash must not be null");
        this.children = children != null ? new ArrayList<>(children) : Collections.emptyList();
    }

    public String relativePath() {
        return relativePath;
    }

    public Type type() {
        return type;
    }

    public String hash() {
        return hash;
    }

    public List<MerkleNode> children() {
        return Collections.unmodifiableList(children);
    }
}
