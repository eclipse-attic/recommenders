/**
 * Copyright (c) 2011 Stefan Henss.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Henss - initial API and implementation.
 *    Patrick Gottschaemmer, Olav Lenz - externalize Strings.
 */
package org.eclipse.recommenders.internal.calls.rcp;

import static com.google.common.collect.Iterables.isEmpty;
import static java.lang.String.format;
import static org.eclipse.recommenders.calls.ICallModel.DefinitionKind.PARAM;
import static org.eclipse.recommenders.internal.apidocs.rcp.ApidocsViewUtils.*;
import static org.eclipse.recommenders.rcp.JavaElementSelectionEvent.JavaElementSelectionLocation.*;
import static org.eclipse.recommenders.rcp.utils.JdtUtils.resolveMethod;
import static org.eclipse.recommenders.utils.Recommendations.*;
import static org.eclipse.swt.SWT.COLOR_INFO_FOREGROUND;

import java.util.Set;

import javax.inject.Inject;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.recommenders.apidocs.rcp.ApidocProvider;
import org.eclipse.recommenders.apidocs.rcp.JavaSelectionSubscriber;
import org.eclipse.recommenders.calls.ICallModel;
import org.eclipse.recommenders.calls.ICallModel.DefinitionKind;
import org.eclipse.recommenders.calls.ICallModelProvider;
import org.eclipse.recommenders.models.QualifiedTypeName;
import org.eclipse.recommenders.models.rcp.IProjectCoordinateResolver;
import org.eclipse.recommenders.rcp.IAstProvider;
import org.eclipse.recommenders.rcp.JavaElementResolver;
import org.eclipse.recommenders.rcp.JavaElementSelectionEvent;
import org.eclipse.recommenders.rcp.utils.JdtUtils;
import org.eclipse.recommenders.utils.Recommendation;
import org.eclipse.recommenders.utils.names.IMethodName;
import org.eclipse.recommenders.utils.names.Names;
import org.eclipse.recommenders.utils.names.VmMethodName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;

@Beta
@SuppressWarnings("restriction")
public final class CallsApidocProvider extends ApidocProvider {

    @Inject
    ICallModelProvider modelProvider;
    @Inject
    IProjectCoordinateResolver pcProvider;
    @Inject
    JavaElementResolver jdtResolver;
    @Inject
    EventBus workspaceBus;
    @Inject
    IAstProvider astProvider;

    private IType receiverType;
    private ICallModel model;
    private QualifiedTypeName baseName;

    @JavaSelectionSubscriber
    public void onVariableSelection(final ILocalVariable var, final JavaElementSelectionEvent event,
            final Composite parent) {
        handle(var, var.getElementName(), var.getTypeSignature(), event, parent);
    }

    @JavaSelectionSubscriber(METHOD_BODY)
    public void onFieldSelection(final IField var, final JavaElementSelectionEvent event, final Composite parent)
            throws JavaModelException {
        handle(var, var.getElementName(), var.getTypeSignature(), event, parent);
    }

    private void handle(final IJavaElement variable, final String elementName, final String typeSignature,
            final JavaElementSelectionEvent event, final Composite parent) {
        final Optional<ASTNode> opt = event.getSelectedNode();
        if (!opt.isPresent()) {
            return;
        }

        final Optional<IType> varType = findVariableType(typeSignature, variable);
        if (!varType.isPresent()) {
            return;
        }

        receiverType = varType.get();
        baseName = pcProvider.toQualifiedName(receiverType).orNull();
        if (baseName == null || !acquireModel()) {
            return;
        }
        try {
            final ASTNode node = opt.get();
            final Optional<MethodDeclaration> optAstMethod = findEnclosingMethod(node);
            final Optional<IMethod> optJdtMethod = resolveMethod(optAstMethod.orNull());
            if (!optJdtMethod.isPresent()) {
                return;
            }
            AstDefUseFinder defUse = new AstDefUseFinder(variable.getElementName(), optAstMethod.orNull());
            IMethod findFirstDeclaration = JdtUtils.findFirstDeclaration(optJdtMethod.get());
            IMethodName overrideContext = jdtResolver.toRecMethod(findFirstDeclaration).or(VmMethodName.NULL);
            Set<IMethodName> calls = Sets.newHashSet(defUse.getCalls());
            IMethodName definingMethod = defUse.getDefiningMethod().orNull();
            DefinitionKind kind = defUse.getDefinitionKind();
            // In the case of parameters we replace the defining method with the overridesContext
            if (PARAM == kind) {
                definingMethod = overrideContext;
            }

            model.setObservedOverrideContext(overrideContext);
            model.setObservedDefiningMethod(definingMethod);
            model.setObservedCalls(calls);
            model.setObservedDefinitionKind(kind);

            Iterable<Recommendation<IMethodName>> methodCalls = sortByRelevance(filterRelevance(model.recommendCalls(),
                    0.05d));
            runSyncInUiThread(new CallRecommendationsRenderer(overrideContext, methodCalls, calls,
                    variable.getElementName(), definingMethod, kind, parent));
        } finally {
            releaseModel();
        }
    }

    private Optional<IType> findVariableType(final String typeSignature, final IJavaElement parent) {
        final Optional<IType> varType = JdtUtils.findTypeFromSignature(typeSignature, parent);
        return varType;
    }

    private Optional<MethodDeclaration> findEnclosingMethod(final ASTNode node) {
        MethodDeclaration declaringNode = null;
        for (ASTNode p = node; p != null; p = p.getParent()) {
            if (p instanceof MethodDeclaration) {
                declaringNode = (MethodDeclaration) p;
                break;
            }
        }
        return Optional.fromNullable(declaringNode);
    }

