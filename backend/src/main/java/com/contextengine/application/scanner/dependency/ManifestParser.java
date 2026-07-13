package com.contextengine.application.scanner.dependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses standard project manifest files to extract dependency information.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public class ManifestParser {

    private static final Pattern POM_DEPENDENCY_PATTERN = Pattern.compile(
        "<dependency>([\\s\\S]*?)</dependency>",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern GROUP_ID_PATTERN = Pattern.compile("<groupId>([^<]+)</groupId>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARTIFACT_ID_PATTERN = Pattern.compile("<artifactId>([^<]+)</artifactId>", Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION_PATTERN = Pattern.compile("<version>([^<]+)</version>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCOPE_PATTERN = Pattern.compile("<scope>([^<]+)</scope>", Pattern.CASE_INSENSITIVE);

    private static final Pattern NPM_DEP_BLOCK_PATTERN = Pattern.compile(
        "\"(dependencies|devDependencies)\"\\s*:\\s*\\{([^}]+)\\}",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NPM_DEP_ENTRY_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");

    private static final Pattern PIP_DEP_PATTERN = Pattern.compile("^([a-zA-Z0-9_\\-]+)\\s*==\\s*([a-zA-Z0-9_\\-\\.\\+]+)");

    /**
     * Constructs a ManifestParser.
     */
    public ManifestParser() {
    }

    /**
     * Parses the manifest file content and returns the extracted dependencies.
     *
     * @param fileName manifest file name (e.g. pom.xml)
     * @param content manifest file content string
     * @return collection of extracted ProjectDependencies
     */
    public Collection<ProjectDependency> parse(String fileName, String content) {
        Objects.requireNonNull(fileName, "FileName must not be null");
        Objects.requireNonNull(content, "Content must not be null");

        List<ProjectDependency> dependencies = new ArrayList<>();
        String nameLower = fileName.toLowerCase();

        if (nameLower.endsWith("pom.xml")) {
            parsePom(content, dependencies);
        } else if (nameLower.endsWith("package.json")) {
            parsePackageJson(content, dependencies);
        } else if (nameLower.equals("requirements.txt")) {
            parseRequirements(content, dependencies);
        }

        return dependencies;
    }

    private void parsePom(String content, List<ProjectDependency> dependencies) {
        Matcher matcher = POM_DEPENDENCY_PATTERN.matcher(content);
        while (matcher.find()) {
            String block = matcher.group(1);
            String groupId = getTagValue(block, GROUP_ID_PATTERN);
            String artifactId = getTagValue(block, ARTIFACT_ID_PATTERN);
            String version = getTagValue(block, VERSION_PATTERN);
            String scope = getTagValue(block, SCOPE_PATTERN);

            if (groupId != null && artifactId != null) {
                dependencies.add(new ProjectDependency(
                    groupId + ":" + artifactId,
                    version != null ? version : "LATEST",
                    "MAVEN",
                    scope != null ? scope.toUpperCase() : "COMPILE"
                ));
            }
        }
    }

    private String getTagValue(String block, Pattern pattern) {
        Matcher matcher = pattern.matcher(block);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private void parsePackageJson(String content, List<ProjectDependency> dependencies) {
        Matcher blockMatcher = NPM_DEP_BLOCK_PATTERN.matcher(content);
        while (blockMatcher.find()) {
            String blockType = blockMatcher.group(1);
            String blockContent = blockMatcher.group(2);
            String scope = blockType.equals("devDependencies") ? "DEV" : "PROD";

            Matcher entryMatcher = NPM_DEP_ENTRY_PATTERN.matcher(blockContent);
            while (entryMatcher.find()) {
                String name = entryMatcher.group(1);
                String version = entryMatcher.group(2).replace("^", "").replace("~", "");
                dependencies.add(new ProjectDependency(name, version, "NPM", scope));
            }
        }
    }

    private void parseRequirements(String content, List<ProjectDependency> dependencies) {
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#") || trimmed.isEmpty()) {
                continue;
            }
            Matcher matcher = PIP_DEP_PATTERN.matcher(trimmed);
            if (matcher.find()) {
                String name = matcher.group(1);
                String version = matcher.group(2);
                dependencies.add(new ProjectDependency(name, version, "PIP", "COMPILE"));
            } else if (!trimmed.contains("==")) {
                // If it is just a package name without specific version
                dependencies.add(new ProjectDependency(trimmed, "LATEST", "PIP", "COMPILE"));
            }
        }
    }
}
