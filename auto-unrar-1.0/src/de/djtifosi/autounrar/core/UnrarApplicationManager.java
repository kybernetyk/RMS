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
import java.util.List;

import org.apache.log4j.Logger;

import de.djtifosi.autounrar.conf.Configuration;
import de.djtifosi.autounrar.exceptions.CRCFailedException;
import de.djtifosi.autounrar.exceptions.NoFilesToExtractException;
import de.djtifosi.autounrar.exceptions.PasswordIncorrectException;
import de.djtifosi.autounrar.exceptions.UnrarFailedException;

public class UnrarApplicationManager {

	private static Logger log = Logger.getLogger(UnrarApplicationManager.class);

	public List<String> unrarAllFiles(String fileName, String targetPath,
			String password) throws UnrarFailedException,
			PasswordIncorrectException, NoFilesToExtractException,
			CRCFailedException {
		Process prozess = null;
		if (password.isEmpty()) {
			password = "-";
		} else {
			password = Utils.escapeCharacters(password);
		}
		String cmd = String.format(Configuration.UNRAR_APPLICATION_FOLDER
				+ "unrar x -p%s -o- '%s' '%s'", password, fileName, targetPath);
		log.debug("Executing command: " + cmd);

		try {
			prozess = Utils.execRuntimeCommand(cmd);

			InputStreamHandler errStreamHandler = new InputStreamHandler(
					"Error Stream", prozess.getErrorStream());
			errStreamHandler.start();
			InputStreamHandler inStreamHandler = new InputStreamHandler(
					"Input Stream", prozess.getInputStream());
			inStreamHandler.start();

			errStreamHandler.join();
			inStreamHandler.join();

			String input = inStreamHandler.getInputAsString();
			String error = errStreamHandler.getInputAsString();

			if (error.contains("password incorrect")) {
				throw new PasswordIncorrectException();
			} else if (input.contains("No files to extract")) {
				throw new NoFilesToExtractException();
			} else if (error.contains("CRC failed")) {
				throw new CRCFailedException(error);
			} else if (!input.contains("All OK")) {
				log.error("Error Stream:" + error);
				throw new UnrarFailedException();
			}

			String[] split = input.split("\n");
			List<String> partFileNames = new ArrayList<String>();
			for (String string2 : split) {
				if (string2.startsWith("Extracting from ")) {
					string2 = string2.replace("Extracting from ", "");
					partFileNames.add(string2);
				}
			}

			return partFileNames;

		} catch (IOException e) {
			log.error("Error executing shell command", e);
			throw new UnrarFailedException();
		} catch (InterruptedException e) {
			log.error("Error executing shell command", e);
			throw new UnrarFailedException();
		}
	}

	public List<String> unrarFilesByMaxFilesize(String fileName,
			String targetPath, String password, long fileSize)
			throws UnrarFailedException, PasswordIncorrectException,
			NoFilesToExtractException, CRCFailedException {
		Process prozess = null;
		if (password.isEmpty()) {
			password = "-";
		} else {
			password = Utils.escapeCharacters(password);
		}
		String cmd = String.format(Configuration.UNRAR_APPLICATION_FOLDER
				+ "unrar x -p%s -o- -sl%s '%s' '%s'", password,
				fileSize * 1024, fileName, targetPath);
		log.debug("Executing command: " + cmd);

		try {
			prozess = Utils.execRuntimeCommand(cmd);

			InputStreamHandler errStreamHandler = new InputStreamHandler(
					"Error Stream", prozess.getErrorStream());
			errStreamHandler.start();
			InputStreamHandler inStreamHandler = new InputStreamHandler(
					"Input Stream", prozess.getInputStream());
			inStreamHandler.start();

			errStreamHandler.join();
			inStreamHandler.join();

			String input = inStreamHandler.getInputAsString();
			String error = errStreamHandler.getInputAsString();

			if (error.contains("password incorrect")) {
				throw new PasswordIncorrectException();
			} else if (input.contains("No files to extract")) {
				throw new NoFilesToExtractException();
			} else if (error.contains("CRC failed")) {
				throw new CRCFailedException(error);
			} else if (!input.contains("All OK")) {
				log.error("Error Stream:" + error);
				throw new UnrarFailedException();
			}

			String[] split = input.split("\n");
			List<String> unraredFileNames = new ArrayList<String>();
			for (String string2 : split) {
				if (string2.startsWith("Extracting  ")) {
					string2 = string2.replace("Extracting  ", "");
					String[] split2 = string2.split("\b");
					String string3 = split2[0].trim();
					unraredFileNames.add(string3);
				}
			}

			return unraredFileNames;

		} catch (IOException e) {
			log.error("Error executing shell command", e);
			throw new UnrarFailedException();
		} catch (InterruptedException e) {
			log.error("Error executing shell command", e);
			throw new UnrarFailedException();
		}
	}

	public boolean hasFollowerPart(String filename, String password)
			throws PasswordIncorrectException {
		Process prozess = null;
		if (password.isEmpty()) {
			password = "-";
		} else {
			password = Utils.escapeCharacters(password);
		}
		String cmd = String.format(Configuration.UNRAR_APPLICATION_FOLDER
				+ "unrar vt -p%s '%s'", password, filename);
		log.debug("Executing command: " + cmd);

		try {
			prozess = Utils.execRuntimeCommand(cmd);
			InputStreamHandler errStreamHandler = new InputStreamHandler(
					"Error Stream", prozess.getErrorStream());
			errStreamHandler.start();
			InputStreamHandler inStreamHandler = new InputStreamHandler(
					"Input Stream", prozess.getInputStream());
			inStreamHandler.start();

			errStreamHandler.join();
			inStreamHandler.join();

			if (errStreamHandler.getInputAsString().contains(
					"password incorrect")) {
				throw new PasswordIncorrectException();
			}

			String input = inStreamHandler.getInputAsString().toString();

			if (input.contains("->")) {
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			log.error("Error executing shell command", e);
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			log.error("Error executing shell command", e);
			throw new RuntimeException(e);
		}
	}
	
	public boolean hasPredecessorPart(String filename, String password)
	throws PasswordIncorrectException {
		Process prozess = null;
		if (password.isEmpty()) {
			password = "-";
		} else {
			password = Utils.escapeCharacters(password);
		}
		String cmd = String.format(Configuration.UNRAR_APPLICATION_FOLDER
				+ "unrar vt -p%s '%s'", password, filename);
		log.debug("Executing command: " + cmd);

		try {
			prozess = Utils.execRuntimeCommand(cmd);
			InputStreamHandler errStreamHandler = new InputStreamHandler(
					"Error Stream", prozess.getErrorStream());
			errStreamHandler.start();
			InputStreamHandler inStreamHandler = new InputStreamHandler(
					"Input Stream", prozess.getInputStream());
			inStreamHandler.start();

			errStreamHandler.join();
			inStreamHandler.join();

			if (errStreamHandler.getInputAsString().contains(
					"password incorrect")) {
				throw new PasswordIncorrectException();
			}

			String input = inStreamHandler.getInputAsString().toString();

			if (input.contains("<-")) {
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			log.error("Error executing shell command", e);
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			log.error("Error executing shell command", e);
			throw new RuntimeException(e);
		}
	}
}
