/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.
 */
package dk.kb.netarchivesuite.solrwayback.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * socks client class - one per client connection
 */
class SocksClient {
    private static final Logger log = LoggerFactory.getLogger(SocksClient.class);
    private final Set<String> allowedHosts;

    SocketChannel client, remote;
    private boolean connected = false;
    private boolean failed = false;
    private boolean eos = false;

    private long blockedCount = 0;
    private long acceptedCount = 0;
    long lastData;

    SocksClient(Set<String> allowedHosts, SocketChannel client) throws IOException {
        this.client = client;
        this.client.configureBlocking(false);
        lastData = System.currentTimeMillis();
        this.allowedHosts = allowedHosts;
    }

    public Callable<String> getNewRemoteDataCallable(final Selector selector, final SelectionKey sk) {
        return new Callable<String>() {
            @Override
            public String call() throws Exception {
                newRemoteData(selector, sk);
                return "newRemoteData completed";
            }
        };
    }
    public synchronized void newRemoteData(Selector selector, SelectionKey sk) throws IOException {
        copyData(remote, client);
    }

    public Callable<String> getNewClientDataCallable(final Selector selector, final SelectionKey sk) {
        return new Callable<String>() {
            @Override
            public String call() throws Exception {
                newClientData(selector, sk);
                return "newClientData completed";
            }
        };
    }
    // https://en.wikipedia.org/wiki/SOCKS#SOCKS4
    public synchronized void newClientData(Selector selector, SelectionKey sk) throws IOException {
        if (connected) {
            copyData(client, remote);
            return;
        }
        if (shouldClose()) {
            log.warn("Attempting to use SOCKS client marked as shouldClose");
            return;
        }

        // Read client request into buffer
        ByteBuffer inbuf = ByteBuffer.allocate(512);
        if (client.read(inbuf) < 1) {
            eos = true;
            return;
        }
        inbuf.flip();

        // Is this the correct SOCKS version?
        int ver = inbuf.get();
        if (ver != 4) {
            failed = true;
            log.info("Incorrection socks version " + ver + ", expected 4");
            client.close();
            throw new IOException("Incorrection socks version " + ver + ", expected 4");
        }

        // Read command (1 == TCP/IP stream, 2 == port binding). Only TCP/IP stream is supported
        int cmd = inbuf.get();
        if (cmd != 1) {
            failed = true;
            log.info("Incorrect command " + cmd + ", expected 1");
            client.close();
            throw new IOException("Incorrect command " + cmd + ", expected 1");
        }

        // Port number
        final int port = inbuf.getShort();

        // IP address
        final byte ip[] = new byte[4];
        inbuf.get(ip);
        InetAddress remoteAddr;
        try {
            remoteAddr = InetAddress.getByAddress(ip);
        } catch (Exception e) {
            client.close();
            throw new IOException("Unable to determine IP address", e);
        }

        // ID String
        String user = readString("user ID", inbuf);

        // Optional host (implies invalid IP address)
        if (ip[0] == 0 && ip[1] == 0 && ip[2] == 0 && ip[3] != 0) { // host provided
            String host = readString("host", inbuf);

            if (allowedHosts.contains(host.toLowerCase(Locale.ENGLISH))) {
                message("Allowing connection to host " + host);
            } else {
                message("Leaking prevented for host " + host);
                failConnectionToHost(remoteAddr, port);
                return;
            }

            try {
                remoteAddr = InetAddress.getByName(host);
            } catch (Exception e) {
                throw new IOException("Unable to get IP for allowed host " + host);
            }
        } else {
            // TODO: Why do we get all these IP lookups with Chrome? Is the check for host too picky?
            if (allowedHosts.contains(remoteAddr.getHostAddress())) {
                log.info("Allowing connection to IP-address " + remoteAddr.getHostAddress());
            } else {
                log.info("Leaking prevented for IP-address " + remoteAddr.getHostAddress());
                failConnectionToHost(remoteAddr, port);
                return;
            }
        }

        connectToRemote(selector, port, remoteAddr);
    }

    private void message(String message) {
        log.info(message);
        //System.out.println(message);
    }

    public boolean isFailed() {
        return failed;
    }

    public long getBlockedCount() {
        return blockedCount;
    }

    public boolean shouldClose() {
        return isFailed() || eos;
    }

    private String readString(String designation, ByteBuffer inbuf) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte b;
        try {
            while ((b = inbuf.get()) != 0) {
                sb.append((char) b);
            }
        } catch (Exception e) {
            throw new IOException("Error reading '" + designation + "' from connect request", e);
        }
        return sb.toString();
    }

    private void connectToRemote(Selector selector, int port, InetAddress remoteAddr) throws IOException {
        message("Establishing connection to IP " + remoteAddr +
                " (total accepted connections for this SOCKS client: " + ++acceptedCount + ")");
        remote = SocketChannel.open(new InetSocketAddress(remoteAddr, port));

        ByteBuffer out = ByteBuffer.allocate(20);
        out.put((byte) 0);
        out.put((byte) (remote.isConnected() ? 0x5a : 0x5b));
        out.putShort((short) port);
        out.put(remoteAddr.getAddress());
        out.flip();
        client.write(out);
        if (!remote.isConnected()) {
            log.info("connect failed");
            return; // Not a fatal error, just a missed connection
            // TODO: Should this be marked as EOS or failed?
            //throw new IOException("connect failed");
        }

        remote.configureBlocking(false);
        remote.register(selector, SelectionKey.OP_READ);
        connected = true;
    }

    private void failConnectionToHost(InetAddress remoteAddr, int port) throws IOException {
        blockedCount++;
        ByteBuffer out = ByteBuffer.allocate(20);
        out.put((byte) 0);
        out.put((byte) (0x5b)); // Not connected
        out.putShort((short) port);
        out.put(remoteAddr.getAddress());
        out.flip();
        client.write(out);
    }

    private void copyData(SocketChannel source, SocketChannel destination) {
        final long startTime = System.nanoTime();
        if (failed) {
            log.info("Skipping copyData as SocksClient is marked as failed");
            return;
        }
        long total = 0;
        try {
            ByteBuffer buf = ByteBuffer.allocate(8192);
            int bufSize;
            while ((bufSize = source.read(buf)) != -1 && bufSize != 0) {
                total += bufSize;
                buf.flip();
                destination.write(buf);
            }
            if (total != 0) {
                message("Copied full buffer size " + total + " bytes in " +
                        (System.nanoTime() - startTime) / 1000000 + " ms");
            }
            if (bufSize == -1) {
                message("Closing SOCKS client as EOS (-1) was received");
                eos = true;
            }
            lastData = System.currentTimeMillis(); // Even if total == 0 to keep alive
        } catch (Exception e) {
            log.warn("Exception copying data. Marking SOCKSClient as failed", e);
            failed = true;
        }
        // else if (bufSize == -1) {
//            message("Logic error during copyData: Got -1 as only result. Client disconnected?");
  //      }
    }

}
