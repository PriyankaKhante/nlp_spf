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
package edu.uw.cs.lil.tiny.parser.ccg.rules.coordination;

import java.util.ArrayList;
import java.util.List;

import edu.uw.cs.lil.tiny.parser.ccg.rules.BinaryRuleSet;
import edu.uw.cs.lil.tiny.parser.ccg.rules.IBinaryParseRule;

public class CoordinationRule<MR> extends BinaryRuleSet<MR> {
	
	private CoordinationRule(List<IBinaryParseRule<MR>> rules) {
		super(rules);
	}
	
	public static <MR> CoordinationRule<MR> create(
			ICoordinationServices<MR> services) {
		final List<IBinaryParseRule<MR>> rules = new ArrayList<IBinaryParseRule<MR>>(
				3);
		
		rules.add(new C1Rule<MR>(services));
		rules.add(new C2Rule<MR>(services));
		rules.add(new CXRule<MR>(services));
		
		return new CoordinationRule<MR>(rules);
	}
	
}
