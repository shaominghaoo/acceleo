/*******************************************************************************
 * Copyright (c) 2010, 2011 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.acceleo.ui.interpreter.internal.language.acceleo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.acceleo.engine.event.IAcceleoTextGenerationListener;
import org.eclipse.acceleo.engine.generation.AcceleoEngine;
import org.eclipse.acceleo.engine.generation.IAcceleoEngine2;
import org.eclipse.acceleo.engine.generation.strategy.PreviewStrategy;
import org.eclipse.acceleo.model.mtl.Module;
import org.eclipse.acceleo.model.mtl.ModuleElement;
import org.eclipse.acceleo.model.mtl.Query;
import org.eclipse.acceleo.model.mtl.Template;
import org.eclipse.acceleo.ui.interpreter.AcceleoInterpreterPlugin;
import org.eclipse.acceleo.ui.interpreter.internal.InterpreterMessages;
import org.eclipse.acceleo.ui.interpreter.language.CompilationResult;
import org.eclipse.acceleo.ui.interpreter.language.EvaluationContext;
import org.eclipse.acceleo.ui.interpreter.language.EvaluationResult;
import org.eclipse.acceleo.ui.interpreter.view.Variable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.ocl.ecore.CallOperationAction;
import org.eclipse.ocl.ecore.Constraint;
import org.eclipse.ocl.ecore.OCL;
import org.eclipse.ocl.ecore.OCLExpression;
import org.eclipse.ocl.ecore.SendSignalAction;

/**
 * This will be used by the interpreter in order to evaluate Acceleo expressions from the interpreter.
 * 
 * @author <a href="mailto:laurent.goubet@obeo.fr">Laurent Goubet</a>
 */
public class AcceleoEvaluationTask implements Callable<EvaluationResult> {
	/** Context of this evaluation as passed from the interpreter. */
	private EvaluationContext context;

	/**
	 * Instantiates our evaluation task given the current evaluation context.
	 * 
	 * @param context
	 *            The current interpreter evaluation context.
	 */
	public AcceleoEvaluationTask(EvaluationContext context) {
		this.context = context;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	@SuppressWarnings("restriction")
	public EvaluationResult call() throws Exception {
		CompilationResult compilationResult = context.getCompilationResult();
		if (compilationResult == null || compilationResult.getCompiledExpression() == null) {
			return new EvaluationResult(new Status(IStatus.ERROR, AcceleoInterpreterPlugin.PLUGIN_ID,
					InterpreterMessages.getString("acceleo.interpreter.unresolved.compilation.issue"))); //$NON-NLS-1$
		}

		Object compiledExpression = compilationResult.getCompiledExpression();

		List<EObject> target = context.getTargetEObjects();
		if (target == null) {
			target = Collections.emptyList();
		}

		Object result = null;
		if (compiledExpression instanceof ModuleElement) {
			ModuleElement moduleElement = (ModuleElement)compiledExpression;

			// Prepare arguments
			List<Object> arguments = new ArrayList<Object>();
			List<Variable> variables = context.getVariables();
			Iterator<EObject> targetItr = target.iterator();

			List<org.eclipse.ocl.ecore.Variable> parameters = Collections.emptyList();
			if (moduleElement instanceof Template) {
				parameters = ((Template)moduleElement).getParameter();
			} else if (moduleElement instanceof Query) {
				parameters = ((Query)moduleElement).getParameter();
			}
			for (org.eclipse.ocl.ecore.Variable param : parameters) {
				boolean found = false;
				Iterator<Variable> vars = variables.iterator();
				while (!found && vars.hasNext()) {
					Variable next = vars.next();
					if (param.getName().equals(next.getName())) {
						arguments.add(next.getValue());
						vars.remove();
						found = true;
					}
				}
				if (!found && targetItr.hasNext()) {
					arguments.add(targetItr.next());
					found = true;
				}
				if (!found) {
					// FIXME throw exception from here
				}
			}

			IAcceleoEngine2 engine = new AcceleoEngine();
			if (moduleElement instanceof Template) {
				result = engine.evaluate((Template)moduleElement, arguments, new BasicMonitor());
			} else if (moduleElement instanceof Query) {
				result = engine.evaluate((Query)moduleElement, arguments, new BasicMonitor());
			}
		} else if (compiledExpression instanceof OCLExpression) {
			OCLExpression expression = (OCLExpression)compiledExpression;

			final Module module = (Module)EcoreUtil.getRootContainer(expression);
			// We won't have listeners or property files here
			List<IAcceleoTextGenerationListener> listeners = Collections.emptyList();
			org.eclipse.acceleo.engine.internal.environment.AcceleoEnvironmentFactory factory = new org.eclipse.acceleo.engine.internal.environment.AcceleoEnvironmentFactory(
					null, module, listeners,
					new org.eclipse.acceleo.engine.internal.environment.AcceleoPropertiesLookup(),
					new PreviewStrategy(), new BasicMonitor());
			OCL ocl = OCL.newInstance(factory);
			org.eclipse.acceleo.engine.internal.evaluation.AcceleoEvaluationVisitor<EPackage, EClassifier, EOperation, EStructuralFeature, EEnumLiteral, EParameter, EObject, CallOperationAction, SendSignalAction, Constraint, EClass, EObject> evaluationVisitor = (org.eclipse.acceleo.engine.internal.evaluation.AcceleoEvaluationVisitor<EPackage, EClassifier, EOperation, EStructuralFeature, EEnumLiteral, EParameter, EObject, CallOperationAction, SendSignalAction, Constraint, EClass, EObject>)factory
					.createEvaluationVisitor(ocl.getEnvironment(), ocl.getEvaluationEnvironment(),
							ocl.getExtentMap());

			Object actualArgument = target;
			if (target.size() == 1) {
				actualArgument = target.get(0);
			}
			ocl.getEvaluationEnvironment().add("self", actualArgument); //$NON-NLS-1$
			ocl.getEvaluationEnvironment().add("target", actualArgument); //$NON-NLS-1$

			EObject modelRoot = null;
			if (target.size() > 0) {
				modelRoot = EcoreUtil.getRootContainer(target.get(0));
			}
			ocl.getEvaluationEnvironment().add("model", modelRoot); //$NON-NLS-1$

			for (Variable variable : context.getVariables()) {
				ocl.getEvaluationEnvironment().add(variable.getName(), variable.getValue());
			}

			result = evaluationVisitor.visitExpression(expression);
		}

		return new EvaluationResult(result);
	}
}