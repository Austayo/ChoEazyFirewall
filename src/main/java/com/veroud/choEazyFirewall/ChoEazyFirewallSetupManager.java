package com.veroud.choEazyFirewall;

import java.io.IOException;
import java.nio.file.*;

public class ChoEazyFirewallSetupManager {

    private final Class<?> pluginClass;
    private final Path setupFolder;

    public ChoEazyFirewallSetupManager(Class<?> pluginClass, Path setupFolder) {
        this.pluginClass = pluginClass;
        this.setupFolder = setupFolder;
    }

    public void checkAndExtractIfNeeded() {
        try {
            if (Files.notExists(setupFolder) || isEmptyDirectory(setupFolder)) {
                System.out.println("[ChoEazyFirewall] First run detected, extracting setup scripts...");
                extractAllSetupScripts();
                System.out.println("[ChoEazyFirewall] Setup scripts extracted to " + setupFolder.toAbsolutePath());
            } else {
                System.out.println("[ChoEazyFirewall] Setup scripts already present, skipping extraction.");
            }
        } catch (IOException e) {
            System.err.println("[ChoEazyFirewall] Failed to extract setup scripts:");
            e.printStackTrace();
        }
    }

    private boolean isEmptyDirectory(Path dir) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            return !dirStream.iterator().hasNext();
        }
    }

    private void extractAllSetupScripts() throws IOException {
        String[] files = { "/setup/setup.sh", "/setup/choeazy_add_ip.sh", "/setup/sudoers.example" };

        for (String resourcePath : files) {
            Path targetPath = setupFolder.resolve(resourcePath.substring("/setup/".length()));
            extractResource(resourcePath, targetPath);
        }
    }

    private void extractResource(String resourcePath, Path targetPath) throws IOException {
        try (var in = pluginClass.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Files.createDirectories(targetPath.getParent());
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);

            if (targetPath.toString().endsWith(".sh")) {
                targetPath.toFile().setExecutable(true);
            }
        }
    }
}
