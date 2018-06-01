/**
 * File: Filesystem.java
 * Created: 14.07.2014
 * Author: K. Ludwig
 * Synopsis: Abstracting java.nio in a simpler interface including handy filesystem operations
 */
package de.ovgu.spldev.featurecopp.filesystem;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import de.ovgu.spldev.featurecopp.log.Logger;

/**
 * Filesystem provides an abstraction layer to java.nio. Besides a set of static
 * functions to make several tests on folders and files it additionally provides
 * a set of frequently needed operations like traversing a directory tree and
 * copying or deleting its content.
 * 
 * @author K. Ludwig
 * @since 1.7
 */
public class Filesystem {
	/**
	 * Provides a basic implementation of a derived SimpleFileVisitor. This is
	 * used within Files.walkFileTree. An instance of Loggable is stored here
	 * and a default implementation of visitFileFailed is setup here (which is
	 * mostly the same for all derivates). If null is provided as Loggable it
	 * does not output anything at all.
	 * 
	 * @author K. Ludwig
	 * @see Files.walkFileTree
	 * @since 1.7
	 */
	public static class BaseLoggedVisitor extends SimpleFileVisitor<Path> {
		/**
		 * Creates a new BaseLoggedVisitor using given Loggable 'logger'.
		 * 
		 * @param logger
		 *            : Implementation of Loggable to write messages to
		 */
		public BaseLoggedVisitor(Logger logger) {
			this.logger = logger;
		}

		/**
		 * Writes a messages to given Loggable if set and indicates further
		 * processing. If Loggable is null no output is written.
		 * 
		 * @param file
		 *            : file or folder to inspect
		 * @throws IOException
		 *             in case an error occured
		 */
		public FileVisitResult visitFileFailed(Path file, IOException ioe)
				throws IOException {
			if (logger != null) {
				logger.writeFail("Unable to inspect " + file + " ("
						+ ioe.getClass() + ")");
			}
			return FileVisitResult.CONTINUE;
		}

		/** messaging output */
		protected Logger logger;
	}

	/**
	 * Copies a on filesystem object into another.
	 * 
	 * @author K. Ludwig
	 * @since 1.7
	 */
	public static class Copy extends BaseLoggedVisitor {
		/**
		 * Creates a new Copy with given object to copy 'src' to given object
		 * 'dst' as destination.
		 * 
		 * @param src
		 *            : object to copy
		 * @param dst
		 *            : destination , where src has to be copied
		 * @param logger
		 *            : Implementation of Loggable to write messages to
		 */
		public Copy(Path src, Path dst, Logger logger) {
			super(logger);
			this.src = src;
			this.dst = dst;
		}

		/**
		 * Activity to be done before a filesystem object gets handled. The
		 * given directory is stripped and its pathname is incorporated into the
		 * destination path. If this directory did not exist yet it is created
		 * within destination. If Loggable is null, this method does not output
		 * anything at all.
		 * 
		 * @param dir
		 *            : filesystem object assumed as a directory
		 * @param attrs
		 *            : addtional attributes forced by interface (not explicitly
		 *            used here)
		 * @see Path
		 * @see Files
		 */
		public FileVisitResult preVisitDirectory(Path dir,
				BasicFileAttributes attrs) throws IOException {
			Path targetPath = dst.resolve(src.relativize(dir));
			if (!Files.exists(targetPath)) {
				Files.createDirectory(targetPath);
				if (logger != null) {
					logger.writeInfo("Created: " + targetPath);
				}
			}
			return FileVisitResult.CONTINUE;
		}

		/**
		 * Activity to be done when a filesystem object is currently processed.
		 * The pathname of the given filesystem object gets integrated into the
		 * destination folder structure and is copied to its acquired
		 * destination. If Loggable is null, this method does not output
		 * anything at all.
		 * 
		 * @param file
		 *            : filesystem object to copy
		 * @param attrs
		 *            : its attributes forced by interface (not explicitly used
		 *            here)
		 * @see Files.copy
		 */
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException {
			Path target = dst.resolve(src.relativize(file));
			Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.COPY_ATTRIBUTES);
			if (logger != null) {
				logger.writeInfo("Copied " + file + " to: " + target);
			}
			return FileVisitResult.CONTINUE;
		}

