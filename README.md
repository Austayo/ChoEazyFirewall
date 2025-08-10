ChoEazyFirewall Setup & Usage Guide
Overview
ChoEazyFirewall protects your Velocity proxy from Minecraft scanners and handshake spam by dynamically blocking IPs using ipset and iptables. It includes helper shell scripts and automatically configures necessary permissions.

First-Time Setup
Start the Velocity proxy once to let the plugin extract the setup scripts to:

arduino
Copy
Edit
plugins/ChoEazyFirewall/setup/
Run the setup script as root to configure firewall rules, install helper scripts, and set sudoers permissions:

bash
Copy
Edit
sudo bash plugins/ChoEazyFirewall/setup/setup.sh
The setup script will:

Prompt for the username running the Velocity proxy

Install choeazy_add_ip.sh to /usr/local/bin/ and set executable permissions

Create the choeazy_block ipset (if missing)

Add an iptables rule to drop IPs in that set (if missing)

Automatically create a sudoers file entry allowing the chosen user to run the helper script without a password

Configuration
The config file is located at:

bash
Copy
Edit
plugins/ChoEazyFirewall/choeazy.conf
Defaults include:

block_timeout: Duration (seconds) IPs are blocked (default: 7200)

blocked_ranges: List of IP prefixes to block immediately

helper_script: Path to the helper script used to add IPs to the firewall

log_blocks: Whether to log blocked IPs (true/false)

Modify this file as needed and restart Velocity to apply changes.

Runtime Behavior
The plugin automatically blocks IPs that connect too rapidly or are in blocked ranges.

Block commands are run asynchronously with sudo using your helper script.

Logs are printed to console for each blocked IP if log_blocks is enabled.

Troubleshooting
If blocks do not work, verify:

The helper script exists at the configured path and is executable

The sudoers file at /etc/sudoers.d/choeazyfirewall grants the Velocity user passwordless access to the helper script

Firewall rules are correctly installed by the setup script

Check Velocity console logs for error messages from blocking commands.

Updating Setup Scripts
If you update embedded setup scripts in a plugin update:

Manually delete the extracted setup folder or files to force re-extraction on next start

Then re-run setup.sh as root to apply any new firewall rules or scripts