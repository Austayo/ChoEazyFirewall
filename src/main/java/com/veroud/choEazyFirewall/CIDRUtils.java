package com.veroud.choEazyFirewall;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class CIDRUtils {
    private final InetAddress inetAddress;
    private InetAddress startAddress;
    private InetAddress endAddress;
    private final int prefixLength;

    public CIDRUtils(String cidr) throws UnknownHostException {
        if (cidr == null || !cidr.contains("/")) {
            throw new IllegalArgumentException("Invalid CIDR notation: " + cidr);
        }

        String[] parts = cidr.split("/");
        inetAddress = InetAddress.getByName(parts[0]);
        prefixLength = Integer.parseInt(parts[1]);

        calculate();
    }

    private void calculate() throws UnknownHostException {
        ByteBuffer maskBuffer;
        ByteBuffer targetBuffer = ByteBuffer.wrap(inetAddress.getAddress());
        int targetSize = targetBuffer.capacity();

        if (inetAddress.getAddress().length == 4) {
            maskBuffer = ByteBuffer.allocate(4);
        } else {
            maskBuffer = ByteBuffer.allocate(16);
        }

        for (int i = 0; i < maskBuffer.capacity(); i++) {
            int mask;
            if (prefixLength >= (i + 1) * 8) {
                mask = 0xFF;
            } else if (prefixLength > i * 8) {
                mask = (0xFF << (8 - (prefixLength - i * 8))) & 0xFF;
            } else {
                mask = 0;
            }
            maskBuffer.put((byte) mask);
        }

        byte[] startIp = new byte[targetSize];
        byte[] endIp = new byte[targetSize];

        for (int i = 0; i < targetSize; i++) {
            startIp[i] = (byte) (targetBuffer.get(i) & maskBuffer.array()[i]);
            endIp[i] = (byte) (startIp[i] | ~maskBuffer.array()[i]);
        }

        startAddress = InetAddress.getByAddress(startIp);
        endAddress = InetAddress.getByAddress(endIp);
    }

    public boolean isInRange(String ipAddress) {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            byte[] target = address.getAddress();

            byte[] start = startAddress.getAddress();
            byte[] end = endAddress.getAddress();

            for (int i = 0; i < target.length; i++) {
                int t = target[i] & 0xFF;
                int s = start[i] & 0xFF;
                int e = end[i] & 0xFF;

                if (t < s || t > e) {
                    return false;
                }
            }
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
