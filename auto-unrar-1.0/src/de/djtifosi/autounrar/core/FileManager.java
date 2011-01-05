/*
 * Auto UnRar - a free automated batch extraction tool for RAR-Archives.
 * Copyright (C) 2010 djtifosi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.djtifosi.autounrar.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import de.djtifosi.autounrar.conf.Configuration;
import de.djtifosi.autounrar.conf.PasswordList;
import de.djtifosi.autounrar.exceptions.CRCFailedException;
import de.djtifosi.autounrar.exceptions.NoFilesToExtractException;
import de.djtifosi.autounrar.exceptions.NoFittingPasswordFoundException;
import de.djtifosi.autounrar.exceptions.PasswordIncorrectException;
import de.djtifosi.autounrar.exceptions.UnrarFailedException;

public abstract class FileManager {
	private static UnrarApplicationManager unrarApplicationManager = new UnrarApplicationManager();

	private static PasswordList passwordList;

	private static Logger log = Logger.getLogger(FileManager.class);

	public static ArrayList<String> filterRARFiles(String folder) {
		final File file = new File(folder);
		final ArrayList<String> filteredFiles = new ArrayList<String>();
		final String[] list = file.list(new FilenameFilter() {

			// All Files ending with .rar or .r01, r02, etc.
			final Pattern p = Pattern.compile("([^/]*)\\.r([ar,0-9]*)");

			@Override
			public boolean accept(File dir, String name) {
				final String lowerName = name.toLowerCase();
				final Matcher m = p.matcher(lowerName);
				if (m.matches()) {
					return true;
				}
				return false;
			}
		});
		if (list != null) {
			for (String filename : list) {
				if (new File(folder + filename).isFile()) {
					filteredFiles.add(filename);
				}
			}
		}
		return filteredFiles;
	}

	public static ArrayList<SplitsetVo> convertFilenamesToSplitsets(
			ArrayList<String> rarFiles) {
		final ArrayList<SplitsetVo> splitSets = new ArrayList<SplitsetVo>();

		// All Files ending with part01.rar, part 001.rar, etc.
		final Pattern partPattern = Pattern.compile("([^/]*)part([0])*1.rar");

		// All Files ending with .r0,.r00, .r000, etc.
		final Pattern rPattern = Pattern.compile("([^/]*).r([0])+");

		// Single volume archives ending with rar
		final Pattern rarPattern = Pattern.compile("([^/]*).rar");

		for (int i = 0; i < rarFiles.size(); i++) {
			final String fileName = rarFiles.get(i);
			final String lowerName = fileName.toLowerCase();
			final Matcher mPartPattern = partPattern.matcher(lowerName);
			final Matcher mRPattern = rPattern.matcher(lowerName);
			final Matcher mRarPattern = rarPattern.matcher(lowerName);

			HashMap<Integer, String> rarMap = null;

			if (mPartPattern.matches()) {
				/*
				 * Handle Part* Pattern (filename ending with part01.rar, part
				 * 001.rar, part02.rar, etc.)
				 */
				rarMap = handlePartPattern(fileName, rarFiles);
			} else if (mRPattern.matches()) {
				/*
				 * Handle r* Pattern (filename ending with .rar, r01, r001, r02,
				 * etc.)
				 */
				rarMap = handleRPattern(fileName, rarFiles);
			} else if (mRarPattern.matches()
					&& checkSingleVolumeRar(fileName, rarFiles)) {
				/*
				 * Handle Single volume archives ending with rar
				 */
				rarMap = handleRarPattern(fileName, rarFiles);
			}
			if (rarMap != null) {
				SplitsetVo splitSet = new SplitsetVo();
				splitSet.rarMap = rarMap;
				splitSets.add(splitSet);
				rarMap = null;
			}
		}
		return splitSets;
	}

	private static boolean checkSingleVolumeRar(String fileName,
			ArrayList<String> rarFiles) {
		final Pattern partPattern = Pattern.compile("([^/]*)part([0-9])*.rar");
		final Matcher mPartPattern = partPattern
				.matcher(fileName.toLowerCase());
		if (mPartPattern.matches()) {
			return false;
		}

		String name = fileName.substring(0, fileName.indexOf(".rar"));
		for (String name2 : rarFiles) {
			if (name2.startsWith(name) && !name2.equals(fileName)) {
				return false;
			}
		}

		return true;
	}

	private static HashMap<Integer, String> handleRarPattern(String fileName,
			ArrayList<String> rarFiles) {
		final HashMap<Integer, String> rarMap = new HashMap<Integer, String>();
		rarMap.put(1, fileName);
		return rarMap;
	}

	private static HashMap<Integer, String> handlePartPattern(String fileName,
			ArrayList<String> rarFiles) {
		final HashMap<Integer, String> rarMap = new HashMap<Integer, String>();
		final String[] split = fileName.toLowerCase().split("part");
		for (String name : rarFiles) {
			if (name.toLowerCase().startsWith(split[0])) {
				final String[] split2 = name.toLowerCase().split("part");
				int fileNumber = Integer
						.parseInt(split2[1].replace(".rar", ""));
				rarMap.put(fileNumber, name);
			}
		}
		return rarMap;
	}

	private static HashMap<Integer, String> handleRPattern(String fileName,
			ArrayList<String> rarFiles) {
		final HashMap<Integer, String> rarMap = new HashMap<Integer, String>();
		final String[] split = fileName.toLowerCase().split("\\.r");
		for (String name : rarFiles) {
			if (name.toLowerCase().startsWith(split[0])) {
				final String[] split2 = name.toLowerCase().split("\\.r");
				final String replaced = split2[1].replace(".r", "");
				int fileNumber;
				if (replaced.equals("ar")) {
					fileNumber = 1;
				} else {
					fileNumber = Integer.parseInt(split2[1]) + 2;
				}
				rarMap.put(fileNumber, name);
			}
		}
		return rarMap;
	}

	public static ArrayList<SplitsetVo> filterSplitsetsWithGaps(
			ArrayList<SplitsetVo> splitsets) {
		final ArrayList<SplitsetVo> filteredSplitSets = new ArrayList<SplitsetVo>();

		for (SplitsetVo splitset : splitsets) {
			ArrayList<Integer> missingVolumes = splitset
					.getMissingVolumes();
			if (missingVolumes.isEmpty()) {
				filteredSplitSets.add(splitset);
				log.info("The volumes of archive " + splitset.getSplitsetName()
						+ " are without gap.");
			} else {
				log.warn("Cannot extract archive " + splitset.getSplitsetName()
						+ " because the following volumes are missing: ");
				for (Integer integer : missingVolumes) {
					log.warn("Volume " + integer);
				}
			}
		}
		return filteredSplitSets;
	}

	public static ArrayList<SplitsetVo> filterSplitsetsWithIncompleteBegin(
			ArrayList<SplitsetVo> splitsets) {
		final ArrayList<SplitsetVo> filteredSplitSets = new ArrayList<SplitsetVo>();

		for (SplitsetVo splitset : splitsets) {
			String firstFile = splitset.getFirstFileWithinSplitset();

			if (firstFile != null) {
				boolean hasPredecessorPart = false;
				boolean success = false;
				String pwd = "";

				try {
					log
							.debug("Trying to analyze last volume of archive "
									+ splitset.getSplitsetName()
									+ " without password.");
					hasPredecessorPart = unrarApplicationManager
							.hasPredecessorPart(Configuration.SOURCE_FOLDER
									+ firstFile, "");
					success = true;
				} catch (PasswordIncorrectException e) {
					log.debug("Analyzing first volume of archive "
							+ splitset.getSplitsetName()
							+ " without password failed ... "
							+ "processing password list.");
				}

				if (!success) {
					if (passwordList != null) {
						for (String password : passwordList) {
							try {
								log
										.debug("Trying to analyze last volume of archive "
												+ splitset.getSplitsetName()
												+ " with password: " + password);
								hasPredecessorPart = unrarApplicationManager
										.hasPredecessorPart(
												Configuration.SOURCE_FOLDER
														+ firstFile, password);
								pwd = password;
								log.debug("Analyzing first volume of archive "
										+ splitset.getSplitsetName()
										+ " with password: " + password
										+ " completed without error.");
								success = true;
								break;
							} catch (PasswordIncorrectException e1) {
								log.debug("Analyzing first volume of archive "
										+ splitset.getSplitsetName()
										+ " with password: " + password
										+ " failed.");
							}
						}
					} else {
						log.warn("Password list is missing ... skipping "
								+ "process of trying to analyze first volume "
								+ "of archive with password.");
					}
				}

				if (success && !hasPredecessorPart) {
					log.debug("Sucessfully analyzed first volume of archive "
							+ splitset.getSplitsetName());
					SplitsetVo splitsetVo = new SplitsetVo();
					splitsetVo.rarMap = splitset.rarMap;
					splitsetVo.password = pwd;
					filteredSplitSets.add(splitsetVo);
				} else {
					log.warn("Cannot extract archive "
							+ splitset.getSplitsetName() + " because "
							+ splitset.getFirstFileWithinSplitset()
							+ " is not the first volume. First "
							+ "volume is missing.");
				}
			}
		}
		return filteredSplitSets;
	}

	public static ArrayList<SplitsetVo> filterSplitsetsWithIncompleteEnd(
			ArrayList<SplitsetVo> splitsets) {
		final ArrayList<SplitsetVo> filteredSplitSets = new ArrayList<SplitsetVo>();

		for (SplitsetVo splitset : splitsets) {
			String lastFile = splitset.getLastFileWithinSplitset();

			if (lastFile != null) {
				boolean hasFollowerPart = false;
				boolean success = false;
				String pwd = "";

				try {
					log
							.debug("Trying to analyze last volume of archive "
									+ splitset.getSplitsetName()
									+ " without password.");
					hasFollowerPart = unrarApplicationManager.hasFollowerPart(
							Configuration.SOURCE_FOLDER + lastFile, "");
					success = true;
				} catch (PasswordIncorrectException e) {
					log.debug("Analyzing last volume of archive "
							+ splitset.getSplitsetName()
							+ " without password failed ... "
							+ "processing password list.");
				}

				if (!success) {
					if (passwordList != null) {
						for (String password : passwordList) {
							try {
								log
										.debug("Trying to analyze last volume of archive "
												+ splitset.getSplitsetName()
												+ " with password: " + password);
								hasFollowerPart = unrarApplicationManager
										.hasFollowerPart(
												Configuration.SOURCE_FOLDER
														+ lastFile, password);
								pwd = password;
								log.debug("Analyzing last volume of archive "
										+ splitset.getSplitsetName()
										+ " with password: " + password
										+ " completed without error.");
								success = true;
								break;
							} catch (PasswordIncorrectException e1) {
								log.debug("Analyzing last volume of archive "
										+ splitset.getSplitsetName()
										+ " with password: " + password
										+ " failed.");
							}
						}
					} else {
						log.warn("Password list is missing ... skipping "
								+ "process of trying to analyze last volume "
								+ "of archive with password.");
					}
				}

				if (success && !hasFollowerPart) {
					log.debug("Sucessfully analyzed last volume of archive "
							+ splitset.getSplitsetName());
					SplitsetVo splitsetVo = new SplitsetVo();
					splitsetVo.rarMap = splitset.rarMap;
					splitsetVo.password = pwd;
					filteredSplitSets.add(splitsetVo);
				} else {
					log.warn("Cannot extract archive "
							+ splitset.getSplitsetName() + " because "
							+ splitset.getLastFileWithinSplitset()
							+ " is not the last volume. Last "
							+ "volume is missing.");
				}
			}
		}
		return filteredSplitSets;
	}

	public static List<String> unrarAllFiles(SplitsetVo splitsetVo,
			String targetDir) throws UnrarFailedException,
			NoFilesToExtractException, PasswordIncorrectException,
			NoFittingPasswordFoundException, CRCFailedException {
		List<String> sourceFiles = null;
		final String sourceFile = Configuration.SOURCE_FOLDER
				+ splitsetVo.getFirstFileWithinSplitset();
		if (targetDir == null) {
			targetDir = Configuration.TARGET_FOLDER;
		}
		if (splitsetVo.password.isEmpty()) {
			boolean success = false;
			try {
				log.debug("Trying to unrar archive without password.");
				sourceFiles = unrarApplicationManager.unrarAllFiles(sourceFile,
						targetDir, "");
				success = true;
			} catch (PasswordIncorrectException e) {
				log.debug("Unraring archive without password failed ... "
						+ "processing password list.");
			} catch (CRCFailedException e) {
				writeErrorReportFile(e.getErrorStream(), targetDir);
				throw e;
			}

			if (!success) {
				if (passwordList != null) {
					for (String password : passwordList) {
						try {
							log.debug("Trying to unrar archive with password: "
									+ password);
							sourceFiles = unrarApplicationManager
									.unrarAllFiles(sourceFile, targetDir,
											password);
							log.debug("Unraring archive without password: "
									+ password + " completed without error.");
							success = true;
							break;
						} catch (PasswordIncorrectException e1) {
							log.debug("Unraring archive without password: "
									+ password + " failed.");
						} catch (CRCFailedException e) {
							writeErrorReportFile(e.getErrorStream(), targetDir);
							throw e;
						}
					}
				} else {
					log.warn("Password list is missing ... skipping unrar "
							+ "process with password.");
				}
			}

			if (!success) {
				throw new NoFittingPasswordFoundException();
			}

		} else {
			sourceFiles = unrarApplicationManager.unrarAllFiles(sourceFile,
					targetDir, splitsetVo.password);
		}
		return sourceFiles;
	}

	public static List<String> unrarFilesByMaxFilesize(SplitsetVo splitsetVo)
			throws UnrarFailedException, NoFittingPasswordFoundException,
			NoFilesToExtractException, CRCFailedException {
		List<String> unraredFiles = null;
		if (splitsetVo.password.isEmpty()) {
			boolean success = false;
			try {
				log
						.debug("Trying to unrar small files of archive without password.");
				unraredFiles = unrarApplicationManager.unrarFilesByMaxFilesize(
						Configuration.SOURCE_FOLDER
								+ splitsetVo.getFirstFileWithinSplitset(),
						Configuration.TEMPORARY_FOLDER, "",
						Configuration.SMALL_FILE_THRESHOLD);
				success = true;
			} catch (PasswordIncorrectException e) {
				log
						.debug("Unraring small files of archive without password failed ... "
								+ "processing password list.");
			}

			if (!success) {
				if (passwordList != null) {
					for (String password : passwordList) {
						try {
							log
									.debug("Trying to unrar small files of archive with password: "
											+ password);
							unraredFiles = unrarApplicationManager
									.unrarFilesByMaxFilesize(
											Configuration.SOURCE_FOLDER
													+ splitsetVo
															.getFirstFileWithinSplitset(),
											Configuration.TEMPORARY_FOLDER,
											password,
											Configuration.SMALL_FILE_THRESHOLD);
							log
									.debug("Unraring small files of archive with password: "
											+ password
											+ " completed without error.");
							success = true;
							splitsetVo.password = password;
							break;
						} catch (PasswordIncorrectException e1) {
							log
									.debug("Unraring small files of archive with password: "
											+ password + " failed.");
						}
					}
				} else {
					log.warn("Password list is missing ... skipping "
							+ "process of trying to unrar small files "
							+ "with password.");
				}
			}
			if (!success) {
				throw new NoFittingPasswordFoundException();
			}
		}
		return unraredFiles;
	}

	public static void deleteFiles(List<String> partFiles) {
		for (String fileName : partFiles) {
			File file = new File(fileName);
			log.info("Deleting " + fileName);
			file.delete();
		}
	}

	public static String createDirectoryForSplitset(SplitsetVo splitsetVo) {
		String path = getDirectoryForSplitset(splitsetVo);
		File folder = new File(path);
		folder.mkdir();

		if (folder.exists()) {
			log.info("Successful created directory " + path);
			return path;
		} else {
			log.warn("Creating directory " + path
					+ " was not possible. Extracting to "
					+ Configuration.TARGET_FOLDER);
			return null;
		}
	}

	public static String getDirectoryForSplitset(SplitsetVo splitsetVo) {
		String splitsetName = splitsetVo.getSplitsetName();
		if (splitsetName.isEmpty()) {
			return Configuration.TARGET_FOLDER;
		} else {
			return Configuration.TARGET_FOLDER + splitsetVo.getSplitsetName();
		}
	}

	public static boolean removeDirectory(String path) {
		if (path != null) {
			File folder = new File(path);
			File renamedFolder = new File(path + ".renamed");
			if (folder.exists() && folder.isDirectory()
					&& folder.list().length == 0) {
				folder.delete();
				log.info("Removed directory " + path);
				return true;
			} else if (renamedFolder.exists()) {
				removeDirectory(path + ".renamed");
			}
		}
		return false;
	}

	public static boolean moveSingleFileWithinDirectoryToTargetFolder(
			String path) {
		if (path != null) {
			File folder = new File(path);
			if (folder.isDirectory() && folder.list().length == 1) {
				log.info("Folder " + folder
						+ " contains only one file/directory: "
						+ folder.list()[0] + ". Move it to "
						+ Configuration.TARGET_FOLDER);
				String folderToMove = folder + "/" + folder.list()[0];
				File conflictFolder = new File(Configuration.TARGET_FOLDER
						+ folder.list()[0]);
				if (conflictFolder.exists() && conflictFolder.isDirectory()
						&& folder.equals(conflictFolder)) {
					log.info("A directory with the name " + folder.list()[0]
							+ " already exists in "
							+ Configuration.TARGET_FOLDER
							+ ". Renaming it to avoid conflicts.");
					String innerFolderName = folder.list()[0];
					renameFileOrDirectory(Configuration.TARGET_FOLDER
							+ folder.list()[0], Configuration.TARGET_FOLDER
							+ folder.list()[0] + ".renamed");
					folderToMove = path + ".renamed/" + innerFolderName;
				}
				return moveFileOrDirectory(folderToMove,
						Configuration.TARGET_FOLDER);
			} else {
				log.debug("Not moving files in " + path);
			}
		}
		return false;
	}

	private static boolean moveFileOrDirectory(String source, String destination) {
		// File (or directory) to be moved
		File sourceFileOrDir = new File(source);
		// Destination directory File
		File destinationDir = new File(destination);
		// Move file to new directory
		boolean success = sourceFileOrDir.renameTo(new File(destinationDir,
				sourceFileOrDir.getName()));
		if (success) {
			log.info("Successful moved " + source + " to " + destination);
		} else {
			log.warn("Moving " + source + " to " + destination
					+ " was not possible.");
		}
		return success;
	}

	private static boolean renameFileOrDirectory(String source,
			String destination) {
		// File (or directory) to be moved
		File sourceFileOrDir = new File(source);
		// Destination directory File
		File destinationDir = new File(destination);
		// Move file to new directory
		boolean success = sourceFileOrDir.renameTo(destinationDir);
		if (success) {
			log.info("Successful renamed " + source + " to " + destination);
		} else {
			log.warn("Renaming " + source + " to " + destination
					+ " was not possible.");
		}
		return success;
	}

	public static void markDirectoryAsIncomplete(String dir) {
		File file = new File(dir);
		if (file.exists() && file.isDirectory()) {
			String targetDir = dir.substring(0, dir.length()) + ".incomplete";
			renameFileOrDirectory(dir, targetDir);
		}
	}

	public static boolean sourceFolderExists() {
		File file = new File(Configuration.SOURCE_FOLDER);
		if (file != null && file.exists() && file.isDirectory()) {
			return true;
		}
		return false;
	}

	public static void clearTempFolder() {
		log.debug("Clearing temporary folder.");
		deleteDirectoryRecursive(new File(Configuration.TEMPORARY_FOLDER));
	}

	private static void deleteDirectoryRecursive(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectoryRecursive(files[i]);
					files[i].delete();
				} else {
					files[i].delete();
				}
			}
		}
	}

	public static void setPasswordList(PasswordList passwordList) {
		FileManager.passwordList = passwordList;
	}

	private static void writeErrorReportFile(String errorStream,
			String targetDir) {
		File f = new File(targetDir + "/" + "auto-unrar.error");
		try {
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(errorStream.getBytes());
		} catch (Exception e) {
			log.warn("Error creating the file " + targetDir + "/"
					+ "auto-unrar.error");
		}
	}

	public static Set<String> searchForRarFilesContainingFolders(
			String rootFolder) {
		return searchForRarFilesContainingFoldersRecursive(new File(rootFolder));
	}

	private static Set<String> searchForRarFilesContainingFoldersRecursive(
			File path) {
		Set<String> rarFileContainingFolders = new HashSet<String>();
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					rarFileContainingFolders
							.addAll(searchForRarFilesContainingFoldersRecursive(files[i]));
				} else if (files[i].getName().endsWith(".rar")
						|| files[i].getName().endsWith(".RAR")) {
					rarFileContainingFolders.add(path.getAbsolutePath() + "/");
				}
			}
		}
		return rarFileContainingFolders;
	}
}
