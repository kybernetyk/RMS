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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

public class InputStreamHandler extends Thread {
	private InputStream in;
	private ByteArrayOutputStream out = new ByteArrayOutputStream();
	private Utils utils = new Utils();
	private static Logger log = Logger.getLogger(InputStreamHandler.class);

	public InputStreamHandler(String name, InputStream in) {
		super(name);
		this.in = in;
	}

	@Override
	public void run() {
		int c;
		try {
			while ((c = in.read()) != -1) {
				out.write(c);
				if (log.isDebugEnabled()) {
					utils.logUnrarOutput((char) c, log);
				} else if(log.isInfoEnabled()){
					utils.logUnrarProgressOnly((char) c, log);
				}
			}
			in.close();
			out.close();
		} catch (IOException e) {
			log.error("Error reading input stream.");
		}
	}

	public String getInputAsString() {
		return out.toString();
	}
}
