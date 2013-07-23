/**
 * Copyright 2013 Peergreen S.A.S.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.peergreen.webcontainer.tomcat7.internal.httpservice;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.apache.catalina.Container;
import org.apache.catalina.Host;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.startup.ContextConfig;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

import com.peergreen.deployment.Artifact;
import com.peergreen.deployment.facet.endpoint.Endpoints;
import com.peergreen.deployment.model.BundleArtifactManager;
import com.peergreen.webcontainer.tomcat7.internal.InternalTomcat7HttpService;
import com.peergreen.webcontainer.tomcat7.internal.InternalTomcat7Service;
import com.peergreen.webcontainer.tomcat7.internal.core.InstanceManagerLifeCycleListener;

/**
 * This class implements the specific part of HTTP service for Tomcat. Calls are
 * made from the {@link BasicHttpService} service. It doesn't implement directly
 * HttpService as HttpService API needs to be exposed through a
 * {@link ServiceFactory} interface <br/>
 * It maps all the code on the Tomcat7 Service.
 * @author Florent Benoit
 */
@Component
@Provides
@Instantiate
public class BasicTomcat7HttpService implements InternalTomcat7HttpService {

    /**
     * Internal Tomcat7 service.
     */
    private final InternalTomcat7Service tomcat7Service;

    /**
     * Lock when accessing wrappers
     */
    private final Lock wrapperLock;

    /**
     * Wrappers.
     */
    private final List<Wrapper> wrappers;

    /**
     * Artifact manager for bundle
     */
    private final BundleArtifactManager bundleArtifactManager;


    /**
     * Instantiate http service.
     */
    public BasicTomcat7HttpService(@Requires InternalTomcat7Service tomcat7Service, @Requires BundleArtifactManager bundleArtifactManager) {
        this.tomcat7Service = tomcat7Service;
        this.bundleArtifactManager = bundleArtifactManager;
        this.wrapperLock = new ReentrantReadWriteLock().writeLock();
        this.wrappers = new ArrayList<>();
    }

    /**
     * Gets the context for the given alias info object
     * @param aliasInfo the context and servlet path
     * @return null if not found or the Tomcat context if it exists.
     */
    public HttpServiceStandardContext getStandardContext(AliasInfo aliasInfo) {
        // Gets the default host
        final Host host = this.tomcat7Service.getDefaultHost();

        // Gets the container for the given /context name
        Container container = host.findChild(aliasInfo.getContextPath());
        if (container == null) {
            return null;
        }

        if (container instanceof HttpServiceStandardContext) {
            // reuse a previous registered context
            return (HttpServiceStandardContext) container;
        }

        throw new IllegalArgumentException(
                String.format(
                        "The path %s has been previously deployed but without using OSGi deployment,  thus it cannot be reused",
                        aliasInfo.getContextPath()));

    }

    /**
     * Gets the context for the given alias info and the given http context
     * @param aliasInfo the context and servlet path
     * @param httpContext the HTTP context to use if the context is not existing
     * @param servlet the servlet that is being registered
     * @return a new HttpServiceStandardContext if it was not found or an existing HttpServiceStandardContext
     * @throws ServletException
     */
    public HttpServiceStandardContext getStandardContext(AliasInfo aliasInfo, HttpContext httpContext) throws ServletException {
        HttpServiceStandardContext httpServiceStandardContext = getStandardContext(aliasInfo);

        // If the container is missing, create it
        if (httpServiceStandardContext == null) {
            // TODO: Code should be shared with Tomcat7 deployer processor
            httpServiceStandardContext = new HttpServiceStandardContext(httpContext);
            httpServiceStandardContext.setPath(aliasInfo.getContextPath());

            // name equals to the path so that findChild() method can use the path to search the context on the host
            httpServiceStandardContext.setName(aliasInfo.getContextPath());

            //FIXME : use a service
            Path tmpFile;
            try {
                tmpFile = Files.createTempDirectory("tomcat");
                httpServiceStandardContext.setDocBase(tmpFile.toFile().getAbsolutePath());
            } catch (IOException e) {
                throw new ServletException(String.format("Unable to create a temporary directory"), e);
            }

            // add the context config
            final ContextConfig config = tomcat7Service.createContextConfig();
            httpServiceStandardContext.addLifecycleListener(config);

            // Set the PG Instance Manager
            httpServiceStandardContext.addLifecycleListener(new InstanceManagerLifeCycleListener(null));

            // Gets the default host
            final Host host = this.tomcat7Service.getDefaultHost();

            // add the context
            host.addChild(httpServiceStandardContext);
        }
        return httpServiceStandardContext;
    }

    /**
     * Extract context and servlet path from the given alias.
     * @param alias the alias to analyze
     * @return the alias info object containing both context and servlet path
     */
    public AliasInfo getAliasInfo(String alias) {
        // ensure the alias is well formed
        // The alias is the name in the URI namespace of the Http Service at
        // which the registration will be mapped. An alias must begin with slash
        // (’/’) and must not end with slash (’/’), with the exception that an
        // alias of the form “/” is used to denote the root alias. The name
        // parameter must also not end with slash (’/’) with the exception that
        // a name of the form “/” is used to denote the root of the bundle. See
        // the specification text for details on how HTTP requests are mapped to
        // servlet and resource registra- tions.

        if (alias == null) {
            throw new IllegalArgumentException("The alias cannot be null");
        }

        if (!alias.startsWith("/")) {
            throw new IllegalArgumentException(String.format("The alias needs to start with a /. Value found is %s",
                    alias));
        }
        if (!"/".equals(alias) && alias.endsWith("/")) {
            throw new IllegalArgumentException(String.format("The alias shouldn't ends with a /. Value found is %s",
                    alias));
        }

        int slash = alias.indexOf('/', 1);
        if (slash >= 0) {
            return new AliasInfo(alias.substring(0, slash), alias.substring(slash));
        }
        return new AliasInfo(alias, "");
    }

