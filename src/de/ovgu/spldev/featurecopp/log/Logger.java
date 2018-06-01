package de.ovgu.spldev.featurecopp.log;

import java.io.PrintStream;

import de.ovgu.spldev.featurecopp.time.Time;

public class Logger {
	public void setInfoStrms(final PrintStream... info_strm) {
		if (info_strm != null) {
			this.info_strm = new PrintStream[info_strm.length];
			for (int i = 0; i < info_strm.length; i++) {
				this.info_strm[i] = info_strm[i];
			}
		}
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
	public void setFailStrms(final PrintStream... fail_strm) {
		if (fail_strm != null) {
			this.fail_strm = new PrintStream[fail_strm.length];
			for (int i = 0; i < fail_strm.length; i++) {
				this.fail_strm[i] = fail_strm[i];
			}
		}
	}

	public void writeInfo(final String msg) {
		if (info_strm != null) {
			for (PrintStream strm : info_strm) {
				write(strm, "[INFO] " + msg);
			}
		}
	}

	public void writeFail(final String msg) {
		if (fail_strm != null) {
			for (PrintStream strm : fail_strm) {
				write(strm, "[FAIL] " + msg);
			}
		}
	}

	private void write(final PrintStream strm, final String msg) {
		strm.println(Time.logDate() + " -- " + msg);
	}

	private PrintStream[] info_strm;
	private PrintStream[] fail_strm;
}
