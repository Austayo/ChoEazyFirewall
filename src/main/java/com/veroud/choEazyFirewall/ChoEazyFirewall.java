package com.veroud.choEazyFirewall;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import javax.inject.Inject;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@Plugin(
        id = "choeazyfirewall",
        name = "ChoEazyFirewall",
        version = "1.0",
        description = "Blocks Minecraft scanners and handshake spam using ipset + iptables",
        authors = {"Aidan Heaslip"}
)
public class ChoEazyFirewall {

    private final ProxyServer server;
    private final Path setupFolder = Paths.get("plugins/ChoEazyFirewall/setup");

    private Map<String, Instant> recentConnections = new HashMap<>();

    private int blockTimeout;
    private List<String> blockedRanges;
    private List<CIDRUtils> blockedCidrs = new ArrayList<>();
    private String helperScript;
    private boolean logBlocks;

    @Inject
    public ChoEazyFirewall(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Extract setup scripts once if needed
        ChoEazyFirewallSetupManager setupManager = new ChoEazyFirewallSetupManager(this.getClass(), setupFolder);
        setupManager.checkAndExtractIfNeeded();

        // Load config AFTER extracting setup scripts
        loadConfig();
    }

    private void loadConfig() {
        try {
            Path configPath = Path.of("plugins/ChoEazyFirewall/choeazy.conf");
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath.getParent());
                try (InputStream in = getClass().getResourceAsStream("/choeazy.conf")) {
                    Files.copy(in, configPath);
                }
                System.out.println("[ChoEazyFirewall] First run detected.");
                System.out.println("[ChoEazyFirewall] Please run 'setup/setup.sh' as root before starting Velocity.");
                return;
            }

            var yaml = new org.yaml.snakeyaml.Yaml();
            try (InputStream in = Files.newInputStream(configPath)) {
                Map<String, Object> cfg = yaml.load(in);
                this.blockTimeout = (int) cfg.getOrDefault("block_timeout", 7200);
                this.blockedRanges = (List<String>) cfg.getOrDefault("blocked_ranges", List.of());
                this.helperScript = (String) cfg.getOrDefault("helper_script", "/usr/local/bin/choeazy_add_ip.sh");
                this.logBlocks = (boolean) cfg.getOrDefault("log_blocks", true);
            }

            // Convert blockedRanges strings to CIDRUtils for CIDR-aware matching
            blockedCidrs.clear();
            for (String cidr : blockedRanges) {
                try {
                    blockedCidrs.add(new CIDRUtils(cidr));
                } catch (Exception e) {
                    System.err.println("[ChoEazyFirewall] Invalid CIDR in config: " + cidr);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        InetAddress addr = event.getConnection().getRemoteAddress().getAddress();
        String ip = addr.getHostAddress();

        // Check blocked CIDRs
        for (CIDRUtils cidr : blockedCidrs) {
            if (cidr.isInRange(ip)) {
                blockIp(ip);
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                        Component.text("Connection refused.")
                ));
                return;
            }
        }

        // Simple rate-limit (3 seconds)
        Instant now = Instant.now();
        if (recentConnections.containsKey(ip)) {
            if (now.minusSeconds(3).isBefore(recentConnections.get(ip))) {
                blockIp(ip);
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                        Component.text("Too many connections. Try again later.")
                ));
                return;
            }
        }
        recentConnections.put(ip, now);
    }

    private void blockIp(String ip) {
        // Run blocking async so we don't freeze event thread
        server.getScheduler().buildTask(this, () -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("sudo", helperScript, ip, String.valueOf(blockTimeout));
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor();
                if (logBlocks) {
                    System.out.println("[ChoEazyFirewall] Blocked IP: " + ip);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).schedule();
    }
}

