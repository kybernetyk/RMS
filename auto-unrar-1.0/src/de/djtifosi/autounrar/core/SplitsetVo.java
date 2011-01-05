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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SplitsetVo {
	public HashMap<Integer, String> rarMap;
	public String password;
	public boolean extractedSucessfully = false;

	public String getFirstFileWithinSplitset() {
		return rarMap.get(getMinFileNumber());
	}

	public String getLastFileWithinSplitset() {
		return rarMap.get(getMaxFileNumber());
	}

	private int getMinFileNumber() {
		int min = 999999999;
		for (Map.Entry<Integer, String> entry : rarMap.entrySet()) {
			if (min > entry.getKey()) {
				min = entry.getKey();
			}
		}
		return min;
	}

	private int getMaxFileNumber() {
		int max = 0;
		for (Map.Entry<Integer, String> entry : rarMap.entrySet()) {
			if (max < entry.getKey()) {
				max = entry.getKey();
			}
		}
		return max;
	}

	public String getSplitsetName() {
		String firstFileWithinSplitset = getFirstFileWithinSplitset();
		String lowerFirstFile = firstFileWithinSplitset.toLowerCase();

		// Extract substring before .r**
		int endIndex = lowerFirstFile.lastIndexOf(".r");
		if (endIndex != -1) {
			String substring = firstFileWithinSplitset.substring(0, endIndex);
			String lowerSubstring = lowerFirstFile.substring(0, endIndex);

			// Extract substring pefore .part01, .part001, etc.
			endIndex = lowerSubstring.lastIndexOf(".part");
			if (endIndex != -1) {
				substring = substring.substring(0, endIndex);
			}

			return substring;
		} else {
			if (firstFileWithinSplitset.isEmpty()) {
				return "";
			} else {
				return firstFileWithinSplitset;
			}
		}
	}

	public ArrayList<Integer> getMissingVolumes() {
		ArrayList<Integer> missingVolumes = new ArrayList<Integer>();
		for (int i = getMinFileNumber(); i < getMaxFileNumber(); i++) {
			if (!rarMap.containsKey(i)) {
				missingVolumes.add(i - getMinFileNumber() + 1);
			}
		}
		return missingVolumes;
	}

}
