package de.ovgu.spldev.featurecopp.io;

import java.io.FileWriter;
import java.io.IOException;

import de.ovgu.spldev.featurecopp.config.Configuration;

/**
 * Provides an uniform interface for analysis reports.
 * @author K. Ludwig
 *
 */
public abstract class ReportWriter {
	public ReportWriter(String reportFile) throws IOException {
		fw = new FileWriter(reportFile);
	}
	/**
	 * Reporting actions done before file system traversal.
	 * Useful e.g. for writing a CSV-header, a 'xml-root-open-markup'
	 * @param params variable amount of arguments to process by concrete ReportWriters
	 * @throws Exception any kind of error occurred in preliminary actions of a concrete ReportWriter
	 */
	public abstract void preProcess(Object...params) throws Exception;
	/**
	 * Reporting actions done while performing file system traversal.
	 * Useful e.g. for writing a related analysis incident
	 * @param params variable amount of arguments to process by concrete ReportWriters
	 * @throws Exception any kind of error occurred in preliminary actions of a concrete ReportWriter
	 */
	public abstract void process(Object...params) throws Exception;
	/**
	 * Reporting actions done after file system traversal.
	 * Useful e.g. for closing file handles or writing 'xml.root-close-markup'
	 * @param params variable amount of arguments to process by concrete ReportWriters
	 * @throws Exception any kind of error occurred in preliminary actions of a concrete ReportWriter
	 */
	public abstract void postProcess(Object...params) throws Exception;
	/**
	 * Closes associated report file stream
	 * @throws IOException if stream could not be closed
	 */
	protected void close() throws IOException {
		fw.close();
	}
	/**
	 * Writes give text line to associated report file stream.
	 * @param line text line
	 * @throws IOException if writing failed
	 */
	protected void write(final String line) throws IOException {
		fw.write(line);
	}
	/**
	 * Writes give text line in conjunction with platform dependent line separator to associated report file stream.
	 * @param line text line
	 * @throws IOException if writing failed
	 */
	protected void writeln(final String line) throws IOException {
		write(line + Configuration.LINE_SEPARATOR);
	}
	/** FileWriter related to reportFile */
	private FileWriter fw;
}
