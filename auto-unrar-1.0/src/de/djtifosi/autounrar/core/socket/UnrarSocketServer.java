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
package de.djtifosi.autounrar.core.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

import de.djtifosi.autounrar.conf.Configuration;
import de.djtifosi.autounrar.conf.PasswordList;
import de.djtifosi.autounrar.core.ProcessOrchestrator;
import de.djtifosi.autounrar.exceptions.SoecketServerException;
import de.djtifosi.autounrar.exceptions.StatusMonitorException;
import de.djtifosi.autounrar.start.AutoUnrarStarter;

public class UnrarSocketServer {

	private static Logger log = Logger.getLogger(AutoUnrarStarter.class);

	public UnrarSocketServer() throws SoecketServerException,
			StatusMonitorException {

		try {
			String command = null;
			do {
				ServerSocket serverSocket = new ServerSocket(
						Configuration.SOCKETSERVER_PORT);
				log.info("-------------------------------------------------");
				log
						.info("Socket server is listening for incomming connections on port "
								+ Configuration.SOCKETSERVER_PORT);
				log.info("-------------------------------------------------");
				// Blocks until client connection
				Socket socket = serverSocket.accept();

				log.info("Incomming socket connection");

				BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));

				command = bufferedReader.readLine();
				socket.close();
				serverSocket.close();

				log.info("Received command: " + command);

				if (command != null) {
					handleCommand(command);
				}
			} while (!command.equals(SocketCommand.EXIT.getText()));
		} catch (IOException e) {
			throw new SoecketServerException(e);
		}
	}

	private void handleCommand(String text) throws StatusMonitorException {
		SocketCommand socketCommand = SocketCommand.getSocketCommandByText(text
				.split(" ")[0]);
		if (socketCommand != null) {
			switch (socketCommand) {
			case UNRAR:
				new ProcessOrchestrator().startProcess();
				break;
			case ADD_PW:
				String password = text.substring(text.indexOf(" ") + 1);
				log.info("Adding password: " + password + " to list");
				try {
					PasswordList.getInstance().addPassword(password);
				} catch (IOException e) {
					log.error(
							"Error adding password: " + password + " to list",
							e);
					e.printStackTrace();
				}
				break;
			}
		} else {
			log.error("Didn't understand command " + text);
		}
	}
}
