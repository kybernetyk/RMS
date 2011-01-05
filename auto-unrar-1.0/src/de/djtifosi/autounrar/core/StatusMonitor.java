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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import de.djtifosi.autounrar.conf.Configuration;
import de.djtifosi.autounrar.exceptions.AnotherProcessIsRunningException;
import de.djtifosi.autounrar.exceptions.StatusMonitorException;

public abstract class StatusMonitor {
	private static File statusFile = new File(
			Configuration.STATUS_MONITOR_FOLDER + "status.info");

	private static ProcessStatus status;
	private static Logger log = Logger.getLogger(StatusMonitor.class);

	private enum ProcessStatus {
		RUNNING("running"), ENDED("ended"), PROCESSING_JOB("processing job");

		private String name;

		private ProcessStatus(String name) {
			this.name = name;
		}

		public static ProcessStatus getProcessStatusByName(String name) {
			for (ProcessStatus status : ProcessStatus.values()) {
				if (status.getName().equals(name)) {
					return status;
				}
			}
			return null;
		}

		public String getName() {
			return name;
		}
	}

	public synchronized static void afterStart() throws StatusMonitorException,
			AnotherProcessIsRunningException {
		try {
			if (Configuration.ACTIVATE_STATUS_MONITOR) {
				if (isAnotherProcessRunning()) {
					throw new AnotherProcessIsRunningException();
				}
				status = ProcessStatus.RUNNING;
				writeStatus(status.getName());
			}
		} catch (IOException e) {
			throw new StatusMonitorException(e);
		}
		if (Configuration.ACTIVATE_INTERCEPTOR_SCRIPT) {
			executeScript("interceptors/afterstart.sh");
		}
	}

	public synchronized static void beforeEnd() throws StatusMonitorException {
		if (Configuration.ACTIVATE_INTERCEPTOR_SCRIPT) {
			executeScript("interceptors/beforeend.sh");
		}
		try {
			if (Configuration.ACTIVATE_STATUS_MONITOR) {
				status = ProcessStatus.ENDED;
				writeStatus(status.getName());
			}
		} catch (IOException e) {
			throw new StatusMonitorException(e);
		} finally {
			FileManager.clearTempFolder();
		}
	}

	public synchronized static void afterJobStart()
			throws StatusMonitorException {
		if (Configuration.ACTIVATE_INTERCEPTOR_SCRIPT) {
			executeScript("interceptors/afterjobstart.sh");
		}
		try {
			if (Configuration.ACTIVATE_STATUS_MONITOR) {
				status = ProcessStatus.PROCESSING_JOB;
				writeStatus(status.getName());
			}
		} catch (IOException e) {
			throw new StatusMonitorException(e);
		}
	}

	public synchronized static void beforeJobEnd(
			ArrayList<SplitsetVo> splitsetVos) throws StatusMonitorException {
		try {
			if (Configuration.ACTIVATE_STATUS_MONITOR) {
				status = ProcessStatus.RUNNING;
				writeStatus(status.getName());
			}
		} catch (IOException e) {
			throw new StatusMonitorException(e);
		}
		if (Configuration.ACTIVATE_INTERCEPTOR_SCRIPT) {
			StringBuilder successfulExtractions = new StringBuilder()
					.append("'");
			int numberOfSucessfulExtractions = 0;
			StringBuilder errors = new StringBuilder().append("'");
			int numberOfErrors = 0;
			if (splitsetVos != null) {
				for (SplitsetVo splitsetVo : splitsetVos) {
					if (splitsetVo.extractedSucessfully) {
						successfulExtractions.append(
								splitsetVo.getSplitsetName()).append("\n");
						numberOfSucessfulExtractions++;
					} else {
						errors.append(splitsetVo.getSplitsetName())
								.append("\n");
						numberOfErrors++;
					}
				}
			}
			successfulExtractions.append("'");
			errors.append("'");
			executeScript("interceptors/beforejobend.sh "
					+ numberOfSucessfulExtractions + " " + numberOfErrors + " "
					+ successfulExtractions + " " + errors);
		}
	}

	private static boolean isAnotherProcessRunning() throws IOException {
		if (statusFile.exists()) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(statusFile)));
			String statusName = reader.readLine();
			ProcessStatus processStatus = ProcessStatus
					.getProcessStatusByName(statusName);
			if (processStatus == ProcessStatus.RUNNING
					|| processStatus == ProcessStatus.PROCESSING_JOB) {
				return true;
			}
		}
		return false;
	}

	private static void writeStatus(String status) throws IOException {
		if (!statusFile.exists()) {
			statusFile.createNewFile();
		}

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(statusFile)));
		writer.write(status);
		writer.close();
	}

	private static void executeScript(String scriptPath) {
		log.debug("Executing script: " + scriptPath);
		try {
			Process prozess = Utils.execRuntimeCommand(scriptPath);

			InputStreamHandler errStreamHandler = new InputStreamHandler(
					"Error Stream", prozess.getErrorStream());
			errStreamHandler.start();
			InputStreamHandler inStreamHandler = new InputStreamHandler(
					"Input Stream", prozess.getInputStream());
			inStreamHandler.start();

			errStreamHandler.join();
			inStreamHandler.join();
		} catch (IOException e) {
			log.warn("Error executing script" + scriptPath, e);
		} catch (InterruptedException e) {
			log.warn("Error executing script" + scriptPath, e);
		}
	}
}
