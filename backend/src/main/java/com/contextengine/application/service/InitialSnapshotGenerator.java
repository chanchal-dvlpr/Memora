package com.contextengine.application.service;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.application.port.GitPort;
import com.contextengine.domain.entity.Context;
import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.repository.ContextRepository;
import com.contextengine.domain.valueobject.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that scans a project directory, builds the initial knowledge model,
 * and compiles & persists the initial Markdown context snapshot.
 */
public class InitialSnapshotGenerator {

    private final ContextRepository contextRepository;
    private final FilesystemPort filesystemPort;
    private final GitPort gitPort;

    public InitialSnapshotGenerator(
        ContextRepository contextRepository,
        FilesystemPort filesystemPort,
        GitPort gitPort
    ) {
        this.contextRepository = Objects.requireNonNull(contextRepository, "ContextRepository must not be null");
        this.filesystemPort = Objects.requireNonNull(filesystemPort, "FilesystemPort must not be null");
        this.gitPort = Objects.requireNonNull(gitPort, "GitPort must not be null");
    }

    /**
     * Scans the project workspace and builds/persists the initial snapshot.
     *
     * @param project the registered project aggregate
     * @return the created ContextSnapshot
     */
    public ContextSnapshot generateInitialSnapshot(Project project) {
        Objects.requireNonNull(project, "Project must not be null");

        Path root = project.rootDirectory();
        
        // 1. Traverse workspace with exclusions
        List<String> exclusions = List.of(
            "**/node_modules/**", "node_modules/**", "node_modules",
            "**/.git/**", ".git/**", ".git",
            "**/target/**", "target/**", "target",
            "**/build/**", "build/**", "build",
            "**/out/**", "out/**", "out",
            "**/dist/**", "dist/**", "dist",
            "**/coverage/**", "coverage/**", "coverage",
            "**/.vscode-test/**", ".vscode-test/**", ".vscode-test"
        );
        
        List<Path> allPaths = filesystemPort.listFiles(root, exclusions);
        
        // Additional defensive filtering to ensure we skip ignored directories
        List<Path> filteredPaths = allPaths.stream()
            .filter(p -> {
                String val = p.value().replace('\\', '/');
                return !val.contains("node_modules/") && !val.startsWith("node_modules") &&
                       !val.contains(".git/") && !val.startsWith(".git") &&
                       !val.contains("target/") && !val.startsWith("target") &&
                       !val.contains("build/") && !val.startsWith("build") &&
                       !val.contains("out/") && !val.startsWith("out") &&
                       !val.contains("dist/") && !val.startsWith("dist") &&
                       !val.contains("coverage/") && !val.startsWith("coverage") &&
                       !val.contains(".vscode-test/") && !val.startsWith(".vscode-test");
            })
            .collect(Collectors.toList());

        // Supported file types check
        List<Path> sourceFiles = filteredPaths.stream()
            .filter(p -> isSupportedFile(p.value()))
            .collect(Collectors.toList());

        // 2. Technology & Language Detection
        Set<String> languages = new TreeSet<>();
        Set<String> buildSystems = new TreeSet<>();
        
        for (Path f : sourceFiles) {
            String val = f.value().toLowerCase();
            if (val.endsWith(".java")) languages.add("Java");
            else if (val.endsWith(".kt")) languages.add("Kotlin");
            else if (val.endsWith(".ts") || val.endsWith(".tsx")) languages.add("TypeScript");
            else if (val.endsWith(".js") || val.endsWith(".jsx")) languages.add("JavaScript");
            else if (val.endsWith(".json")) languages.add("JSON");
            else if (val.endsWith(".xml")) languages.add("XML");
            else if (val.endsWith(".yml") || val.endsWith(".yaml")) languages.add("YAML");
            else if (val.endsWith(".md")) languages.add("Markdown");
            else if (val.endsWith(".gradle")) languages.add("Gradle");

            if (val.endsWith("pom.xml")) buildSystems.add("Maven (pom.xml detected)");
            else if (val.endsWith("package.json")) buildSystems.add("npm (package.json detected)");
            else if (val.endsWith("build.gradle") || val.endsWith("build.gradle.kts")) buildSystems.add("Gradle (build.gradle detected)");
            else if (val.endsWith("yarn.lock")) buildSystems.add("Yarn (yarn.lock detected)");
            else if (val.endsWith("pnpm-lock.yaml")) buildSystems.add("pnpm (pnpm-lock.yaml detected)");
        }

        // Git Repository Detection
        boolean isGitRepo = gitPort.isGitRepository(root);

        // 3. Build Directory Tree
        String dirTree = buildDirectoryTree(sourceFiles);

        // 4. README Summary
        String readmeSummary = "No README.md found.";
        Path readmePath = null;
        for (Path f : filteredPaths) {
            if (f.value().equalsIgnoreCase("README.md") || f.value().equalsIgnoreCase("README")) {
                readmePath = f;
                break;
            }
        }
        if (readmePath != null) {
            try {
                String readmeContent = filesystemPort.readFile(new Path(root.value() + "/" + readmePath.value()));
                if (readmeContent != null && !readmeContent.trim().isEmpty()) {
                    readmeSummary = readmeContent.length() > 2000 
                        ? readmeContent.substring(0, 2000) + "\n\n...(truncated)"
                        : readmeContent;
                }
            } catch (Exception e) {
                readmeSummary = "Failed to read README.md: " + e.getMessage();
            }
        }

        // 5. Assemble Markdown Payload
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Memora Context Snapshot\n\n");
        
        markdown.append("## Project\n");
        markdown.append("Name: ").append(project.title()).append("\n");
        markdown.append("Root: ").append(root.value()).append("\n\n");

        markdown.append("## Languages\n");
        if (languages.isEmpty()) {
            markdown.append("- None detected\n");
        } else {
            for (String lang : languages) {
                markdown.append("- ").append(lang).append("\n");
            }
        }
        markdown.append("\n");

        markdown.append("## Build\n");
        if (buildSystems.isEmpty()) {
            markdown.append("- None detected\n");
        } else {
            for (String bs : buildSystems) {
                markdown.append("- ").append(bs).append("\n");
            }
        }
        if (isGitRepo) {
            markdown.append("- Git repository detected\n");
        }
        markdown.append("\n");

        markdown.append("## Modules\n");
        markdown.append("- Root (").append(project.title()).append(")\n\n");

        markdown.append("## Directory Tree\n");
        markdown.append("```\n");
        markdown.append(dirTree);
        markdown.append("```\n\n");

        markdown.append("## Source Files\n");
        if (sourceFiles.isEmpty()) {
            markdown.append("- No source files found\n");
        } else {
            for (Path sf : sourceFiles) {
                markdown.append("- ").append(sf.value()).append("\n");
            }
        }
        markdown.append("\n");

        markdown.append("## README Summary\n");
        markdown.append(readmeSummary).append("\n");

        String payload = markdown.toString();
        int tokenCount = payload.length() / 4;

        // 6. Persist Context Snapshot
        int nextVersion = 1;
        Optional<ContextSnapshot> latestSnapshot = contextRepository.findLatestSnapshotForProject(project.id());
        if (latestSnapshot.isPresent()) {
            nextVersion = latestSnapshot.get().version().value() + 1;
        }

        ContextSummary summary = new ContextSummary(sourceFiles.size(), tokenCount, new ArrayList<>(languages));
        List<EngineeringEvidence> evidences = new ArrayList<>();
        
        // Build mock evidences for files to fit Context schema constraints
        for (Path sf : sourceFiles) {
            evidences.add(new EngineeringEvidence(
                new Path(sf.value()),
                1,
                1,
                new Hash("0000000000000000000000000000000000000000000000000000000000000000")
            ));
        }

        ContextSnapshot snapshot = new ContextSnapshot(
            SnapshotId.generate(),
            project.id(),
            new Version(nextVersion),
            Timestamp.now(),
            summary,
            evidences,
            payload
        );

        Context context = new Context(project.id(), new TokenBudget(1000000));
        context.addSnapshot(snapshot);
        contextRepository.save(context);

        return snapshot;
    }

