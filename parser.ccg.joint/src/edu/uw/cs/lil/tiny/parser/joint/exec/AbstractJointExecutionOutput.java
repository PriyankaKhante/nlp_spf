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
package edu.uw.cs.lil.tiny.parser.joint.exec;

import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.exec.IExecOutput;
import edu.uw.cs.lil.tiny.exec.IExecution;
import edu.uw.cs.lil.tiny.parser.joint.IJointOutput;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.filter.IFilter;

/**
 * Abstract execution output wrapper for a joint inference output. Doesn't
 * define the actual execution output.
 * 
 * @author Yoav Artzi
 * @param <MR>
 *            Semantic formal meaning representation.
 * @param <ERESULT>
 *            Semantic evaluation result.
 * @param <RESULT>
 *            Final execution output.
 */
public abstract class AbstractJointExecutionOutput<MR, ERESULT, RESULT>
		implements IExecOutput<RESULT> {
	
	private final List<IExecution<RESULT>>		maxExecutions;
	protected final List<IExecution<RESULT>>	executions;
	protected final IJointOutput<MR, ERESULT>	jointOutput;
	
	public AbstractJointExecutionOutput(IJointOutput<MR, ERESULT> jointOutput,
			List<IExecution<RESULT>> executions,
			List<IExecution<RESULT>> maxExecutions) {
		this.jointOutput = jointOutput;
		this.executions = executions;
		this.maxExecutions = maxExecutions;
	}
	
	@Override
	public List<IExecution<RESULT>> getAllExecutions() {
		return executions;
	}
	
	@Override
	public long getExecTime() {
		return jointOutput.getInferenceTime();
	}
	
	@Override
	public List<IExecution<RESULT>> getExecutions(final IFilter<RESULT> filter) {
		final LinkedList<IExecution<RESULT>> filtered = new LinkedList<IExecution<RESULT>>(
				executions);
		CollectionUtils.filterInPlace(filtered,
				new IFilter<IExecution<RESULT>>() {
					
					@Override
					public boolean isValid(IExecution<RESULT> e) {
						return filter.isValid(e.getResult());
					}
				});
		return filtered;
	}
	
	@Override
	public List<IExecution<RESULT>> getExecutions(final RESULT label) {
		return getExecutions(new IFilter<RESULT>() {
			
			@Override
			public boolean isValid(RESULT e) {
				return e.equals(label);
			}
		});
	}
	
	@Override
	public List<IExecution<RESULT>> getMaxExecutions() {
		return maxExecutions;
	}
	
}
