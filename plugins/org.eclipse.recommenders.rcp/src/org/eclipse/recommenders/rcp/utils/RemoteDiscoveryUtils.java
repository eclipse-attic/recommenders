package org.eclipse.recommenders.rcp.utils;

import java.util.Dictionary;

import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.discovery.compatibility.RemoteBundleDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogConfiguration;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryWizard;
import org.eclipse.jface.wizard.WizardDialog;

@SuppressWarnings("restriction")
public class RemoteDiscoveryUtils {
    private static final String DISCOVERY_URL = "http://download.eclipse.org/recommenders/discovery/2.0/completion/directory.xml"; //$NON-NLS-1$

    /** Opens a dialog wizard displaying new code recommenders extensions. */
    public static void openDiscoveryDialog() {
        Catalog catalog = new Catalog();
        Dictionary<Object, Object> env = DiscoveryCore.createEnvironment();
        catalog.setEnvironment(env);
        catalog.setVerifyUpdateSiteAvailability(false);

        // add strategy for retrieving remote catalog

        // look for remote descriptor
        RemoteBundleDiscoveryStrategy remoteDiscoveryStrategy = new RemoteBundleDiscoveryStrategy();
        remoteDiscoveryStrategy.setDirectoryUrl(DISCOVERY_URL);
        catalog.getDiscoveryStrategies().add(remoteDiscoveryStrategy);

        CatalogConfiguration configuration = new CatalogConfiguration();
        configuration.setShowTagFilter(false);

        DiscoveryWizard wizard = new DiscoveryWizard(catalog, configuration);
        WizardDialog dialog = new WizardDialog(WorkbenchUtil.getShell(), wizard);
        dialog.open();
    }

}
