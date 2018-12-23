package de.ovgu.spldev.featurecopp.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashSet;

import de.ovgu.spldev.featurecopp.time.Time;

public class Logger {
	public Logger() {
		this.fail_strm = new HashSet<>();
		this.info_strm = new HashSet<>();
	}
	@Override
	public String toString() {
		return String.format("info=%s;fail=%s", info_strm, fail_strm);
	}
	/**
	 * Rotates present log files (if any) and creates a new log file number with 0.
	 * Additionally appends new log file to all stream containers.
	 * @param logFormat e.g., "prefix_infix.%d.suffix"
	 * @param rotate_n rotation count, max file has number rotate_n - 1
	 * @return self for chaining calls
	 * @throws FileNotFoundException
	 */
	public Logger addRotatedLogFileToAllStreams(String logFormat, int rotate_n) throws FileNotFoundException {
		rotate(logFormat, rotate_n);
		PrintStream logFile = new PrintStream(String.format(logFormat, 0));
		return addInfoStream(logFile).addFailStream(logFile);
	}
	public Logger addInfoStream(PrintStream strm) {
		info_strm.add(strm);
		return this;
	}
	public Logger addFailStream(PrintStream strm) {
		fail_strm.add(strm);
		return this;
	}
	
	public void flushAllStrms() {
		flushInfoStrms();
		flushFailStrms();
	}
	public void flushFailStrms() {
		for (PrintStream strm : fail_strm) {
			strm.flush();
		}		
	}
	public void flushInfoStrms() {
		for (PrintStream strm : info_strm) {
			strm.flush();
		}		
	}
	
	public void closeAllStreams() {
		closeInfoStreams();
		closeFailStreams();
	}
	public void closeInfoStreams() {
		closeIn(info_strm);
	}
	public void closeFailStreams() {
		closeIn(fail_strm);
	}
	private void closeIn(HashSet<PrintStream> strms) {
		for (PrintStream strm : strms) {
			if(strm != System.err && strm != System.out) {
				strm.close();
			}
		}
	}

	public void writeInfo(final String msg) {
		if (info_strm != null) {
			String timeStamp = Time.logDate();
			for (PrintStream strm : info_strm) {
				write(strm, timeStamp, String.format("[INFO] %s", msg));
			}
		}
	}

	public void writeFail(final String msg) {
		if (fail_strm != null) {
			String timeStamp = Time.logDate();
			for (PrintStream strm : fail_strm) {
				write(strm, timeStamp, String.format("[FAIL] %s", msg));
			}
		}
	}

	private void write(final PrintStream strm, final String timeStamp, final String msg) {
		strm.println(String.format("%s -- %s", timeStamp, msg));
	}
	
	public void rotate(String logFormat, int rotate_n) {
		int lastLogIndex = rotate_n - 1;
		for(int i = lastLogIndex; i >= 0; i--) {			
			File currLogFile = new File(String.format(logFormat, i));
			// logs from previous runs existing?
			if(currLogFile.isFile()) {
				// oldest log gets deleted
				if(i == lastLogIndex) {
					currLogFile.delete();				
				}
				// curr log grows older by index increment
				else {
					File newName = new File(String.format(logFormat, i + 1));
					// older successor does not longer exist at that time
					currLogFile.renameTo(newName);					
				}
			}
		}
	}
	private HashSet<PrintStream> info_strm;
	private HashSet<PrintStream> fail_strm;
}
