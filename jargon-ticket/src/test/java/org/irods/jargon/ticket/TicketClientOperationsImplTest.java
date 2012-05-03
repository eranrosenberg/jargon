package org.irods.jargon.ticket;

import java.io.File;
import java.util.Properties;

import junit.framework.Assert;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.CatNoAccessException;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.packinstr.TransferOptions.ForceOption;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.DataTransferOperations;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.transfer.TransferControlBlock;
import org.irods.jargon.testutils.IRODSTestSetupUtilities;
import org.irods.jargon.testutils.TestingPropertiesHelper;
import org.irods.jargon.testutils.filemanip.FileGenerator;
import org.irods.jargon.testutils.filemanip.ScratchFileUtils;
import org.irods.jargon.ticket.io.FileStreamAndInfo;
import org.irods.jargon.ticket.packinstr.TicketCreateModeEnum;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TicketClientOperationsImplTest {

	private static Properties testingProperties = new Properties();
	private static TestingPropertiesHelper testingPropertiesHelper = new TestingPropertiesHelper();
	private static IRODSFileSystem irodsFileSystem;
	private static final String IRODS_TEST_SUBDIR_PATH = "TicketClientOperationsImplTest";
	private static ScratchFileUtils scratchFileUtils = null;
	private static IRODSTestSetupUtilities irodsTestSetupUtilities = null;
	private static boolean testTicket = false;
	private static org.irods.jargon.testutils.AssertionHelper assertionHelper = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestingPropertiesHelper testingPropertiesLoader = new TestingPropertiesHelper();
		testingProperties = testingPropertiesLoader.getTestProperties();
		testTicket = testingPropertiesLoader
				.isTestRemoteExecStream(testingProperties);
		scratchFileUtils = new ScratchFileUtils(testingProperties);
		scratchFileUtils
				.clearAndReinitializeScratchDirectory(IRODS_TEST_SUBDIR_PATH);
		irodsTestSetupUtilities = new IRODSTestSetupUtilities();
		irodsTestSetupUtilities.initializeIrodsScratchDirectory();
		irodsTestSetupUtilities
				.initializeDirectoryForTest(IRODS_TEST_SUBDIR_PATH);
		irodsFileSystem = IRODSFileSystem.instance();
		assertionHelper = new org.irods.jargon.testutils.AssertionHelper();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (testTicket) {
			irodsFileSystem.closeAndEatExceptions();
		}
	}

	@Test
	public final void testTicketClientOperationsImpl() throws Exception {
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		TicketClientOperations ticketClientOperations = new TicketClientOperationsImpl(
				accessObjectFactory, irodsAccount);
		Assert.assertNotNull("null ticketClientOperations",
				ticketClientOperations); // really just looking for no errors

	}

	@Test(expected = IllegalArgumentException.class)
	public final void testTicketClientOperationsImplNullAccessObjectFactory()
			throws Exception {
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = null;
		new TicketClientOperationsImpl(accessObjectFactory, irodsAccount);

	}

	@Test(expected = IllegalArgumentException.class)
	public final void testTicketClientOperationsImplNullAccount()
			throws Exception {
		IRODSAccount irodsAccount = null;
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		new TicketClientOperationsImpl(accessObjectFactory, irodsAccount);

	}

	@Test
	public final void testPutFileToIRODSUsingTicket() throws Exception {

		if (!testTicket) {
			return;
		}

		String testCollection = "testPutFileToIRODSUsingTicket";
		String testFileName = "testPutFileToIRODSUsingTicket.txt";
		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH);
		String localFileName = FileGenerator
				.generateFileOfFixedLengthGivenName(absPath, testFileName, 1);

		File localFile = new File(localFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testCollection);
		IRODSFile targetFile = accessObjectFactory.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(targetIrodsCollection);
		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);
		TicketAdminService ticketSvc = new TicketAdminServiceImpl(
				accessObjectFactory, irodsAccount);
		ticketSvc.deleteTicket(testCollection);
		IRODSFile targetFileToCleanUp = accessObjectFactory
				.getIRODSFileFactory(secondaryAccount).instanceIRODSFile(
						targetIrodsCollection + "/" + testFileName);
		targetFileToCleanUp.deleteWithForceOption();
		targetFile.mkdirs();

		String ticketString = ticketSvc.createTicket(
				TicketCreateModeEnum.TICKET_CREATE_WRITE, targetFile,
				testCollection);

		TicketClientOperations ticketClientService = new TicketClientOperationsImpl(
				accessObjectFactory, secondaryAccount);

		ticketClientService.putFileToIRODSUsingTicket(ticketString, localFile,
				targetFile, null, null);

		IRODSFile actualFile = irodsFileSystem
				.getIRODSFileFactory(irodsAccount).instanceIRODSFile(
						targetFile.getAbsolutePath(), testFileName);
		Assert.assertTrue("target file not written", actualFile.exists());
		ticketSvc.deleteTicket(testCollection);

	}

	/**
	 * Put a file to irods, then put to it as a secondary user with a ticket
	 * using overwrite, giving that existing file name, and using a force option
	 * 
	 * @throws Exception
	 */
	@Test
	public final void testPutFileToIRODSUsingTicketExistingFileSpecifyFile()
			throws Exception {

		if (!testTicket) {
			return;
		}

		String testCollection = "testPutFileToIRODSUsingTicketExistingFileSpecifyFile";
		String testFileName = "testPutFileToIRODSUsingTicketExistingFileSpecifyFile.txt";
		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH);
		String localFileName = FileGenerator
				.generateFileOfFixedLengthGivenName(absPath, testFileName, 1);

		File localFile = new File(localFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testCollection);
		IRODSFile targetFile = accessObjectFactory.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(targetIrodsCollection);
		targetFile.mkdirs();
		DataTransferOperations dataTransferOperations = accessObjectFactory
				.getDataTransferOperations(irodsAccount);
		dataTransferOperations.putOperation(localFile, targetFile, null, null);

		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);
		TicketAdminService ticketSvc = new TicketAdminServiceImpl(
				accessObjectFactory, irodsAccount);
		ticketSvc.deleteTicket(testCollection);
		IRODSFile targetFileToCleanUp = accessObjectFactory
				.getIRODSFileFactory(secondaryAccount).instanceIRODSFile(
						targetIrodsCollection + "/" + testFileName);
		targetFileToCleanUp.deleteWithForceOption();

		String ticketString = ticketSvc.createTicket(
				TicketCreateModeEnum.TICKET_CREATE_WRITE, targetFile,
				testCollection);

		TicketClientOperations ticketClientService = new TicketClientOperationsImpl(
				accessObjectFactory, secondaryAccount);

		TransferControlBlock tcb = irodsFileSystem
				.getIRODSAccessObjectFactory()
				.buildDefaultTransferControlBlockBasedOnJargonProperties();
		tcb.getTransferOptions().setForceOption(ForceOption.USE_FORCE);

		ticketClientService.putFileToIRODSUsingTicket(ticketString, localFile,
				targetFile, null, tcb);

		IRODSFile actualFile = irodsFileSystem
				.getIRODSFileFactory(irodsAccount).instanceIRODSFile(
						targetFile.getAbsolutePath(), testFileName);
		Assert.assertTrue("target file not written", actualFile.exists());
		ticketSvc.deleteTicket(testCollection);

	}

	@Test(expected = CatNoAccessException.class)
	public final void testPutFileToIRODSUsingInvalidTicket() throws Exception {

		if (!testTicket) {
			return;
		}

		String testCollection = "testPutFileToIRODSUsingInvalidTicket";
		String testFileName = "testPutFileToIRODSUsingInvalidTicket.txt";
		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH);
		String localFileName = FileGenerator
				.generateFileOfFixedLengthGivenName(absPath, testFileName, 1);

		File localFile = new File(localFileName);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);
		IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
				.getIRODSAccessObjectFactory();
		String targetIrodsCollection = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testCollection);
		IRODSFile targetFile = accessObjectFactory.getIRODSFileFactory(
				irodsAccount).instanceIRODSFile(targetIrodsCollection);
		targetFile.mkdirs();

		String ticketString = "testPutFileToIRODSUsingInvalidTicket";

		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);

		TicketClientOperations ticketClientService = new TicketClientOperationsImpl(
				accessObjectFactory, secondaryAccount);

		ticketClientService.putFileToIRODSUsingTicket(ticketString, localFile,
				targetFile, null, null);

	}

	@Test
	public final void testGetFileFromIRODSUsingTicketOnFile() throws Exception {

		if (!testTicket) {
			return;
		}

		// generate a local scratch file
		String testFileName = "testGetFileFromIRODSUsingTicketOnFile.txt";
		String testRetrievedFileName = "testGetFileFromIRODSUsingTicketOnFileRetrieved.txt";

		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH);
		String localFileName = FileGenerator
				.generateFileOfFixedLengthGivenName(absPath, testFileName, 1);

		String targetIrodsFile = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testFileName);
		File localFile = new File(localFileName);

		// now put the file
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSFileFactory irodsFileFactory = irodsFileSystem
				.getIRODSFileFactory(irodsAccount);
		IRODSFile destFile = irodsFileFactory
				.instanceIRODSFile(targetIrodsFile);
		DataTransferOperations dataTransferOperationsAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getDataTransferOperations(
						irodsAccount);

		dataTransferOperationsAO.putOperation(localFile, destFile, null, null);

		// put a read ticket on the file

		TicketAdminService ticketSvc = new TicketAdminServiceImpl(
				irodsFileSystem.getIRODSAccessObjectFactory(), irodsAccount);
		ticketSvc.deleteTicket(testFileName);
		ticketSvc.createTicket(TicketCreateModeEnum.TICKET_CREATE_READ,
				destFile, testFileName);

		IRODSFile getIRODSFile = irodsFileFactory
				.instanceIRODSFile(targetIrodsFile);
		File getLocalFile = new File(absPath + "/" + testRetrievedFileName);
		getLocalFile.delete();

		// now get the file as secondary user with ticket

		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);

		TicketClientOperations ticketClientService = new TicketClientOperationsImpl(
				irodsFileSystem.getIRODSAccessObjectFactory(), secondaryAccount);

		ticketClientService.getOperationFromIRODSUsingTicket(testFileName,
				getIRODSFile, getLocalFile, null, null);

		assertionHelper.assertIrodsFileMatchesLocalFileChecksum(
				getIRODSFile.getAbsolutePath(), getLocalFile.getAbsolutePath());

	}

	@Test
	public final void testGetFileFromIRODSUsingTicketOnFileAsAnonymous()
			throws Exception {

		if (!testTicket) {
			return;
		}

		// generate a local scratch file
		String testFileName = "testGetFileFromIRODSUsingTicketOnFileAsAnonymous.txt";
		String testRetrievedFileName = "testGetFileFromIRODSUsingTicketOnFileAsAnonymousRetrieved.txt";

		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH);
		String localFileName = FileGenerator
				.generateFileOfFixedLengthGivenName(absPath, testFileName, 1);

		String targetIrodsFile = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testFileName);
		File localFile = new File(localFileName);

		// now put the file
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSFileFactory irodsFileFactory = irodsFileSystem
				.getIRODSFileFactory(irodsAccount);
		IRODSFile destFile = irodsFileFactory
				.instanceIRODSFile(targetIrodsFile);
		DataTransferOperations dataTransferOperationsAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getDataTransferOperations(
						irodsAccount);

		dataTransferOperationsAO.putOperation(localFile, destFile, null, null);

		// put a read ticket on the file

		TicketAdminService ticketSvc = new TicketAdminServiceImpl(
				irodsFileSystem.getIRODSAccessObjectFactory(), irodsAccount);
		ticketSvc.deleteTicket(testFileName);
		ticketSvc.createTicket(TicketCreateModeEnum.TICKET_CREATE_READ,
				destFile, testFileName);

		IRODSFile getIRODSFile = irodsFileFactory
				.instanceIRODSFile(targetIrodsFile);
		File getLocalFile = new File(absPath + "/" + testRetrievedFileName);
		getLocalFile.delete();

		// now get the file as secondary user with ticket

		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildAnonymousIRODSAccountFromTestProperties(testingProperties);

		TicketClientOperations ticketClientService = new TicketClientOperationsImpl(
				irodsFileSystem.getIRODSAccessObjectFactory(), secondaryAccount);

		ticketClientService.getOperationFromIRODSUsingTicket(testFileName,
				getIRODSFile, getLocalFile, null, null);

		assertionHelper.assertIrodsFileMatchesLocalFileChecksum(
				getIRODSFile.getAbsolutePath(), getLocalFile.getAbsolutePath());

	}

	@Test(expected = DataNotFoundException.class)
	public final void testGetFileFromIRODSUsingTicketOnFileAsAnonymousNoTicketAccess()
			throws Exception {

		if (!testTicket) {
			return;
		}

		// generate a local scratch file
		String testFileName = "testGetFileFromIRODSUsingTicketOnFileAsAnonymousNoTicketAccess.txt";
		String testRetrievedFileName = "testGetFileFromIRODSUsingTicketOnFileAsAnonymousNoTicketAccessRetrieved.txt";

		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH);
		String localFileName = FileGenerator
				.generateFileOfFixedLengthGivenName(absPath, testFileName, 1);

		String targetIrodsFile = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testFileName);
		File localFile = new File(localFileName);

		// now put the file
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSFileFactory irodsFileFactory = irodsFileSystem
				.getIRODSFileFactory(irodsAccount);
		IRODSFile destFile = irodsFileFactory
				.instanceIRODSFile(targetIrodsFile);
		DataTransferOperations dataTransferOperationsAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getDataTransferOperations(
						irodsAccount);

		dataTransferOperationsAO.putOperation(localFile, destFile, null, null);

		IRODSFile getIRODSFile = irodsFileFactory
				.instanceIRODSFile(targetIrodsFile);
		File getLocalFile = new File(absPath + "/" + testRetrievedFileName);
		getLocalFile.delete();

		// now get the file as secondary user with invalid ticket

		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildAnonymousIRODSAccountFromTestProperties(testingProperties);

		TicketClientOperations ticketClientService = new TicketClientOperationsImpl(
				irodsFileSystem.getIRODSAccessObjectFactory(), secondaryAccount);

		ticketClientService.getOperationFromIRODSUsingTicket(testFileName,
				getIRODSFile, getLocalFile, null, null);

	}

	/**
	 * [#637] nested subdirs with ticket issued on parent up the tree - no
	 * access?
	 * 
	 * @throws Exception
	 */
	@Ignore
	public final void testGetCollectionFromIRODSUsingTicketOnCollectionAsAnonymous()
			throws Exception {

		if (!testTicket) {
			return;
		}

		String testSubdir = "testSubdir-testGetCollectionFromIRODSUsingTicketOnCollectionAsAnonymous";
		String rootCollectionAndTicketName = "testGetCollectionFromIRODSUsingTicketOnCollectionAsAnonymous";
		String returnedLocalCollection = "testGetCollectionFromIRODSUsingTicketOnCollectionAsAnonymousReturnedLocalFiles";

		String localCollectionAbsolutePath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH
						+ '/' + rootCollectionAndTicketName);

		String irodsCollectionRootAbsolutePath = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testSubdir);

		FileGenerator
				.generateManyFilesAndCollectionsInParentCollectionByAbsolutePath(
						localCollectionAbsolutePath,
						"testGetCollectionWithTwoFilesNoCallbacks", 1, 1, 1,
						"testFile", ".txt", 2, 2, 1, 2);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSFileFactory irodsFileFactory = irodsFileSystem
				.getIRODSFileFactory(irodsAccount);
		IRODSFile destFile = irodsFileFactory
				.instanceIRODSFile(irodsCollectionRootAbsolutePath);
		destFile.mkdirs();
		TicketAdminService ticketSvc = new TicketAdminServiceImpl(
				irodsFileSystem.getIRODSAccessObjectFactory(), irodsAccount);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);

		collectionAO.setAccessPermissionInherit("", destFile.getAbsolutePath(),
				true);
		ticketSvc.deleteTicket(rootCollectionAndTicketName);
		ticketSvc.createTicket(TicketCreateModeEnum.TICKET_CREATE_READ,
				destFile, rootCollectionAndTicketName);
		DataTransferOperations dataTransferOperationsAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getDataTransferOperations(
						irodsAccount);
		File localFile = new File(localCollectionAbsolutePath);

		dataTransferOperationsAO.putOperation(localFile, destFile, null, null);
		destFile.reset();

		localCollectionAbsolutePath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH
						+ '/' + rootCollectionAndTicketName);
		File getLocalFile = new File(localCollectionAbsolutePath + "/"
				+ returnedLocalCollection);
		getLocalFile.delete();
		getLocalFile.mkdirs();

		// now get the file as secondary user with ticket

		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildAnonymousIRODSAccountFromTestProperties(testingProperties);

		TicketClientOperations ticketClientService = new TicketClientOperationsImpl(
				irodsFileSystem.getIRODSAccessObjectFactory(), secondaryAccount);

		ticketClientService
				.getOperationFromIRODSUsingTicket(rootCollectionAndTicketName,
						destFile, getLocalFile, null, null);

		File transferredCollection = getLocalFile.listFiles()[0];

		assertionHelper
				.assertIrodsFileOrCollectionExists(returnedLocalCollection);
		assertionHelper.assertTwoFilesAreEqualByRecursiveTreeComparison(
				localFile, transferredCollection);

	}

	/**
	 * [#637] nested subdirs with ticket issued on parent up the tree - no
	 * access?
	 * 
	 * @throws Exception
	 */
	@Ignore
	public final void testGetCollectionFromIRODSUsingTicketOnCollectionAsSecondaryUser()
			throws Exception {

		if (!testTicket) {
			return;
		}

		String testSubdir = "testSubdir-testGetCollectionFromIRODSUsingTicketOnCollectionAsSecondaryUser";
		String rootCollectionAndTicketName = "testGetCollectionFromIRODSUsingTicketOnCollectionAsSecondaryUser";
		String returnedLocalCollection = "testGetCollectionFromIRODSUsingTicketOnCollectionAsSecondaryUserReturnedLocalFiles";

		String localCollectionAbsolutePath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH
						+ '/' + rootCollectionAndTicketName);

		String irodsCollectionRootAbsolutePath = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + "/"
								+ testSubdir);

		FileGenerator
				.generateManyFilesAndCollectionsInParentCollectionByAbsolutePath(
						localCollectionAbsolutePath,
						"testGetCollectionWithTwoFilesNoCallbacks", 1, 1, 1,
						"testFile", ".txt", 2, 2, 1, 2);

		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSFileFactory irodsFileFactory = irodsFileSystem
				.getIRODSFileFactory(irodsAccount);
		IRODSFile destFile = irodsFileFactory
				.instanceIRODSFile(irodsCollectionRootAbsolutePath);
		destFile.mkdirs();
		TicketAdminService ticketSvc = new TicketAdminServiceImpl(
				irodsFileSystem.getIRODSAccessObjectFactory(), irodsAccount);
		CollectionAO collectionAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);

		collectionAO.setAccessPermissionInherit("", destFile.getAbsolutePath(),
				true);
		ticketSvc.deleteTicket(rootCollectionAndTicketName);
		ticketSvc.createTicket(TicketCreateModeEnum.TICKET_CREATE_READ,
				destFile, rootCollectionAndTicketName);
		DataTransferOperations dataTransferOperationsAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getDataTransferOperations(
						irodsAccount);
		File localFile = new File(localCollectionAbsolutePath);

		dataTransferOperationsAO.putOperation(localFile, destFile, null, null);
		destFile.reset();

		localCollectionAbsolutePath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH
						+ '/' + rootCollectionAndTicketName);
		File getLocalFile = new File(localCollectionAbsolutePath + "/"
				+ returnedLocalCollection);
		getLocalFile.delete();
		getLocalFile.mkdirs();

		// now get the file as secondary user with ticket

		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);

		TicketClientOperations ticketClientService = new TicketClientOperationsImpl(
				irodsFileSystem.getIRODSAccessObjectFactory(), secondaryAccount);

		ticketClientService
				.getOperationFromIRODSUsingTicket(rootCollectionAndTicketName,
						destFile, getLocalFile, null, null);

		File transferredCollection = getLocalFile.listFiles()[0];

		assertionHelper
				.assertIrodsFileOrCollectionExists(returnedLocalCollection);
		assertionHelper.assertTwoFilesAreEqualByRecursiveTreeComparison(
				localFile, transferredCollection);

	}

	/**
	 * Get a ticket on a data object, then get the data back as a stream
	 * 
	 * @throws Exception
	 */
	@Test
	public final void redeemTicketGetDataObjectAndStreamBack() throws Exception {

		if (!testTicket) {
			return;
		}

		String retrievedSubdir = "redeemTicketGetDataObjectAndStreamBack";
		long size = 3 * 1024;

		// generate a local scratch file
		String testFileName = "redeemTicketGetDataObjectAndStreamBack.txt";

		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH);
		String localFileName = FileGenerator
				.generateFileOfFixedLengthGivenName(absPath, testFileName, size);

		String targetIrodsFile = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testFileName);
		File localFile = new File(localFileName);

		// now put the file
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSFileFactory irodsFileFactory = irodsFileSystem
				.getIRODSFileFactory(irodsAccount);
		IRODSFile destFile = irodsFileFactory
				.instanceIRODSFile(targetIrodsFile);
		DataTransferOperations dataTransferOperationsAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getDataTransferOperations(
						irodsAccount);

		dataTransferOperationsAO.putOperation(localFile, destFile, null, null);

		// put a read ticket on the file

		TicketAdminService ticketSvc = new TicketAdminServiceImpl(
				irodsFileSystem.getIRODSAccessObjectFactory(), irodsAccount);
		ticketSvc.deleteTicket(testFileName);
		ticketSvc.createTicket(TicketCreateModeEnum.TICKET_CREATE_READ,
				destFile, testFileName);

		IRODSFile getIRODSFile = irodsFileFactory
				.instanceIRODSFile(targetIrodsFile);
		File tempCacheFile = new File(absPath + "/" + retrievedSubdir
				+ "/tempCache");
		tempCacheFile.mkdirs();

		// now get the file as secondary user with ticket

		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);

		TicketClientOperations ticketClientService = new TicketClientOperationsImpl(
				irodsFileSystem.getIRODSAccessObjectFactory(), secondaryAccount);

		FileStreamAndInfo fileStreamAndInfo = ticketClientService
				.redeemTicketGetDataObjectAndStreamBack(testFileName,
						getIRODSFile, tempCacheFile);

		int totalBytes = 0;
		while ((fileStreamAndInfo.getInputStream().read()) > -1) {
			totalBytes++;
		}

		fileStreamAndInfo.getInputStream().close();

		Assert.assertEquals("all bytes not read", size, totalBytes);
		Assert.assertTrue("should be no files in the temp cache",
				tempCacheFile.listFiles().length == 0);
		Assert.assertEquals(
				"did not correctly set lenght in fileStreamAndInfo", size,
				fileStreamAndInfo.getLength());

	}

	/**
	 * Try to stream from a collection
	 * 
	 * @throws Exception
	 */
	@Test(expected = JargonException.class)
	public final void redeemTicketGetDataObjectAndStreamBackIrodsFileIsCollection()
			throws Exception {

		if (!testTicket) {
			return;
		}

		String testCollection = "redeemTicketGetDataObjectAndStreamBackIrodsFileIsCollection";

		String targetIrodsFile = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testCollection);

		// now put the file
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSFileFactory irodsFileFactory = irodsFileSystem
				.getIRODSFileFactory(irodsAccount);
		IRODSFile destFile = irodsFileFactory
				.instanceIRODSFile(targetIrodsFile);
		destFile.mkdirs();

		// put a read ticket on the file

		TicketAdminService ticketSvc = new TicketAdminServiceImpl(
				irodsFileSystem.getIRODSAccessObjectFactory(), irodsAccount);
		ticketSvc.deleteTicket(testCollection);
		ticketSvc.createTicket(TicketCreateModeEnum.TICKET_CREATE_READ,
				destFile, testCollection);

		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH);

		IRODSFile getIRODSFile = irodsFileFactory
				.instanceIRODSFile(targetIrodsFile);
		File tempCacheFile = new File(absPath + "/" + "/tempCache");
		tempCacheFile.mkdirs();

		// now get the file as secondary user with ticket

		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);

		TicketClientOperations ticketClientService = new TicketClientOperationsImpl(
				irodsFileSystem.getIRODSAccessObjectFactory(), secondaryAccount);

		ticketClientService.redeemTicketGetDataObjectAndStreamBack(
				testCollection, getIRODSFile, tempCacheFile);

	}

	/**
	 * Get a ticket on a data object, but the root cache dir does not exist
	 * 
	 * @throws Exception
	 */
	@Test(expected = JargonException.class)
	public final void redeemTicketGetDataObjectAndStreamBackCaceRootDirNotExists()
			throws Exception {

		if (!testTicket) {
			return;
		}

		long size = 3 * 1024;

		// generate a local scratch file
		String testFileName = "redeemTicketGetDataObjectAndStreamBack.txt";

		String absPath = scratchFileUtils
				.createAndReturnAbsoluteScratchPath(IRODS_TEST_SUBDIR_PATH);
		String localFileName = FileGenerator
				.generateFileOfFixedLengthGivenName(absPath, testFileName, size);

		String targetIrodsFile = testingPropertiesHelper
				.buildIRODSCollectionAbsolutePathFromTestProperties(
						testingProperties, IRODS_TEST_SUBDIR_PATH + '/'
								+ testFileName);
		File localFile = new File(localFileName);

		// now put the file
		IRODSAccount irodsAccount = testingPropertiesHelper
				.buildIRODSAccountFromTestProperties(testingProperties);

		IRODSFileFactory irodsFileFactory = irodsFileSystem
				.getIRODSFileFactory(irodsAccount);
		IRODSFile destFile = irodsFileFactory
				.instanceIRODSFile(targetIrodsFile);
		DataTransferOperations dataTransferOperationsAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getDataTransferOperations(
						irodsAccount);

		dataTransferOperationsAO.putOperation(localFile, destFile, null, null);

		// put a read ticket on the file

		TicketAdminService ticketSvc = new TicketAdminServiceImpl(
				irodsFileSystem.getIRODSAccessObjectFactory(), irodsAccount);
		ticketSvc.deleteTicket(testFileName);
		ticketSvc.createTicket(TicketCreateModeEnum.TICKET_CREATE_READ,
				destFile, testFileName);

		IRODSFile getIRODSFile = irodsFileFactory
				.instanceIRODSFile(targetIrodsFile);
		File tempCacheFile = new File("/i/dont/exist/at/all");

		// now get the file as secondary user with ticket

		IRODSAccount secondaryAccount = testingPropertiesHelper
				.buildIRODSAccountFromSecondaryTestProperties(testingProperties);

		TicketClientOperations ticketClientService = new TicketClientOperationsImpl(
				irodsFileSystem.getIRODSAccessObjectFactory(), secondaryAccount);

		ticketClientService.redeemTicketGetDataObjectAndStreamBack(
				testFileName, getIRODSFile, tempCacheFile);

	}

}