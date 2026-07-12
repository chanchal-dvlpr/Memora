package com.contextengine.infrastructure.git;

import com.contextengine.application.port.GitPort;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.infrastructure.exception.InfrastructureException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Git version control adapter implementing GitPort using physical Git CLI command subprocesses.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Port: GitPort
 * </p>
 */
public class GitCLIAdapter implements GitPort {

    @Override
    public boolean isGitRepository(Path directory) {
        Objects.requireNonNull(directory, "Directory path must not be null");
        File gitDir = new File(directory.value(), ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }

    @Override
    public String getActiveBranch(Path directory) {
        Objects.requireNonNull(directory, "Directory path must not be null");
        return executeGitCommand(directory, "rev-parse", "--abbrev-ref", "HEAD");
    }

    @Override
    public String getLatestCommitHash(Path directory) {
        Objects.requireNonNull(directory, "Directory path must not be null");
        return executeGitCommand(directory, "rev-parse", "HEAD");
    }

    private String executeGitCommand(Path directory, String... args) {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);

        try {
            Process process = new ProcessBuilder(command)
                .directory(new File(directory.value()))
                .redirectErrorStream(true)
                .start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new InfrastructureException("Git command execution timed out");
            }

            if (process.exitValue() != 0) {
                return "";
            }

            return output.toString().trim();
        } catch (Exception e) {
            // Safely fallback to empty string on missing command or directory context issues
            return "";
        }
    }
}
