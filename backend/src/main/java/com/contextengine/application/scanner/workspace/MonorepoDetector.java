package com.contextengine.application.scanner.workspace;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.domain.valueobject.Path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service detector that identifies workspace configurations (monorepos)
 * and resolves nested subprojects/modules without executing external build tools.
 */
public class MonorepoDetector {

    /**
     * Inspects the workspace files and reads configurations to build a MonorepoDescriptor.
     *
     * @param rootPath canonical root workspace path
     * @param candidates collection of scanned file candidates in the workspace
     * @param filesystemPort filesystem port to read config files safely
     * @return MonorepoDescriptor containing workspace type and resolved modules
     */
    public MonorepoDescriptor detect(String rootPath, Collection<ScanCandidate> candidates, FilesystemPort filesystemPort) {
        Objects.requireNonNull(rootPath, "RootPath must not be null");
        Objects.requireNonNull(candidates, "Candidates must not be null");
        Objects.requireNonNull(filesystemPort, "FilesystemPort must not be null");

        // 1. PNPM Workspace Check
        ScanCandidate pnpmWorkspace = findCandidate(candidates, "pnpm-workspace.yaml");
        if (pnpmWorkspace == null) {
            pnpmWorkspace = findCandidate(candidates, "pnpm-workspace.yml");
        }
        if (pnpmWorkspace != null) {
            List<WorkspaceModule> modules = detectPnpmModules(rootPath, pnpmWorkspace, candidates, filesystemPort);
            return new MonorepoDescriptor(WorkspaceType.PNPM, rootPath, modules);
        }

        // 2. Lerna Check
        ScanCandidate lernaConfig = findCandidate(candidates, "lerna.json");
        if (lernaConfig != null) {
            List<WorkspaceModule> modules = detectLernaModules(rootPath, lernaConfig, candidates, filesystemPort);
            return new MonorepoDescriptor(WorkspaceType.LERNA, rootPath, modules);
        }

        // 3. Rush Check
        ScanCandidate rushConfig = findCandidate(candidates, "rush.json");
        if (rushConfig != null) {
            List<WorkspaceModule> modules = detectRushModules(rootPath, rushConfig, candidates, filesystemPort);
            return new MonorepoDescriptor(WorkspaceType.RUSH, rootPath, modules);
        }

        // 4. TurboRepo Check
        ScanCandidate turboConfig = findCandidate(candidates, "turbo.json");
        if (turboConfig != null) {
            // TurboRepo sits on top of npm/yarn/pnpm workspaces.
            // Fall back to npm/yarn workspaces parsing if available, otherwise just report type.
            List<WorkspaceModule> modules = detectNpmWorkspaces(rootPath, candidates, filesystemPort);
            return new MonorepoDescriptor(WorkspaceType.TURBO, rootPath, modules);
        }

        // 5. Nx Check
        ScanCandidate nxConfig = findCandidate(candidates, "nx.json");
        if (nxConfig != null) {
            List<WorkspaceModule> modules = detectNxModules(rootPath, candidates, filesystemPort);
            return new MonorepoDescriptor(WorkspaceType.NX, rootPath, modules);
        }

        // 6. NPM / Yarn Workspaces Check (root package.json workspaces array)
        List<WorkspaceModule> npmModules = detectNpmWorkspaces(rootPath, candidates, filesystemPort);
        if (!npmModules.isEmpty()) {
            return new MonorepoDescriptor(WorkspaceType.NPM, rootPath, npmModules);
        }

        // 7. Maven Multi-Module Check
        ScanCandidate mavenPom = findCandidate(candidates, "pom.xml");
        if (mavenPom != null) {
            List<WorkspaceModule> modules = detectMavenModules(rootPath, mavenPom, candidates, filesystemPort);
            if (!modules.isEmpty()) {
                return new MonorepoDescriptor(WorkspaceType.MAVEN, rootPath, modules);
            }
        }

        // 8. Gradle Multi-Project Check
        ScanCandidate gradleSettings = findCandidate(candidates, "settings.gradle");
        if (gradleSettings == null) {
            gradleSettings = findCandidate(candidates, "settings.gradle.kts");
        }
        if (gradleSettings != null) {
            List<WorkspaceModule> modules = detectGradleModules(rootPath, gradleSettings, candidates, filesystemPort);
            if (!modules.isEmpty()) {
                return new MonorepoDescriptor(WorkspaceType.GRADLE, rootPath, modules);
            }
        }

        // 9. Default Fallback: Single-project / None
        return new MonorepoDescriptor(WorkspaceType.NONE, rootPath, new ArrayList<>());
    }

    private ScanCandidate findCandidate(Collection<ScanCandidate> candidates, String relativePath) {
        for (ScanCandidate c : candidates) {
            if (c.relativePath().equals(relativePath)) {
                return c;
            }
        }
        return null;
    }

