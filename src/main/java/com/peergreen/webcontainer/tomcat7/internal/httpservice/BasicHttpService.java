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

import java.util.Dictionary;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import com.peergreen.webcontainer.tomcat7.internal.InternalTomcat7HttpService;

/**
 * Implementation of the {@link HttpService} interface. This component is not an
 * iPOJO component as there is one HttpService instance per client through the
 * {@link ServiceFactory} {@link BasicHttpServiceFactory}
 * @author Florent Benoit
 */
public class BasicHttpService implements HttpService {

    /**
     * Service (OSGi component) used to delegate operations.
     */
    private final InternalTomcat7HttpService tomcat7HttpService;

    /**
     * Bundle which is perfoming the request on the http service.
     */
    private final Bundle bundle;

    /**
     * New instance of the Http Service.
     * @param tomcat7HttpService delegatin service
     * @param bundle the bundle performing requests.
     */
    public BasicHttpService(InternalTomcat7HttpService tomcat7HttpService, Bundle bundle) {
        this.tomcat7HttpService = tomcat7HttpService;
        this.bundle = bundle;
    }

    /**
     * Registers a servlet into the URI namespace.
     * <p>
     * The alias is the name in the URI namespace of the Http Service at which
     * the registration will be mapped.
     * <p>
     * An alias must begin with slash ('/') and must not end with slash ('/'),
     * with the exception that an alias of the form &quot;/&quot; is used to
     * denote the root alias. See the specification text for details on how HTTP
     * requests are mapped to servlet and resource registrations.
     * <p>
     * The Http Service will call the servlet's {@code init} method before
     * returning.
     *
     * <pre>
     * httpService.registerServlet(&quot;/myservlet&quot;, servlet, initparams, context);
     * </pre>
     * <p>
     * Servlets registered with the same {@code HttpContext} object will share
     * the same {@code ServletContext}. The Http Service will call the
     * {@code context} argument to support the {@code ServletContext} methods
     * {@code getResource},{@code getResourceAsStream} and {@code getMimeType},
     * and to handle security for requests. If the {@code context} argument is
     * {@code null}, a default {@code HttpContext} object is used (see
     * {@link #createDefaultHttpContext()}).
     * @param alias name in the URI namespace at which the servlet is registered
     * @param servlet the servlet object to register
     * @param initparams initialization arguments for the servlet or
     * {@code null} if there are none. This argument is used by the servlet's
     * {@code ServletConfig} object.
     * @param context the {@code HttpContext} object for the registered servlet,
     * or {@code null} if a default {@code HttpContext} is to be created and
     * used.
     * @throws NamespaceException if the registration fails because the alias is
     * already in use.
     * @throws javax.servlet.ServletException if the servlet's {@code init}
     * method throws an exception, or the given servlet object has already been
     * registered at a different alias.
     * @throws java.lang.IllegalArgumentException if any of the arguments are
     * invalid
     */
    @SuppressWarnings("unchecked")
    @Override
    public void registerServlet(String alias, Servlet servlet, @SuppressWarnings("rawtypes") Dictionary initparams,
            HttpContext context) throws ServletException, NamespaceException {

        // Create a new context if there is none
        if (context == null) {
            context = createDefaultHttpContext();
        }

        tomcat7HttpService.registerServlet(alias, servlet, initparams, context, bundle);

    }

