/**
 * Copyright (c) 2013 Timur Achmetow.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Timur Achmetow - initial API and implementation
 */
package org.eclipse.recommenders.stats.rcp;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.recommenders.stats.rcp.CompletionEvent.ProposalKind;
import org.eclipse.recommenders.stats.rcp.StatisticsView.ProposalLabelProvider;
import org.eclipse.recommenders.stats.rcp.TriggeredCommandsView.CommandsLabelProvider;
import org.eclipse.recommenders.utils.names.ITypeName;
import org.eclipse.recommenders.utils.names.Names;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

public class TableSorters {

    public static void setCompletionTypeSorter(final TableViewer viewer, final TableViewerColumn column) {
        new ColumnViewerSorter(viewer, column) {
            @Override
            protected int doCompare(Viewer viewer, Object object1, Object object2) {
                ProposalKind proposal1 = (ProposalKind) object1;
                ProposalKind proposal2 = (ProposalKind) object2;
                return proposal1.toString().compareToIgnoreCase(proposal2.toString());
            }
        };
    }

    public static void setUsedCompletionSorter(final TableViewer viewer, final TableViewerColumn column,
            final Multimap<ProposalKind, CompletionEvent> multiMap) {
        new ColumnViewerSorter(viewer, column) {
            @Override
            protected int doCompare(Viewer viewer, Object object1, Object object2) {
                int listSize1 = multiMap.get((ProposalKind) object1).size();
                int listSize2 = multiMap.get((ProposalKind) object2).size();
                return new Integer(listSize1).compareTo(listSize2);
            }
        };
    }

    public static void setLastUsedSorter(final TableViewer viewer, final TableViewerColumn column,
            final Multimap<ProposalKind, CompletionEvent> multiMap) {
        new ColumnViewerSorter(viewer, column) {
            @Override
            protected int doCompare(Viewer viewer, Object object1, Object object2) {
                StatisticsView statsPage = new StatisticsView();
                ProposalLabelProvider proposalProvider = statsPage.new ProposalLabelProvider(multiMap);
                Long session1 = proposalProvider.getLastSessionStartedFor((ProposalKind) object1);
                Long session2 = proposalProvider.getLastSessionStartedFor((ProposalKind) object2);
                return session1.compareTo(session2);
            }
        };
    }

    public static void setTypeSorter(final TableViewer viewer, final TableViewerColumn column) {
        new ColumnViewerSorter(viewer, column) {
            @Override
            protected int doCompare(Viewer viewer, Object e1, Object e2) {
                String type1 = Names.vm2srcQualifiedType((ITypeName) e1);
                String type2 = Names.vm2srcQualifiedType((ITypeName) e2);
                return type1.compareToIgnoreCase(type2);
            }
        };
    }

    public static void setCountSorter(final TableViewer viewer, final TableViewerColumn column,
            final Multiset<ITypeName> b) {
        new ColumnViewerSorter(viewer, column) {
            @Override
            protected int doCompare(Viewer viewer, Object e1, Object e2) {
                Integer count1 = new Integer(b.count(e1));
                Integer count2 = new Integer(b.count(e2));
                return count1.compareTo(count2);
            }
        };
    }

    public static void setCommandNameSorter(final TableViewer viewer, final TableViewerColumn column) {
        new ColumnViewerSorter(viewer, column) {
            @Override
            protected int doCompare(Viewer viewer, Object object1, Object object2) {
                TriggeredCommandsView commandsView = new TriggeredCommandsView();
                CommandsLabelProvider labelProvider = commandsView.new CommandsLabelProvider();
                TriggeredCommandObject command1 = (TriggeredCommandObject) object1;
                TriggeredCommandObject command2 = (TriggeredCommandObject) object2;
                String name1 = labelProvider.getCommandName(command1.commandID);
                String name2 = labelProvider.getCommandName(command2.commandID);
                return name1.compareToIgnoreCase(name2);
            }
        };
    }

    public static void setCommandCountSorter(final TableViewer viewer, final TableViewerColumn column) {
        new ColumnViewerSorter(viewer, column) {
            @Override
            protected int doCompare(Viewer viewer, Object object1, Object object2) {
                TriggeredCommandObject command1 = (TriggeredCommandObject) object1;
                TriggeredCommandObject command2 = (TriggeredCommandObject) object2;
                return new Integer(command1.count).compareTo(command2.count);
            }
        };
    }

    public static void setCommandID(final TableViewer viewer, final TableViewerColumn column) {
        new ColumnViewerSorter(viewer, column) {
            @Override
            protected int doCompare(Viewer viewer, Object object1, Object object2) {
                TriggeredCommandObject command1 = (TriggeredCommandObject) object1;
                TriggeredCommandObject command2 = (TriggeredCommandObject) object2;
                return command1.commandID.compareToIgnoreCase(command2.commandID);
            }
        };
    }

    private TableSorters() {
        // do not instantiate
    }
}
