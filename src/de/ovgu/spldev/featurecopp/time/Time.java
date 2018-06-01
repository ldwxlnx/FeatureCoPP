package de.ovgu.spldev.featurecopp.time;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Time {
	public static String logDate() {
		DateFormat df = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss:SSS]");
		return df.format(new Date());
	}
	public static String timeStamp() {
		DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		return df.format(new Date());
	}
	public static long getCurrentNanoSecs() {
		return System.nanoTime();
	}
	public static long elapsedNanoSecs(long start) {
		long end = getCurrentNanoSecs();
		return end - start;
	}
	public static double nano2Sec(long nanoSecs) {
		return nanoSecs * 1e-9;
	}
}
