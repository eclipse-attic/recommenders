/**
 * Copyright (c) 2010 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Johannes Lerch - initial API and implementation.
 */
package org.eclipse.recommenders.rcp.codecompletion.subwords;

import static org.eclipse.recommenders.commons.utils.Checks.ensureIsNotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.viewers.StyledString;

import com.google.common.collect.Lists;

public class SubwordsProposalContext {

    private String prefix;
    private final String subwordsMatchingRegion;
    private final IJavaCompletionProposal jdtProposal;
    private final CompletionProposal proposal;
    private final JavaContentAssistInvocationContext ctx;
    private List<String> prefixBigrams;
    private List<String> matchingRegionBigrams;
    private Pattern pattern;

    public SubwordsProposalContext(final String prefix, final CompletionProposal proposal,
            final IJavaCompletionProposal jdtProposal, final JavaContentAssistInvocationContext ctx) {
        this.proposal = ensureIsNotNull(proposal);
        this.ctx = ensureIsNotNull(ctx);
        setPrefix(prefix);
        this.subwordsMatchingRegion = SubwordsUtils.getTokensBetweenLastWhitespaceAndFirstOpeningBracket(jdtProposal
                .getDisplayString());
        this.jdtProposal = ensureIsNotNull(jdtProposal);
        calculateMatchingRegionBigrams();
    }

    private void calculateMatchingRegionBigrams() {
        matchingRegionBigrams = SubwordsUtils.createLowerCaseNGrams(subwordsMatchingRegion, 2);
    }

    @SuppressWarnings("unchecked")
    public <T extends IJavaCompletionProposal> T getJdtProposal() {
        return (T) jdtProposal;
    }

    public CompletionProposal getProposal() {
        return proposal;
    }

    public JavaContentAssistInvocationContext getContext() {
        return ctx;
    }

    public void setPrefix(final String prefix) {
        this.prefix = ensureIsNotNull(prefix);
        this.pattern = SubwordsUtils.createRegexPatternFromPrefix(prefix);
        this.prefixBigrams = SubwordsUtils.createLowerCaseNGrams(prefix, 2);
    }

    public boolean isRegexMatch() {
        return createMatcher().matches();
    }

    private Matcher createMatcher() {
        return pattern.matcher(subwordsMatchingRegion);
    }

    public StyledString getStyledDisplayString(final StyledString origin) {
        final StyledString copy = SubwordsUtils.deepCopy(origin);
        highlighBigramMatches(copy);
        highlightRegexMatches(copy);
        return copy;
    }

    private void highlighBigramMatches(final StyledString copy) {
        for (final SourceRange range : findBigramHighlightRanges()) {
            copy.setStyle(range.getOffset(), range.getLength(), StyledString.COUNTER_STYLER);
        }
    }

    protected List<SourceRange> findBigramHighlightRanges() {
        final List<SourceRange> res = Lists.newLinkedList();
        for (final String bigram : prefixBigrams) {
            final int indexOf = StringUtils.indexOfIgnoreCase(subwordsMatchingRegion, bigram);
            if (indexOf != -1) {
                final SourceRange range = new SourceRange(indexOf, bigram.length());
                res.add(range);
            }
        }
        return res;
    }

    private void highlightRegexMatches(final StyledString copy) {
        for (final SourceRange range : findRegexHighlightRanges()) {
            copy.setStyle(range.getOffset(), range.getLength(), StyledString.COUNTER_STYLER);
        }
    }

    protected List<SourceRange> findRegexHighlightRanges() {
        final Matcher m = createMatcher();
        final List<SourceRange> res = Lists.newLinkedList();
        if (m.find()) {
            for (int i = 1; i <= m.groupCount(); i++) {
                final int start = m.start(i);
                final int end = m.end(i);
                final int length = end - start;
                res.add(new SourceRange(start, length));
            }
        }
        return res;
    }

    public List<String> getPrefixBigrams() {
        return prefixBigrams;
    }

    public List<String> getMatchingRegionBigrams() {
        return matchingRegionBigrams;
    }

    public boolean isPrefixMatch() {
        return subwordsMatchingRegion.startsWith(prefix);
    }

}