    private List<WorkspaceModule> detectPnpmModules(String rootPath, ScanCandidate config, Collection<ScanCandidate> candidates, FilesystemPort port) {
        List<WorkspaceModule> modules = new ArrayList<>();
        String content = port.readFile(new Path(rootPath + "/" + config.relativePath()));
        if (content == null || content.isEmpty()) {
            return modules;
        }

        List<String> globPatterns = new ArrayList<>();
        Pattern p = Pattern.compile("^\\s*-\\s*['\"]?([^'\"\\r\\n]+)['\"]?", Pattern.MULTILINE);
        Matcher m = p.matcher(content);
        while (m.find()) {
            globPatterns.add(m.group(1));
        }

        if (globPatterns.isEmpty()) {
            globPatterns.add("packages/*");
        }

        for (ScanCandidate c : candidates) {
            if (c.relativePath().endsWith("package.json") && !c.relativePath().equals("package.json")) {
                String subDirPath = getParentDirectory(c.relativePath());
                if (matchesAnyGlob(subDirPath, globPatterns)) {
                    String subName = extractJsPackageName(rootPath, c, port);
                    modules.add(new WorkspaceModule(subName, subDirPath, "pnpm", c.language().name()));
                }
            }
        }
        return modules;
    }

    private List<WorkspaceModule> detectLernaModules(String rootPath, ScanCandidate config, Collection<ScanCandidate> candidates, FilesystemPort port) {
        List<WorkspaceModule> modules = new ArrayList<>();
        String content = port.readFile(new Path(rootPath + "/" + config.relativePath()));
        if (content == null || content.isEmpty()) {
            return modules;
        }

        List<String> globPatterns = new ArrayList<>();
        Pattern p = Pattern.compile("\"packages\"\\s*:\\s*\\[([^\\]]+)\\]");
        Matcher m = p.matcher(content);
        if (m.find()) {
            Pattern q = Pattern.compile("\"([^\"]+)\"");
            Matcher qm = q.matcher(m.group(1));
            while (qm.find()) {
                globPatterns.add(qm.group(1));
            }
        }

        if (globPatterns.isEmpty()) {
            globPatterns.add("packages/*");
        }

        for (ScanCandidate c : candidates) {
            if (c.relativePath().endsWith("package.json") && !c.relativePath().equals("package.json")) {
                String subDirPath = getParentDirectory(c.relativePath());
                if (matchesAnyGlob(subDirPath, globPatterns)) {
                    String subName = extractJsPackageName(rootPath, c, port);
                    modules.add(new WorkspaceModule(subName, subDirPath, "lerna", c.language().name()));
                }
            }
        }
        return modules;
    }

