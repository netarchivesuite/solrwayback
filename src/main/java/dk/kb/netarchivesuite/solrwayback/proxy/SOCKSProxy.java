package dk.kb.netarchivesuite.solrwayback.proxy;

/*************************************
 * SOCKS Proxy in JAVA
 * By Gareth Owen
 * drgowen@gmail.com
 * MIT Licence
 ************************************/


import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SOCKSProxy implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(SOCKSProxy.class);
  private int port;
  private String proxy_allow_host;
  private boolean running=true;
  
    // socks client class - one per client connection
    class SocksClient {
        SocketChannel client, remote;
        boolean connected;
        long lastData = 0;

        SocksClient(SocketChannel c) throws IOException {  
          client = c;
            client.configureBlocking(false);
            lastData = System.currentTimeMillis();
        }

        public void newRemoteData(Selector selector, SelectionKey sk) throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(1024);
            if(remote.read(buf) == -1)
                throw new IOException("disconnected");
            lastData = System.currentTimeMillis();
            buf.flip();
            client.write(buf);
        }

        public void newClientData(Selector selector, SelectionKey sk) throws IOException {
            if(!connected) {
                ByteBuffer inbuf = ByteBuffer.allocate(512);
                if(client.read(inbuf)<1)
                    return;
                inbuf.flip();

                // read socks header
                int ver = inbuf.get();
                if (ver != 4) {
                  log.info("incorrection version:"+ver);
                    throw new IOException("incorrect version" + ver);
                }
                int cmd = inbuf.get();

                // check supported command
                if (cmd != 1) {
                  log.info("incorrection version:"+ver);
                    throw new IOException("incorrect version");
                }

                final int port = inbuf.getShort();

                final byte ip[] = new byte[4];
                // fetch IP
                inbuf.get(ip);                
                InetAddress remoteAddr=  null;
                try{
                remoteAddr = InetAddress.getByAddress(ip);
                }
                catch(Exception e){
                 log.info("error IP lookup",e); 
                }
                                
                while ((inbuf.get()) != 0) ; // username
                
                // hostname provided, not IP
                if (ip[0] == 0 && ip[1] == 0 && ip[2] == 0 && ip[3] != 0) { // host provided
                
                  String host = "";
                    byte b;
                    while ((b = inbuf.get()) != 0) {
                        host += (char)b;
                    }
                    try{
                    remoteAddr = InetAddress.getByName(host);
                    }
                    catch(Exception e){
                      e.printStackTrace();
                    }                    
                   if (remoteAddr.toString().toLowerCase().startsWith(proxy_allow_host.toLowerCase())){
                     log.info("Proxying:"+remoteAddr);
                    }
                   else{
                     log.info("leaking prevented for url:"+remoteAddr);
                     //throw new IOException("leaking prevented for url:"+remoteAddr);
                   }                                     
                }

                remote = SocketChannel.open(new InetSocketAddress(remoteAddr, port));

                ByteBuffer out = ByteBuffer.allocate(20);
                out.put((byte)0);
                out.put((byte) (remote.isConnected() ? 0x5a : 0x5b));
                out.putShort((short) port);
                out.put(remoteAddr.getAddress());
                out.flip();
                client.write(out);
                if(!remote.isConnected()){
                  log.info("connect failed");
                  throw new IOException("connect failed");
                }

                remote.configureBlocking(false);
                remote.register(selector, SelectionKey.OP_READ);

                connected = true;
            } else {
                ByteBuffer buf = ByteBuffer.allocate(1024);
                if(client.read(buf) == -1){
                  throw new IOException("disconnected");
                }
                lastData = System.currentTimeMillis();
                buf.flip();
                remote.write(buf);
            }
        }
    }

    static ArrayList <SocksClient> clients = new ArrayList<SocksClient>();

    // utility function
    public SocksClient addClient(SocketChannel s) {
        SocksClient cl;
        try {
            cl = new SocksClient(s);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        clients.add(cl);
        return cl;
    }

    public SOCKSProxy(int port,String proxy_allow_host) throws IOException {
        this.port = port;        
        this.proxy_allow_host=proxy_allow_host;
        }

    public void run() {
      try{
        log.info("Starting up socksproxy on port:"+port);
        
        
        ServerSocketChannel socks = ServerSocketChannel.open();
          socks.socket().bind(new InetSocketAddress(port));
          socks.configureBlocking(false);
          Selector select = Selector.open();
          socks.register(select, SelectionKey.OP_ACCEPT);

          int lastClients = clients.size();
          // select loop
          while(running) {
              select.select(1000);

              Set keys = select.selectedKeys();
              Iterator iterator = keys.iterator();
              while (iterator.hasNext()) {
                  SelectionKey k = (SelectionKey) iterator.next();

                  if (!k.isValid())
                      continue;

                  // new connection?
                  if (k.isAcceptable() && k.channel() == socks) {
                      // server socket
                      SocketChannel csock = socks.accept();
                      if (csock == null)
                          continue;
                      addClient(csock);
                      csock.register(select, SelectionKey.OP_READ);
                  } else if (k.isReadable()) {
                      // new data on a client/remote socket
                      for (int i = 0; i < clients.size(); i++) {
                          SocksClient cl = clients.get(i);
                          try {
                              if (k.channel() == cl.client) // from client (e.g. socks client)
                                  cl.newClientData(select, k);
                              else if (k.channel() == cl.remote) {  // from server client is connected to (e.g. website)
                                  cl.newRemoteData(select, k);
                              }
                          } catch (IOException e) { // error occurred - remove client
                              cl.client.close();
                              if (cl.remote != null)
                                  cl.remote.close();
                              k.cancel();
                              clients.remove(cl);
                          }

                      }
                  }
              }

              // client timeout check
              for (int i = 0; i < clients.size(); i++) {
                  SocksClient cl = clients.get(i);
                  if((System.currentTimeMillis() - cl.lastData) > 10000L) { //10 secs. Not sure this is total timeout, or connect timeout
                      cl.client.close();
                      if(cl.remote != null)
                          cl.remote.close();
                      clients.remove(cl);
                  }
              }
              if(clients.size() != lastClients) {
                  lastClients = clients.size();
              }
          }
              
      }
      catch(Exception e){
        log.error("Could not start SocksProxy:"+e.getMessage());
        e.printStackTrace();
      }
      
  }

    public void stopProxy(){
      running=false;
    }
    
  /*
   * Just to run it from the IDE for testing...
   *   
   */    
  public static void main(String args[]) throws Exception {  
    Thread proxy = new Thread(new SOCKSProxy(9000,"teg-desktop.sb.statsbiblioteket.dk"));
    proxy.setDaemon(true);
    proxy.start();      

    Thread.sleep(100000000000L); //Keep alive
  }

    

}

