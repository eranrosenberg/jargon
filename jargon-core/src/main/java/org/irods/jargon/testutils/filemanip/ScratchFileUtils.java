package org.irods.jargon.testutils.filemanip;

import static org.irods.jargon.testutils.TestingPropertiesHelper.GENERATED_FILE_DIRECTORY_KEY;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import org.irods.jargon.testutils.TestConfigurationException;

/**
 * @author Mike Conway, DICE (www.irods.org)
 * @since 11/03/2009 common utilities to manipulate and validate scratch files
 *        for unit testing
 */

public class ScratchFileUtils {
	private Properties testingProperties = new Properties();

	public ScratchFileUtils(final Properties testingProperties) throws TestConfigurationException {
		this.testingProperties = testingProperties;
		checkTrailingSlash(testingProperties.getProperty(GENERATED_FILE_DIRECTORY_KEY));
	}

	/**
	 * Create the scratch dir as described in testing.properties if it does not
	 * already exist. * @param pathUnderScratch {@code String} giving the relative
	 * path of the file/directory underneath the scratch area (no leading / delim is
	 * necessary
	 *
	 * @param pathUnderScratch
	 *            {@code String}
	 */
	public void createScratchDirIfNotExists(final String pathUnderScratch) {
		File scratchDir = new File(testingProperties.getProperty(GENERATED_FILE_DIRECTORY_KEY) + pathUnderScratch);
		scratchDir.mkdirs();
	}

	public void createBaseScratchDir() {
		createScratchDirIfNotExists("");
	}

	/**
	 * Utility to check if a given directory exists, if so, delete it, then
	 * reinitialize it as an empty directory. Handy for tests where you want an
	 * empty scratch directory at test initialization or tear-down.
	 *
	 * @param pathUnderScratch
	 *            {@code String} containing a relative path (no leading '/') under
	 *            the configured scratch directory pointing to the directory to
	 *            initialize
	 */
	public void clearAndReinitializeScratchDirectory(final String pathUnderScratch) {
		File scratchDir = new File(testingProperties.getProperty(GENERATED_FILE_DIRECTORY_KEY) + pathUnderScratch);

		// if exists, delete it
		if (scratchDir.exists()) {
			removeFiles(scratchDir);
			scratchDir.delete();

		}

		scratchDir.delete();
		scratchDir.mkdirs();
	}

	private void removeFiles(final File file) {
		if (file == null) {
			throw new IllegalArgumentException("null file");
		}
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File file2 : files) {
					removeFiles(file2);
				}
			}
		}

		file.delete();

	}

	/**
	 * Check if the given file exists in the scratch area
	 *
	 * @param pathUnderScratch
	 *            {@code String} giving the relative path of the file/directory
	 *            underneath the scratch area (no leading / delim is necessary
	 * @return <code>boolean</code> if file exists in the scratch area
	 */
	public boolean checkIfFileExistsInScratch(final String pathUnderScratch) {
		File targetFile = new File(testingProperties.getProperty(GENERATED_FILE_DIRECTORY_KEY) + pathUnderScratch);

		return targetFile.exists();
	}

	public void createDirectoryUnderScratch(final String relativePath) {

		createScratchDirIfNotExists(relativePath);

	}

	/**
	 * Convenience method to tack the relative path and file name to the known
	 * scratch path, while creating any necessary intermediate directories
	 *
	 * @param path
	 *            {@code String} giving relative path (no leading '/') to the
	 *            desired new subdirectory file name. No leading '/' used
	 * @return {@code String} absolute path to the file name, up to the last
	 *         subdirectory, with a trailing '/'
	 */
	public String createAndReturnAbsoluteScratchPath(final String path) {

		// this creates intermediate directories
		createScratchDirIfNotExists(path);

		StringBuilder pathBuilder = new StringBuilder();
		pathBuilder.append(testingProperties.getProperty(GENERATED_FILE_DIRECTORY_KEY));
		pathBuilder.append(path);
		pathBuilder.append('/');
		return pathBuilder.toString();
	}

	/**
	 * @param pathUnderScratch
	 *            {@code String} with relative file path under scratch (no leading
	 *            '/')
	 * @return {@code long} with the file's checksum value
	 * @throws TestConfigurationException
	 *             {@link TestConfigurationException}
	 */
	public byte[] computeFileCheckSum(final String pathUnderScratch) throws TestConfigurationException {

		StringBuilder pathBuilder = new StringBuilder();
		pathBuilder.append(testingProperties.getProperty(GENERATED_FILE_DIRECTORY_KEY));
		pathBuilder.append(pathUnderScratch);

		InputStream fis = null;

		MessageDigest complete;
		try {
			fis = new FileInputStream(pathBuilder.toString());

			byte[] buffer = new byte[1024];
			complete = MessageDigest.getInstance("MD5");
			int numRead;
			do {
				numRead = fis.read(buffer);
				if (numRead > 0) {
					complete.update(buffer, 0, numRead);
				}
			} while (numRead != -1);
		} catch (FileNotFoundException fnfe) {
			StringBuilder message = new StringBuilder();
			message.append("could not find file to checksum at:");
			message.append(pathBuilder);
			throw new TestConfigurationException(message.toString(), fnfe);
		} catch (NoSuchAlgorithmException nsae) {
			throw new TestConfigurationException("could not MD5 algorithim for checksum", nsae);
		} catch (IOException ioe) {
			StringBuilder message = new StringBuilder();
			message.append("io exception generating checksum for file:");
			message.append(pathBuilder);
			throw new TestConfigurationException(message.toString(), ioe);
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (Exception e) {
				// ignore
			}
		}

		return complete.digest();

	}

	/**
	 * @param absolutePathToLocalFile
	 *            {@code String} with absolute local file path under scratch (no
	 *            leading '/')
	 *
	 * @return {@code long} with the file's checksum value
	 * @throws TestConfigurationException
	 *             {@link TestConfigurationException}
	 */
	public long computeFileCheckSumViaAbsolutePath(final String absolutePathToLocalFile)
			throws TestConfigurationException {

		FileInputStream file;
		try {
			file = new FileInputStream(absolutePathToLocalFile);
		} catch (FileNotFoundException e1) {
			throw new TestConfigurationException("error computing checksum, file not found:" + absolutePathToLocalFile,
					e1);

		}
		CheckedInputStream check = new CheckedInputStream(file, new CRC32());
		BufferedInputStream in = new BufferedInputStream(check);
		try {
			while (in.read() != -1) {
			}
		} catch (IOException e) {
			throw new TestConfigurationException("error computing checksum for file:" + absolutePathToLocalFile, e);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				// ignore
			}
		}

		return check.getChecksum().getValue();

	}

	private void checkTrailingSlash(final String path) throws TestConfigurationException {
		String trimmedPath = path.trim();
		String lastChar = trimmedPath.substring(trimmedPath.length() - 1);
		if (lastChar.equals("/") || lastChar.equals("\\")) {
		} else {
			throw new TestConfigurationException(
					"please set the test.data.directory property in testing.properties to have a trailing / char ");
		}
	}

}