    /**
     * Register the servlet with the given alias and init parameters
     */
    @Override
    public void registerServlet(String alias, Servlet servlet, Dictionary<String, String> initparams,
            HttpContext httpContext, Bundle callerBundle) throws ServletException, NamespaceException {

        // Extract data from the alias
        AliasInfo aliasInfo = getAliasInfo(alias);

        // Gets the context for the given contextPath
        HttpServiceStandardContext httpServiceStandardContext = getStandardContext(aliasInfo, httpContext);

        // adds the servlet endpoint
        Artifact artifact = bundleArtifactManager.getArtifact(callerBundle);
        if (artifact != null) {
            Endpoints endpoints = artifact.as(Endpoints.class);
            for (URI uri : httpServiceStandardContext.getContextURIs()) {
                try {
                    // remove context from uri
                    String rootUri = uri.toString().substring(0, uri.toString().length() - httpServiceStandardContext.getPath().length());
                    String[] categories;
                    if (servlet instanceof WrappingResourceInServlet) {
                        categories = new String[] { "OSGi", "Resource" };
                    } else {
                        categories = new String[] { "OSGi", "Servlet" };
                    }
                    endpoints.register(new URI(rootUri.concat(alias)), categories);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Unable to register endpoint", e);
                }
            }
        }

        // Check servlet path is unique
        if (httpServiceStandardContext.findChild(aliasInfo.getServletPath()) != null) {
            throw new NamespaceException(String.format(
                    "Unable to register the given servlet as the path %s is already used", aliasInfo));
        }

        // Creates the Tomcat wrapper for the given servlet
        Wrapper wrapper = httpServiceStandardContext.createWrapper();
        wrapper.setName(aliasInfo.getServletPath());
        wrapper.setServlet(servlet);

        WebServlet webServlet = Servlet.class.getAnnotation(WebServlet.class);
        if (webServlet != null && webServlet.asyncSupported()) {
                wrapper.setAsyncSupported(true);
        }
        wrapper.getPipeline().addValve(new HttpContextSecurityValve(httpContext));

        // adds the wrapper
        wrapperLock.lock();
        try {
            wrappers.add(wrapper);
        } finally {
            wrapperLock.unlock();
        }

        // Sets the classloader
        wrapper.setParentClassLoader(servlet.getClass().getClassLoader());

        // add parameters for the servlet
        if (initparams != null) {
            Enumeration<String> keys = initparams.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                String value = initparams.get(key);
                wrapper.addInitParameter(key, value);
            }
        }

        // Adds the servlet in the given standard context
        httpServiceStandardContext.addChild(wrapper);

        // initialize the servlet (use allocate and then deallocate to put object back into the pool)
        // the allocate method allowing us to initialize the servlet
        Servlet instance = wrapper.allocate();
        wrapper.deallocate(instance);

        // Adds the servlet mapping
        httpServiceStandardContext.addServletMapping(aliasInfo.getServletPath() + "/*", wrapper.getName(), true);
    }

    /**
     * Unregister the servlet found for the given alias
     */
    @Override
    public void unregister(String alias, Bundle callerBundle) {

        // Get alias
        AliasInfo aliasInfo = getAliasInfo(alias);

        // Gets the Hosting context
        HttpServiceStandardContext httpServiceStandardContext = getStandardContext(aliasInfo);
        if (httpServiceStandardContext == null) {
            throw new IllegalArgumentException(String.format(
                    "Unable to unregister alias %s as this alias is not registered", alias));
        }

        // Gets the associated wrapper
        Container container = httpServiceStandardContext.findChild(aliasInfo.getServletPath());
        // Is it a wrapper ?
        if (!(container instanceof StandardWrapper)) {
            throw new IllegalArgumentException(String.format(
                    "Unable to unregister alias %s as this alias is not a wrapper", alias));
        }
        wrapperLock.lock();
        try {
            Wrapper wrapper = (Wrapper) container;
            unregisterWrapper(wrapper);
            wrappers.remove(wrapper);

            // If we only have jsp and default wrapper, remove the context itself
            if (httpServiceStandardContext.findChildren().length == 2 && httpServiceStandardContext.findChild("default") != null && httpServiceStandardContext.findChild("jsp") != null) {
                httpServiceStandardContext.getParent().removeChild(httpServiceStandardContext);
            }

        } finally {
            wrapperLock.unlock();
        }
    }

    /**
     * Unregister all the wrappers.
     */
    @Override
    public void unregisterAll(Bundle callerBundle) {
        wrapperLock.lock();
        try {
            for (Wrapper wrapper : wrappers) {
                unregisterWrapper(wrapper);
            }
            wrappers.clear();
        } finally {
            wrapperLock.unlock();
        }
    }

    /**
     * Unregister the given wrapper.
     * @param wrapper the wrapper to unregister
     */
    protected void unregisterWrapper(Wrapper wrapper) {
        wrapperLock.lock();
        try {
            Container container = wrapper.getParent();
            StandardContext standardContext = (StandardContext) container;

            // Remove the mapping
            standardContext.removeServletMapping(wrapper.getName() + "/*");

            // remove the wrapper from the parent
            container.removeChild(wrapper);
        } finally {
            // unlock
            wrapperLock.unlock();
        }
    }

}
