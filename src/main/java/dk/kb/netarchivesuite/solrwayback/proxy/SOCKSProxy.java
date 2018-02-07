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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SOCKS 4-proxy originally from 
 */
@SuppressWarnings({"UnusedReturnValue"})
public class SOCKSProxy implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SOCKSProxy.class);

    private final Set<String> allowedHosts;
    private final int port;

    private final ArrayList <SocksClient> clients = new ArrayList<>();
    private boolean running = true;
    private final ExecutorService executor = Executors.newFixedThreadPool(16, new ThreadFactory() {
        int threadID = 0;
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "SOCKS-" + threadID++);
            t.setDaemon(true);
            return t;
        }
    });

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

                Set<SelectionKey> keys = select.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();

                    if (!key.isValid()) {
                        continue;
                    }
                    // new connection?
                    if (key.isAcceptable() && key.channel() == socks) {
                        if (!handleNewConnection(select, socks)) {
                            iterator.remove();
                        }
                    } else if (key.isReadable()) {
                        // new data on a client/remote socket
                        if (!handleNewData(select, key)) {
                            iterator.remove();
                        }
                    }
                }

                checkForTimeout();
            }

        } catch(Exception e){
            log.error("Could not start SocksProxy", e);
        }
    }

    /**
     * @return true if a client was created.
     */
    private boolean handleNewConnection(Selector select, ServerSocketChannel socks) throws IOException {
        // server socket
        SocketChannel csock = socks.accept();
        if (csock == null) {
            return false;
        }
        addClient(csock);
        csock.register(select, SelectionKey.OP_READ);
        return true;
    }

    /**
     * @return false if the selectionKey is no longer active.
     */
    private boolean handleNewData(Selector selector, SelectionKey selectionKey) throws IOException {
        boolean foundOne = false;
        boolean stillActive = true;
        synchronized (clients) {
            for (int i = 0; i < clients.size(); i++) { // Don't use foreach as it triggers ConcurrentModificationException
                SocksClient cl = clients.get(i);
                try {
                    if (selectionKey.channel() == cl.client) { // from client (e.g. socks client)
                        // Using threaded processing seems to make the proxy unstable
                        //executor.submit(cl.getNewClientDataCallable(selector, selectionKey));
                        cl.newClientData(selector, selectionKey);
                        if (cl.shouldClose()) {
                            stillActive = false;
                        }
                        foundOne = true;
                        break;
                    } else if (selectionKey.channel() == cl.remote) {  // from server client is connected to (e.g. website)
                        //executor.submit(cl.getNewRemoteDataCallable(selector, selectionKey));
                        cl.newRemoteData(selector, selectionKey);
                        if (cl.shouldClose()) {
                            stillActive = false;
                        }
                        foundOne = true;
                        break;
                    }
                    // TODO: Move error-handling to executors
                } catch (Exception e) { // error occurred - remove client
                    log.info("IO-Exception, closing", e);
                    clients.remove(cl);
                    cl.client.close();
                    if (cl.remote != null) {
                        cl.remote.close();
                    }
                    return false;
                }
            }
        }
        if (!foundOne) {
            log.debug("Logic error: Could not locate client by channel");
        }
        return foundOne && stillActive;
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
        log.debug("Added new SocksClient. Total live clients " + clients.size());
        return cl;
    }

    private int checkForTimeout() throws IOException {
        synchronized (clients) {
            for (int i = 0; i < clients.size(); i++) {
                SocksClient cl = clients.get(i);
                // 10 secs. Not sure this is total timeout, or connect timeout
                if (cl.shouldClose() || (System.currentTimeMillis() - cl.lastData) > 10000L) {
                    if (cl.isFailed()) {
                        log.warn("Closing client with state failed=" + cl.isFailed() +
                                 ". Remaining clients " + (clients.size()-1));
                    }
                    cl.client.close();
                    if (cl.remote != null) {
                        cl.remote.close();
                    }
                    clients.remove(cl);
                    // TODO: Also deregister from select
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
        Thread proxy = new Thread(new SOCKSProxy(9002, "belinda.statsbiblioteket.dk", "172.16.206.19"));
        proxy.setDaemon(true);
        proxy.start();
        System.out.println("Started proxy");
        Thread.sleep(100000000000L); //Keep alive
    }
}
