#!/bin/bash
set -e

# Check running as root
if [[ $EUID -ne 0 ]]; then
   echo "[ChoEazyFirewall] Please run as root or with sudo."
   exit 1
fi

read -rp "Enter the username running Velocity proxy: " velocityuser

if ! id -u "$velocityuser" >/dev/null 2>&1; then
  echo "User '$velocityuser' does not exist."
  exit 1
fi

echo "[ChoEazyFirewall] Installing helper script..."
cp choeazy_add_ip.sh /usr/local/bin/
chmod +x /usr/local/bin/choeazy_add_ip.sh

echo "[ChoEazyFirewall] Creating IPv4 ipset..."
if ipset list choeazy_block >/dev/null 2>&1; then
    echo "[ChoEazyFirewall] IPv4 ipset 'choeazy_block' already exists, skipping creation."
else
    ipset create choeazy_block hash:ip timeout 3600
    echo "[ChoEazyFirewall] IPv4 ipset 'choeazy_block' created."
fi

echo "[ChoEazyFirewall] Creating IPv6 ipset..."
if ipset list choeazy_block_v6 >/dev/null 2>&1; then
    echo "[ChoEazyFirewall] IPv6 ipset 'choeazy_block_v6' already exists, skipping creation."
else
    ipset create choeazy_block_v6 hash:ip family inet6 timeout 3600
    echo "[ChoEazyFirewall] IPv6 ipset 'choeazy_block_v6' created."
fi

echo "[ChoEazyFirewall] Adding iptables rule..."
if iptables -C INPUT -m set --match-set choeazy_block src -j DROP 2>/dev/null; then
    echo "[ChoEazyFirewall] IPv4 iptables rule already exists, skipping."
else
    iptables -I INPUT -m set --match-set choeazy_block src -j DROP
    echo "[ChoEazyFirewall] IPv4 iptables rule added."
fi

echo "[ChoEazyFirewall] Adding ip6tables rule..."
if ip6tables -C INPUT -m set --match-set choeazy_block_v6 src -j DROP 2>/dev/null; then
    echo "[ChoEazyFirewall] IPv6 ip6tables rule already exists, skipping."
else
    ip6tables -I INPUT -m set --match-set choeazy_block_v6 src -j DROP
    echo "[ChoEazyFirewall] IPv6 ip6tables rule added."
fi

echo ""
echo "[ChoEazyFirewall] Setup complete."
echo "Adding sudoers entry for user '$velocityuser'..."

sudoersfile="/etc/sudoers.d/choeazyfirewall"
echo "$velocityuser ALL=(root) NOPASSWD: /usr/local/bin/choeazy_add_ip.sh" > "$sudoersfile"
chmod 440 "$sudoersfile"

echo "Sudoers entry added to $sudoersfile"
echo "You can review it by running: sudo cat $sudoersfile"
