/**
 * Copyright (c) 2010, 2012 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.internal.completion.rcp.subwords;

import static java.lang.Character.isLetter;
import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

public class SequenceFinder {

    List<int[]> curSequences = Lists.newLinkedList();
    List<int[]> nextSequences = Lists.newLinkedList();

    private int pCompletion, pToken;
    private String completion, token;

    public SequenceFinder(String completion, String token) {
        this.completion = completion;
        this.token = token;
    }

    public List<int[]> findSeqeuences() {

        if (isConstantName(completion)) {
            rewriteCompletion();
        }

        int[] start = new int[0];
        curSequences.add(start);

        for (pToken = 0; pToken < token.length(); pToken++) {
            char t = token.charAt(pToken);

            boolean mustmatch = false;
            for (int[] activeSequence : curSequences) {
                int startIndex = activeSequence.length == 0 ? 0 : activeSequence[activeSequence.length - 1] + 1;

                for (pCompletion = startIndex; pCompletion < completion.length(); pCompletion++) {
                    char c = completion.charAt(pCompletion);
                    if (!Character.isLetter(c)) {
                        mustmatch = true;
                        continue;
                    } else if (Character.isUpperCase(c)) {
                        mustmatch = true;
                    }

                    if (mustmatch && !isSameIgnoreCase(c, t)) {
                        jumpToEndOfWord();
                    } else if (isSameIgnoreCase(c, t)) {
                        addNewSubsequenceForNext(activeSequence);
                    }
                }
            }
            curSequences = nextSequences;
            nextSequences = Lists.newLinkedList();
        }

        // filter
        for (Iterator<int[]> it = curSequences.iterator(); it.hasNext();) {
            int[] candidate = it.next();
            if (candidate.length < token.length()) {
                it.remove();
                continue;
            }
        }

        return curSequences;
    }

    private void addNewSubsequenceForNext(int[] activeSequence) {
        int[] copy = Arrays.copyOf(activeSequence, activeSequence.length + 1);
        copy[pToken] = pCompletion;
        nextSequences.add(copy);
    }

    private void rewriteCompletion() {
        StringBuilder sb = new StringBuilder();

        boolean toUpperCase = false;
        for (char c : completion.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(toUpperCase ? Character.toUpperCase(c) : Character.toLowerCase(c));
                toUpperCase = false;
            } else {
                sb.append(c);
                toUpperCase = true;
            }
        }
        completion = sb.toString();
    }

    private void jumpToEndOfWord() {
        for (pCompletion++; pCompletion < completion.length(); pCompletion++) {
            char next = completion.charAt(pCompletion);

            if (!isLetter(next)) {
                // . or _ word boundary found:
                break;
            }

            if (isUpperCase(next)) {
                pCompletion--;
                break;
            }
        }
    }

    private boolean isConstantName(String completion) {
        for (char c : completion.toCharArray()) {
            if (Character.isLetter(c) && Character.isLowerCase(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSameIgnoreCase(char c1, char c2) {
        if (c1 == c2)
            return true;
        c2 = isLowerCase(c2) ? toUpperCase(c2) : toLowerCase(c2);
        return c1 == c2;
    }

}
