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
package edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.composition;

import java.util.ArrayList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.parser.ccg.rules.BinaryRuleSet;
import edu.uw.cs.lil.tiny.parser.ccg.rules.IBinaryParseRule;

public class CompositionCreator<MR> implements
		IResourceObjectCreator<BinaryRuleSet<MR>> {
	
	private String	type;
	
	public CompositionCreator() {
		this("rule.composition");
	}
	
	public CompositionCreator(String type) {
		this.type = type;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public BinaryRuleSet<MR> create(Parameters params, IResourceRepository repo) {
		int maxOrder = params.getAsInteger("maxOrder", 0);
		final boolean crossing = params.getAsBoolean("crossing", false);
		final List<IBinaryParseRule<MR>> rules = new ArrayList<IBinaryParseRule<MR>>(
				(maxOrder + 1) * 2 + (crossing ? 2 * (maxOrder + 1) : 0));
		
		while (maxOrder >= 0) {
			rules.add(new ForwardComposition<MR>(
					(ICategoryServices<MR>) repo
							.getResource(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
					maxOrder));
			rules.add(new BackwardComposition<MR>(
					(ICategoryServices<MR>) repo
							.getResource(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
					maxOrder));
			--maxOrder;
		}
		
		return new BinaryRuleSet<MR>(rules);
	}
	
	@Override
	public String type() {
		return type;
	}
	
	@Override
	public ResourceUsage usage() {
		return ResourceUsage.builder(type, AbstractComposition.class).build();
	}
	
}
