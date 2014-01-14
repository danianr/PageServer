package edu.columbia.cuit.pageServer;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;


import org.apache.log4j.Logger;

public class Pcd implements Runnable{
	
	private ServerSocket socket;
	private AtomicReference<Thread> acceptThread;
	
	private final InetAddress iaddr;
	private final int port;
	private final int backlog;
	private final ConcurrentHashMap<PcdRequest,Thread> pcdRequestThreads;
	private final Pagecontrol pagecontrol;
	private final AccessManager accessManager;
	private boolean shutdownFlag;
	
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	
	/* This class implements the PCD2 protocol but utilizes
	 * and is intended to be instantiated from the PersistenceLayer
	 * class.  
     */
		

	public Pcd(Pagecontrol pagecontrol, AccessManager accessManager, InetAddress iaddr, int port, int backlog) {
		this.iaddr = iaddr;
		this.port = port;
		this.backlog = backlog;
		this.pcdRequestThreads = new ConcurrentHashMap<PcdRequest,Thread>();
		this.accessManager = accessManager;
		this.pagecontrol = pagecontrol;
		this.shutdownFlag = false;
		acceptThread = new AtomicReference<Thread>(null);
	}
		
	
	private void listen() {
		
		//Only one instance should invoke listen
		if (acceptThread.compareAndSet(null, Thread.currentThread())){
		
		   try {
			   socket = new ServerSocket();
			   socket.setReuseAddress(true);
			   socket.setSoTimeout(0);
			   InetSocketAddress sockaddr;
			   if (iaddr == null){
				   // If the iaddr was not specified, use the wildcard address
				   sockaddr = new InetSocketAddress(port);
			   }else{
				   sockaddr = new InetSocketAddress(iaddr, port);
			   }
			   socket.bind(sockaddr, backlog);
				
		   }catch (SocketException e){
			   acceptThread = null;
			   log.error(e);
			   return;
		   }catch (IOException e) {
			   acceptThread = null;
			   log.error(e);
			   return;
		   }

		
		   LISTEN: while (!shutdownFlag){

			   Socket client = null;
			   try {
				   client = socket.accept();
			   } catch (InterruptedIOException e){
				   log.warn(e);
			   } catch (SocketException e){
				   log.warn(e);
			   } catch (IOException e) {
				   log.warn(e);
			   } finally {
				   if (shutdownFlag){
					   if ( (client != null) && client.isConnected() ){
						try {
							client.close();
						} catch (IOException e1){
							log.warn("Caught IOException closing down PCD socket", e1);
						}
					   }
					   break LISTEN;
				   }
			   }
	
			   if ( client.isConnected() && pcdAllowAccess(client)){
				   PcdRequest request = new PcdRequest(pcdRequestThreads, client, pagecontrol);									
				   request.process();
			   }else {
				   if (client.isConnected()){
					   // If the client is not authorized but is performing more than a simple
					   // portscan log the access attempt as at the Warning Level
					   InetAddress connectedPeer = client.getInetAddress();
					   String peerHostname = "UNKNOWN";
					   if (connectedPeer != null) peerHostname =  connectedPeer.getCanonicalHostName();
				 
					   log.info("Unauthorized PCD access attempt from " + peerHostname);
					   try {
						   client.close(); 
					   } catch (IOException e) {
						   log.warn("Caught IOException while trying to close PCD connection", e);
					   }
				   }else{
					   // Networking monitoring will perform a naive open of the port
					   // in order to "monitor" service status.  This should really
					   // be a DEBUG-level statement, but the log volume produced would
					   // be too high.
					   log.trace("Client closed connection before request submitted");
				   }
			   }
			
		   } // LISTEN

		
		   // Make sure all of the requests finish before releasing the socket
		   log.trace("Attempting to join all active requestThreads");
		   for (Thread requestThread: pcdRequestThreads.values()){
			   if ( (requestThread != null) && requestThread.isAlive() ){
				   //Wait a reasonable amount of time before killing the request
				   try {
					   log.trace("Attemping to join requestThread(" + requestThread.getName() + ")");
					   requestThread.join(2000l);
				   } catch (InterruptedException e1) {
					   log.warn("Caught InterruptedException attempting to join requestThread", e1);
					   requestThread.interrupt();
				   }
			   }
		   }
		   log.trace("Finished accounting for all active requestThreads");
		   
		   try { socket.close(); } 
		   catch (IOException e) {
			   log.warn("Caught IOException closing ServerSocket:", e);
		   }
		   acceptThread = null;
		   return;
		}
	}
	
	public void run(){ this.listen(); }
	
	public void shutdown(){
		log.info("Shutting down Pcd");
		shutdownFlag=true;	
		if (socket.isBound() && !socket.isClosed()){
			try {
				socket.close();
				if (socket.isClosed()) log.trace("ServerSocket for PcdAdapter closed");
				if ( (acceptThread != null) && acceptThread.get().isAlive() ){
					log.trace("acceptThread is still alive");
					if (log.isTraceEnabled()){
						log.trace("Attempting to join " + acceptThread.get().getName());
					}
					acceptThread.get().join(10000l);
					log.trace("AcceptThread joined");
				}
			} catch (IOException e) {
				log.warn("Caught IOException closing ServerSocket", e);
			} catch (InterruptedException e) {
				log.trace("Was unable to join " + acceptThread.get().getId(), e);
			}
		}		
	}
	
	
	private boolean pcdAllowAccess(Socket incoming){
		InetAddress peer = incoming.getInetAddress();
		
		if (peer == null) return false;

		if (log.isTraceEnabled()){
			String peerIP = peer.getHostAddress();
			log.trace("PcdAllowAccess: peer ip address is " + peerIP );
		}
		
		if (peer.isLoopbackAddress() || peer.isLinkLocalAddress() ){
			log.trace("Allowing access for loopback/link local IP");
			return true;
		}
		
		return accessManager.allowedIP(peer);
	}

}
