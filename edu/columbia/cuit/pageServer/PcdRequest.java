package edu.columbia.cuit.pageServer;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class PcdRequest implements Runnable {

	private final ConcurrentHashMap<PcdRequest, Thread> threadMap;
	private final Socket client;
	private final Pagecontrol pagecontrol;
	private static final Logger log = Logger.getLogger("edu.columbia.cuit.PageServer");
	private static final int LINGER = 6;  // allow the Socket.close() to block for this many seconds to send pending data
	
	
	public PcdRequest(ConcurrentHashMap<PcdRequest, Thread> threadMap, Socket client, Pagecontrol pagecontrol){
		this.threadMap = threadMap;
		this.client = client;
		this.pagecontrol = pagecontrol;
	}
	
	public void process(){
		Thread processThread = new Thread(this);
		threadMap.put(this, processThread);
		processThread.start();
	}
	
	
	public void run() {
		

		try {
			log.debug("Started new PcdRequest thread");
			
			// Due to the 1 byte nature of the reply, without TcpNoDelay
			// responses may not be sent before the connection is closed,
			// resulting in the unsent datagram being dropped without the
			// SO_LINGER option set.
			client.setTcpNoDelay(true);
			client.setSoLinger(true, LINGER);
			
			OutputStreamWriter out = null;
			InetAddress peerAddress = client.getInetAddress();
			String printer = "UNKNOWN";
			if (peerAddress != null) printer = peerAddress.getCanonicalHostName();
			
			out = new OutputStreamWriter(client.getOutputStream());
			InputStreamReader in = new InputStreamReader(client.getInputStream());
			BufferedReader bin = new BufferedReader(in);
						
			String pcdCommand = bin.readLine();

			Pattern pcdRegexp = Pattern.compile("^(QUERY|DEDUCT):([^:]{2,8}):(\\d+)");
			Matcher pcdMatcher = pcdRegexp.matcher(pcdCommand);
			
			// Note that the pcd2 protocol only responds with a single byte:
			// 1 for success, and 0 for any failure state including insufficient funds
			// or a malformed protocol request.  Hey, I didn't write it.
			if (pcdMatcher.matches()){
				String command = pcdMatcher.group(1);
				String username = pcdMatcher.group(2);
				int pages = Integer.parseInt(pcdMatcher.group(3));

				if ("QUERY".equals(command)){
					if (pagecontrol.query(printer, username, pages)){
						if (log.isTraceEnabled()) log.trace("ALLOWED " + printer + "> QUERY:"+ username + ":"+ pages);
						out.write('1');
						out.flush();
					}else{
						if (log.isTraceEnabled()) log.trace("REJECTED " + printer + "> QUERY:"+ username + ":"+ pages);
						out.write('0');
						out.flush();
					}
				}else if ("DEDUCT".equals(command)){
					if(pagecontrol.query(printer, username, pages)){
						pagecontrol.deduct(printer, username, pages);						
						if (log.isTraceEnabled()) log.trace("ALLOWED " + printer + "> DEDUCT:"+ username + ":"+ pages);
						out.write('1');
						out.flush();
					}else{
						if (log.isTraceEnabled()) log.trace("REJECTED " + printer + "> DEDUCT:"+ username + ":"+ pages);
						out.write('0');
						out.flush();
					}
				}else{
					// This code should not be reachable given the above alternation
					out.write('0');
					out.flush();
				}				
			}else{
				log.debug("Invalid command: "+ pcdCommand);
				out.write('0');
				out.flush();
			}
		} catch (SocketTimeoutException e){
			log.trace(e);
		} catch (InterruptedIOException e){
			log.warn("Interrupted PCDRequest thread", e);
		} catch (SocketException e){
			log.trace(e);
		}catch (IOException e) {
			log.debug(e);
		} finally{
			threadMap.remove(this);
			try {
				client.close();
			} catch (IOException e) {
				log.warn("Caught IOException trying to close pcd client connection", e);
			}
			log.trace("Finished PcdRequest Thread");
		}
	}

}
