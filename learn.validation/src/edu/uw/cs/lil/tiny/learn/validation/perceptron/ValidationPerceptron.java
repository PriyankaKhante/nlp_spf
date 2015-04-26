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
package edu.uw.cs.lil.tiny.learn.validation.perceptron;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.utils.IValidator;
import edu.uw.cs.lil.tiny.explat.IResourceRepository;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment;
import edu.uw.cs.lil.tiny.explat.ParameterizedExperiment.Parameters;
import edu.uw.cs.lil.tiny.explat.resources.IResourceObjectCreator;
import edu.uw.cs.lil.tiny.explat.resources.usage.ResourceUsage;
import edu.uw.cs.lil.tiny.genlex.ccg.ILexiconGenerator;
import edu.uw.cs.lil.tiny.learn.validation.AbstractLearner;
import edu.uw.cs.lil.tiny.parser.IOutputLogger;
import edu.uw.cs.lil.tiny.parser.IDerivation;
import edu.uw.cs.lil.tiny.parser.IParser;
import edu.uw.cs.lil.tiny.parser.IParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.test.ITester;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.filter.IFilter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Validation-based perceptron learner. See Artzi and Zettlemoyer 2013 for
 * detailed description.
 * <p>
 * Parameter update step inspired by: Natasha Singh-Miller and Michael Collins.
 * 2007. Trigger-based Language Modeling using a Loss-sensitive Perceptron
 * Algorithm. In proceedings of ICASSP 2007.
 * </p>
 * 
 * @author Yoav Artzi
 * @param <SAMPLE>
 *            Data item to use for inference.
 * @param <DI>
 *            Data item for learning.
 * @param <MR>
 *            Meaning representation.
 */
