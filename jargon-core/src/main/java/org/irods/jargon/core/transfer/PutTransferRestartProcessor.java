/**
 *
 */
package org.irods.jargon.core.transfer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.exception.NoResourceDefinedException;
import org.irods.jargon.core.packinstr.DataObjInp.OpenFlags;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.io.FileIOOperations.SeekWhenceType;
import org.irods.jargon.core.pub.io.IRODSRandomAccessFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mike Conway - DICE
 */
public class PutTransferRestartProcessor extends
		AbstractTransferRestartProcessor {

	private static Logger log = LoggerFactory
			.getLogger(PutTransferRestartProcessor.class);

	/**
	 * @param irodsAccessObjectFactory
	 * @param irodsAccount
	 * @param restartManager
	 */
	public PutTransferRestartProcessor(
			final IRODSAccessObjectFactory irodsAccessObjectFactory,
			final IRODSAccount irodsAccount,
			final AbstractRestartManager restartManager) {
		super(irodsAccessObjectFactory, irodsAccount, restartManager);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.irods.jargon.core.transfer.AbstractTransferRestartProcessor#
	 * restartIfNecessary()
	 */
	@Override
	public void restartIfNecessary(final String irodsAbsolutePath)
			throws RestartFailedException, FileRestartManagementException {
		log.info("restartIfNecessary()");

		FileRestartInfo fileRestartInfo = retrieveRestartIfConfiguredOrNull(
				irodsAbsolutePath, FileRestartInfo.RestartType.PUT);
		if (fileRestartInfo == null) {
			log.info("no restart");
			return;
		}

		try {
			processRestart(irodsAbsolutePath, fileRestartInfo);
		} catch (JargonException e) {
			log.error("exception accessing restart manager", e);
			throw new FileRestartManagementException("restart manager error", e);
		}
	}

	/**
	 * @throws RestartFailedException
	 *             Wraps restart processing to neatly catch exceptions in
	 *             calling method
	 * 
	 * @param irodsAbsolutePath
	 * @param fileRestartInfo
	 * @throws
	 */
	private void processRestart(final String irodsAbsolutePath,

	FileRestartInfo fileRestartInfo) throws RestartFailedException {
		IRODSRandomAccessFile irodsRandomAccessFile;
		try {
			irodsRandomAccessFile = getIrodsAccessObjectFactory()
					.getIRODSFileFactory(getIrodsAccount())
					.instanceIRODSRandomAccessFile(irodsAbsolutePath,
							OpenFlags.READ_WRITE_CREATE_IF_NOT_EXISTS);
		} catch (NoResourceDefinedException e1) {
			log.error("no resource defined", e1);
			throw new RestartFailedException(
					"cannot get irodsRandomAccessFile", e1);
		} catch (JargonException e1) {
			log.error("general jargon error getting irods random file", e1);
			throw new RestartFailedException(
					"cannot get irodsRandomAccessFile", e1);
		}

		RandomAccessFile localFile = null;
		byte[] buffer;
		/*
		 * See rcPortalOpr.cpp lfRestartPutWithInfo at about line 1522
		 */
		try {
			localFile = localFileAsFileAndCheckExists(fileRestartInfo,
					OpenType.READ);

			// now put each segment
			buffer = new byte[getIrodsAccessObjectFactory()
					.getJargonProperties().getPutBufferSize()];
			long currentOffset = 0L;
			long gap;
			long lengthToUpdate;
			FileRestartDataSegment segment = null;
			for (int i = 0; i < fileRestartInfo.getFileRestartDataSegments()
					.size(); i++) {
				segment = fileRestartInfo.getFileRestartDataSegments().get(i);
				log.info("process segment:{}", segment);
				gap = segment.getOffset() - currentOffset;
				if (gap < 0) {
					log.warn("my segment has a gap < 0..continuing:{}", segment);
				} else if (gap > 0) {

					// ok, have a gap > 0, let's get our restart on

					if (i == 0) {
						// should not be here
						lengthToUpdate = 0;
					} else {
						lengthToUpdate = fileRestartInfo
								.getFileRestartDataSegments().get(i - 1)
								.getLength();
					}

					putSegment(gap, localFile, buffer, fileRestartInfo,
							segment, i, lengthToUpdate, irodsRandomAccessFile);
					currentOffset += gap;
				}

				if (segment.getLength() > 0) {
					currentOffset += segment.getLength();
					localFile.seek(currentOffset);
					irodsRandomAccessFile.seek(currentOffset,
							SeekWhenceType.SEEK_CURRENT);
				}
			}

			// put final segment based on file size

			/*
			 * See rcPortalOpr.cpp at about line 1616
			 */
			log.info("computing gap for last segment");
			log.info("local file length:{}", localFile.length());
			log.info("current offset:{}", currentOffset);

			gap = localFile.length() - currentOffset;
			log.info("last segment gap:{}", gap);
			if (gap > 0) {
				log.info("writing last segment based on file length");
				int i = fileRestartInfo.getFileRestartDataSegments().size() - 1;

				putSegment(gap, localFile, buffer, fileRestartInfo, segment, i,
						gap, irodsRandomAccessFile);
			}

			log.info("restart completed..remove from the cache");
			this.getRestartManager().deleteRestart(
					fileRestartInfo.identifierFromThisInfo());
			log.info("removed restart");
		} catch (FileNotFoundException e) {
			log.error("file not found exception with localFile:{}", localFile,
					e);
			throw new RestartFailedException(e);
		} catch (JargonException e) {
			log.error("general jargon error getting irods random file", e);
			throw new RestartFailedException("cannot get local file", e);
		} catch (IOException e) {
			log.error("end of file exception with localFile:{}", localFile, e);
			throw new RestartFailedException(e);
		} finally {

			try {
				irodsRandomAccessFile.close();
			} catch (IOException e) {
				log.error(
						"error closing irods random access file during restart",
						e);
				throw new RestartFailedException(
						"exception closing irods restart file", e);
			}
			try {
				localFile.close();
			} catch (IOException e) {
				log.warn("error closing local file, logged and ignored", e);
			}

		}

	}

	/**
	 * Put the segment to iRODS, and update the length of the given segment
	 * 
	 * @param gap
	 * @param localFile
	 * @param buffer
	 * @param fileRestartInfo
	 * @param segment
	 * @param indexOfCurrentSegment
	 * @param lengthToUpdate
	 * @param irodsRandomAccessFile
	 * @throws RestartFailedException
	 * @throws FileRestartManagementException
	 */
	private void putSegment(final long gap, final RandomAccessFile localFile,
			final byte[] buffer, final FileRestartInfo fileRestartInfo,
			final FileRestartDataSegment segment,
			final int indexOfCurrentSegment, final long lengthToUpdate,
			final IRODSRandomAccessFile irodsRandomAccessFile)
			throws RestartFailedException, FileRestartManagementException {

		long myGap = gap;
		long writtenSinceUpdated = 0L;
		int toRead = 0;
		while (myGap > 0) {
			if (myGap > buffer.length) {
				toRead = buffer.length;
			} else {
				toRead = (int) myGap;
			}

			log.info("reading buffer from input file...");
			int amountRead;
			try {
				amountRead = localFile.read(buffer, 0, toRead);
				irodsRandomAccessFile.write(buffer, 0, amountRead);
				myGap -= amountRead;
				writtenSinceUpdated += amountRead;

				if (writtenSinceUpdated >= AbstractTransferRestartProcessor.RESTART_FILE_UPDATE_SIZE) {
					log.info("need to update restart");
					this.getRestartManager().updateLengthForSegment(
							fileRestartInfo.identifierFromThisInfo(),
							segment.getThreadNumber(), writtenSinceUpdated);
					writtenSinceUpdated = 0;
				}
			} catch (IOException e) {
				log.error("IOException reading local file", e);
				throw new RestartFailedException(
						"IO Exception reading local file", e);
			}
		}
	}
}
