/**
 * Copyright (c) 2010, 2012 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Sebastian Proksch - initial API and implementation
 */
package org.eclipse.recommenders.internal.extdoc.rcp.ui;

import static java.lang.Integer.parseInt;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.recommenders.extdoc.rcp.providers.ExtdocProvider;
import org.eclipse.recommenders.internal.extdoc.rcp.wiring.ExtdocPlugin;
import org.eclipse.recommenders.rcp.RecommendersPlugin;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class ExtdocPreferences {

    public static final String PROVIDER_RANKING = "providerRanking";
    private static final String DISABLED_PROVIDERS = "disabledProviders";
    private static final String SASH_WEIGHTS = "sashWeights";

    private final IEclipsePreferences pluginPreferences;
    private final Preferences providerRankingPreferences;

    public ExtdocPreferences() {
        pluginPreferences = InstanceScope.INSTANCE.getNode(ExtdocPlugin.PLUGIN_ID);
        providerRankingPreferences = pluginPreferences.node(PROVIDER_RANKING);
    }

    public void storeProviderRanking(final List<ExtdocProvider> providerRanking) {
        clearPreferences(providerRankingPreferences);

        for (int i = 0; i < providerRanking.size(); i++) {
            providerRankingPreferences.put(Integer.toString(i), providerRanking.get(i).getId());
        }
        flush();
    }

    public List<String> loadOrderedProviderNames() {
        String[] keys = fetchKeys(providerRankingPreferences);
        List<String> providerNames = new LinkedList<String>();

        for (int i = 0; i < keys.length; i++) {
            String tmp = providerRankingPreferences.get(Integer.toString(i), null);
            if (tmp != null) {
                providerNames.add(tmp);
            } else {
                RecommendersPlugin.logWarning("Loading entry for key " + i + " failed. No such entry.");
            }
        }
        return providerNames;
    }

    private String[] fetchKeys(Preferences node) {
        String[] keys = new String[0];
        try {
            keys = node.keys();
        } catch (BackingStoreException e) {
            RecommendersPlugin.logError(e, "Exception during loading the keys of: " + node.absolutePath());
        }
        return keys;
    }

    private boolean clearPreferences(Preferences prefs) {
        try {
            prefs.clear();
            return true;
        } catch (BackingStoreException e) {
            RecommendersPlugin.logError(e, "Caught exception while clearing the preferences : " + prefs.absolutePath());
            return false;
        }
    }

    private String createString(final String[] names) {
        if (names.length > 0) {
            String out = "";
            for (final String name : names) {
                out += "," + name;
            }
            return out.substring(1);
        } else {
            return "";
        }
    }

    public boolean isProviderEnabled(final ExtdocProvider p) {
        final String arrayString = pluginPreferences.get(DISABLED_PROVIDERS, "");
        final String[] deactivatedProviders = arrayString.split(",");
        final String providerName = p.getDescription().getName();
        for (final String deactivatedName : deactivatedProviders) {
            if (deactivatedName.equals(providerName)) {
                return false;
            }
        }
        return true;
    }

    public void storeProviderEnablement(final List<ExtdocProvider> providers) {
        final String[] disabledProviderNames = getDisabledProviderNames(providers);
        final String toSave = createString(disabledProviderNames);
        pluginPreferences.put(DISABLED_PROVIDERS, toSave);
        flush();
    }

    private String[] getDisabledProviderNames(final List<ExtdocProvider> providers) {
        final List<String> disabledProviders = new ArrayList<String>();
        for (final ExtdocProvider p : providers) {
            if (!p.isEnabled()) {
                disabledProviders.add(p.getDescription().getName());
            }
        }
        return disabledProviders.toArray(new String[0]);
    }

    public int[] loadSashWeights() {
        final String weightString = pluginPreferences.get(SASH_WEIGHTS, "1,3");
        final String[] weights = weightString.split(",");
        return new int[] { parseInt(weights[0]), parseInt(weights[1]) };
    }

    public void storeSashWeights(final int[] weights) {
        final String toSave = weights[0] + "," + weights[1];
        pluginPreferences.put(SASH_WEIGHTS, toSave);
        flush();
    }

    private boolean flush() {
        try {
            pluginPreferences.flush();
            return true;
        } catch (BackingStoreException e) {
            RecommendersPlugin.logError(e, "Caught exception while saving the order of ExtdocProviders");
            return false;
        }
    }

    public boolean clearProviderRankingPreferences() {
        return clearPreferences(providerRankingPreferences);
    }
}