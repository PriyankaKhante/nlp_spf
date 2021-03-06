/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework
 * <p>
 * Copyright (C) 2013 Yoav Artzi
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 ******************************************************************************/
package edu.uw.cs.lil.tiny.mr.lambda.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.base.LispReader;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;

public class ConstantsReader {
	private ConstantsReader() {
		// Nothing to do. Private ctor.
	}
	
	public static List<LogicalConstant> readConstantsFile(File file)
			throws IOException {
		// First, strip the comments and prepare a clean LISP string to
		// parse
		final BufferedReader reader = new BufferedReader(new FileReader(file));
		final StringBuilder strippedFile = new StringBuilder();
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				line = line.split("\\s*//")[0];
				if (!line.equals("")) {
					strippedFile.append(line).append(" ");
				}
			}
		} finally {
			reader.close();
		}
		
		// Read the constants
		final List<LogicalConstant> constants = new LinkedList<LogicalConstant>();
		final LispReader lispReader = new LispReader(new StringReader(
				strippedFile.toString()));
		while (lispReader.hasNext()) {
			final LogicalConstant exp = LogicalConstant
					.read(lispReader.next());
			constants.add(exp);
		}
		
		return constants;
		
	}
	
}
