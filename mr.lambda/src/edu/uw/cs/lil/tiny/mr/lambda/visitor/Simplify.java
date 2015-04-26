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
package edu.uw.cs.lil.tiny.mr.lambda.visitor;

import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;

/**
 * Simplify a given logical expression.
 * 
 * @author Yoav Artzi
 */
public class Simplify extends AbstrcatSimplify {
	
	private Simplify(boolean stripLambdas) {
		super(stripLambdas);
	}
	
	public static LogicalExpression of(LogicalExpression exp) {
		return of(exp, false);
	}
	
	public static LogicalExpression of(LogicalExpression exp,
			boolean stripLambdas) {
		final Simplify visitor = new Simplify(stripLambdas);
		visitor.visit(exp);
		return visitor.tempReturn;
	}
	
	@Override
	public void visit(Variable variable) {
		tempReturn = variable;
	}
}