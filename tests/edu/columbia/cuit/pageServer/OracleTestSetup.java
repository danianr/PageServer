package edu.columbia.cuit.pageServer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import oracle.jdbc.pool.OracleDataSource;

public class OracleTestSetup {
	
	private OracleDataSource ods;
	private Connection conn;

	class PrintDate{
		private final String datestamp;
		private final static String datefmt = "YYYY-MM-DD HH24:MI:SS";
	
		protected PrintDate(String datestamp){
			this.datestamp = datestamp;
		}
		
		public String toString(){
			StringBuffer sb = new StringBuffer("to_date('");
			sb.append(datestamp);
			sb.append("','");
			sb.append(datefmt);
			sb.append("')");
			return sb.toString();
		}
	}
	
	
	private void insertMacro(String table, List<?> values) throws SQLException{

		Vector<Integer> valuesIndex = new Vector<Integer>(values.size());
		
		StringBuffer sb = new StringBuffer("INSERT into ");
		PreparedStatement ins;
						
		sb.append(table);
		sb.append("values ( ");
		for (int i=0; i < values.size(); i++){
			if (i > 0) sb.append(", ");
			if (values.get(i).getClass().equals(PrintDate.class)){
				sb.append(values.get(i));
			}else{
				sb.append("?");
				valuesIndex.add(i);
			}
		}
		sb.append(" )");

		ins = conn.prepareStatement(sb.toString());

		Iterator<Integer> inparam = valuesIndex.iterator();
		int p = 1;  //holds the position of the IN parameter
		while (inparam.hasNext()){	
		   Integer i = inparam.next();
			
		   if (values.get(i).getClass().equals(String.class)){
				ins.setString(p, (String) values.get(i));
			}else if (values.get(i).getClass().equals(Long.class)){
				ins.setLong(p, (Long) values.get(i));
			}else if (values.get(i).getClass().equals(Integer.class)){
				ins.setInt(p, (Integer) values.get(i));
			}else{
				System.out.println("Something went very wrong; not a String, Long, or Integer in values");
			}
		    p++;
		}
					
		ins.executeUpdate();
	}
	
	
	
	
	public OracleTestSetup(String jdbcURI, String dumpfile) throws SQLException{

		FileInputStream fin;
		
		ods = new OracleDataSource();
		ods.setURL(jdbcURI);
		conn = ods.getConnection();
		conn.setAutoCommit(true);


		File dumpData = new File(dumpfile);
	    if (dumpData.exists() && dumpData.canRead()){
	    	
	    	FileChannel in = new FileInputStream(dumpData).getChannel();
	    	ByteBuffer buf = ByteBuffer.allocateDirect(64*1024);
	    	
	    	// scan for the position of each table start
	    	ArrayList<Long> gs = new ArrayList<Long>(20);
	    	while (in.position() < in.size()){
	    		long pos = in.position();
	    		in.read(buf);
	    		while (buf.position() <= buf.limit()){
	    			long offset = buf.position() + pos;
	    			int b = buf.get();
	    			if (b == 29){
	    			   //gs is used to denote table and sequence breaks
	    					gs.add(new Long(b));
	    			}
	    		}
	    		buf.clear();
	    	}

	    	for (int i=0; i < gs.size(); i += 2){
	    		in.position(gs.get(i));
	    		long len = gs.get(i+1) - gs.get(i);
	    		buf.position(0);
	    		buf.limit((int) len);
	    		in.read(buf);
	    		table
	    	}
	    }
		
		
		conn.createStatement().executeUpdate("DROP print_log_seq");
		conn.createStatement().executeUpdate("CREATE SEQUENCE print_log_seq INCREMENT BY 1 START WITH " + Long.toString(printlogSeqLastNumber + 1));

		conn.createStatement().executeUpdate("DROP printer_seq");
		conn.createStatement().executeUpdate("CREATE SEQUENCE printer_seq INCREMENT BY 1 START WITH "   + Long.toString(printerSeqLastNumber + 1));

		conn.createStatement().executeUpdate("DROP sale_log_seq");
		conn.createStatement().executeUpdate("CREATE SEQUENCE sale_log_seq INCREMENT BY 1 START WITH "  + Long.toString(salelogSeqLastNumber + 1));

		
		conn.commit();
		conn.close();
	
	}

	
}
