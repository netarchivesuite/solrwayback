package dk.kb.netarchivesuite.solrwayback.proxy;

/*************************************
 * SOCKS Proxy in JAVA
 * By Gareth Owen
 * drgowen@gmail.com
 * MIT Licence
 ************************************/


import java.nio.channels.*;
import java.io.*;
import java.net.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SOCKS 4-proxy originally from 
 */
public class SOCKSProxy implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SOCKSProxy.class);

    private final Set<String> allowedHosts;
    private final int port;

    private final ArrayList <SocksClient> clients = new ArrayList<>();
    private boolean running = true;

    public SOCKSProxy(int port, String... allowedHosts) throws IOException {
        this(port, new HashSet<>(Arrays.asList(allowedHosts)));
    }
    public SOCKSProxy(int port, Set<String> allowedHosts) throws IOException {
        this.port = port;
        Set<String> hostsLowered = new HashSet<>(allowedHosts.size());
        for (String allowedHost: allowedHosts) {
            hostsLowered.add(allowedHost.toLowerCase(Locale.ENGLISH));
        }
        this.allowedHosts = hostsLowered;
    }

    @Override
    public void run() {
        try{
            log.info("Starting up SocksProxy on port " + port);

            ServerSocketChannel socks = ServerSocketChannel.open();
            socks.socket().bind(new InetSocketAddress(port));
            socks.configureBlocking(false);
            Selector select = Selector.open();
            socks.register(select, SelectionKey.OP_ACCEPT);

            // select loop
            while (running) {
                select.select(1000);

                for (SelectionKey k: select.selectedKeys()) {

                    if (!k.isValid()) {
                        continue;
                    }

                    // new connection?
                    if (k.isAcceptable() && k.channel() == socks) {
                        handleNewConnection(select, socks);
                    } else if (k.isReadable()) {
                        // new data on a client/remote socket
                        handleNewData(select, k);
                    }
                }

                checkForTimeout();
            }

        } catch(Exception e){
            log.error("Could not start SocksProxy", e);
        }
    }

    private void handleNewConnection(Selector select, ServerSocketChannel socks) throws IOException {
        // server socket
        SocketChannel csock = socks.accept();
        if (csock == null) {
            return;
        }
        addClient(csock);
        csock.register(select, SelectionKey.OP_READ);
    }

    private void handleNewData(Selector selector, SelectionKey selectionKey) throws IOException {
        boolean foundOne = false;
        synchronized (clients) {
            for (int i = 0; i < clients.size(); i++) { // Don't use foreach as it triggers ConcurrentModificationException
                SocksClient cl = clients.get(i);
                try {
                    if (selectionKey.channel() == cl.client) { // from client (e.g. socks client)
                        cl.newClientData(selector, selectionKey);
                        foundOne = true;
                    } else if (selectionKey.channel() == cl.remote) {  // from server client is connected to (e.g. website)
                        cl.newRemoteData(selector, selectionKey);
                        foundOne = true;
                    }
                } catch (IOException e) { // error occurred - remove client
                    log.info("IO-Exception,  closing", e);
                    clients.remove(cl);
                    cl.client.close();
                    if (cl.remote != null)
                        cl.remote.close();
                    selectionKey.cancel();
                }
            }
        }
        if (!foundOne) {
            log.debug("Logic error: Could not locate client by channel");
        }
    }

    private SocksClient addClient(SocketChannel s) {
        SocksClient cl;
        try {
            cl = new SocksClient(allowedHosts, s);
        } catch (IOException e) {
            log.warn("Unable to create SocksClient from SocketChannel", e);
            return null;
        }
        synchronized (clients) {
            clients.add(cl);
        }
        log.debug("Added new SocksClient");
        return cl;
    }

    private int checkForTimeout() throws IOException {
        synchronized (clients) {
            for (int i = 0; i < clients.size(); i++) {
                SocksClient cl = clients.get(i);
                if ((System.currentTimeMillis() - cl.lastData) > 10000L) { //10 secs. Not sure this is total timeout, or connect timeout
                    cl.client.close();
                    if (cl.remote != null)
                        cl.remote.close();
                    clients.remove(cl);
                }
            }
            return clients.size();
        }
    }

    public void stopProxy(){
        running = false;
    }

    /*
     * Just to run it from the IDE for testing...
     *
     */
    public static void main(String args[]) throws Exception {
        Thread proxy = new Thread(new SOCKSProxy(9001, "belinda.statsbiblioteket.dk", "172.16.206.19"));
        proxy.setDaemon(true);
        proxy.start();
        System.out.println("Started proxy");
        Thread.sleep(100000000000L); //Keep alive
    }
}

