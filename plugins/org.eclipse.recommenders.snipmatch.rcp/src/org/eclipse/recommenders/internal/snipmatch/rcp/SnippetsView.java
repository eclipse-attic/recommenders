/**
 * Copyright (c) 2010, 2014 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 *    Olav Lenz - change list viewer to tree viewer
 */
package org.eclipse.recommenders.internal.snipmatch.rcp;

import static java.text.MessageFormat.format;
import static org.eclipse.jface.databinding.swt.WidgetProperties.enabled;
import static org.eclipse.jface.databinding.viewers.ViewerProperties.singleSelection;
import static org.eclipse.recommenders.rcp.SharedImages.Images.ELCL_COLLAPSE_ALL;
import static org.eclipse.recommenders.rcp.SharedImages.Images.ELCL_EXPAND_ALL;
import static org.eclipse.recommenders.utils.Checks.cast;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.databinding.viewers.IViewerObservableList;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.recommenders.internal.snipmatch.rcp.EclipseGitSnippetRepository.SnippetRepositoryClosedEvent;
import org.eclipse.recommenders.internal.snipmatch.rcp.EclipseGitSnippetRepository.SnippetRepositoryContentChangedEvent;
import org.eclipse.recommenders.internal.snipmatch.rcp.EclipseGitSnippetRepository.SnippetRepositoryOpenedEvent;
import org.eclipse.recommenders.internal.snipmatch.rcp.editors.SnippetEditorInput;
import org.eclipse.recommenders.rcp.IRcpService;
import org.eclipse.recommenders.rcp.SharedImages;
import org.eclipse.recommenders.rcp.SharedImages.ImageResource;
import org.eclipse.recommenders.rcp.SharedImages.Images;
import org.eclipse.recommenders.snipmatch.ISnippet;
import org.eclipse.recommenders.snipmatch.ISnippetRepository;
import org.eclipse.recommenders.snipmatch.Snippet;
import org.eclipse.recommenders.utils.Recommendation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;

public class SnippetsView extends ViewPart implements IRcpService {

    private static Logger LOG = LoggerFactory.getLogger(SnippetsView.class);

    private Text txtSearch;
    private TreeViewer treeViewer;
    private Tree tree;
    private Button btnEdit;
    private Button btnRemove;
    private Button btnAdd;
    private Button btnReconnect;

    private DataBindingContext ctx;
    private IViewerObservableList selection;

    private SharedImages images;

    private final Repositories repos;
    private boolean initializeTableData = true;

    private List<String> availableRepositories = Lists.newArrayList();
    private ListMultimap<String, KnownSnippet> snippetsGroupedByRepoName = LinkedListMultimap.create();
    private ListMultimap<String, KnownSnippet> filteredSnippetsGroupedByRepoName = LinkedListMultimap.create();

    private final Function<KnownSnippet, String> toStringRepresentation = new Function<KnownSnippet, String>() {

        @Override
        public String apply(KnownSnippet input) {
            return SnippetProposal.createDisplayString(input.snippet);
        }
    };

    @Inject
    public SnippetsView(Repositories repos, SharedImages images) {
        this.repos = repos;
        this.images = images;
    }

    @Override
    public void createPartControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayoutFactory.swtDefaults().spacing(0, 5).numColumns(2).equalWidth(false).applyTo(composite);