    /**
     * Registers resources into the URI namespace.
     * <p>
     * The alias is the name in the URI namespace of the Http Service at which
     * the registration will be mapped. An alias must begin with slash ('/') and
     * must not end with slash ('/'), with the exception that an alias of the
     * form &quot;/&quot; is used to denote the root alias. The name parameter
     * must also not end with slash ('/') with the exception that a name of the
     * form &quot;/&quot; is used to denote the root of the bundle. See the
     * specification text for details on how HTTP requests are mapped to servlet
     * and resource registrations.
     * <p>
     * For example, suppose the resource name /tmp is registered to the alias
     * /files. A request for /files/foo.txt will map to the resource name
     * /tmp/foo.txt.
     *
     * <pre>
     * httpservice.registerResources(&quot;/files&quot;, &quot;/tmp&quot;, context);
     * </pre>
     *
     * The Http Service will call the {@code HttpContext} argument to map
     * resource names to URLs and MIME types and to handle security for
     * requests. If the {@code HttpContext} argument is {@code null}, a default
     * {@code HttpContext} is used (see {@link #createDefaultHttpContext()}).
     * @param alias name in the URI namespace at which the resources are
     * registered
     * @param name the base name of the resources that will be registered
     * @param context the {@code HttpContext} object for the registered
     * resources, or {@code null} if a default {@code HttpContext} is to be
     * created and used.
     * @throws NamespaceException if the registration fails because the alias is
     * already in use.
     * @throws java.lang.IllegalArgumentException if any of the parameters are
     * invalid
     */
    @Override
    public void registerResources(String alias, String name, HttpContext context) throws NamespaceException {

        // Create a new context if there is none
        if (context == null) {
            context = createDefaultHttpContext();
        }

        // wrap the resource in a servlet
        Servlet servlet = new WrappingResourceInServlet(name, context);

        // And register the servlet
        try {
            this.registerServlet(alias, servlet, null, context);
        } catch (ServletException e) {
            throw new NamespaceException("Unable to wrap resource in a servlet", e);
        }
    }

    /**
     * Unregisters a previous registration done by {@code registerServlet} or
     * {@code registerResources} methods.
     * <p>
     * After this call, the registered alias in the URI name-space will no
     * longer be available. If the registration was for a servlet, the Http
     * Service must call the {@code destroy} method of the servlet before
     * returning.
     * <p>
     * If the bundle which performed the registration is stopped or otherwise
     * "unget"s the Http Service without calling {@link #unregister(String)}
     * then Http Service must automatically unregister the registration.
     * However, if the registration was for a servlet, the {@code destroy}
     * method of the servlet will not be called in this case since the bundle
     * may be stopped. {@link #unregister(String)} must be explicitly called to
     * cause the {@code destroy} method of the servlet to be called. This can be
     * done in the {@code BundleActivator.stop} method of the bundle registering
     * the servlet.
     * @param alias name in the URI name-space of the registration to unregister
     * @throws java.lang.IllegalArgumentException if there is no registration
     * for the alias or the calling bundle was not the bundle which registered
     * the alias.
     */
    @Override
    public void unregister(String alias) {
        tomcat7HttpService.unregister(alias, bundle);
    }

    /**
     * Creates a default {@code HttpContext} for registering servlets or
     * resources with the HttpService, a new {@code HttpContext} object is
     * created each time this method is called.
     * <p>
     * The behavior of the methods on the default {@code HttpContext} is defined
     * as follows:
     * <ul>
     * <li>{@code getMimeType}- Does not define any customized MIME types for
     * the Content-Type header in the response, and always returns {@code null}.
     * <li>{@code handleSecurity}- Performs implementation-defined
     * authentication on the request.
     * <li>{@code getResource}- Assumes the named resource is in the context
     * bundle; this method calls the context bundle's {@code Bundle.getResource}
     * method, and returns the appropriate URL to access the resource. On a Java
     * runtime environment that supports permissions, the Http Service needs to
     * be granted {@code org.osgi.framework.AdminPermission[*,RESOURCE]}.
     * </ul>
     * @return a default {@code HttpContext} object.
     * @since 1.1
     */
    @Override
    public HttpContext createDefaultHttpContext() {
        // Default context load the resources in the bundle
        return new BasicHttpContext(bundle);
    }

    /**
     * Extra method used to stop this http service by unregistering all aliases
     * that were registered()
     */
    public void stop() {
        // unregister all aliases
        tomcat7HttpService.unregisterAll(bundle);
    }

}
