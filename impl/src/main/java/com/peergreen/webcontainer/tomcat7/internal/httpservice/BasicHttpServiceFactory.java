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

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

import com.peergreen.webcontainer.tomcat7.internal.InternalTomcat7HttpService;

@Component
@Provides
@Instantiate
public class BasicHttpServiceFactory implements ServiceFactory<HttpService> {

    /**
     * Bundle Context.
     */
    private final BundleContext bundleContext;

    /**
     * Tomcat7 Http Service.
     */
    @Requires
    private InternalTomcat7HttpService tomcat7httpService;

    /**
     * Service Registration.
     */
    private ServiceRegistration<HttpService> serviceRegistration;


    /**
     * Default constructor with the given bundle context.
     * @param bundleContext used to register the service
     */
    public BasicHttpServiceFactory(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }


    /**
     * Register this factory as HttpService provider.
     */
    @SuppressWarnings("unchecked")
    @Validate
    public void start() {
        serviceRegistration = (ServiceRegistration<HttpService>) bundleContext.registerService(HttpService.class.getName(), this, null);
    }

    /**
     * Unregister this factory as HttpService provider.
     */
    @Invalidate
    public void stop() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }



    /**
     * Creates a new service object.
     *
     * <p>
     * The Framework invokes this method the first time the specified
     * {@code bundle} requests a service object using the
     * {@code BundleContext.getService(ServiceReference)} method. The service
     * factory can then return a specific service object for each bundle.
     *
     * <p>
     * The Framework must check that the returned service object is valid. If
     * the returned service object is {@code null} or is not an
     * {@code instanceof} all the classes named when the service was registered,
     * a framework event of type {@link FrameworkEvent#ERROR} is fired
     * containing a service exception of type
     * {@link ServiceException#FACTORY_ERROR} and {@code null} is returned to
     * the bundle. If this method throws an exception, a framework event of type
     * {@link FrameworkEvent#ERROR} is fired containing a service exception of
     * type {@link ServiceException#FACTORY_EXCEPTION} with the thrown exception
     * as the cause and {@code null} is returned to the bundle. If this method
     * is recursively called for the specified bundle, a framework event of type
     * {@link FrameworkEvent#ERROR} is fired containing a service exception of
     * type {@link ServiceException#FACTORY_RECURSION} and {@code null} is
     * returned to the bundle.
     *
     * <p>
     * The Framework caches the valid service object and will return the same
     * service object on any future call to {@code BundleContext.getService} for
     * the specified bundle. This means the Framework must not allow this method
     * to be concurrently called for the specified bundle.
     *
     * @param bundle The bundle requesting the service.
     * @param registration The {@code ServiceRegistration} object for the
     *        requested service.
     * @return A service object that <strong>must</strong> be an instance of all
     *         the classes named when the service was registered.
     * @see BundleContext#getService(ServiceReference)
     */
    @Override
    public HttpService getService(Bundle bundle, ServiceRegistration<HttpService> registration) {
        return new BasicHttpService(tomcat7httpService, bundle);
    }


    /**
     * Releases a service object.
     *
     * <p>
     * The Framework invokes this method when a service has been released by a
     * bundle. The service object may then be destroyed.
     *
     * <p>
     * If this method throws an exception, a framework event of type
     * {@link FrameworkEvent#ERROR} is fired containing a service exception of
     * type {@link ServiceException#FACTORY_EXCEPTION} with the thrown exception
     * as the cause.
     *
     * @param bundle The bundle releasing the service.
     * @param registration The {@code ServiceRegistration} object for the
     *        service being released.
     * @param service The service object returned by a previous call to the
     *        {@link #getService(Bundle, ServiceRegistration) getService}
     *        method.
     * @see BundleContext#ungetService(ServiceReference)
     */
    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<HttpService> registration, HttpService service) {
       ((BasicHttpService) service).stop();
    }


}