        txtSearch = new Text(composite, SWT.BORDER | SWT.ICON_SEARCH | SWT.SEARCH | SWT.CANCEL);
        txtSearch.setMessage(Messages.SEARCH_PLACEHOLDER_SEARCH_TEXT);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(txtSearch);
        txtSearch.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                Job refreshJob = new UIJob(Messages.JOB_REFRESHING_SNIPPETS_VIEW) {
                    @Override
                    public IStatus runInUIThread(IProgressMonitor monitor) {
                        filterData();
                        refreshTable();
                        return Status.OK_STATUS;
                    }
                };
                refreshJob.schedule();
            }
        });

        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.ARROW_DOWN && tree.getItemCount() != 0) {
                    tree.setFocus();
                    tree.setSelection(tree.getTopItem());
                }
            }
        });
        new Label(composite, SWT.NONE);

        Composite treeComposite = new Composite(composite, SWT.NONE);

        TreeColumnLayout treeLayout = new TreeColumnLayout();
        treeComposite.setLayout(treeLayout);
        treeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        treeViewer = new TreeViewer(treeComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
        ColumnViewerToolTipSupport.enableFor(treeViewer);
        tree = treeViewer.getTree();
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);
        GridDataFactory.fillDefaults().grab(true, false).span(1, 1).applyTo(tree);

        TreeViewerColumn snippetViewerColumn = new TreeViewerColumn(treeViewer, SWT.NONE);
        TreeColumn snippetColumn = snippetViewerColumn.getColumn();
        treeLayout.setColumnData(snippetColumn, new ColumnWeightData(1, ColumnWeightData.MINIMUM_WIDTH, true));
        snippetColumn.setText(Messages.TABLE_COLUMN_TITLE_SNIPPETS);
        snippetViewerColumn.setLabelProvider(new StyledCellLabelProvider() {

            @Override
            public void update(ViewerCell cell) {
                Object element = cell.getElement();
                StyledString text = new StyledString();
                if (element instanceof String) {
                    String repoName = (String) element;
                    text.append(repoName);
                    text.append(" "); //$NON-NLS-1$
                    text.append(format(Messages.TABLE_CELL_SUFFIX_SNIPPETS, fetchNumberOfSnippets(repoName)),
                            StyledString.COUNTER_STYLER);
                    cell.setImage(images.getImage(Images.OBJ_REPOSITORY));
                }
                if (element instanceof KnownSnippet) {
                    KnownSnippet knownSnippet = (KnownSnippet) element;
                    text.append(toStringRepresentation.apply(knownSnippet));
                }
                cell.setText(text.toString());
                cell.setStyleRanges(text.getStyleRanges());
                super.update(cell);
            }
        });

        treeViewer.addOpenListener(new IOpenListener() {

            @Override
            public void open(OpenEvent event) {
                doOpen();
            }
        });
        treeViewer.setUseHashlookup(true);
        treeViewer.setContentProvider(new ILazyTreeContentProvider() {

            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void updateElement(Object parent, int index) {
                if (parent instanceof IViewSite) {
                    String element = availableRepositories.get(index);
                    treeViewer.replace(parent, index, element);
                    treeViewer.setChildCount(element, getChildren(element).length);
                } else if (parent instanceof String) {
                    treeViewer.replace(parent, index, getChildren(parent)[index]);
                }
            }

            private Object[] getChildren(Object element) {
                if (element instanceof String) {
                    return filteredSnippetsGroupedByRepoName.get((String) element).toArray();
                }
                return new Object[0];
            }

            @Override
            public void updateChildCount(Object element, int currentChildCount) {
                int count = 0;

                if (element instanceof IViewSite) {
                    count = availableRepositories.size();
                }

                if (availableRepositories.contains(element)) {
                    count = getChildren(element).length;
                }

                if (count != currentChildCount) {
                    treeViewer.setChildCount(element, count);
                }
            }

            @Override
            public Object getParent(Object element) {
                if (element instanceof KnownSnippet) {
                    KnownSnippet knownSnippet = (KnownSnippet) element;
                    return knownSnippet.repoName;
                }
                return null;
            }
        });

        Composite buttonComposite = new Composite(composite, SWT.NONE);
        GridLayoutFactory.swtDefaults().numColumns(1).equalWidth(false).applyTo(buttonComposite);
        GridDataFactory.fillDefaults().grab(false, false).applyTo(buttonComposite);

        btnAdd = new Button(buttonComposite, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(btnAdd);
        btnAdd.setText(Messages.SNIPPETS_VIEW_BUTTON_ADD);
        btnAdd.setEnabled(isImportSupported());
        btnAdd.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for (ISnippetRepository repo : repos.getRepositories()) {
                    if (repo.isImportSupported()) {
                        // TODO Make the repo selectable
                        // don't just store in the first that can import
                        doAdd(repo);
                        break;
                    }
                }
            }
        });

        btnEdit = new Button(buttonComposite, SWT.NONE);
        btnEdit.setEnabled(false);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(btnEdit);
        btnEdit.setText(Messages.SNIPPETS_VIEW_BUTTON_EDIT);
        btnEdit.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doOpen();
            }
        });

        btnRemove = new Button(buttonComposite, SWT.NONE);
        btnRemove.setEnabled(false);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(btnRemove);
        btnRemove.setText(Messages.SNIPPETS_VIEW_BUTTON_REMOVE);
        btnRemove.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                for (int i = 0; i < selection.size(); i++) {
                    Object selectedItem = selection.get(i);
                    if (selectedItem instanceof KnownSnippet) {
                        KnownSnippet knownSnippet = cast(selection.get(i));
                        try {
                            for (ISnippetRepository repo : repos.getRepositories()) {
                                repo.delete(knownSnippet.snippet.getUuid());
                            }
                        } catch (Exception e) {
                            Throwables.propagate(e);
                        }
                    }
                }
            }
        });

        btnReconnect = new Button(buttonComposite, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(btnReconnect);
        btnReconnect.setText(Messages.SNIPPETS_VIEW_BUTTON_RECONNECT);
        btnReconnect.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                reconnect();
            }
        });

        IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();

        addAction(Messages.TOOLBAR_TOOLTIP_EXPAND_ALL, ELCL_EXPAND_ALL, toolBarManager, new Action() {
            @Override
            public void run() {
                for (int i = 0; i < treeViewer.getTree().getItemCount(); i++) {
                    treeViewer.getTree().getItem(i).setExpanded(true);
                }
            }

        });

        addAction(Messages.TOOLBAR_TOOLTIP_COLLAPSE_ALL, ELCL_COLLAPSE_ALL, toolBarManager, new Action() {
            @Override
            public void run() {
                for (int i = 0; i < treeViewer.getTree().getItemCount(); i++) {
                    treeViewer.getTree().getItem(i).setExpanded(false);
                }
            }
        });

        refreshUI();
        selection = ViewersObservables.observeMultiSelection(treeViewer);
        initDataBindings();
    }

    private int fetchNumberOfSnippets(String repoName) {
        return snippetsGroupedByRepoName.get(repoName).size();
    }

    private void addAction(String text, ImageResource imageResource, IContributionManager contributionManager,
            IAction action) {
        action.setImageDescriptor(images.getDescriptor(imageResource));
        action.setText(text);
        action.setToolTipText(text);
        contributionManager.add(action);
    }

    private void filterData() {
        if (txtSearch.isDisposed()) {
            return;
        }
        snippetsGroupedByRepoName = searchSnippets(""); //$NON-NLS-1$
        filteredSnippetsGroupedByRepoName = searchSnippets(txtSearch.getText());
        availableRepositories = Lists.newArrayList(filteredSnippetsGroupedByRepoName.keySet());
    }

    private ListMultimap<String, KnownSnippet> searchSnippets(String searchTerm) {
        ListMultimap<String, KnownSnippet> snippetsGroupedByRepositoryName = LinkedListMultimap.create();
        for (ISnippetRepository repo : repos.getRepositories()) {
            String repoName = repo.getName();
            Set<KnownSnippet> knownSnippets = Sets.newHashSet();
            for (Recommendation<ISnippet> recommendation : repo.search(searchTerm)) {
                knownSnippets.add(new KnownSnippet(repoName, recommendation.getProposal()));
            }
            List<KnownSnippet> sorted = Ordering.from(String.CASE_INSENSITIVE_ORDER).onResultOf(toStringRepresentation)
                    .sortedCopy(knownSnippets);
            snippetsGroupedByRepositoryName.putAll(repoName, sorted);
        }
        return snippetsGroupedByRepositoryName;
    }

    private void reconnect() {
        Job reconnectJob = new Job(Messages.JOB_RECONNECTING_SNIPPET_REPOSITORY) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        btnReconnect.setEnabled(false);
                    }
                });
                try {
                    repos.close();
                } catch (IOException e) {
                    // Snipmatch's default repositories cannot throw an IOException here
                    LOG.error(e.getMessage(), e);
                }
                try {
                    repos.open();
                } catch (IOException e) {
                    // Snipmatch's default repositories cannot throw an IOException here
                    LOG.error(e.getMessage(), e);
                }
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        btnReconnect.setEnabled(true);
                    }
                });
                return Status.OK_STATUS;
            }
        };
        reconnectJob.schedule();
    }

    @Subscribe
    public void onEvent(SnippetRepositoryOpenedEvent e) throws IOException {
        refreshUI();
    }

    @Subscribe
    public void onEvent(SnippetRepositoryClosedEvent e) throws IOException {
        refreshUI();
    }

    private boolean isImportSupported() {
        for (ISnippetRepository repo : repos.getRepositories()) {
            if (repo.isImportSupported()) {
                return true;
            }
        }
        return false;
    }

    @Subscribe
    public void onEvent(SnippetRepositoryContentChangedEvent e) throws IOException {
        refreshUI();
    }

    private void refreshUI() {

        Job refreshJob = new UIJob(Messages.JOB_REFRESHING_SNIPPETS_VIEW) {

            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                if (!treeViewer.getControl().isDisposed()) {
                    filterData();
                    if (initializeTableData) {
                        treeViewer.setInput(getViewSite());
                        initializeTableData = false;
                    } else {
                        refreshTable();
                    }
                }
                if (!btnAdd.isDisposed()) {
                    btnAdd.setEnabled(isImportSupported());
                }
                if (!btnEdit.isDisposed()) {
                    btnEdit.setEnabled(false);
                }
                if (!btnRemove.isDisposed()) {
                    btnRemove.setEnabled(false);
                }
                return Status.OK_STATUS;
            }
        };
        refreshJob.schedule();
    }

    private void refreshTable() {
        int numberOfVisibleElements = treeViewer.getTree().getSize().y / treeViewer.getTree().getItemHeight() + 1;
        treeViewer.refresh();
        int replacedElementsCount = 0;
        for (int i = 0; i < availableRepositories.size(); i++) {
            String repoName = availableRepositories.get(i);
            List<KnownSnippet> elements = filteredSnippetsGroupedByRepoName.get(repoName);
            treeViewer.setChildCount(repoName, elements.size());
            for (int j = 0; j < elements.size() && replacedElementsCount < numberOfVisibleElements; j++) {
                treeViewer.replace(repoName, j, elements.get(j));
                replacedElementsCount++;
            }
            treeViewer.getTree().getItem(i).setExpanded(true);
        }
    }

    private void doAdd(ISnippetRepository repo) {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        try {
            ISnippet snippet = new Snippet(UUID.randomUUID(), "", "", Collections.<String>emptyList(), //$NON-NLS-1$ //$NON-NLS-2$
                    Collections.<String>emptyList(), ""); //$NON-NLS-1$

            final SnippetEditorInput input = new SnippetEditorInput(snippet, repo);
            page.openEditor(input, "org.eclipse.recommenders.snipmatch.rcp.editors.snippet"); //$NON-NLS-1$
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    private void doOpen() {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        for (int i = 0; i < selection.size(); i++) {
            Object selectedItem = selection.get(i);
            if (selectedItem instanceof KnownSnippet) {
                KnownSnippet knownSnippet = cast(selectedItem);
                try {
                    ISnippet snippet = knownSnippet.snippet;
                    ISnippetRepository repository = findRepoForOriginalSnippet(snippet);

                    final SnippetEditorInput input = new SnippetEditorInput(snippet, repository);
                    page.openEditor(input, "org.eclipse.recommenders.snipmatch.rcp.editors.snippet"); //$NON-NLS-1$
                } catch (Exception e) {
                    Throwables.propagate(e);
                }
            }
        }
    }

    private ISnippetRepository findRepoForOriginalSnippet(ISnippet snippet) {
        for (ISnippetRepository repo : repos.getRepositories()) {
            if (repo.hasSnippet(snippet.getUuid())) {
                return repo;
            }
        }
        return null;
    }

    @Override
    public void setFocus() {
        treeViewer.getControl().setFocus();
    }

    protected void initDataBindings() {
        ctx = new DataBindingContext();

        UpdateValueStrategy editSupportedStrategy = new UpdateValueStrategy();
        editSupportedStrategy.setConverter(new IConverter() {

            @Override
            public Object getFromType() {
                return KnownSnippet.class;
            }

            @Override
            public Object getToType() {
                return Boolean.class;
            }

            @Override
            public Boolean convert(Object fromObject) {
                if (fromObject == null) {
                    return false;
                }
                if (fromObject instanceof KnownSnippet) {
                    return true;
                }
                return false;
            }
        });

        IObservableValue selectionValue = singleSelection().observe(treeViewer);
        IObservableValue enabledBtnEditValue = enabled().observe(btnEdit);
        ctx.bindValue(selectionValue, enabledBtnEditValue, editSupportedStrategy, null);

        UpdateValueStrategy deleteSupportedStrategy = new UpdateValueStrategy();

        deleteSupportedStrategy.setConverter(new IConverter() {

            @Override
            public Object getFromType() {
                return KnownSnippet.class;
            }

            @Override
            public Object getToType() {
                return Boolean.class;
            }

            @Override
            public Boolean convert(Object fromObject) {
                if (fromObject == null) {
                    return false;
                }
                if (fromObject instanceof KnownSnippet) {

                    KnownSnippet selection = cast(fromObject);
                    ISnippet snippet = selection.snippet;
                    for (ISnippetRepository repo : repos.getRepositories()) {
                        if (repo.isDeleteSupported()) {
                            if (repo.hasSnippet(snippet.getUuid())) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }

        });
        IObservableValue enabledBtnRemoveValue = enabled().observe(btnRemove);
        ctx.bindValue(selectionValue, enabledBtnRemoveValue, deleteSupportedStrategy, null);
    }

    public class KnownSnippet {
        public ISnippet snippet;
        public String repoName;

        public KnownSnippet(String repoName, ISnippet snippet) {
            this.repoName = repoName;
            this.snippet = snippet;
        }
    }

}