public class ValidationPerceptron<SAMPLE extends IDataItem<?>, DI extends ILabeledDataItem<SAMPLE, ?>, MR>
		extends AbstractLearner<SAMPLE, DI, IParserOutput<MR>, MR> {
	public static final ILogger			LOG	= LoggerFactory
													.create(ValidationPerceptron.class);
	/**
	 * Only consider highest scoring valid parses for correct parses for
	 * parameter update.
	 */
	private final boolean				hardUpdates;
	
	/**
	 * Update criterion margin.
	 */
	private final double				margin;
	
	private final IParser<SAMPLE, MR>	parser;
	private final IValidator<DI, MR>	validator;
	
	private ValidationPerceptron(int numIterations,
			IDataCollection<DI> trainingData, Map<DI, MR> trainingDataDebug,
			int lexiconGenerationBeamSize, IParser<SAMPLE, MR> parser,
			IOutputLogger<MR> parserOutputLogger, ITester<SAMPLE, MR> tester,
			boolean conflateGenlexAndPrunedParses, boolean errorDriven,
			ICategoryServices<MR> categoryServices,
			ILexiconGenerator<DI, MR, IModelImmutable<SAMPLE, MR>> genlex,
			double margin, boolean hardUpdates, IValidator<DI, MR> validator,
			IFilter<DI> processingFilter) {
		super(numIterations, trainingData, trainingDataDebug,
				lexiconGenerationBeamSize, parserOutputLogger, tester,
				conflateGenlexAndPrunedParses, errorDriven, categoryServices,
				genlex, processingFilter);
		this.margin = margin;
		this.parser = parser;
		this.hardUpdates = hardUpdates;
		this.validator = validator;
		LOG.info(
				"Init ValidationPerceptron: numIterations=%d, margin=%f, trainingData.size()=%d, trainingDataDebug.size()=%d  ...",
				numIterations, margin, trainingData.size(),
				trainingDataDebug.size());
		LOG.info("Init ValidationPerceptron: ... lexiconGenerationBeamSize=%d",
				lexiconGenerationBeamSize);
		LOG.info(
				"Init ValidationPerceptron: ... conflateParses=%s, errorDriven=%s",
				conflateGenlexAndPrunedParses ? "true" : "false",
				errorDriven ? "true" : "false");
	}
	
	public static <MR, P extends IDerivation<MR>, MODEL extends IModelImmutable<?, MR>> IHashVector constructUpdate(
			List<P> violatingValidParses, List<P> violatingInvalidParses,
			MODEL model) {
		// Create the parameter update
		final IHashVector update = HashVectorFactory.create();
		
		// Get the update for valid violating samples
		for (final P parse : violatingValidParses) {
			parse.getAverageMaxFeatureVector().addTimesInto(
					1.0 / violatingValidParses.size(), update);
		}
		
		// Get the update for the invalid violating samples
		for (final P parse : violatingInvalidParses) {
			parse.getAverageMaxFeatureVector().addTimesInto(
					-1.0 * (1.0 / violatingInvalidParses.size()), update);
		}
		
		// Prune small entries from the update
		update.dropNoise();
		
		// Validate the update
		if (!model.isValidWeightVector(update)) {
			throw new IllegalStateException("invalid update: " + update);
		}
		
		return update;
	}
	
	public static <LF, P extends IDerivation<LF>, MODEL extends IModelImmutable<?, LF>> Pair<List<P>, List<P>> marginViolatingSets(
			MODEL model, double margin, List<P> validParses,
			List<P> invalidParses) {
		// Construct margin violating sets
		final List<P> violatingValidParses = new LinkedList<P>();
		final List<P> violatingInvalidParses = new LinkedList<P>();
		
		// Flags to mark that we inserted a parse into the violating
		// sets, so no need to check for its violation against others
		final boolean[] validParsesFlags = new boolean[validParses.size()];
		final boolean[] invalidParsesFlags = new boolean[invalidParses.size()];
		int validParsesCounter = 0;
		for (final P validParse : validParses) {
			int invalidParsesCounter = 0;
			for (final P invalidParse : invalidParses) {
				if (!validParsesFlags[validParsesCounter]
						|| !invalidParsesFlags[invalidParsesCounter]) {
					// Create the delta vector if needed, we do it only
					// once. This is why we check if we are going to
					// need it in the above 'if'.
					final IHashVector featureDelta = validParse
							.getAverageMaxFeatureVector().addTimes(-1.0,
									invalidParse.getAverageMaxFeatureVector());
					final double deltaScore = featureDelta.vectorMultiply(model
							.getTheta());
					
					// Test valid parse for insertion into violating
					// valid parses
					if (!validParsesFlags[validParsesCounter]) {
						// Case this valid sample is still not in the
						// violating set
						if (deltaScore < margin * featureDelta.l1Norm()) {
							// Case of violation
							// Add to the violating set
							violatingValidParses.add(validParse);
							// Mark flag, so we won't test it again
							validParsesFlags[validParsesCounter] = true;
						}
					}
					
					// Test invalid parse for insertion into
					// violating invalid parses
					if (!invalidParsesFlags[invalidParsesCounter]) {
						// Case this invalid sample is still not in
						// the violating set
						if (deltaScore < margin * featureDelta.l1Norm()) {
							// Case of violation
							// Add to the violating set
							violatingInvalidParses.add(invalidParse);
							// Mark flag, so we won't test it again
							invalidParsesFlags[invalidParsesCounter] = true;
						}
					}
				}
				
				// Increase the counter, as we move to the next sample
				++invalidParsesCounter;
			}
			// Increase the counter, as we move to the next sample
			++validParsesCounter;
		}
		
		return Pair.of(violatingValidParses, violatingInvalidParses);
		
	}
	
	/**
	 * Collect valid and invalid parses.
	 * 
	 * @param dataItem
	 * @param realOutput
	 * @param goodOutput
	 * @return
	 */
	private Pair<List<IDerivation<MR>>, List<IDerivation<MR>>> createValidInvalidSets(
			DI dataItem, IParserOutput<MR> realOutput,
			IParserOutput<MR> goodOutput) {
		
		final List<IDerivation<MR>> validParses = new LinkedList<IDerivation<MR>>();
		final List<IDerivation<MR>> invalidParses = new LinkedList<IDerivation<MR>>();
		
		// Track invalid parses, so we won't aggregate a parse more than once --
		// this is an approximation, but it's a best effort
		final Set<IDerivation<MR>> invalidSemantics = new HashSet<IDerivation<MR>>();
		
		// Collect invalid parses from readlOutput
		for (final IDerivation<MR> parse : realOutput.getAllParses()) {
			if (!validate(dataItem, parse.getSemantics())) {
				invalidParses.add(parse);
				invalidSemantics.add(parse);
			}
		}
		
		// Collect valid and invalid parses from goodOutput
		double validScore = -Double.MAX_VALUE;
		for (final IDerivation<MR> parse : goodOutput.getAllParses()) {
			if (validate(dataItem, parse.getSemantics())) {
				if (hardUpdates) {
					// Case using hard updates, only keep the highest scored
					// valid ones
					if (parse.getScore() > validScore) {
						validScore = parse.getScore();
						validParses.clear();
						validParses.add(parse);
					} else if (parse.getScore() == validScore) {
						validParses.add(parse);
					}
				} else {
					validParses.add(parse);
				}
			} else if (!invalidSemantics.contains(parse)) {
				invalidParses.add(parse);
			}
		}
		return Pair.of(validParses, invalidParses);
	}
	
	@Override
	protected void parameterUpdate(DI dataItem, IParserOutput<MR> realOutput,
			IParserOutput<MR> goodOutput, Model<SAMPLE, MR> model,
			int itemCounter, int epochNumber) {
		
		final IDataItemModel<MR> dataItemModel = model
				.createDataItemModel(dataItem.getSample());
		
		// Split all parses to valid and invalid sets
		final Pair<List<IDerivation<MR>>, List<IDerivation<MR>>> validInvalidSetsPair = createValidInvalidSets(
				dataItem, realOutput, goodOutput);
		final List<IDerivation<MR>> validParses = validInvalidSetsPair.first();
		final List<IDerivation<MR>> invalidParses = validInvalidSetsPair.second();
		LOG.info("%d valid parses, %d invalid parses", validParses.size(),
				invalidParses.size());
		LOG.info("Valid parses:");
		for (final IDerivation<MR> parse : validParses) {
			logParse(dataItem, parse, true, true, dataItemModel);
		}
		
		// Record if a valid parse was found
		if (!validParses.isEmpty()) {
			stats.hasValidParse(itemCounter, epochNumber);
		}
		
		// Skip update if there are no valid or invalid parses
		if (validParses.isEmpty() || invalidParses.isEmpty()) {
			LOG.info("No valid/invalid parses -- skipping");
			return;
		}
		
		// Construct margin violating sets
		final Pair<List<IDerivation<MR>>, List<IDerivation<MR>>> marginViolatingSets = marginViolatingSets(
				model, margin, validParses, invalidParses);
		final List<IDerivation<MR>> violatingValidParses = marginViolatingSets
				.first();
		final List<IDerivation<MR>> violatingInvalidParses = marginViolatingSets
				.second();
		LOG.info("%d violating valid parses, %d violating invalid parses",
				violatingValidParses.size(), violatingInvalidParses.size());
		if (violatingValidParses.isEmpty()) {
			LOG.info("There are no violating valid/invalid parses -- skipping");
			return;
		}
		LOG.info("Violating valid parses: ");
		for (final IDerivation<MR> pair : violatingValidParses) {
			logParse(dataItem, pair, true, true, dataItemModel);
		}
		LOG.info("Violating invalid parses: ");
		for (final IDerivation<MR> parse : violatingInvalidParses) {
			logParse(dataItem, parse, false, true, dataItemModel);
		}
		
		// Construct weight update vector
		final IHashVector update = constructUpdate(violatingValidParses,
				violatingInvalidParses, model);
		
		// Update the parameters vector
		LOG.info("Update: %s", update);
		update.addTimesInto(1.0, model.getTheta());
		stats.triggeredUpdate(itemCounter, epochNumber);
		
	}
	
	@Override
	protected IParserOutput<MR> parse(DI dataItem,
			IDataItemModel<MR> dataItemModel) {
		return parser.parse(dataItem.getSample(), dataItemModel);
	}
	
	@Override
	protected IParserOutput<MR> parse(DI dataItem, IFilter<MR> pruningFilter,
			IDataItemModel<MR> dataItemModel) {
		return parser.parse(dataItem.getSample(), pruningFilter, dataItemModel);
	}
	
	@Override
	protected IParserOutput<MR> parse(DI dataItem, IFilter<MR> pruningFilter,
			IDataItemModel<MR> dataItemModel, ILexicon<MR> generatedLexicon,
			int beamSize) {
		return parser.parse(dataItem.getSample(), pruningFilter, dataItemModel,
				false, generatedLexicon, beamSize);
	}
	
	@Override
	protected boolean validate(DI dataItem, MR hypothesis) {
		return validator.isValid(dataItem, hypothesis);
	}
	
	/**
	 * Builder for {@link ValidationPerceptron}.
	 * 
	 * @author Yoav Artzi
	 */
	public static class Builder<SAMPLE extends IDataItem<?>, DI extends ILabeledDataItem<SAMPLE, ?>, MR> {
		
		/**
		 * Required for lexicon learning.
		 */
		private ICategoryServices<MR>									categoryServices				= null;
		
		/**
		 * Recycle the lexical induction parser output as the pruned one for
		 * parameter update.
		 */
		private boolean													conflateGenlexAndPrunedParses	= false;
		
		private boolean													errorDriven						= false;
		
		/**
		 * GENLEX procedure. If 'null' skips lexicon induction.
		 */
		private ILexiconGenerator<DI, MR, IModelImmutable<SAMPLE, MR>>	genlex							= null;
		
		/**
		 * Use hard updates. Meaning: consider only highest-scored valid parses
		 * for parameter updates, instead of all valid parses.
		 */
		private boolean													hardUpdates						= false;
		
		/**
		 * Beam size to use when doing loss sensitive pruning with generated
		 * lexicon.
		 */
		private int														lexiconGenerationBeamSize		= 20;
		
		/** Margin to scale the relative loss function */
		private double													margin							= 1.0;
		
		/** Number of training iterations */
		private int														numIterations					= 4;
		
		private final IParser<SAMPLE, MR>								parser;
		private IOutputLogger<MR>										parserOutputLogger				= new IOutputLogger<MR>() {
																											
																											public void log(
																													IParserOutput<MR> output,
																													IDataItemModel<MR> dataItemModel) {
																												// Stub
																												
																											}
																										};
		
		/**
		 * Processing filter, if 'false', skip sample.
		 */
		private IFilter<DI>												processingFilter				= new IFilter<DI>() {
																											
																											@Override
																											public boolean isValid(
																													DI e) {
																												return true;
																											}
																										};
		
		private ITester<SAMPLE, MR>										tester							= null;
		
		/** Training data */
		private final IDataCollection<DI>								trainingData;
		
		/**
		 * Mapping a subset of training samples into their gold label for debug.
		 */
		private Map<DI, MR>												trainingDataDebug				= new HashMap<DI, MR>();
		
		private final IValidator<DI, MR>								validator;
		
		public Builder(IDataCollection<DI> trainingData,
				IParser<SAMPLE, MR> parser, IValidator<DI, MR> validator) {
			this.trainingData = trainingData;
			this.parser = parser;
			this.validator = validator;
		}
		
		public ValidationPerceptron<SAMPLE, DI, MR> build() {
			return new ValidationPerceptron<SAMPLE, DI, MR>(numIterations,
					trainingData, trainingDataDebug, lexiconGenerationBeamSize,
					parser, parserOutputLogger, tester,
					conflateGenlexAndPrunedParses, errorDriven,
					categoryServices, genlex, margin, hardUpdates, validator,
					processingFilter);
		}
		
		public Builder<SAMPLE, DI, MR> setConflateGenlexAndPrunedParses(
				boolean conflateGenlexAndPrunedParses) {
			this.conflateGenlexAndPrunedParses = conflateGenlexAndPrunedParses;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setErrorDriven(boolean errorDriven) {
			this.errorDriven = errorDriven;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setGenlex(
				ILexiconGenerator<DI, MR, IModelImmutable<SAMPLE, MR>> genlex,
				ICategoryServices<MR> categoryServices) {
			this.genlex = genlex;
			this.categoryServices = categoryServices;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setHardUpdates(boolean hardUpdates) {
			this.hardUpdates = hardUpdates;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setLexiconGenerationBeamSize(
				int lexiconGenerationBeamSize) {
			this.lexiconGenerationBeamSize = lexiconGenerationBeamSize;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setMargin(double margin) {
			this.margin = margin;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setNumTrainingIterations(
				int numTrainingIterations) {
			this.numIterations = numTrainingIterations;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setParserOutputLogger(
				IOutputLogger<MR> parserOutputLogger) {
			this.parserOutputLogger = parserOutputLogger;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setProcessingFilter(
				IFilter<DI> processingFilter) {
			this.processingFilter = processingFilter;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setTester(ITester<SAMPLE, MR> tester) {
			this.tester = tester;
			return this;
		}
		
		public Builder<SAMPLE, DI, MR> setTrainingDataDebug(
				Map<DI, MR> trainingDataDebug) {
			this.trainingDataDebug = trainingDataDebug;
			return this;
		}
	}
	
	public static class Creator<SAMPLE extends IDataItem<?>, DI extends ILabeledDataItem<SAMPLE, ?>, MR>
			implements
			IResourceObjectCreator<ValidationPerceptron<SAMPLE, DI, MR>> {
		
		private final String	type;
		
		public Creator() {
			this("learner.validation.perceptron");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public ValidationPerceptron<SAMPLE, DI, MR> create(Parameters params,
				IResourceRepository repo) {
			
			final IDataCollection<DI> trainingData = repo.getResource(params
					.get("data"));
			
			final Builder<SAMPLE, DI, MR> builder = new ValidationPerceptron.Builder<SAMPLE, DI, MR>(
					trainingData,
					(IParser<SAMPLE, MR>) repo
							.getResource(ParameterizedExperiment.PARSER_RESOURCE),
					(IValidator<DI, MR>) repo.getResource(params
							.get("validator")));
			
			if ("true".equals(params.get("hard"))) {
				builder.setHardUpdates(true);
			}
			
			if (params.contains("parseLogger")) {
				builder.setParserOutputLogger((IOutputLogger<MR>) repo
						.getResource(params.get("parseLogger")));
			}
			
			if (params.contains("genlex")) {
				builder.setGenlex(
						(ILexiconGenerator<DI, MR, IModelImmutable<SAMPLE, MR>>) repo
								.getResource(params.get("genlex")),
						(ICategoryServices<MR>) repo
								.getResource(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE));
			}
			
			if (params.contains("genlexbeam")) {
				builder.setLexiconGenerationBeamSize(Integer.valueOf(params
						.get("genlexbeam")));
			}
			
			if (params.contains("conflateParses")) {
				builder.setConflateGenlexAndPrunedParses("true".equals(params
						.get("conflateParses")));
			}
			
			if (params.contains("errorDriven")) {
				builder.setErrorDriven("true".equals(params.get("errorDriven")));
			}
			
			if (params.contains("margin")) {
				builder.setMargin(Double.valueOf(params.get("margin")));
			}
			
			if (params.contains("tester")) {
				builder.setTester((ITester<SAMPLE, MR>) repo.getResource(params
						.get("tester")));
			}
			
			if (params.contains("filter")) {
				builder.setProcessingFilter((IFilter<DI>) repo
						.getResource(params.get("filter")));
			}
			
			if (params.contains("iter")) {
				builder.setNumTrainingIterations(Integer.valueOf(params
						.get("iter")));
			}
			
			return builder.build();
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), ValidationPerceptron.class)
					.setDescription("Validation-based perceptron")
					.addParam("data", "id", "Training data")
					.addParam("genlex", "ILexiconGenerator", "GENLEX procedure")
					.addParam(
							"hard",
							"boolean",
							"Use hard updates (i.e., only use max scoring valid parses/evaluation as positive samples). Options: true, false. Default: false")
					.addParam("parseLogger", "id",
							"Parse logger for debug detailed logging of parses")
					.addParam("tester", "ITester",
							"Intermediate tester to use between epochs")
					.addParam("genlexbeam", "int",
							"Beam to use for GENLEX inference (parsing).")
					.addParam("margin", "double",
							"Margin to use for updates. Updates will be done when this margin is violated.")
					.addParam("filter", "IFilter", "Processing filter")
					.addParam("iter", "int", "Number of training iterations")
					.addParam("validator", "IValidator", "Validation function")
					.addParam("conflateParses", "boolean",
							"Recyle lexical induction parsing output as pruned parsing output")
					.addParam(
							"errorDriven",
							"boolean",
							"Error driven lexical generation, if the can generate a valid parse, skip lexical induction")
					.build();
		}
		
	}
}