		/** source path */
		private Path src;
		/** destination path */
		private Path dst;
	}

	/**
	 * Deletes a given directory recursively.
	 * 
	 * @author K. Ludwig
	 * @since 1.7
	 */
	public static class DeleteDirRecursive extends BaseLoggedVisitor {
		/**
		 * Creates a new Visitor being capable of deleting a directory
		 * recursively.
		 * 
		 * @param logger
		 *            : Implementation of Loggable to write messages to
		 */
		public DeleteDirRecursive(Logger logger) {
			super(logger);
		}

		/**
		 * Deletes a given file from the filesystem. If Loggable is null, this
		 * method does not output anything at all.
		 * 
		 * @param file
		 *            : found file to delete
		 * @param attrs
		 *            : additional attributes forced by interface (not
		 *            explicitly used here)
		 * @see Files.delete
		 */
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException {
			try {
				Files.delete(file);
				if (logger != null) {
					logger.writeInfo("Deleted: " + file);
				}
			} catch(Exception e) {
				if (logger != null) {
					logger.writeFail("Unable to delete: " + e.getMessage());
				}
			}
			return FileVisitResult.CONTINUE;
		}

		/**
		 * Deletes a given empty directory object from the filesystem. If
		 * Loggable is null, this method does not output anything at all.
		 * 
		 * @param file
		 *            : found file to delete
		 * @param ioe
		 *            : a meanwhile occurred IOException thrown during traversal
		 * @see Files.delete
		 */
		public FileVisitResult postVisitDirectory(Path dir, IOException ioe)
				throws IOException {
			if (ioe != null) {
				throw ioe;
			}
			// dir is now empty -- can delete
			try {
				Files.delete(dir);
				if (logger != null) {
					logger.writeInfo("Deleted: " + dir);
				}
			}
			catch(Exception e) {
				if (logger != null) {
					logger.writeFail("Unable to delete: " + e.getMessage());
				}	
			}

			return FileVisitResult.CONTINUE;
		}
	}

	/**
	 * Facade to copy a directory given by 'src' to given 'dst' whereby messages
	 * should be written to 'logger'.
	 * 
	 * @param src
	 *            : directory to copy
	 * @param dst
	 *            : where to copy directory
	 * @param logger
	 *            : where to write messages
	 * @throws Exception
	 *             in case Files.walkFileTree encounters an error
	 */
	public static void copyDirRecursive(final String src, final String dst,
			final Logger logger) throws Exception {
		Path srcPath = genPath(src);
		Path dstPath = genPath(dst);
		Files.walkFileTree(srcPath, new Copy(srcPath, dstPath, logger));
	}

	/**
	 * Facade to delete a directory given by 'dir' recursively whereby messages
	 * should be written to 'logger'.
	 * 
	 * @param src
	 *            : directory to delete
	 * @param logger
	 *            : where to write messages
	 * @throws Exception
	 *             in case Files.walkFileTree encounters an error
	 */
	public static void deleteDirRecursive(final String dir,
			final Logger logger) throws Exception {
		Files.walkFileTree(genPath(dir), new DeleteDirRecursive(logger));
	}
	/**
	 * Creates a Path from given components.
	 * 
	 * @param name
	 *            : minimum required path portion
	 * @param portions
	 *            : optional subcomponents
	 * @return : newly generated Path
	 * @see FileSystems.getDefault().getPath
	 * @since 1.7
	 */
	public static Path genPath(final String name, String... portions) {
		return FileSystems.getDefault().getPath(name, portions);
	}

	/**
	 * Creates a path structure from given arguments like mkdir --parent.
	 * 
	 * @param name
	 *            : minimum portion of directories to create
	 * @param portions
	 *            : additional portions for in-depth creation
	 * @throws IOException
	 *             thrown in case of failure
	 */
	public static void makePath(final String name, String... portions)
			throws IOException {
		Path newPath = genPath(name, portions);
		Files.createDirectories(newPath);
	}
	
	public static void makePath(final Path name)
			throws IOException {
		Files.createDirectories(name);
	}

	/**
	 * Retrieves basename of given File f.
	 * 
	 * @param f
	 *            : File to obtain basename from
	 * @return : new File instance representing f's basename
	 */
	public static File basename(final File f) {
		return new File(f.getName());
	}

	/**
	 * Retrieves basename of given Path p.
	 * 
	 * @param p
	 *            : Path to obtain basename from
	 * @return : new Path instance representing p's basename
	 * @since 1.7
	 */
	public static Path basename(final Path p) {
		return p.getFileName();
	}

	/**
	 * Retrieves basename of given String s.
	 * 
	 * @param s
	 *            : String to obtain basename from
	 * @return : new Path instance representing s's basename
	 * @since 1.7
	 */

	public static String basename(final String s) {
		Path p = genPath(s);
		return basename(p).toString();
	}

	/**
	 * Retrieves dirname of given File f.
	 * 
	 * @param f
	 *            : File to obtain dirname from
	 * @return : new File instance representing f's dirname
	 */
	public static File dirname(final File f) {
		return new File(f.getParent());
	}

	/**
	 * Retrieves dirname of given Path p.
	 * 
	 * @param p
	 *            : Path to obtain dirname from
	 * @return : new Path instance representing p's dirname
	 * @since 1.7
	 */
	public static Path dirname(final Path p) {
		return p.getParent();
	}

	/**
	 * Retrieves dirname of given String s.
	 * 
	 * @param s
	 *            : String to obtain dirname from
	 * @return : new Path instance representing s's dirname
	 * @since 1.7
	 */

	public static String dirname(final String s) {
		Path p = genPath(s);
		return dirname(p).toString();
	}
	public static Path substitute(final Path path, final Path pattern, final Path replacement) {
		return substitute(path, pattern.toString(), replacement.toString());
		//return genPath(path.toString().replace(pattern.toString(), replacement.toString()));
	}
	public static Path substitute(final Path path, final String pattern, final String replacement) {
		return genPath(path.toString().replace(pattern, replacement));
	}
	public static String getSuffix(final String filename) {
		int lastOccurence = filename.lastIndexOf(".");
		return lastOccurence < 0 ? "" : filename.substring(lastOccurence); 
	}
	public static String getSuffix(final Path filename) {
		return getSuffix(filename.toString());
	}
}
