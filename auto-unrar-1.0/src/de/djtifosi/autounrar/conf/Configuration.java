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
package de.djtifosi.autounrar.conf;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

import de.djtifosi.autounrar.exceptions.ConfigurationException;

public class Configuration {

	public static String UNRAR_APPLICATION_FOLDER = "/opt/bin/";
	public static String SOURCE_FOLDER = "/home/Public/Fritzload/";
	public static String TARGET_FOLDER = "/home/Public/Fritzload/Extracted/";
	public static String STATUS_MONITOR_FOLDER = "/home/sysadmin/auto-unrar/log/";
	public static String TEMPORARY_FOLDER = "/home/sysadmin/auto-unrar/tmp/";
	public static int SOCKETSERVER_PORT = 4444;
	public static boolean DELETE_SOURCE_FILES_AFTER_UNRAR = false;
	public static boolean ACTIVATE_STATUS_MONITOR = false;
	public static int SMALL_FILE_THRESHOLD = 1024;
	public static boolean ACTIVATE_RECURSIVE_EXTRACTION = false;
	public static boolean ACTIVATE_DEEP_RECURSIVE_EXTRACTION = false;
	public static boolean ACTIVATE_INTERCEPTOR_SCRIPT = false;

	private static final Logger log = Logger.getLogger(Configuration.class);

	public Configuration() throws ConfigurationException {
		loadProperties();
		logProperties();
	}

	private void loadProperties() throws ConfigurationException {
		try {
			Properties properties = new Properties();
			FileInputStream stream;
			stream = new FileInputStream("conf/configuration.properties");
			properties.load(stream);
			stream.close();
			UNRAR_APPLICATION_FOLDER = properties
					.getProperty("UNRAR_APPLICATION_FOLDER");
			SOURCE_FOLDER = properties.getProperty("SOURCE_FOLDER");
			TARGET_FOLDER = properties.getProperty("TARGET_FOLDER");
			STATUS_MONITOR_FOLDER = properties
					.getProperty("STATUS_MONITOR_FOLDER");
			TEMPORARY_FOLDER = properties.getProperty("TEMPORARY_FOLDER");
			SOCKETSERVER_PORT = Integer.valueOf(properties
					.getProperty("SOCKETSERVER_PORT"));
			DELETE_SOURCE_FILES_AFTER_UNRAR = Boolean.valueOf(properties
					.getProperty("DELETE_SOURCE_FILES_AFTER_UNRAR"));
			ACTIVATE_STATUS_MONITOR = Boolean.valueOf(properties
					.getProperty("ACTIVATE_STATUS_MONITOR"));
			SMALL_FILE_THRESHOLD = Integer.valueOf(properties
					.getProperty("SMALL_FILE_THRESHOLD"));
			ACTIVATE_RECURSIVE_EXTRACTION = Boolean.valueOf(properties
					.getProperty("ACTIVATE_RECURSIVE_EXTRACTION"));
			ACTIVATE_DEEP_RECURSIVE_EXTRACTION = Boolean.valueOf(properties
					.getProperty("ACTIVATE_DEEP_RECURSIVE_EXTRACTION"));
			ACTIVATE_INTERCEPTOR_SCRIPT = Boolean.valueOf(properties
					.getProperty("ACTIVATE_INTERCEPTOR_SCRIPT"));
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}

	private void logProperties() {
		log.info("-------------------------------------------------");
		log.info("Configuration settings:");
		log.info("UNRAR_APPLICATION_FOLDER = " + UNRAR_APPLICATION_FOLDER);
		log.info("SOURCE_FOLDER = " + SOURCE_FOLDER);
		log.info("TARGET_FOLDER = " + TARGET_FOLDER);
		log.info("STATUS_MONITOR_FOLDER = " + STATUS_MONITOR_FOLDER);
		log.info("TEMPORARY_FOLDER = " + TEMPORARY_FOLDER);
		log.info("SOCKETSERVER_PORT = " + SOCKETSERVER_PORT);
		log.info("DELETE_SOURCE_FILES_AFTER_UNRAR = "
				+ DELETE_SOURCE_FILES_AFTER_UNRAR);
		log.info("ACTIVATE_STATUS_MONITOR = " + ACTIVATE_STATUS_MONITOR);
		log.info("SMALL_FILE_THRESHOLD = " + SMALL_FILE_THRESHOLD);
		log.info("ACTIVATE_RECURSIVE_EXTRACTION = "
				+ ACTIVATE_RECURSIVE_EXTRACTION);
		log.info("ACTIVATE_DEEP_RECURSIVE_EXTRACTION = "
				+ ACTIVATE_DEEP_RECURSIVE_EXTRACTION);
		log.info("ACTIVATE_INTERCEPTOR_SCRIPT = " + ACTIVATE_INTERCEPTOR_SCRIPT);
		log.info("-------------------------------------------------");
	}
}
