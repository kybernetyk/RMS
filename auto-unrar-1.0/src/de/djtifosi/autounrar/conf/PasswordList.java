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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class PasswordList extends ArrayList<String> {

	private static final long serialVersionUID = 7895394082555009210L;

	private static PasswordList instance = null;

	private final String path = "conf/passwordlist.txt";

	private static Logger log = Logger.getLogger(PasswordList.class);

	private PasswordList() throws IOException {
		reload();
	}


	public static PasswordList getInstance() throws IOException {
		if (instance == null) {
			instance = new PasswordList();
		}
		return instance;
	}

	public void refresh() throws IOException {
		this.clear();
		this.reload();

		log.debug("-------------------------------------------------");
		log.debug("Password list contains the following passwords:");
		for (String password : this) {
			log.debug(password);
		}
		log.debug("-------------------------------------------------");
	}

	private void reload() throws IOException {
		FileInputStream fis = null;
		fis = new FileInputStream(path);
		BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
		
		String thisLine;
		while ((thisLine = reader.readLine()) != null) {
			this.add(thisLine);
		}
		
		fis.close();
	}

	public void addPassword(String password) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(path, "rw");
		if (raf.length() > 1) {
			long lastBytePos = raf.length() - 2;
			raf.seek(lastBytePos);
			if (raf.readByte() == '\n' || raf.readByte() == '\r') {
				raf.writeBytes(password);
			} else {
				raf.write('\n');
				raf.writeBytes(password);
			}
		} else {
			raf.writeBytes(password);
		}
		raf.close();
		this.add(password);
	}
}