    private List<WorkspaceModule> detectRushModules(String rootPath, ScanCandidate config, Collection<ScanCandidate> candidates, FilesystemPort port) {
        List<WorkspaceModule> modules = new ArrayList<>();
        String content = port.readFile(new Path(rootPath + "/" + config.relativePath()));
        if (content == null || content.isEmpty()) {
            return modules;
        }

        Pattern p = Pattern.compile("\"projectFolder\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(content);
        while (m.find()) {
            String folder = m.group(1);
            // Look for package.json in that folder
            String targetJson = folder + "/package.json";
            ScanCandidate jsonCandidate = findCandidate(candidates, targetJson);
            if (jsonCandidate != null) {
                String subName = extractJsPackageName(rootPath, jsonCandidate, port);
                modules.add(new WorkspaceModule(subName, folder, "rush", jsonCandidate.language().name()));
            } else {
                modules.add(new WorkspaceModule(getLastSegment(folder), folder, "rush", "JAVASCRIPT"));
            }
        }
        return modules;
    }

    private List<WorkspaceModule> detectNxModules(String rootPath, Collection<ScanCandidate> candidates, FilesystemPort port) {
        List<WorkspaceModule> modules = new ArrayList<>();
        // Search candidates for project.json files in subdirectories
        for (ScanCandidate c : candidates) {
            if (c.relativePath().endsWith("project.json") && !c.relativePath().equals("project.json")) {
                String folder = getParentDirectory(c.relativePath());
                String content = port.readFile(new Path(rootPath + "/" + c.relativePath()));
                String name = null;
                if (content != null) {
                    Pattern p = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
                    Matcher m = p.matcher(content);
                    if (m.find()) {
                        name = m.group(1);
                    }
                }
                if (name == null) {
                    name = getLastSegment(folder);
                }
                modules.add(new WorkspaceModule(name, folder, "nx", c.language().name()));
            }
        }
        return modules;
    }

    private List<WorkspaceModule> detectNpmWorkspaces(String rootPath, Collection<ScanCandidate> candidates, FilesystemPort port) {
        List<WorkspaceModule> modules = new ArrayList<>();
        ScanCandidate rootJson = findCandidate(candidates, "package.json");
        if (rootJson == null) {
            return modules;
        }

        String content = port.readFile(new Path(rootPath + "/" + rootJson.relativePath()));
        if (content == null || content.isEmpty()) {
            return modules;
        }

        List<String> globPatterns = new ArrayList<>();
        Pattern p = Pattern.compile("\"workspaces\"\\s*:\\s*\\[([^\\]]+)\\]");
        Matcher m = p.matcher(content);
        if (m.find()) {
            Pattern q = Pattern.compile("\"([^\"]+)\"");
            Matcher qm = q.matcher(m.group(1));
            while (qm.find()) {
                globPatterns.add(qm.group(1));
            }
        }

        if (globPatterns.isEmpty()) {
            return modules;
        }

        for (ScanCandidate c : candidates) {
            if (c.relativePath().endsWith("package.json") && !c.relativePath().equals("package.json")) {
                String subDirPath = getParentDirectory(c.relativePath());
                if (matchesAnyGlob(subDirPath, globPatterns)) {
                    String subName = extractJsPackageName(rootPath, c, port);
                    modules.add(new WorkspaceModule(subName, subDirPath, "npm", c.language().name()));
                }
            }
        }
        return modules;
    }

    private List<WorkspaceModule> detectMavenModules(String rootPath, ScanCandidate config, Collection<ScanCandidate> candidates, FilesystemPort port) {
        List<WorkspaceModule> modules = new ArrayList<>();
        String content = port.readFile(new Path(rootPath + "/" + config.relativePath()));
        if (content == null || content.isEmpty()) {
            return modules;
        }

        Pattern p = Pattern.compile("<module>([^<]+)</module>");
        Matcher m = p.matcher(content);
        while (m.find()) {
            String path = m.group(1).trim();
            String targetPom = path + "/pom.xml";
            ScanCandidate subPom = findCandidate(candidates, targetPom);
            if (subPom != null) {
                String subName = extractMavenArtifactId(rootPath, subPom, port);
                modules.add(new WorkspaceModule(subName, path, "maven", "JAVA"));
            }
        }
        return modules;
    }

    private List<WorkspaceModule> detectGradleModules(String rootPath, ScanCandidate config, Collection<ScanCandidate> candidates, FilesystemPort port) {
        List<WorkspaceModule> modules = new ArrayList<>();
        String content = port.readFile(new Path(rootPath + "/" + config.relativePath()));
        if (content == null || content.isEmpty()) {
            return modules;
        }

        Pattern p = Pattern.compile("include\\s+['\"]:?([^'\"]+)['\"]");
        Matcher m = p.matcher(content);
        while (m.find()) {
            String moduleName = m.group(1).trim();
            // Convert gradle module path colon e.g. ':sub-project:child' to 'sub-project/child'
            String path = moduleName.replace(':', '/');
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (path.isEmpty()) {
                path = moduleName;
            }
            modules.add(new WorkspaceModule(moduleName, path, "gradle", "JAVA"));
        }
        return modules;
    }

    private String extractJsPackageName(String rootPath, ScanCandidate config, FilesystemPort port) {
        String content = port.readFile(new Path(rootPath + "/" + config.relativePath()));
        if (content != null) {
            Pattern p = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher m = p.matcher(content);
            if (m.find()) {
                return m.group(1);
            }
        }
        return getLastSegment(getParentDirectory(config.relativePath()));
    }

    private String extractMavenArtifactId(String rootPath, ScanCandidate config, FilesystemPort port) {
        String content = port.readFile(new Path(rootPath + "/" + config.relativePath()));
        if (content != null) {
            Pattern p = Pattern.compile("<artifactId>([^<]+)</artifactId>");
            Matcher m = p.matcher(content);
            if (m.find()) {
                return m.group(1).trim();
            }
        }
        return getLastSegment(getParentDirectory(config.relativePath()));
    }

    private String getParentDirectory(String path) {
        int idx = path.lastIndexOf('/');
        if (idx == -1) {
            return "";
        }
        return path.substring(0, idx);
    }

    private String getLastSegment(String path) {
        int idx = path.lastIndexOf('/');
        if (idx == -1) {
            return path;
        }
        return path.substring(idx + 1);
    }

    private boolean matchesAnyGlob(String path, List<String> globPatterns) {
        for (String pattern : globPatterns) {
            if (matchesGlob(path, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesGlob(String path, String glob) {
        // Convert simple glob pattern (e.g. packages/* or apps/**) to regex
        String regex = glob.replace(".", "\\.")
                           .replace("?", ".")
                           .replace("**", ".*")
                           .replace("*", "[^/]+");
        return path.matches(regex) || path.equals(glob.replace("/*", "").replace("/**", ""));
    }
}
