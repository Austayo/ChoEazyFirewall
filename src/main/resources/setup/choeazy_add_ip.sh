#!/bin/bash
IP="$1"
TIMEOUT="$2"

if [[ -z "$IP" || -z "$TIMEOUT" ]]; then
  echo "Usage: $0 <ip> <timeout_seconds>"
  exit 1
fi

# Function to check if IP is IPv6
is_ipv6() {
  [[ "$IP" == *:* ]]
}

# Create sets if missing (ipset creation is idempotent in setup, but double-check here)
ipset list choeazy_block >/dev/null 2>&1 || ipset create choeazy_block hash:ip timeout 3600
ipset list choeazy_block_v6 >/dev/null 2>&1 || ipset create choeazy_block_v6 hash:ip family inet6 timeout 3600

if is_ipv6; then
  ipset -exist add choeazy_block_v6 "$IP" timeout "$TIMEOUT"
else
  ipset -exist add choeazy_block "$IP" timeout "$TIMEOUT"
fi

exit $?
