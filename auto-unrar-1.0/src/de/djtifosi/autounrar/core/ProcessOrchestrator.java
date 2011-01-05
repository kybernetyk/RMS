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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import de.djtifosi.autounrar.conf.Configuration;
import de.djtifosi.autounrar.conf.PasswordList;
import de.djtifosi.autounrar.exceptions.CRCFailedException;
import de.djtifosi.autounrar.exceptions.NoFilesToExtractException;
import de.djtifosi.autounrar.exceptions.NoFittingPasswordFoundException;
import de.djtifosi.autounrar.exceptions.PasswordIncorrectException;
import de.djtifosi.autounrar.exceptions.StatusMonitorException;
import de.djtifosi.autounrar.exceptions.UnrarFailedException;

public class ProcessOrchestrator {

	private static Logger log = Logger.getLogger(ProcessOrchestrator.class);

	public void startProcess() throws StatusMonitorException {
		ArrayList<SplitsetVo> splitsetVos = null;
		try {
			log.info("-------------------------------------------------");
			log.info("Starting process...");
			log.info("-------------------------------------------------");

			StatusMonitor.afterJobStart();
			refreshPasswordList();
			ArrayList<String> rarFiles = getRarFilenames();
			splitsetVos = performValidations(rarFiles);
			splitsetVos = checkPasswords(splitsetVos);
			unrarSplitsets(splitsetVos);

			if (Configuration.ACTIVATE_RECURSIVE_EXTRACTION) {
				if (Configuration.ACTIVATE_DEEP_RECURSIVE_EXTRACTION
						&& Configuration.DELETE_SOURCE_FILES_AFTER_UNRAR) {
					performDeepRecursiveProcess(splitsetVos);
				} else {
					performRecursiveProcess(splitsetVos);
				}
			}

			FileManager.clearTempFolder();
		} finally {
			StatusMonitor.beforeJobEnd(splitsetVos);
		}

	}

	private void refreshPasswordList() {
		log.debug("Refreshing password list.");
		try {
			PasswordList passwordList = PasswordList.getInstance();
			passwordList.refresh();
			FileManager.setPasswordList(passwordList);
		} catch (IOException e) {
			log
					.warn("Error loading Password List ... proceeding without password support.");
		}
	}

	private void performRecursiveProcess(ArrayList<SplitsetVo> splitsetVos) {
		String sourceFolderBackup = Configuration.SOURCE_FOLDER;
		String targetFolderBackup = Configuration.TARGET_FOLDER;

		try {
			log.info("-------------------------------------------------");
			log.info("Starting recursive process...");
			log.info("-------------------------------------------------");

			for (SplitsetVo splitsetVo : splitsetVos) {
				Configuration.SOURCE_FOLDER = FileManager
						.getDirectoryForSplitset(splitsetVo)
						+ "/";
				Configuration.TARGET_FOLDER = FileManager
						.getDirectoryForSplitset(splitsetVo)
						+ "/";

				if (FileManager.sourceFolderExists()) {
					log.info("Processing source folder: "
							+ Configuration.SOURCE_FOLDER);

					ArrayList<String> recursiveRarFiles = getRarFilenames();
					ArrayList<SplitsetVo> recursiveSplitsetVos = performValidations(recursiveRarFiles);
					recursiveSplitsetVos = checkPasswords(recursiveSplitsetVos);
					unrarSplitsets(recursiveSplitsetVos);
				}
			}
		} finally {
			Configuration.SOURCE_FOLDER = sourceFolderBackup;
			Configuration.TARGET_FOLDER = targetFolderBackup;
		}
	}

