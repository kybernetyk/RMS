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

public abstract class Constants {

	public static final String AUTO_UNRAR_VERSION = "1.0 (stable)";
	
	public static final int RETURN_CODE_NO_ERRORS = 0;
	
	public static final int RETURN_CODE_UNEXPECTED_ERROR = -1;

	public static final int RETURN_CODE_ANOTHER_PROCESS_IS_RUNNING = -2;
	
	public static final int RETURN_CODE_STATUS_INVALID_ARGUMENTS = -3;
	
	public static final int RETURN_CODE_STATUS_MONITOR_ERROR = -4;
	
	public static final int RETURN_CODE_SOCKET_SERVER_ERROR = -5;
	
}
