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
package de.djtifosi.autounrar.start;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import de.djtifosi.autounrar.conf.Configuration;
import de.djtifosi.autounrar.conf.Constants;
import de.djtifosi.autounrar.core.ProcessOrchestrator;
import de.djtifosi.autounrar.core.StatusMonitor;
import de.djtifosi.autounrar.core.socket.UnrarSocketServer;
import de.djtifosi.autounrar.exceptions.AnotherProcessIsRunningException;
import de.djtifosi.autounrar.exceptions.ConfigurationException;
import de.djtifosi.autounrar.exceptions.MissingOrInvalidArgumentException;
import de.djtifosi.autounrar.exceptions.SoecketServerException;
import de.djtifosi.autounrar.exceptions.StatusMonitorException;

public class AutoUnrarStarter {

	private static Logger log = Logger.getLogger(AutoUnrarStarter.class);

	private enum ModeType {
		LOCAL("-l"), REMOTE("-n");
		private String arg;

		private ModeType(String arg) {
			this.arg = arg;
		}

		public static ModeType getModeTypeByArg(String arg) {
			for (ModeType modeType : ModeType.values()) {
				if (modeType.arg.equals(arg)) {
					return modeType;
				}
			}
			return null;
		}
	}

	public static void main(String[] args) {
		printDisclaimer();
		try {
			ModeType modeType = extractModeType(args);

			if (modeType != null) {
				initializeLogger();
				logVersion();
				loadConfig();
				StatusMonitor.afterStart();
				startProcess(modeType);
			}
		} catch (Exception e) {
			handleFatalExceptions(e);
		} finally {
			try {
				StatusMonitor.beforeEnd();
			} catch (Exception e) {
				handleFatalExceptions(e);
			}
		}

		log.info("Exiting application.");
		System.exit(Constants.RETURN_CODE_NO_ERRORS);
	}

	private static void logVersion() {
		log.info("-------------------------------------------------");
		log.info("Starting Auto UnRar, Version " + Constants.AUTO_UNRAR_VERSION
				+ "  Copyright (C) 2010  djtifosi");
		log.info("-------------------------------------------------");

	}

	private static void printDisclaimer() {
		System.out.println("");
		System.out.println("Auto UnRar, Version "
				+ Constants.AUTO_UNRAR_VERSION
				+ "  Copyright (C) 2010  djtifosi");
		System.out.println("");
		System.out.println("This program comes with ABSOLUTELY NO WARRANTY.");
		System.out.println("This is free software, and you are welcome to "
				+ "redistribute it ");
		System.out.println("under certain conditions; see "
				+ "<http://www.gnu.org/licenses/> for details.");
		System.out.println("");
		System.out.println("Updates of this program are available on the "
				+ "Auto UnRar project page at ");
		System.out.println("<http://sourceforge.net/projects/auto-unrar/>.");
		System.out.println("Please report bugs using the Sourceforge "
				+ "bugs tracker.");
		System.out.println("");

	}

	private static ModeType extractModeType(String[] args)
			throws MissingOrInvalidArgumentException {
		ModeType modeType = null;

		if (args != null && args.length == 1) {
			modeType = ModeType.getModeTypeByArg(args[0]);
		}

		if (modeType != null) {
			return modeType;
		} else {
			throw new MissingOrInvalidArgumentException(args.toString());
		}
	}

	private static void initializeLogger() {
		DOMConfigurator.configureAndWatch("conf/log4j.xml");
	}

	private static void loadConfig() {
		try {
			log.debug("Reading configuration from "
					+ "conf/configuration.properties");
			new Configuration();
		} catch (ConfigurationException e) {
			log.warn("Error reading conf/configuration.properties file, "
					+ "proceeding with default settings.");
		}
	}

	private static void startProcess(ModeType modeType)
			throws SoecketServerException, StatusMonitorException {
		if (modeType == ModeType.REMOTE) {
			new UnrarSocketServer();
		} else if (modeType == ModeType.LOCAL) {
			new ProcessOrchestrator().startProcess();
		}
	}

	private static void handleFatalExceptions(Exception e) {
		if (e instanceof MissingOrInvalidArgumentException) {
			System.err.println("Missing or invalid argument. "
					+ "Valid arguemnts are:");
			System.err.println("");
			System.err.println("-l  Local mode \t  Auto UnRar will start an "
					+ "unrar process immediately ");
			System.err.println("\t\t  and then terminate.");
			System.err.println("-n  Network mode  Auto UnRar will run in "
					+ "the background and wait ");
			System.err.println("\t\t  for an incomming remote command, "
					+ "before starting ");
			System.err.println("\t\t  an unrar process.");
			System.exit(Constants.RETURN_CODE_STATUS_INVALID_ARGUMENTS);
		} else if (e instanceof AnotherProcessIsRunningException) {
			System.err.println("FATAL: Another Auto Unrar process is running, "
					+ "exiting.");
			e.printStackTrace();
			System.exit(Constants.RETURN_CODE_ANOTHER_PROCESS_IS_RUNNING);
		} else if (e instanceof StatusMonitorException) {
			System.err.println("FATAL: Could not access status monitor file "
					+ "log/status.info, exiting.");
			e.printStackTrace();
			log.fatal("Could not access status monitor file log/status.info, "
					+ "exiting.");
			System.exit(Constants.RETURN_CODE_STATUS_MONITOR_ERROR);
		} else if (e instanceof SoecketServerException) {
			System.err.print("FATAL: Error establishing socket server, "
					+ "exiting.");
			e.printStackTrace();
			log.fatal("Error establishing socket server, exiting.", e);
			System.exit(Constants.RETURN_CODE_SOCKET_SERVER_ERROR);
		} else if (e instanceof RuntimeException) {
			System.err.print("Unexpected Error during execution occurred, "
					+ "exiting.");
			e.printStackTrace();
			log.fatal("FATAL: Unexpected Error during execution occurred, "
					+ "exiting.", e);
			System.exit(Constants.RETURN_CODE_UNEXPECTED_ERROR);
		}
	}
}
