package edu.columbia.cuit.pageServer;

import java.util.Set;

public interface AcademicYear {

	// Defines the interface used to determine
	// the start of academic periods (semesters, quarters, etc.)
	// within an academic year
	
	// return the starting time in msec of the period covering
	// the current time
	long current();
	
	// return the starting time in msec of the period covering
	// the supplied date
	long current(long ms);
	
	//return the time in msec of the start of the previous period
	long prev(long ms);

	// return the time in msec of the start of the previous period
	// which also matches the supplied tag
	long prev(long ms, String tag);
	
	// Return the String describing the period specified
	String currrentTag();
	String currentTag(long ms);
	String prevTag(long ms);
	String nextTag(long ms);
	
	// return the time in msec of the start of the next period 
	long next(long ms);

	// return the time in msec of the start of the next period 
	// which also matches the supplied tag
	long next(long ms, String tag);
	
	// Returns the full list of Identifiers used as tags
	// such as "Spring", "Summer I", "Fall", "Break", etc.
	Set<String> getTags();
}
