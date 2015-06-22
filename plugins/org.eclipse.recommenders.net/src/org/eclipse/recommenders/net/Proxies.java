/**
 * Copyright (c) 2014 Codetrails GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.net;

import static com.google.common.base.Optional.*;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Executor;
import org.eclipse.core.internal.net.ProxyManager;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

@SuppressWarnings("restriction")
public final class Proxies {

    private Proxies() {
        // Not meant to be instantiated
    }

    private static final String DOUBLEBACKSLASH = "\\\\"; //$NON-NLS-1$
    private static final String ENV_USERDOMAIN = "USERDOMAIN"; //$NON-NLS-1$
    private static final String PROP_HTTP_AUTH_NTLM_DOMAIN = "http.auth.ntlm.domain"; //$NON-NLS-1$

    /**
     * Returns the domain of the current machine- if any.
     *
     * @param userName
     *            the user name which may be null. On windows it may contain the domain name as prefix
     *            "domain\\username".
     */
    public static Optional<String> getUserDomain(String userName) {

        // check the app's system properties
        String domain = System.getProperty(PROP_HTTP_AUTH_NTLM_DOMAIN);
        if (domain != null) {
            return of(domain);
        }

        // check the OS environment
        domain = System.getenv(ENV_USERDOMAIN);
        if (domain != null) {
            return of(domain);
        }

        // test the user's name whether it may contain an information about the domain name
        if (StringUtils.contains(userName, DOUBLEBACKSLASH)) {
            return of(substringBefore(userName, DOUBLEBACKSLASH));
        }

        // no domain name found
        return absent();
    }

    /**
     * Returns the host name of this workstation (localhost)
     */
    public static Optional<String> getWorkstation() {
        try {
            return of(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            return absent();
        }
    }

    /**
     * Returns the user name without a (potential) domain prefix
     *
     * @param userName
     *            a String that may look like "domain\\userName"
     */
    public static Optional<String> getUserName(String userName) {
        if (userName == null) {
            return absent();
        }
        return contains(userName, DOUBLEBACKSLASH) ? of(substringAfterLast(userName, DOUBLEBACKSLASH)) : of(userName);
    }

    public static Optional<HttpHost> getProxyHost(URI target) {
        return getProxyHost(target, ProxyManager.getProxyManager());
    }

    @VisibleForTesting
    public static Optional<HttpHost> getProxyHost(URI target, IProxyService proxyService) {
        IProxyData proxy = getProxyData(proxyService, target).orNull();
        if (proxy == null) {
            return Optional.absent();
        }
        return Optional.of(new HttpHost(proxy.getHost(), proxy.getPort()));
    }

    public static Executor proxyAuthentication(Executor executor, URI target) throws IOException {
        return proxyAuthentication(executor, target, ProxyManager.getProxyManager());
    }

    @VisibleForTesting
    public static Executor proxyAuthentication(Executor executor, URI target, IProxyService proxyService)
            throws IOException {
        IProxyData proxy = getProxyData(proxyService, target).orNull();

        if (proxy == null) {
            return executor;
        }

        String userId = proxy.getUserId();
        if (userId == null) {
            return executor;
        }

        String userName = getUserName(userId).orNull();
        String pass = proxy.getPassword();
        String workstation = getWorkstation().orNull();
        String domain = getUserDomain(userId).orNull();

        HttpHost proxyHost = new HttpHost(proxy.getHost(), proxy.getPort());

        return executor.auth(proxyHost, userName, pass, workstation, domain);
    }

    private static Optional<IProxyData> getProxyData(IProxyService service, URI target) {
        IProxyData[] proxies = service.select(target);
        if (isEmpty(proxies)) {
            return Optional.absent();
        }
        return Optional.of(proxies[0]);
    }
}