	private void performDeepRecursiveProcess(ArrayList<SplitsetVo> splitsetVos) {
		String sourceFolderBackup = Configuration.SOURCE_FOLDER;
		String targetFolderBackup = Configuration.TARGET_FOLDER;
		Set<String> noSuccessFolders = new HashSet<String>();

		try {
			log.info("-------------------------------------------------");
			log.info("Starting deep recursive process...");
			log.info("-------------------------------------------------");

			Set<String> rarFileContainingFolders = new HashSet<String>();
			ArrayList<String> visitedRarFiles = new ArrayList<String>();
			for (SplitsetVo splitsetVo : splitsetVos) {
				do {
					Configuration.SOURCE_FOLDER = sourceFolderBackup;
					Configuration.TARGET_FOLDER = targetFolderBackup;
					rarFileContainingFolders = FileManager
							.searchForRarFilesContainingFolders(FileManager
									.getDirectoryForSplitset(splitsetVo)
									+ "/");
					rarFileContainingFolders.removeAll(noSuccessFolders);
					for (String folder : rarFileContainingFolders) {
						Configuration.SOURCE_FOLDER = folder;
						Configuration.TARGET_FOLDER = folder;

						if (FileManager.sourceFolderExists()) {
							log.info("Processing source folder: "
									+ Configuration.SOURCE_FOLDER);

							boolean success = true;
							ArrayList<String> recursiveRarFiles = getRarFilenames();

							for (String rarFile : recursiveRarFiles) {
								if (visitedRarFiles.contains(rarFile)) {
									success = false;
								}
							}
							if (success) {
								visitedRarFiles.addAll(recursiveRarFiles);
								ArrayList<SplitsetVo> recursiveSplitsetVos = performValidations(recursiveRarFiles);
								recursiveSplitsetVos = checkPasswords(recursiveSplitsetVos);
								success = unrarSplitsets(recursiveSplitsetVos);
							}
							/*
							 * If there was an extraction error, the same RARs
							 * still exist. So skip this folder to avoid an
							 * infinite loop.
							 */
							if (!success) {
								noSuccessFolders
										.add(Configuration.SOURCE_FOLDER);
							}
						}
					}
				} while (rarFileContainingFolders.size() > 0);
			}

		} finally {
			Configuration.SOURCE_FOLDER = sourceFolderBackup;
			Configuration.TARGET_FOLDER = targetFolderBackup;
		}

	}

	private ArrayList<String> getRarFilenames() {
		// List all potential RAR-Files in source folder
		ArrayList<String> rarFiles = FileManager
				.filterRARFiles(Configuration.SOURCE_FOLDER);
		log.debug("-------------------------------------------------");
		log.debug("List of all potential RAR-Files in source folder:");
		for (String fileName : rarFiles) {
			log.debug(fileName);
		}
		log.debug("-------------------------------------------------");

		return rarFiles;
	}

	private ArrayList<SplitsetVo> performValidations(ArrayList<String> rarFiles) {
		// Structurize files within splitsets
		ArrayList<SplitsetVo> splitsets = FileManager
				.convertFilenamesToSplitsets(rarFiles);

		// Filter splitsets with gaps
		splitsets = FileManager.filterSplitsetsWithGaps(splitsets);

		// Filter incomplete splitsets
		splitsets = FileManager.filterSplitsetsWithIncompleteBegin(splitsets);
		splitsets = FileManager.filterSplitsetsWithIncompleteEnd(splitsets);

		return splitsets;
	}

	private ArrayList<SplitsetVo> checkPasswords(
			ArrayList<SplitsetVo> splitsetVos) {
		log.info("-------------------------------------------------");
		log.info("Performing password check.");
		log.info("-------------------------------------------------");

		ArrayList<SplitsetVo> unpackableSplitsets = new ArrayList<SplitsetVo>();
		for (SplitsetVo splitsetVo : splitsetVos) {
			if (splitsetVo.password.isEmpty()) {
				try {
					checkPassword(splitsetVo);
					unpackableSplitsets.add(splitsetVo);
				} catch (NoFittingPasswordFoundException e) {
					log.error("Archive cannot be extracted: "
							+ "No fitting password "
							+ "was found in password list.");
				}
			} else {
				unpackableSplitsets.add(splitsetVo);
			}
		}
		return unpackableSplitsets;
	}

