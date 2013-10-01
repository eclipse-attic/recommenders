/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 *    Olav Lenz - change to tree viewer.
 */
package org.eclipse.recommenders.internal.models.rcp;

import static org.eclipse.recommenders.rcp.SharedImages.ELCL_COLLAPSE_ALL;
import static org.eclipse.recommenders.rcp.SharedImages.ELCL_EXPAND_ALL;
import static org.eclipse.recommenders.rcp.SharedImages.ELCL_REFRESH;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.recommenders.models.Coordinates;
import org.eclipse.recommenders.models.IModelIndex;
import org.eclipse.recommenders.models.ModelCoordinate;
import org.eclipse.recommenders.models.ProjectCoordinate;
import org.eclipse.recommenders.models.rcp.ModelEvents.ModelArchiveDownloadedEvent;
import org.eclipse.recommenders.models.rcp.ModelEvents.ModelIndexOpenedEvent;
import org.eclipse.recommenders.models.rcp.actions.TriggerModelDownloadForModelCoordinatesAction;
import org.eclipse.recommenders.rcp.SharedImages;
import org.eclipse.recommenders.rcp.utils.Selections;
import org.eclipse.recommenders.utils.Constants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class ModelCoordinatesView extends ViewPart {

    private DataBindingContext m_bindingContext;

    private Tree tree;

    @Inject
    IModelIndex index;

    @Inject
    SharedImages images;

    @Inject
    EclipseModelRepository repo;

    @Inject
    ModelsRcpPreferences prefs;

    @Inject
    EventBus bus;

    private TreeViewer treeViewer;
    private List<KnownCoordiante> values = Lists.newArrayList();

    private Text txtSearch;

    private Map<ModelCoordinate, KnownCoordiante> triggeredDownloads = Maps.newHashMap();

    @Override
    public void createPartControl(Composite parent) {
        bus.register(this);
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout());

        txtSearch = new Text(container, SWT.BORDER | SWT.ICON_SEARCH | SWT.SEARCH | SWT.CANCEL);
        txtSearch.setMessage("type filter text");
        txtSearch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.ARROW_DOWN && tree.getItemCount() != 0) {
                    tree.setFocus();
                    tree.setSelection(tree.getTopItem());
                }
            }
        });

        Composite composite = new Composite(container, SWT.NONE);

        TreeColumnLayout treeLayout = new TreeColumnLayout();
        composite.setLayout(treeLayout);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        treeViewer = new TreeViewer(composite, SWT.BORDER | SWT.FULL_SELECTION);
        ColumnViewerToolTipSupport.enableFor(treeViewer);
        tree = treeViewer.getTree();
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);

        TreeViewerColumn tvcPrimary = new TreeViewerColumn(treeViewer, SWT.NONE);
        TreeColumn tcPrimary = tvcPrimary.getColumn();
        treeLayout.setColumnData(tcPrimary, new ColumnWeightData(1, ColumnWeightData.MINIMUM_WIDTH, true));
        tcPrimary.setText("Primary");
        tvcPrimary.setLabelProvider(new StyledCellLabelProvider() {

            private boolean even = true;

            @Override
            public void update(ViewerCell cell) {
                if (cell.getColumnIndex() == 0) {
                    even = !even;
                }
                Object element = cell.getElement();
                StyledString text = new StyledString();
                if (element instanceof String) {
                    String url = (String) element;
                    text.append(url);
                    text.append(" (" + fetchNumberOfModels(url) + " known coordiantes) ", StyledString.COUNTER_STYLER);
                    cell.setImage(images.getImage(SharedImages.OBJ_REPOSITORY));
                    even = false;
                }
                if (element instanceof KnownCoordiante) {
                    if (even) {
                        cell.setBackground(tree.getDisplay().getSystemColor(SWT.COLOR_GRAY));
                    }
                    KnownCoordiante v = (KnownCoordiante) element;
                    text.append(v.getProjectCoordiante().toString());
                }
                cell.setText(text.toString());
                cell.setStyleRanges(text.getStyleRanges());
                super.update(cell);
            }

        });

        for (String classifier : Constants.MODEL_CLASSIFIER) {
            newColumn(treeLayout, classifier);
        }

        treeViewer.setContentProvider(new ITreeContentProvider() {

            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

            @Override
            public void dispose() {
            }

            @Override
            public boolean hasChildren(Object element) {
                if (element instanceof String) {
                    return true;
                }
                return false;
            }

            @Override
            public Object getParent(Object element) {
                KnownCoordiante v = (KnownCoordiante) element;
                return v.getUrl();
            }

            @Override
            public Object[] getElements(Object inputElement) {
                return prefs.remotes;
            }

            @Override
            public Object[] getChildren(Object parentElement) {
                if (parentElement instanceof String) {
                    String url = (String) parentElement;
                    ImmutableListMultimap<String, KnownCoordiante> multimap = groupByUrl(values);
                    return multimap.get(url).toArray();
                }
                return new Object[0];
            }
        });

        treeViewer.setComparator(new ViewerComparator() {
            @Override
            public int compare(Viewer viewer, Object e1, Object e2) {
                if (e1 instanceof String && e2 instanceof String) {
                    String s1 = (String) e1;
                    String s2 = (String) e2;
                    return position(s1).compareTo(position(s2));
                }
                if (e1 instanceof KnownCoordiante && e2 instanceof KnownCoordiante) {
                    KnownCoordiante v1 = (KnownCoordiante) e1;
                    KnownCoordiante v2 = (KnownCoordiante) e2;
                    return v1.getProjectCoordiante().toString().compareTo(v2.getProjectCoordiante().toString());
                }
                return super.compare(viewer, e1, e2);
            }

            public Integer position(String string) {
                String[] remotes = prefs.remotes;
                for (int i = 0; i < remotes.length; i++) {
                    if (remotes[i].equals(string)) {
                        return i;
                    }
                }
                return -1;
            }
        });

        initializeContent();
        addRefreshButton();
        addExpandAllButton();
        addCollapseAllButton();
        addDeleteButton();
        addContextMenu();
        m_bindingContext = initDataBindings();
    }

    private ImmutableListMultimap<String, KnownCoordiante> groupByUrl(List<KnownCoordiante> values) {
        return Multimaps.index(values, new Function<KnownCoordiante, String>() {

            @Override
            public String apply(KnownCoordiante arg0) {
                return arg0.getUrl();
            }
        });
    }

    protected int fetchNumberOfModels(String url) {
        return groupByUrl(values).get(url).size();
    }

    public class KnownCoordiante {
        private String url;
        private ProjectCoordinate pc;
        private Collection<ModelCoordinate> mcs;

        public KnownCoordiante(String url, ProjectCoordinate pc, Collection<ModelCoordinate> mcs) {
            this.setUrl(url);
            this.setProjectCoordiante(pc);
            this.setModelCoordinates(mcs);
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public ProjectCoordinate getProjectCoordiante() {
            return pc;
        }

        public void setProjectCoordiante(ProjectCoordinate pc) {
            this.pc = pc;
        }

        public Collection<ModelCoordinate> getModelCooriantes() {
            return mcs;
        }

        public void setModelCoordinates(Collection<ModelCoordinate> mcs) {
            this.mcs = mcs;
        }

        private Optional<ModelCoordinate> searchModelCoordinate(String classifier) {
            for (ModelCoordinate mc : getModelCooriantes()) {
                if (mc.getClassifier().equals(classifier)) {
                    return Optional.of(mc);
                }
            }
            return Optional.absent();
        }

        public boolean isDownloaded(String classifier) {
            Optional<ModelCoordinate> omc = searchModelCoordinate(classifier);
            if (omc.isPresent()) {
                return repo.getLocation(omc.get(), false).isPresent();
            }
            return false;
        }

        public boolean hasModelCoordinate(String classifier) {
            return searchModelCoordinate(classifier).isPresent();
        }

    }

    private void addRefreshButton() {
        IAction refreshAction = new Action() {
            @Override
            public void run() {
                refreshData();
            }
        };
        refreshAction.setToolTipText("Refresh");
        refreshAction.setImageDescriptor(images.getDescriptor(ELCL_REFRESH));
        getViewSite().getActionBars().getToolBarManager().add(refreshAction);
    }

    private void addCollapseAllButton() {
        IAction expandAction = new Action() {
            @Override
            public void run() {
                treeViewer.collapseAll();
            }
        };
        expandAction.setToolTipText("Collaps all");
        expandAction.setImageDescriptor(images.getDescriptor(ELCL_COLLAPSE_ALL));
        getViewSite().getActionBars().getToolBarManager().add(expandAction);
    }

    private void addExpandAllButton() {
        IAction expandAction = new Action() {
            @Override
            public void run() {
                treeViewer.expandAll();
            }
        };
        expandAction.setToolTipText("Expand all");
        expandAction.setImageDescriptor(images.getDescriptor(ELCL_EXPAND_ALL));
        getViewSite().getActionBars().getToolBarManager().add(expandAction);
    }

    private void addDeleteButton() {
        IAction deleteAction = new Action() {

            @Override
            public void run() {
                deleteCacheAndRefresh();
            }
        };
        deleteAction.setText("Delete models");
        deleteAction.setImageDescriptor(images.getDescriptor(SharedImages.ELCL_DELETE));
        getViewSite().getActionBars().getMenuManager().add(deleteAction);
    }

    private void addContextMenu() {
        final MenuManager menuManager = new MenuManager();
        Menu contextMenu = menuManager.createContextMenu(tree);
        menuManager.setRemoveAllWhenShown(true);
        tree.setMenu(contextMenu);

        menuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                Set<KnownCoordiante> selectedValues = Selections.toSet(treeViewer.getSelection());
                Set<ModelCoordinate> selectedModelCoordinates = Sets.newHashSet();
                for (KnownCoordiante value : selectedValues) {
                    Collection<ModelCoordinate> mcs = value.getModelCooriantes();
                    selectedModelCoordinates.addAll(mcs);
                    for (ModelCoordinate modelCoordinate : mcs) {
                        triggeredDownloads.put(modelCoordinate, value);
                    }
                }
                if (!selectedValues.isEmpty()) {
                    menuManager.add(new TriggerModelDownloadForModelCoordinatesAction("Download models",
                            selectedModelCoordinates, repo, bus));
                }
            }
        });
    }

    private void newColumn(TreeColumnLayout treeLayout, final String classifier) {
        TreeViewerColumn tvColumn = new TreeViewerColumn(treeViewer, SWT.CENTER);
        TreeColumn column = tvColumn.getColumn();
        column.setMoveable(true);
        column.setResizable(false);
        treeLayout.setColumnData(column, new ColumnPixelData(20, false, true));
        column.setText(classifier.toUpperCase());
        tvColumn.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                return null;
            }

            @Override
            public String getToolTipText(Object element) {
                if (element instanceof KnownCoordiante) {
                    KnownCoordiante v = (KnownCoordiante) element;

                    if (!v.hasModelCoordinate(classifier)) {
                        return "No model registered";
                    } else if (v.isDownloaded(classifier)) {
                        return "Locally available";
                    } else {
                        return "Remotely available";
                    }
                }
                return null;
            }

            @Override
            public Image getImage(Object element) {
                if (element instanceof KnownCoordiante) {
                    KnownCoordiante v = (KnownCoordiante) element;
                    if (!v.hasModelCoordinate(classifier)) {
                        return images.getImage(SharedImages.OBJ_CROSS_RED);
                    } else if (v.isDownloaded(classifier)) {
                        return images.getImage(SharedImages.OBJ_CHECK_GREEN);
                    } else {
                        return images.getImage(SharedImages.OBJ_BULLET_BLUE);
                    }
                }
                return null;
            }

            boolean even = true;

            @Override
            public Color getBackground(Object element) {
                if (element instanceof KnownCoordiante) {
                    if (even) {
                        even = !even;
                        return null;
                    } else {
                        even = !even;
                        return tree.getDisplay().getSystemColor(SWT.COLOR_GRAY);
                    }
                } else {
                    even = false;
                    return null;
                }
            }
        });
    }

    private void initializeContent() {
        Multimap<String, ModelCoordinate> groupedByRepository = fetchDataGroupedByRepository();

        values = Lists.newArrayList();
        for (Entry<String, Collection<ModelCoordinate>> entry : groupedByRepository.asMap().entrySet()) {
            values.addAll(createValues(entry.getKey(), entry.getValue()));
        }

        treeViewer.setInput(values);
    }

    private List<KnownCoordiante> createValues(String url, Collection<ModelCoordinate> collection) {
        Multimap<ProjectCoordinate, ModelCoordinate> map = groupDataByProjectCoordinate(collection);

        List<KnownCoordiante> values = Lists.newArrayList();
        for (ProjectCoordinate rep : map.keySet()) {
            values.add(new KnownCoordiante(url, rep, map.get(rep)));
        }

        return values;
    }

    private Multimap<String, ModelCoordinate> fetchDataGroupedByRepository() {
        Multimap<String, ModelCoordinate> temp = LinkedListMultimap.create();

        for (String classifier : Constants.MODEL_CLASSIFIER) {
            addModelCoordinateToIndex(temp, classifier);
        }
        return temp;
    }

    private void addModelCoordinateToIndex(Multimap<String, ModelCoordinate> temp, String classifier) {
        for (ModelCoordinate mc : index.getKnownModels(classifier)) {
            Optional<String> hint = mc.getHint(ModelCoordinate.HINT_REPOSITORY_URL);
            if (hint.isPresent()) {
                temp.put(hint.get(), mc);
            }
        }
    }

    private Multimap<ProjectCoordinate, ModelCoordinate> groupDataByProjectCoordinate(
            Collection<ModelCoordinate> collection) {
        Multimap<ProjectCoordinate, ModelCoordinate> map = LinkedListMultimap.create();

        for (ModelCoordinate modelCoordinate : collection) {
            map.put(Coordinates.toProjectCoordinate(modelCoordinate), modelCoordinate);
        }
        return map;
    }

    @Override
    public void setFocus() {
        tree.setFocus();
    }

    // needs to be public to work with PojoProperties
    public void setFilter(final String filter) {
        treeViewer.setFilters(new ViewerFilter[] { new ViewerFilter() {

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof KnownCoordiante) {
                    KnownCoordiante coord = (KnownCoordiante) element;
                    for (String s : coord.getProjectCoordiante().toString().split(":")) {
                        if (s.startsWith(filter)) {
                            return true;
                        }
                    }
                    return false;
                }
                return true;
            }
        }

        });
    }

    @Override
    public void dispose() {
        super.dispose();
        m_bindingContext.dispose();
        bus.unregister(this);
    }

    @Subscribe
    public void onModelIndexOpened(ModelIndexOpenedEvent e) {
        refreshData();
    }

    @Subscribe
    public void onModelArchiveDownloaded(final ModelArchiveDownloadedEvent e) {
        new UIJob("") {

            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                KnownCoordiante value = triggeredDownloads.get(e.model);
                if (value != null) {
                    treeViewer.refresh(value);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private void refreshData() {
        new UIJob("Refreshing Model Index View...") {
            {
                schedule();
            }

            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                initializeContent();
                return Status.OK_STATUS;
            }
        };
    }

    private void deleteCacheAndRefresh() {
        new Job("Deleting model cache...") {
            {
                schedule();
            }

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    repo.deleteModels();
                    // TODO Would be nice to have something like schule(job1).then(job2), rather than having the first
                    // job schedule the second.
                    refreshData();
                    return Status.OK_STATUS;
                } catch (IOException e) {
                    return new Status(Status.ERROR, org.eclipse.recommenders.internal.models.rcp.Constants.BUNDLE_ID,
                            "Failed to delete model cache");
                }
            }
        };
    }

    protected DataBindingContext initDataBindings() {
        DataBindingContext bindingContext = new DataBindingContext();
        IObservableValue search = WidgetProperties.text(SWT.Modify).observeDelayed(400, txtSearch);
        IObservableValue filter = PojoProperties.value("filter").observe(this);
        bindingContext.bindValue(filter, search, new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER), null);
        return bindingContext;
    }
}
