/**
 * File: Find.java
 * Created: 22.03.2017
 * Creator: K. Ludwig
 * Synopsis:
 */
package de.ovgu.spldev.featurecopp.filesystem;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

/**
 * GNU findutils inspired filesystem object locator, heavily inspired by
 * https://docs.oracle.com/javase/tutorial/essential/io/walk.html
 * https://docs.oracle.com/javase/tutorial/essential/io/examples/Find.java
 * Sample code that finds files that match the specified glob pattern. For more
 * information on what constitutes a glob pattern, see
 * http://docs.oracle.com/javase/javatutorials/tutorial/essential/io/fileOps.html#glob
 * 
 * @author K. Ludwig
 */
public class Finder extends SimpleFileVisitor<Path> {
	public static enum TYPE {
		ALL, FILE, DIR // TODO seems not all POSIX types (e.g. named pipes) are detectable java.nio
	}; 

	/**
	 * Callback interface for user-defined actions per found item
	 * @author K. Ludwig
	 */
	public static interface Processable {
		/**
		 * Action to proceed on given filesystem object 'fso'.
		 * @param fso filesystem object to perform activity on 
		 */
		void process(Path fso);
	}
	/**
	 * Default implementation of an outstream writer.
	 * If used as callback each found filesystem object's string representation
	 * is written to applied PrintStream. 
	 * @author K. Ludwig
	 */
	public static class PrintStreamWriter implements Processable {
		/**
		 * Creates a new PrintStreamWriter
		 * @param strm PrintStream to write fso.toString()
		 */
		public PrintStreamWriter(PrintStream strm) {
			this.strm = strm;
		}		
		@Override
		public void process(Path fso) {
			strm.println(fso);
		}
		/** output stream */
		private PrintStream strm;
	}
	/**
	 * Parameter object for fat interface usage of Finder.find
	 * @author K. Ludwig
	 */
	public static class FindParameter {
		public FindParameter(String rootDir, TYPE type, int maxDepth, String globPattern, boolean followLinks, boolean continueOnError,
				Processable callback) {
			init(rootDir, type, maxDepth, globPattern, followLinks, continueOnError, callback);
		}
		public String toString() {
			return  this.getClass().getSimpleName() + "={"
					+ "rootdir=\"" + rootDir + "\";"
					+ "type=\"" + type.name()  + "\";"
					+ "glob=\"" + globPattern  + "\";"
					+ "maxdepth=\"" + maxDepth  + "\";"
					+ "followsyml=\"" + followLinks  + "\";"
					+ "continueOnErr=\"" + continueOnError  + "\";"
					+ "callback\"" + callback + "\""
					+ "}";
		}
		private void init(String rootDir, TYPE type, int maxDepth, String globPattern, boolean followLinks, boolean continueOnError,
				Processable callback) {
			this.rootDir = rootDir == null ? "." : rootDir;
			this.type = type == null ? TYPE.ALL : type;
			// all values below 1 are treated as "don't care"
			this.maxDepth = maxDepth < 1 ? Integer.MAX_VALUE : maxDepth;
			this.globPattern = globPattern == null ? "*" : globPattern;
			this.followLinks = followLinks;
			this.continueOnError = continueOnError;
			this.callback = callback == null ? new PrintStreamWriter(System.out) : callback;
		}
		private String rootDir;
		private TYPE type;
		private int maxDepth;
		private String globPattern;
		private boolean followLinks;
		private boolean continueOnError;
		private Processable callback;
	}

	/**
	 * Fat interface for filesystem object location (related to findutils).
	 * @param fparam parameter object adjusting behavior of find.
	 * @return number of found items
	 * @throws IOException 
	 */
	public static int find(final FindParameter fparam) throws IOException {
		//lastException = null;
		Finder finder = new Finder(fparam);
		EnumSet<FileVisitOption> options = fparam.followLinks ? EnumSet.of(FOLLOW_LINKS)
				: EnumSet.noneOf(FileVisitOption.class);
				
		Files.walkFileTree(Paths.get(fparam.rootDir), options, fparam.maxDepth, finder);
		return finder.found;
	}

	// Invoke the pattern matching
	// method on each file.
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		switch (fparam.type) {
		case ALL:
		case FILE:
			find(file);
			break;
		default:
			break;
		}
		return CONTINUE;
	}

	// Invoke the pattern matching
	// method on each directory.
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		switch (fparam.type) {
		case ALL:
		case DIR:
			find(dir);
			break;
		default:
			break;
		}		
		return CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException ioe) throws IOException {
		// TODO Check for FileSystemLoopException selectively if necessary
			
		if(! fparam.continueOnError) {
			// rethrow
			throw ioe;
		}
		else {
			System.err.println(ioe);
		}
		return CONTINUE;
	}
	
	private Finder(final FindParameter fparam) {
		this.fparam = fparam;
		matcher = FileSystems.getDefault().getPathMatcher("glob:" + fparam.globPattern);
	}
	
	// Compares the glob pattern against
	// the file or directory name.
	private void find(final Path file) {
		Path name = file.getFileName();
		if (name != null && matcher.matches(name)) {
			found++;
			fparam.callback.process(file);
		}
	}
	private FindParameter fparam;
	private final PathMatcher matcher;
	private int found = 0;
}