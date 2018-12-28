package de.ovgu.spldev.featurecopp.io;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;

import de.ovgu.spldev.featurecopp.config.Configuration;
import de.ovgu.spldev.featurecopp.filesystem.Filesystem;
import de.ovgu.spldev.featurecopp.filesystem.Finder.Processable;
import de.ovgu.spldev.featurecopp.log.Logger;
import de.ovgu.spldev.featurecopp.markup.MarkupLexer;
import de.ovgu.spldev.featurecopp.markup.MarkupParser;
import de.ovgu.spldev.featurecopp.markup.MarkupParserDriver;

/* NOTICE
- reading has a bigger effort since files are read repeatedly looking only for defined occ_ids...
- ...but since a depth-first reading on all reference occurrences (<$inline) will open too many files at once (ulimit -n)...
- ...hence instead of an exponential 'read_as_much_at_once' we have to lookup rather linear (following only requested occ_ids)...
- ... at cost of pontential longer run time (currently 5 mins for linux-4.10.4_processed, which seems acceptable) 
*/

public class Merger implements Processable {
	public static class MergerException extends IOException {
		public MergerException(final String message) {
			super(message);
		}
	}
	@Override
	public void process(Path fso) {		
		try {
			logger.writeInfo("Processing " + fso);
			StringBuilder fileBuffer = new StringBuilder();
			includeOccurrences(fso, fileBuffer);
			// write outfile
			writeToFile(prepareOutputFile(fso), fileBuffer);
			//System.out.println(fileBuffer);
		} catch (Exception e) {
			// smth regarding input file went wrong
			logger.writeFail(e.getMessage());
			try {
				Configuration.purgeOutputDir(logger, outputDir.toString());
			} catch(Exception e_del) {
				logger.writeFail(e_del.getMessage());
			}
			System.exit(1);
			//e.printStackTrace();
		}
		
	}
	public Merger(Logger logger, Path inputDir, Path outputDir) {
		this.logger = logger;
		this.inputDir = inputDir;
		this.outputDir = outputDir;
		this.markupParserDriver = new MarkupParserDriver();
	}
	private Path prepareOutputFile(final Path inputFile) throws IOException {
		// substitute input directory portions with outputdir portions
		Path outputfile = Filesystem.substitute(inputFile, inputDir, outputDir);
		// make path portion outfile if not exists
		Filesystem.makePath(Filesystem.dirname(outputfile));
		return outputfile;
	}
	private void writeToFile(final Path outputFile, StringBuilder fileBuffer) throws IOException {
		Writer w = new OutputStreamWriter(new FileOutputStream(outputFile.toFile()), "UTF-8");
		w.write(fileBuffer.toString());
		w.close();
//		FileWriter fw = new FileWriter(outputFile.toFile());
//		fw.write(fileBuffer.toString());
//		fw.close();
		logger.writeInfo("Merged and written: " + outputFile);
	}
	// only for base files
	private void includeOccurrences(final Path fso, StringBuilder filebuffer) throws Exception {		
		NLPreservBufferedReader reader = new NLPreservBufferedReader(new FileReader(fso.toFile()), Configuration.IO_MERGE_READER_BUFFER);
		String currLine = "";
		int line = 0;
		while((currLine = reader.readLine()) != null) {	
			line++;
			if(MarkupLexer.Markup.isReference(currLine)) {
				MarkupLexer.Markup m = null;
				try {
					m = markupParserDriver.run(false, currLine);
				} catch(MarkupParser.ParserException pe) {
					reader.close();
					throw new MergerException(pe.getMessage() + " in line: " + line);
				}
				readOccurrence(m.getOccId(), m.getFile(), filebuffer);
			}
			else {
				// appends newline unconditionally -- differs from last lines without linebreak
				//filebuffer.append(currLine + Configuration.LINE_SEPARATOR);
				filebuffer.append(currLine);
			}
		}		
		reader.close();
	}
	// only for module files
	private void readOccurrence(long occ_id, final Path fso, StringBuilder filebuffer) throws Exception {
		//logger.writeInfo("Searching " + fso + " for occ: " + occ_id);
		NLPreservBufferedReader reader = new NLPreservBufferedReader(new FileReader(fso.toFile()), Configuration.IO_MERGE_READER_BUFFER);
		String currLine = "";
		int line = 0;
		boolean found = false;
		// read module file
		while((currLine = reader.readLine()) != null) {
			line++;
			// begin block?
			if(MarkupLexer.Markup.isBlock(currLine)) {
				MarkupLexer.Markup m = null;
				try {
					m = markupParserDriver.run(false, currLine);
				} catch (MarkupParser.ParserException pe) {
					reader.close();
					throw new MergerException(pe.getMessage() + " in line: " + line + " (" + fso.toString() + ")");
				}
				//logger.writeInfo(m.toString());
				// is current block requested by previous caller?
				if(m.getOccId() == occ_id) {
					found = true;
					//logger.writeInfo("Found occ " + occ_id + " in line " + line);
					// read block
					while((currLine = reader.readLine()) != null && ! MarkupLexer.Markup.isEnd(currLine)) {	
						line++;
						// further reference in block?
						if(MarkupLexer.Markup.isReference(currLine)) {
							MarkupLexer.Markup m_ref = null;
							try {
								m_ref = markupParserDriver.run(false, currLine);
							}
							catch(MarkupParser.ParserException pe) {
								reader.close();
								throw new MergerException(pe.getMessage() + " in line: " + line + " (" + fso.toString() + ")");
							}
							readOccurrence(m_ref.getOccId(), m_ref.getFile(), filebuffer);
						}
						// pure code
						else {
							//filebuffer.append(currLine + Configuration.LINE_SEPARATOR);
							filebuffer.append(currLine);
						}
					}
					reader.close();
					return;
				}
			}
		}
		reader.close();
		if(! found) {
			throw new MergerException("Role #" + occ_id + " not found in " + fso.toString());
		}
	}
	private MarkupParserDriver markupParserDriver;
	private Logger logger;
	private Path inputDir;
	private Path outputDir;
}
