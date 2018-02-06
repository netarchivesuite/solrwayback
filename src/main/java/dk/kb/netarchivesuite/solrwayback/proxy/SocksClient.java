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

/**
 * socks client class - one per client connection
 */
class SocksClient {
    private static final Logger log = LoggerFactory.getLogger(SocksClient.class);
    private final Set<String> allowedHosts;

    SocketChannel client, remote;
    private boolean connected = false;
    long lastData = 0;

    SocksClient(Set<String> allowedHosts, SocketChannel client) throws IOException {
        this.client = client;
        this.client.configureBlocking(false);
        lastData = System.currentTimeMillis();
        this.allowedHosts = allowedHosts;
    }

    void newRemoteData(Selector selector, SelectionKey sk) throws IOException {
        copyData(remote, client);
    }

    void newClientData(Selector selector, SelectionKey sk) throws IOException {
        if (connected) {
            copyData(client, remote);
            return;
        }

        ByteBuffer inbuf = ByteBuffer.allocate(512);
        if (client.read(inbuf) < 1) {
            return;
        }
        inbuf.flip();

        // read socks header
        int ver = inbuf.get();
        if (ver != 4) {
            log.info("Incorrection socks version " + ver + ", expected 4");
            throw new IOException("Incorrection socks version " + ver + ", expected 4");
        }
        int cmd = inbuf.get();

        // check supported command
        if (cmd != 1) {
            log.info("Incorrect command " + cmd + ", expected 1");
            throw new IOException("Incorrect command " + cmd + ", expected 1");
        }
        final int port = inbuf.getShort();

        final byte ip[] = new byte[4];
        // fetch IP
        inbuf.get(ip);
        InetAddress remoteAddr = null;
        try {
            remoteAddr = InetAddress.getByAddress(ip);
        } catch (Exception e) {
            log.info("error IP lookup", e);
        }

        while ((inbuf.get()) != 0) ; // username

        // hostname provided, not IP
        if (ip[0] == 0 && ip[1] == 0 && ip[2] == 0 && ip[3] != 0) { // host provided
            String host = "";
            byte b;
            while ((b = inbuf.get()) != 0) {
                host += (char) b;
            }
            if (allowedHosts.contains(host.toLowerCase(Locale.ENGLISH))) {
                System.out.println("Allowing:" + host);
                log.info("Allowing:" + host);
            } else {
                System.out.println("leaking prevented for url:" + host);
                log.info("leaking prevented for url:" + host);
                failConnectionToHost(remoteAddr, port);
                return;
            }

            try {
                remoteAddr = InetAddress.getByName(host);
                System.out.println("calling remoteaddr " + remoteAddr);
                log.info("calling remoteaddr " + remoteAddr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // TODO: Why do we get all these IP lookups with Chrome? Is the check for host too picky?
            if (remoteAddr == null || !allowedHosts.contains(remoteAddr.getHostAddress())) {
                log.info("Leaking prevented for IP-address " +
                         (remoteAddr == null ? "N/A" : remoteAddr.getHostAddress()));
                failConnectionToHost(remoteAddr, port);
                return;
            } else {
                log.info("Allowing connections to IP-address " + remoteAddr.getHostAddress());
            }
        }

        connectToRemote(selector, port, remoteAddr);
    }

    private void connectToRemote(Selector selector, int port, InetAddress remoteAddr) throws IOException {
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
            throw new IOException("connect failed");
        }

        remote.configureBlocking(false);
        remote.register(selector, SelectionKey.OP_READ);
        connected = true;
    }

    private void failConnectionToHost(InetAddress remoteAddr, int port) throws IOException {
        ByteBuffer out = ByteBuffer.allocate(20);
        out.put((byte) 0);
        out.put((byte) (0x5b)); // Not connected
        out.putShort((short) port);
        out.put(remoteAddr.getAddress());
        out.flip();
        client.write(out);
    }

    private void copyData(SocketChannel source, SocketChannel destination) throws IOException {
      // TODO: Understand why we only copy 1KB here, instead of all available data
      ByteBuffer buf = ByteBuffer.allocate(1024);
      int bufSize;
      while ((bufSize = source.read(buf)) != -1 && bufSize != 0) {
        System.out.println("Buffer size read: " + bufSize);
        log.info("Buffer size read: " + bufSize);
        lastData = System.currentTimeMillis();                
        buf.flip();
        destination.write(buf);
     }
  }
    private void copyDataOld(SocketChannel source, SocketChannel destination) throws IOException {
      // TODO: Understand why we only copy 1KB here, instead of all available data
      ByteBuffer buf = ByteBuffer.allocate(1024);
      int bufSize = source.read(buf);
      System.out.println("Buffer size read: " + bufSize);
      if (bufSize == -1) {
        log.warn("Disconnect under copyData");
        System.out.println("Disconnect under copyData");          
        throw new IOException("disconnected");
      }
      lastData = System.currentTimeMillis();                
      buf.flip();
      destination.write(buf);
  }

}
