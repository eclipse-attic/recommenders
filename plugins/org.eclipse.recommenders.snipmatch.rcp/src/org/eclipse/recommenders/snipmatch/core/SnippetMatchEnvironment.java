/**
 * Copyright (c) 2011 Doug Wightman, Zi Ye
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.recommenders.snipmatch.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateTranslator;
import org.eclipse.jface.text.templates.TemplateVariable;

/**
 * Extends MatchEnvironment to include features specific to match environments whose effects are snippets, and use a
 * special snippet markup. Since this library is only used by the Eclipse plugin, this applies to all effects anyway.
 */
public abstract class SnippetMatchEnvironment extends MatchEnvironment {

    private HashMap<String, ISnippetNode[]> cachedSnippetNodes;
    private final TemplateTranslator translator = new TemplateTranslator();

    public SnippetMatchEnvironment() {
        cachedSnippetNodes = new HashMap<String, ISnippetNode[]>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see snipmatch.core.MatchEnvironment#evaluateEffect(snipmatch.core.EffectMatchNode, java.lang.Object[])
     */
    @Override
    protected Object evaluateEffect(EffectMatchNode effectNode, Object[] args) {
        Effect effect = effectNode.getEffect();
        TemplateBuffer buffer;
        try {
            buffer = translator.translate(effect.getCode());
            return evaluationJDTTemplate(effect, buffer, args);
        } catch (TemplateException e) {
            e.printStackTrace();
            return evaluationNormalSnip(effectNode, args);
        }
    }

    /**
     * Evalute JDT template code snippet
     * 
     * @param effect
     * @param args
     * @return
     */
    private String evaluationJDTTemplate(Effect effect, TemplateBuffer buffer, Object[] args) {
        // Prepare a hash map of snippet variables for use by formulas.
        HashMap<String, String> variableMap = new HashMap<String, String>();
        variableMap.put("cursor", "/*${cursor}*/");

        for (int i = 0; i < args.length; i++) {
            String value = (String) args[i];
            variableMap.put(effect.getParameter(i).getName(), value);
        }
        StringBuilder sb = new StringBuilder(buffer.getString());
        TemplateVariable[] variables = buffer.getVariables();
        List<TemplateVariableElement> varList = new ArrayList<TemplateVariableElement>();
        for (TemplateVariable var : variables) {
            for (int off : var.getOffsets()) {
                String value = variableMap.get(var.getName());
                if (value != null)
                    varList.add(new TemplateVariableElement(off, off + var.getLength(), value));
            }
        }

        for (int i = 0; i < varList.size(); i++)
            for (int j = i + 1; j < varList.size(); j++) {
                TemplateVariableElement itemi = varList.get(i);
                TemplateVariableElement itemj = varList.get(j);
                if (itemj.getStart() > itemi.getStart()) {
                    TemplateVariableElement temp = itemi;
                    varList.set(i, itemj);
                    varList.set(j, temp);
                }
            }

        for (TemplateVariableElement item : varList) {
            sb.replace(item.getStart(), item.getLength(), item.getValue());
        }
        return sb.toString();
    }

    private String evaluationNormalSnip(EffectMatchNode effectNode, Object[] args) {
        Effect effect = effectNode.getEffect();
        StringBuilder sb = new StringBuilder();

        // Prepare a hash map of snippet variables for use by formulas.
        HashMap<String, String> variables = new HashMap<String, String>();

        // First add a variable for each of the effect's arguments.
        for (int i = 0; i < args.length; i++) {
            variables.put(effect.getParameter(i).getName(), (String) args[i]);
        }

        ISnippetNode[] snippetNodes;

        if (cachedSnippetNodes.containsKey(effect.getId())) {
            snippetNodes = cachedSnippetNodes.get(effect.getId());
        } else {
            snippetNodes = SnippetParser.parseSnippetNodes(effect);
            cachedSnippetNodes.put(effect.getId(), snippetNodes);
        }

        // Evaluate the snippet by processing its nodes, and concatenate all the results.
        for (ISnippetNode snippetNode : snippetNodes) {
            sb.append(evaluateSnippetNode(snippetNode, variables, effectNode));
        }

        return sb.toString().trim();
    }

    /**
     * Evaluate one snippet node.
     * 
     * @param snippetNode
     *            The snippet node to evaluate.
     * @param variables
     *            The persistent variable environment.
     * @param effectNode
     *            The effect call whose effect is the current snippet.
     * @return The evaluated result.
     */
    private String evaluateSnippetNode(ISnippetNode snippetNode, HashMap<String, String> variables,
            EffectMatchNode effectNode) {

        if (snippetNode instanceof FormulaSnippetNode) {

            FormulaSnippetNode formNode = (FormulaSnippetNode) snippetNode;
            String value = evaluateFormulaSnippetNode(formNode, variables, effectNode);

            if (formNode.getNewVariableName() != null) {
                variables.put(formNode.getNewVariableName(), value);
            }

            return value;
        } else {
            return evaluateTextSnippetNode((TextSnippetNode) snippetNode, variables, effectNode);
        }
    }

    /**
     * Evaluate a formula snippet node.
     * 
     * @param formNode
     * @param variables
     * @param effectNode
     * @return
     */
    protected abstract String evaluateFormulaSnippetNode(FormulaSnippetNode formNode,
            HashMap<String, String> variables, EffectMatchNode effectNode);

    /**
     * Evaluate a text snippet node.
     * 
     * @param funcNode
     * @param variables
     * @param effectNode
     * @return
     */
    protected abstract String evaluateTextSnippetNode(TextSnippetNode funcNode, HashMap<String, String> variables,
            EffectMatchNode effectNode);
}
