/**
 * Copyright (c) 2011,2012 Doug Wightman, Zi Ye, Cheng Chen
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Cheng Chen - initial API and implementation.
 */

package org.eclipse.recommenders.snipmatch.local;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.recommenders.snipmatch.core.ArgumentMatchNode;
import org.eclipse.recommenders.snipmatch.core.Effect;
import org.eclipse.recommenders.snipmatch.core.EffectMatchNode;
import org.eclipse.recommenders.snipmatch.core.EffectParameter;
import org.eclipse.recommenders.snipmatch.core.MatchNode;
import org.eclipse.recommenders.snipmatch.core.SummaryFileMap;
import org.eclipse.recommenders.snipmatch.preferences.PreferenceConstants;
import org.eclipse.recommenders.snipmatch.rcp.SnipMatchPlugin;
import org.eclipse.recommenders.snipmatch.search.SnipMatchSearchEngine;
import org.eclipse.recommenders.utils.gson.GsonUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.google.gson.reflect.TypeToken;

public class StringCompareSearchEngine implements SnipMatchSearchEngine {
    private boolean initialized = false;
    private static SnipMatchSearchEngine instance = null;

    private String snippetsDir;
    private String indexFilePath;
    private String commonSnippetsDir = "common";
    private String anonymouseSnippetsDir = "local";
    private List<SummaryFileMap> sfMapList = null;

    public static SnipMatchSearchEngine getInstance() {
        if (instance == null) {
            synchronized (SnipMatchSearchEngine.class) {
                SnipMatchSearchEngine inst = instance;
                if (inst == null) {
                    synchronized (SnipMatchSearchEngine.class) {
                        inst = new StringCompareSearchEngine();
                    }
                    instance = inst;
                }
            }
        }
        return instance;
    }

    private StringCompareSearchEngine() {

    }

    @Override
    public boolean isInitialized(String currentSnippetsDir, String currentIndexDir) {

        return initialized && this.snippetsDir.equals(currentSnippetsDir) && currentIndexDir.equals(this.indexFilePath);
    }

    @Override
    public void initialize(String snippetsDir, String indexDir) throws IOException {
        File cDirFile = new File(snippetsDir, this.commonSnippetsDir);
        if (!(cDirFile.exists()) && !(cDirFile.isDirectory()))
            cDirFile.mkdirs();

        File aDirFile = new File(snippetsDir, this.anonymouseSnippetsDir);
        if (!(aDirFile.exists()) && !(aDirFile.isDirectory()))
            aDirFile.mkdirs();

        String indexFilePath = SnipMatchPlugin.getDefault().getPreferenceStore()
                .getString(PreferenceConstants.SNIPPETS_INDEX_FILE);
        File indexFile = new File(indexFilePath);
        if (!indexFile.exists()) {
            indexFile.createNewFile();
            // updateIndex();
        }

        loadIndexFile(indexFile);
        initialized = true;
        this.snippetsDir = snippetsDir;
        this.indexFilePath = indexFilePath;
    }

    private void loadIndexFile(File indexFile) {
        Type listType = new TypeToken<List<SummaryFileMap>>() {
        }.getType();
        sfMapList = GsonUtil.deserialize(indexFile, listType);
    }

    @Override
    public List<MatchNode> search(String query) {
        List<MatchNode> result = new ArrayList<MatchNode>();
        if (query.trim().equals(""))
            return result;
        if (sfMapList != null && sfMapList.size() > 0)
            for (SummaryFileMap map : sfMapList) {
                String summary = map.summary;

                if (summary.toLowerCase().contains(query.toLowerCase().trim())) {
                    File jsonFile = new File(map.filePath);
                    if (jsonFile.exists()) {
                        Effect parent = GsonUtil.deserialize(jsonFile, Effect.class);
                        parent.setId(System.currentTimeMillis() + String.valueOf(Math.random()).substring(5));

                        MatchNode[] children = new MatchNode[parent.getParameters().length];
                        for (int i = 0; i < children.length; i++) {
                            EffectParameter param = parent.getParameters()[i];
                            ArgumentMatchNode childNode = new ArgumentMatchNode(param.getName(), param);
                            children[i] = childNode;
                        }

                        // pattern was used to display in content assist list
                        String pattern = summary;
                        for (String p : parent.getPatterns()) {
                            if (p.toLowerCase().contains(query.toLowerCase().trim())) {
                                pattern = p;
                                break;
                            }
                        }
                        result.add(new EffectMatchNode(parent, pattern, children));
                    }
                }
            }

        return result;
    }

    @Override
    public void updateIndex() {
        if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
            Shell shell = new Shell(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.NO_TRIM
                    | SWT.NO_FOCUS | SWT.NO_BACKGROUND);
            try {
                CreateIndexOperation indexer = new CreateIndexOperation();
                new ProgressMonitorDialog(shell).run(true, true, indexer);
                initialized = false;
                MessageDialog.openInformation(shell, Constants.MSG_DIALOG_TITLE, "Load "+indexer.getIndexNumber()+" tempaltes into index success!");
            } catch (Exception e) {
                e.printStackTrace();
                MessageDialog.openError(shell, Constants.MSG_DIALOG_TITLE, Constants.MSG_FAIL_TO_INDEX_TEMPLATES);
            }
        }
    }

}
