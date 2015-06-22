/**
 * Copyright (c) 2010, 2014 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.net;

import static org.eclipse.recommenders.net.Proxies.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Executor;
import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.junit.Test;
import org.mockito.Mockito;

// XXX a single test does not justify a new test plugin yet. This may be changed later
@SuppressWarnings("restriction")
public class ProxiesTest {

    private static final String EXAMPLE_DOT_COM = "http://www.example.com";
    private static final int PORT = 5000;

    private static final String USER = "user";
    private static final String PASSWORD = "password";

    static final String ENV_USERDOMAIN = "USERDOMAIN";
    static final String PROP_HTTP_AUTH_NTLM_DOMAIN = "http.auth.ntlm.domain";

    @Test
    public void testUserDomain() {
        assertEquals("mydomain", Proxies.getUserDomain("mydomain\\\\user").orNull());

        // System.getenv().put(ProxyUtils.ENV_USERDOMAIN, "mydomain_env\\\\user");
        // assertEquals(null, ProxyUtils.getUserDomain("mydomain_env\\\\user").orNull());

        System.getProperties().put(PROP_HTTP_AUTH_NTLM_DOMAIN, "mydomain_props");
        assertEquals("mydomain_props", Proxies.getUserDomain("mydomain_props").orNull());

    }

    @Test
    public void testWorkstation() {
        assertNotNull(Proxies.getWorkstation().orNull());
    }

    @Test
    public void testUserName() {
        assertEquals("user", Proxies.getUserName("\\\\user").get());
        assertEquals("user2", Proxies.getUserName("domain\\\\user2").get());
        assertEquals("user3", Proxies.getUserName("user3").get());
    }

    @Test
    public void testGetProxyHost() throws URISyntaxException {
        URI uri = new URI(EXAMPLE_DOT_COM);
        String host = uri.getHost();

        IProxyData proxyData = new ProxyData(uri.getScheme(), host, PORT, false, null);

        IProxyService service = Mockito.mock(IProxyService.class);
        when(service.select(uri)).thenReturn(new IProxyData[] { proxyData });

        assertEquals(new HttpHost(host, PORT), Proxies.getProxyHost(uri, service).get());
    }

    @Test
    public void testProxyAuthentication() throws URISyntaxException, IOException {
        URI uri = new URI(EXAMPLE_DOT_COM);
        String host = uri.getHost();

        IProxyData proxyData = new ProxyData(uri.getScheme(), host, PORT, false, null);
        proxyData.setUserid(USER);
        proxyData.setPassword(PASSWORD);

        IProxyService service = Mockito.mock(IProxyService.class);
        Mockito.when(service.select(uri)).thenReturn(new IProxyData[] { proxyData });

        Executor authenticationExecutor = mock(Executor.class);

        Executor executor = mock(Executor.class);
        when(executor.auth(new HttpHost(host, PORT), USER, PASSWORD, getWorkstation().orNull(),
                getUserDomain(USER).orNull())).thenReturn(authenticationExecutor);

        assertEquals(authenticationExecutor, proxyAuthentication(executor, uri, service));
    }

    @Test
    public void testProxyAuthenticationNoUserId() throws URISyntaxException, IOException {
        URI uri = new URI(EXAMPLE_DOT_COM);
        IProxyData proxyData = new ProxyData(uri.getScheme(), uri.getHost(), PORT, false, null);
        IProxyService service = Mockito.mock(IProxyService.class);
        Mockito.when(service.select(uri)).thenReturn(new IProxyData[] { proxyData });

        Executor executor = mock(Executor.class);

        assertEquals(executor, proxyAuthentication(executor, uri, service));

    }
}
