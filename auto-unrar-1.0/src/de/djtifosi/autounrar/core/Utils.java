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
import java.io.IOException;

import org.apache.log4j.Logger;

import de.djtifosi.autounrar.conf.Configuration;

public class Utils {
	private StringBuilder builder = new StringBuilder();
	private String percentValue;

	public void logUnrarOutput(char c, Logger log) {
		if (c == '\n') {
			log.debug(builder.toString());
			builder = new StringBuilder();
		} else {
			builder.append(c);
		}
	}

	public void logUnrarProgressOnly(char c, Logger log) {
		if (c == '\n') {
			String string = builder.toString();
			if (string.endsWith("%")) {
				int index = string.lastIndexOf("\b\b\b\b ") + 4;
				if (index > 0) {
					String percentValue = string.substring(index);
					if (percentValue.length() <= 5
							&& !percentValue.equals(this.percentValue)) {
						log.info("Progress: " + percentValue);
						this.percentValue = percentValue;
					}
				}
			}
		} else {
			builder.append(c);
		}
	}

	public static Process execRuntimeCommand(String cmd) throws IOException {
		File file = new File(Configuration.TEMPORARY_FOLDER + "command.sh");
		file.createNewFile();
		file.setExecutable(true);
		FileOutputStream os = new FileOutputStream(file);
		os.write(cmd.getBytes());
		os.close();

		Runtime shell = Runtime.getRuntime();
		return shell.exec(Configuration.TEMPORARY_FOLDER + "command.sh");
	}

	public static String escapeCharacters(String str) {
		StringBuilder sb = new StringBuilder(str);
		if (str.contains("'")) {
			int index = sb.indexOf("'");
			sb.insert(index, (char) 92);
			return sb.toString();
		}
		if (str.contains("\"")) {
			int index = sb.indexOf("\"");
			sb.insert(index, (char) 92);
			return sb.toString();
		}
		if (str.contains(" ")) {
			int index = sb.indexOf(" ");
			sb.insert(index, (char) 92);
			return sb.toString();
		}
		return str;
	}

}
