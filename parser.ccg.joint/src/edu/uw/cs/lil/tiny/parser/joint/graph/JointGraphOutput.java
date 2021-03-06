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
package edu.uw.cs.lil.tiny.parser.joint.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.base.hashvector.HashVectorUtils;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.parser.graph.IGraphDerivation;
import edu.uw.cs.lil.tiny.parser.graph.IGraphParserOutput;
import edu.uw.cs.lil.tiny.parser.joint.AbstractJointOutput;
import edu.uw.cs.lil.tiny.parser.joint.IEvaluation;
import edu.uw.cs.utils.collections.IScorer;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.filter.FilterUtils;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.math.LogSumExp;

/**
 * Joint graph-based inference output. Doesn't support fancy dynamic programming
 * for semantics evaluation.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Semantics formal meaning representation.
 * @param <ERESULT>
 *            Semantics evaluation result.
 */
public class JointGraphOutput<MR, ERESULT> extends
		AbstractJointOutput<MR, ERESULT, JointGraphDerivation<MR, ERESULT>>
		implements IJointGraphOutput<MR, ERESULT> {
	
	private final IGraphParserOutput<MR>	baseOutput;
	
	public JointGraphOutput(IGraphParserOutput<MR> baseOutput,
			long inferenceTime,
			List<JointGraphDerivation<MR, ERESULT>> derivations,
			List<JointGraphDerivation<MR, ERESULT>> maxDerivations,
			boolean exactEvaluation) {
		super(baseOutput, inferenceTime, derivations, maxDerivations,
				exactEvaluation && baseOutput.isExact());
		this.baseOutput = baseOutput;
	}
	
	@Override
	public IGraphParserOutput<MR> getBaseParserOutput() {
		return baseOutput;
	}
	
	@Override
	public IHashVector logExpectedFeatures() {
		return logExpectedFeatures(FilterUtils.<ERESULT> stubTrue());
	}
	
	@Override
	public IHashVector logExpectedFeatures(IFilter<ERESULT> filter) {
		// Init derivations outside scores. In practice, prune the joint
		// derivation using the filter and implicitly give each an outside score
		// of log(1.0).
		final List<JointGraphDerivation<MR, ERESULT>> derivationsToUse = new LinkedList<JointGraphDerivation<MR, ERESULT>>();
		for (final JointGraphDerivation<MR, ERESULT> derivation : derivations) {
			if (filter.isValid(derivation.getResult())) {
				derivationsToUse.add(derivation);
			}
		}
		
		// To propagate the outside scores into the graph of the base
		// output, we create a scorer that uses the outside scores of the joint
		// derivations.
		
		// Preparing a mapping of logical forms to their initial outside scores.
		// These scores are summation over all execution root (cells) of the
		// execution step score times the outside score of that cell. Iterate
		// over result cells. For each cell, first init result cells outside
		// scores. Then iterate over all parses and sum the outside
		// contribution.
		final Map<MR, Double> initBaseParseOutsideScores = new HashMap<MR, Double>();
		for (final JointGraphDerivation<MR, ERESULT> derivation : derivationsToUse) {
			for (final Pair<IGraphDerivation<MR>, IEvaluation<ERESULT>> pair : derivation
					.getInferencePairs()) {
				final MR semantics = pair.first().getSemantics();
				// The log outside contribution is the current outside score of
				// the derivation (implicitly log(1.0) = 0.0) plus the log score
				// of the evaluation.
				final double logOutsideContribution = pair.second().getScore();
				if (initBaseParseOutsideScores.containsKey(semantics)) {
					initBaseParseOutsideScores.put(semantics, LogSumExp.of(
							initBaseParseOutsideScores.get(semantics),
							logOutsideContribution));
				} else {
					initBaseParseOutsideScores.put(semantics,
							logOutsideContribution);
				}
			}
		}
		
		// Create the scorer.
		final IScorer<MR> scorer = new IScorer<MR>() {
			@Override
			public double score(MR e) {
				// If the MR was executed and has a joint parse, it has an
				// outside score, so use it, otherwise, consider as if the score
				// of the execution is -\inf --> return 0.
				return initBaseParseOutsideScores.containsKey(e) ? initBaseParseOutsideScores
						.get(e) : Double.NEGATIVE_INFINITY;
			}
		};
		
		// Get expected features from base parser output.
		final IHashVector logExpectedFeatures = baseOutput
				.logExpectedFeatures(scorer);
		
		// Add expected features from the execution result cells.
		for (final JointGraphDerivation<MR, ERESULT> derivation : derivationsToUse) {
			for (final Pair<IGraphDerivation<MR>, IEvaluation<ERESULT>> pair : derivation
					.getInferencePairs()) {
				// Explicitly adding 0.0 here to account for the outside
				// score of the evaluation, which is implicitly log(1.0) = 0.0
				// (see above).
				final double logWeight = pair.second().getScore()
						+ pair.first().getLogInsideScore() + 0.0;
				HashVectorUtils.logSumExpAdd(logWeight, pair.second()
						.getFeatures(), logExpectedFeatures);
			}
		}
		
		return logExpectedFeatures;
	}
	
	@Override
	public double logNorm() {
		return logNorm(FilterUtils.<ERESULT> stubTrue());
	}
	
	@Override
	public double logNorm(IFilter<ERESULT> filter) {
		final List<Double> logInsideScores = new ArrayList<Double>(
				derivations.size());
		for (final JointGraphDerivation<MR, ERESULT> derivation : derivations) {
			// Test the result with the filter.
			if (filter.isValid(derivation.getResult())) {
				logInsideScores.add(derivation.getLogInsideScore());
			}
		}
		// Do the log-sum-exp trick and return the log of the sum.
		return LogSumExp.of(logInsideScores);
	}
	
	public static class Builder<MR, ERESULT> {
		
		private final IGraphParserOutput<MR>							baseOutput;
		private boolean													exactEvaluation	= false;
		private final List<Pair<IGraphDerivation<MR>, IEvaluation<ERESULT>>>	inferencePairs	= new LinkedList<Pair<IGraphDerivation<MR>, IEvaluation<ERESULT>>>();
		private final long												inferenceTime;
		
		public Builder(IGraphParserOutput<MR> baseOutput, long inferenceTime) {
			this.baseOutput = baseOutput;
			this.inferenceTime = inferenceTime;
		}
		
		public Builder<MR, ERESULT> addInferencePair(
				Pair<IGraphDerivation<MR>, IEvaluation<ERESULT>> pair) {
			inferencePairs.add(pair);
			return this;
		}
		
		public Builder<MR, ERESULT> addInferencePairs(
				List<Pair<IGraphDerivation<MR>, IEvaluation<ERESULT>>> pairs) {
			inferencePairs.addAll(pairs);
			return this;
		}
		
		public JointGraphOutput<MR, ERESULT> build() {
			final Map<ERESULT, JointGraphDerivation.Builder<MR, ERESULT>> builders = new HashMap<ERESULT, JointGraphDerivation.Builder<MR, ERESULT>>();
			for (final Pair<IGraphDerivation<MR>, IEvaluation<ERESULT>> pair : inferencePairs) {
				final ERESULT pairResult = pair.second().getResult();
				if (!builders.containsKey(pairResult)) {
					builders.put(pairResult,
							new JointGraphDerivation.Builder<MR, ERESULT>(
									pairResult));
				}
				builders.get(pairResult).addInferencePair(pair);
			}
			// Create all derivations.
			final List<JointGraphDerivation<MR, ERESULT>> derivations = Collections
					.unmodifiableList(ListUtils.map(
							builders.values(),
							new ListUtils.Mapper<JointGraphDerivation.Builder<MR, ERESULT>, JointGraphDerivation<MR, ERESULT>>() {
								@Override
								public JointGraphDerivation<MR, ERESULT> process(
										JointGraphDerivation.Builder<MR, ERESULT> obj) {
									return obj.build();
								}
							}));
			// Get max derivations.
			final List<JointGraphDerivation<MR, ERESULT>> maxDerivations = Collections
					.unmodifiableList(filterDerivations(derivations,
							FilterUtils.<ERESULT> stubTrue(), true));
			return new JointGraphOutput<MR, ERESULT>(baseOutput, inferenceTime,
					derivations, maxDerivations, exactEvaluation);
		}
		
		public Builder<MR, ERESULT> setExactEvaluation(boolean exactEvaluation) {
			this.exactEvaluation = exactEvaluation;
			return this;
		}
		
	}
	
}
