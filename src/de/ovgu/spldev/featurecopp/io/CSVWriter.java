package de.ovgu.spldev.featurecopp.io;

import java.io.IOException;

import de.ovgu.spldev.featurecopp.config.Configuration;

/**
 * A line-based CSV-file writer.
 * Remarks:
 * <ul>
 *  <li>Column delimiter is externalized to program configuration (Configuration.CSV_DELIMITER)</li>
 *  <li>Output format and heading is submitted by user</li>
 *  <li>Variable amount of passed column elements (to write) is validated against user submitted format string.</li>
 * </ul>
 * @author K. Ludwig
 */
public class CSVWriter extends ReportWriter {
	/**
	 * Exception related to errors within CSVWriter
	 * @author K. Ludwig
	 */
	public static class CSVWriterException extends Exception {
		/**
		 * gen svuid
		 */
		private static final long serialVersionUID = -5030331554488034859L;

		/**
		 * Creates a new CSVWriterException with given message.
		 * @param msg exception message
		 */
		public CSVWriterException(String msg) {
			super(msg);
		}
	}
	/**
	 * Writes a CSV-row where columns are given arguments 'params'.
	 */
	@Override
	public void preProcess(Object... params) throws IOException, CSVWriterException {
		process(params); // no differentiation
	}
	/**
	 * Writes a CSV-row where columns are given arguments 'params'.
	 */
	@Override
	public void process(Object... params) throws IOException, CSVWriterException {
		writeCSVLine(params); 
	}
	/**
	 * Closes CSVWriter file stream.
	 */
	@Override
	public void postProcess(Object... params) throws IOException, CSVWriterException {
		close();
	}
	/**
	 * Creates a new CSVWriter for given output file based on given format.
	 * @param reportFile output file
	 * @param format row format
	 * @throws IOException if opening reportFile failed
	 */
	public CSVWriter(String reportFile, final String format) throws IOException {
		super(reportFile);
		this.format = format;
		this.fields = format.split(""
				+ Configuration.CSV_DELIMITER).length;
	}
	/**
	 * Writes variable amount of arguments as concatenated csv-row based on set format.
	 * @param parts column elements
	 * @throws IOException if writing to reportFile failed
	 * @throws CSVWriterException if number of va_args differs from submitted format
	 */
	public void writeCSVLine(final Object... parts) throws IOException,
			CSVWriterException {
		if (parts == null || parts.length != fields) {
			throw new CSVWriterException("Invalid amount of submitted fields!");
		}
		writeln(String.format(format, parts));
	}
	/** user submitted row format*/
	private final String format;
	/** number of columns according to format */
	private final int fields;
}
