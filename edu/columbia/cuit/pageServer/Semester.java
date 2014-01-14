package edu.columbia.cuit.pageServer;

import java.util.LinkedHashSet;
import java.util.Set;


public class Semester implements AcademicYear {

	private final long[] starting;
	private final String[] tag;
	private int lastElement;
	private final static String[] tagSet = { "Fall", "Spring", "Summer" };
	
	public Semester(){
		
		lastElement = -1;
		starting = new long[32];
		tag = new String[32];

		// Hard-coded semester start dates in msec
		add(1315195200000l, "Fall");
		add(1326690000000l, "Spring");
		add(1337227200000l, "Summer");
		add(1346644800000l, "Fall");
		add(1358744400000l, "Spring");
		add(1369281600000l, "Summer");
		add(1378094400000l, "Fall");
		add(1390194000000l, "Spring");
		add(1400731200000l, "Summer");
		add(1409544000000l, "Fall");
		add(1421643600000l, "Spring");
		add(1432180800000l, "Summer");
		add(1441598400000l, "Fall");
		add(1453093200000l, "Spring");
		add(1463630400000l, "Summer");
		add(1473048000000l, "Fall");
		add(1484542800000l, "Spring");
		add(1495080000000l, "Summer");
		add(1504497600000l, "Fall");
		add(1515992400000l, "Spring");
		add(1526529600000l, "Summer");
		add(1535947200000l, "Fall");
		add(1548046800000l, "Spring");
		add(1558584000000l, "Summer");
		add(1567396800000l, "Fall");
		add(1579496400000l, "Spring");
		add(1590033600000l, "Summer");
		add(1599451200000l, "Fall");
		add(1610946000000l, "Spring");
		add(1621483200000l, "Summer");
		add(1630900800000l, "Fall");
		add(1642395600000l, "Spring");
	}

	
	private void add(long msec, String tag){
		// Add an entry to the array if there
		// is space remaining
		
		if (++lastElement < starting.length){
			this.starting[lastElement] = msec;
			this.tag[lastElement] = tag;
		}else{
			lastElement--;
		}
	}

	
	public long current() {
		return current(System.currentTimeMillis());
	}

	
	public long current(long time) {
		if (starting[0] > time) return 0l;
		
		for (int k=1; k <= lastElement; k++){
			if ( starting[k] > time)
				 return starting[k-1];
		}
		return starting[lastElement];	
	}

	
	public long prev(long time) {
		if ( starting[0] < time || starting[1] < time) return 0l;
		
		for (int k=2; k <= lastElement; k++){
			if (starting[k] > time)
				return starting[k-2];
		}
		return starting[lastElement - 1];
	}


	public long prev(long time, String tag) {
		if ( starting[0] < time || starting[1] < time) return 0l;
	
		for (int k=2; k <= lastElement; k++){
			if (starting[k] > time && tag.equals(this.tag[k-2]) )
				return starting[k-2];
		}
		return starting[lastElement - 1];
	}

	
	public String currrentTag() {
		return currentTag(System.currentTimeMillis());
	}

	
	public String currentTag(long time) {
		if (starting[0] > time) return null;
		
	    for (int k=1; k <= lastElement; k++){
			if ( starting[k] > time)
				 return tag[k-1];
		}
		return tag[lastElement];
	}
	

	public String prevTag(long time) {
		if ( starting[0] < time || starting[1] < time) return null;
		
		for (int k=2; k <= lastElement; k++){
			if (starting[k] > time)
				return tag[k-2];
		}
		return tag[lastElement - 1];
	}

	
	public String nextTag(long time) {
		for (int k=0; k <= lastElement; k++){
			if (starting[k] > time) 
				return tag[k];
		}
		return tag[lastElement];	
	}

	
	public long next(long time) {
		for (int k=0; k <= lastElement; k++){
			if (starting[k] > time) 
				return starting[k];
		}
		return starting[lastElement];	
	}


	public long next(long time, String tag) {
		for (int k=0; k <= lastElement; k++){
			if ( starting[k] > time && tag.equals(this.tag[k]) )
				return starting[k];
		}

		return starting[lastElement];	
	}

	public Set<String> getTags() {
	   Set<String> retSet = new LinkedHashSet<String>(tagSet.length);
	   for (String tagName: tagSet){
		   retSet.add(tagName);
	   }
	   return retSet;
	}

}