    private boolean isSupportedFile(String filepath) {
        String lower = filepath.toLowerCase();
        return lower.endsWith(".java") ||
               lower.endsWith(".kt") ||
               lower.endsWith(".ts") ||
               lower.endsWith(".tsx") ||
               lower.endsWith(".js") ||
               lower.endsWith(".jsx") ||
               lower.endsWith(".json") ||
               lower.endsWith(".xml") ||
               lower.endsWith(".yml") ||
               lower.endsWith(".yaml") ||
               lower.endsWith(".md") ||
               lower.endsWith(".gradle") ||
               lower.endsWith("pom.xml") ||
               lower.endsWith("package.json");
    }

    private String buildDirectoryTree(List<Path> files) {
        // Build prefix tree of directories
        DirNode treeRoot = new DirNode(".");
        for (Path f : files) {
            String pathVal = f.value().replace('\\', '/');
            String[] parts = pathVal.split("/");
            DirNode current = treeRoot;
            // Traverse up to the last folder part (file name itself is omitted)
            for (int i = 0; i < parts.length - 1; i++) {
                current = current.children.computeIfAbsent(parts[i], DirNode::new);
            }
        }
        
        StringBuilder builder = new StringBuilder();
        renderTree(treeRoot, "", builder);
        return builder.toString();
    }

    private void renderTree(DirNode node, String prefix, StringBuilder builder) {
        List<Map.Entry<String, DirNode>> entries = new ArrayList<>(node.children.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, DirNode> entry = entries.get(i);
            boolean isLast = (i == entries.size() - 1);
            builder.append(prefix)
                   .append(isLast ? "└── " : "├── ")
                   .append(entry.getKey())
                   .append("\n");
            renderTree(entry.getValue(), prefix + (isLast ? "    " : "│   "), builder);
        }
    }

    private static class DirNode {
        String name;
        Map<String, DirNode> children = new TreeMap<>();
        DirNode(String name) { this.name = name; }
    }
}
