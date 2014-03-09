package org.eclipse.recommenders.models.rcp.advisors;

import static org.eclipse.recommenders.models.advisors.MavenCentralFingerprintSearchAdvisor.SEARCH_MAVEN_ORG;

import java.net.URI;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.recommenders.models.DependencyInfo;
import org.eclipse.recommenders.models.IProjectCoordinateAdvisor;
import org.eclipse.recommenders.models.ProjectCoordinate;
import org.eclipse.recommenders.models.advisors.MavenCentralFingerprintSearchAdvisor;
import org.eclipse.recommenders.utils.Urls;

import com.google.common.base.Optional;

public class EclipseMavenCentralFingerprintSearchAdvisor implements IProjectCoordinateAdvisor {

    private static final URI SEARCH_MAVEN_ORG_URI = Urls.toUri(SEARCH_MAVEN_ORG);

    private final MavenCentralFingerprintSearchAdvisor delegate;
    private final IProxyService proxy;

    private ProxyConfig proxyConfig;

    @Inject
    public EclipseMavenCentralFingerprintSearchAdvisor(IProxyService proxy) {
        this.proxy = proxy;
        ProxyConfig newProxyConfig = updateProxyConfig();
        delegate = new MavenCentralFingerprintSearchAdvisor(newProxyConfig.host, newProxyConfig.port,
                newProxyConfig.user, newProxyConfig.password);
    }

    @Override
    public Optional<ProjectCoordinate> suggest(DependencyInfo dependencyInfo) {
        ProxyConfig updatedProxyConfig = updateProxyConfig();
        if (updatedProxyConfig != null) {
            delegate.setProxy(updatedProxyConfig.host, updatedProxyConfig.port, updatedProxyConfig.user,
                    updatedProxyConfig.password);
        }
        return delegate.suggest(dependencyInfo);
    }

    private ProxyConfig updateProxyConfig() {
        final String currentProxyHost;
        final int currentProxyPort;
        final String currentProxyUser;
        final String currentProxyPassword;

        synchronized (proxy) {
            if (!proxy.isProxiesEnabled()) {
                currentProxyHost = null;
                currentProxyPort = -1;
                currentProxyUser = currentProxyPassword = null;
            } else {
                IProxyData[] entries = proxy.select(SEARCH_MAVEN_ORG_URI);
                if (entries.length == 0) {
                    currentProxyHost = null;
                    currentProxyPort = -1;
                    currentProxyUser = currentProxyPassword = null;
                } else {
                    IProxyData proxyData = entries[0];
                    currentProxyHost = proxyData.getHost();
                    currentProxyPort = proxyData.getPort();
                    currentProxyUser = proxyData.getUserId();
                    currentProxyPassword = proxyData.getPassword();
                }
            }
        }

        if (proxyConfig == null
                || !proxyConfig.equals(currentProxyHost, currentProxyPort, currentProxyUser, currentProxyPassword)) {
            proxyConfig = new ProxyConfig(currentProxyHost, currentProxyPort, currentProxyUser, currentProxyPassword);
            return proxyConfig;
        } else {
            return null;
        }
    }

    /**
     * Encapsulating the four-element proxy configuration in a value object ensures that any concurrent updates to it
     * are atomic.
     */
    private static class ProxyConfig {

        private final String host;
        private final int port;
        private final String user;
        private final String password;

        public ProxyConfig(String host, int port, String user, String password) {
            this.host = host;
            this.port = port;
            this.user = user;
            this.password = password;
        }

        public boolean equals(String proxyHost, int proxyPort, String proxyUser, String proxyPassword) {
            return StringUtils.equals(host, proxyHost) && port == proxyPort && StringUtils.equals(user, proxyUser)
                    && StringUtils.equals(password, proxyPassword);
        }
    }
}
