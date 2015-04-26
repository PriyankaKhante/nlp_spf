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
package edu.uw.cs.lil.tiny.parser.ccg.rules;

import java.util.Iterator;
import java.util.List;

import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.utils.collections.ListUtils;

public class BinaryRuleSet<MR> implements Iterable<IBinaryParseRule<MR>> {
	
	private final List<IBinaryParseRule<MR>>	rules;
	
	public BinaryRuleSet(List<IBinaryParseRule<MR>> rules) {
		this.rules = rules;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		final BinaryRuleSet other = (BinaryRuleSet) obj;
		if (rules == null) {
			if (other.rules != null) {
				return false;
			}
		} else if (!rules.equals(other.rules)) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rules == null) ? 0 : rules.hashCode());
		return result;
	}
	
	@Override
	public Iterator<IBinaryParseRule<MR>> iterator() {
		return rules.iterator();
	}
	
	@Override
	public String toString() {
		return rules.toString();
	}
	
	public static class Creator<MR> implements
			IResourceObjectCreator<BinaryRuleSet<MR>> {
		
		private String	type;
		
		public Creator() {
			this("rule.set");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@Override
		public BinaryRuleSet<MR> create(Parameters params,
				final IResourceRepository repo) {
			return new BinaryRuleSet<MR>(ListUtils.map(
					params.getSplit("rules"),
					new ListUtils.Mapper<String, IBinaryParseRule<MR>>() {
						
						@Override
						public IBinaryParseRule<MR> process(String obj) {
							return repo.getResource(obj);
						}
					}));
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return ResourceUsage
					.builder(type, BinaryRuleSet.class)
					.addParam("rules", IBinaryParseRule.class,
							"Binary parse rules.").build();
		}
		
	}
	
}