    private boolean acquireModel() {
        model = modelProvider.acquireModel(baseName).orNull();
        return model != null;
    }

    private void releaseModel() {
        if (model != null) {
            modelProvider.releaseModel(model);
            model = null;
        }
    }

    private final class CallRecommendationsRenderer implements Runnable {
        private final IMethodName ctx;
        private final Iterable<Recommendation<IMethodName>> methodCalls;
        private final Set<IMethodName> calls;
        private final String varName;
        private final IMethodName def;
        private final DefinitionKind kind;
        private final Composite parent;

        private CallRecommendationsRenderer(final IMethodName ctx,
                final Iterable<Recommendation<IMethodName>> methodCalls, final Set<IMethodName> calls,
                final String varName, final IMethodName def, final DefinitionKind kind, final Composite parent) {
            this.ctx = ctx;
            this.methodCalls = methodCalls;
            this.calls = calls;
            this.varName = varName;
            this.def = def;
            this.kind = kind;
            this.parent = parent;
        }

        @Override
        public void run() {
            final Composite container = createComposite(parent, 4);
            final Label preamble2 = new Label(container, SWT.NONE);
            setInfoForegroundColor(preamble2);
            setInfoBackgroundColor(preamble2);
            preamble2.setLayoutData(GridDataFactory.swtDefaults().span(4, 1).indent(0, 0).create());
            if (isEmpty(methodCalls)) {
                preamble2.setText(format(Messages.EXTDOC_RECOMMENDATIONS_ARE_NOT_MADE, receiverType.getElementName(),
                        varName));
            } else {
                preamble2.setText(format(Messages.EXTDOC_RECOMMENDATIONS_ARE_MADE, receiverType.getElementName(),
                        varName));
            }
            new Label(container, SWT.NONE).setLayoutData(GridDataFactory.swtDefaults().span(4, 1).indent(0, 0)
                    .hint(SWT.DEFAULT, 1).create());
            for (final Recommendation<IMethodName> rec : methodCalls) {
                final int percentage = (int) Math.rint(rec.getRelevance() * 100);
                createLabel(container, percentageToRecommendationPhrase(percentage), true, false,
                        COLOR_INFO_FOREGROUND, false);

                createLabel(container, Messages.EXTDOC_CALL + " ", false);
                createMethodLink(container, rec.getProposal());
                createLabel(container, " - " + format(Messages.EXTDOC_PECOMMENDATION_PERCENTAGE, percentage), false); //$NON-NLS-1$
            }
            new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
            createLabel(container, "", false); //$NON-NLS-1$
            createLabel(container, "", false); //$NON-NLS-1$
            createLabel(container, "", false); //$NON-NLS-1$

            final Label preamble = new Label(container, SWT.NONE);
            preamble.setLayoutData(GridDataFactory.swtDefaults().span(4, 1).indent(0, 5).create());
            setInfoForegroundColor(preamble);
            setInfoBackgroundColor(preamble);
            final String text;
            if (ctx == VmMethodName.NULL) {
                text = format(Messages.EXTDOC_PROPOSAL_COMPUTED_UNTRAINED, receiverType.getElementName());
            } else {
                text = format(Messages.EXTDOC_PROPOSAL_COMPUTED, receiverType.getElementName(),
                        Names.vm2srcSimpleTypeName(ctx.getDeclaringType()) + "." + Names.vm2srcSimpleMethod(ctx));
            }
            preamble.setText(text);

            new Label(container, SWT.NONE).setLayoutData(GridDataFactory.swtDefaults().span(4, 1).indent(0, 5)
                    .hint(SWT.DEFAULT, 1).create());

            if (def != null) {
                createLabel(container, Messages.EXTDOC_DEFINED_BY, true, false, SWT.COLOR_DARK_GRAY, false);
                createLabel(container, "", false, false, SWT.COLOR_DARK_GRAY, false); //$NON-NLS-1$
                if (def == VmMethodName.NULL) {
                    createLabel(container, Messages.EXTDOC_UNDEFINED, false, false, SWT.COLOR_DARK_GRAY, false);
                } else {
                    createMethodLink(container, def);
                }
                createLabel(container, "- " + kind.toString().toLowerCase(), true, false, SWT.COLOR_DARK_GRAY, false); //$NON-NLS-1$

            }

            for (final IMethodName observedCall : calls) {
                createLabel(container, Messages.EXTDOC_OBSERVED, true, false, SWT.COLOR_DARK_GRAY, false);

                createLabel(container, Messages.EXTDOC_CALL + " ", false, false, SWT.COLOR_DARK_GRAY, false);
                createMethodLink(container, observedCall);
                createLabel(container, "", true, false, SWT.COLOR_DARK_GRAY, false); //$NON-NLS-1$
            }
        }

        Link createMethodLink(final Composite parent, final IMethodName method) {
            final String text = "<a>" + Names.vm2srcSimpleMethod(method) + "</a>"; // $NON-NLS$
            final String tooltip = Names.vm2srcQualifiedMethod(method);

            final Link link = new Link(parent, SWT.NONE);
            link.setText(text);
            setInfoBackgroundColor(link);
            link.setToolTipText(tooltip);
            link.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    final Optional<IMethod> opt = jdtResolver.toJdtMethod(method);
                    if (opt.isPresent()) {
                        final JavaElementSelectionEvent event = new JavaElementSelectionEvent(opt.get(),
                                METHOD_DECLARATION);
                        workspaceBus.post(event);
                    } else {
                        link.setEnabled(false);
                    }
                }
            });
            return link;
        }
    }
}