	private void checkPassword(SplitsetVo splitsetVo)
			throws NoFittingPasswordFoundException {
		log.info("-------------------------------------------------");
		log.info("Trying to extract files, smaller as "
				+ Configuration.SMALL_FILE_THRESHOLD + " kb of: "
				+ Configuration.SOURCE_FOLDER
				+ splitsetVo.getFirstFileWithinSplitset());
		log.info("Password: " + splitsetVo.password);
		try {
			FileManager.unrarFilesByMaxFilesize(splitsetVo);
			log.info("Unrar of small files completed without error");
			log.info("-------------------------------------------------");

			FileManager.clearTempFolder();
		} catch (UnrarFailedException e) {
			log.warn("Unrar process of small files ended with error", e);
		} catch (NoFilesToExtractException e) {
			log.warn("Archive does not contain any small files... "
					+ " extracting full archive.");
		} catch (CRCFailedException e) {
			log.warn("Unrar process of small files did not complete "
					+ "successfully: Volume(s) are corrupted (CRC failed)");
		}
	}

	private boolean unrarSplitsets(ArrayList<SplitsetVo> splitsetVos) {
		if (splitsetVos.isEmpty()) {
			log.info("-------------------------------------------------");
			log.info("No files are ready for unrar.");
			log.info("-------------------------------------------------");
			return false;
		} else {
			log.info("-------------------------------------------------");
			log.info("The following files are ready for unrar:");
			for (SplitsetVo splitsetVo : splitsetVos) {
				log.info(splitsetVo.getFirstFileWithinSplitset());
			}
			log.info("-------------------------------------------------");

			boolean globalSuccess = true;
			for (SplitsetVo splitsetVo : splitsetVos) {
				String dir = FileManager.createDirectoryForSplitset(splitsetVo);

				boolean success = unrarSplitset(splitsetVo, dir);

				if (success) {
					/*
					 * If only one file or folder is extracted into dir, move it
					 * into the main target directory
					 */
					if (FileManager
							.moveSingleFileWithinDirectoryToTargetFolder(dir)) {
						/*
						 * The file or directory in dir was moved, so dir is
						 * empty and can be deleted
						 */
						FileManager.removeDirectory(dir);
					}
				} else {
					globalSuccess = false;
					// Unrar failed, dir may be empty and can be deleted
					boolean success2 = FileManager.removeDirectory(dir);
					if (!success2) {
						FileManager.markDirectoryAsIncomplete(dir);
					}
				}
			}
			return globalSuccess;
		}
	}

	private boolean unrarSplitset(SplitsetVo splitsetVo, String dir) {
		log.info("-------------------------------------------------");
		log.info("Unraring: " + Configuration.SOURCE_FOLDER
				+ splitsetVo.getFirstFileWithinSplitset());
		List<String> partFiles = null;

		log.info("Password: " + splitsetVo.password);
		try {
			partFiles = FileManager.unrarAllFiles(splitsetVo, dir);
			splitsetVo.extractedSucessfully = true;
			log.info("Unrar completed without error.");
			log.info("-------------------------------------------------");

			if (Configuration.DELETE_SOURCE_FILES_AFTER_UNRAR) {
				FileManager.deleteFiles(partFiles);
			}
			return true;
		} catch (UnrarFailedException e) {
			log.error("Unrar process ended with error", e);
			return false;
		} catch (NoFilesToExtractException e) {
			log.warn("All files in archive are already extracted.");
			return false;
		} catch (PasswordIncorrectException e) {
			log.error("Unexpected error: Examined password is incorrect "
					+ "when extracting full archive.");
			return false;
		} catch (NoFittingPasswordFoundException e) {
			log.error("Archive cannot be extracted: No fitting password "
					+ "was found in password list.");
			return false;
		} catch (CRCFailedException e) {
			log.error("Extraction did not complete successfully: Volume(s) "
					+ "are corrupted (CRC failed)");
			return false;
		}
	}
}
