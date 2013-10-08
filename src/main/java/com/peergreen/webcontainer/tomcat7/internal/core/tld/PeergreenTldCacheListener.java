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
package com.peergreen.webcontainer.tomcat7.internal.core.tld;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;

import org.apache.catalina.InstanceEvent;
import org.apache.catalina.InstanceListener;
import org.apache.jasper.EmbeddedServletOptions;
import org.apache.jasper.compiler.TldLocationsCache;
import org.apache.jasper.servlet.JspServlet;

/**
 * Adds the TLD in the cache
 * @author Florent Benoit
 */
public class PeergreenTldCacheListener implements InstanceListener {

    /**
     * Collection of urls.
     */
    private Collection<URL> urls = null;

    /**
     * Build the cache listener around the given urls
     * @param urls the URLs
     */
    public PeergreenTldCacheListener(final Collection<URL> urls) {
        this.urls = urls;
    }

    /**
     * Notification on lifecycle events
     * @param event the given event
     */
    @Override
    public void instanceEvent(final InstanceEvent event) {
        // Only react when the instance when just alive (but not already inited)
        if (InstanceEvent.AFTER_INIT_EVENT.equals(event.getType())) {

            // The servlet is a JspServlet
            JspServlet jspServlet = (JspServlet) event.getServlet();
            Field optionsField;
            try {
                optionsField = JspServlet.class.getDeclaredField("options");
            } catch (NoSuchFieldException | SecurityException e) {
                throw new IllegalStateException("Unable to get the field", e);
            }
            optionsField.setAccessible(true);
            EmbeddedServletOptions options;
            try {
                options = (EmbeddedServletOptions) optionsField.get(jspServlet);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new IllegalStateException("Unable to get the options", e);
            }
            TldLocationsCache tldLocationCache = options.getTldLocationsCache();

            // Retrieve method
            Method tldScanStreamMethod;
            try {
                tldScanStreamMethod = TldLocationsCache.class.getDeclaredMethod("tldScanStream", String.class,
                        String.class, InputStream.class);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new IllegalStateException("Unable to get method", e);
            }
            // make it callable
            tldScanStreamMethod.setAccessible(true);

            // Parse each given url
            if (urls != null) {
                for (URL url : urls) {
                    URLConnection urlConnection;
                    try {
                        urlConnection = url.openConnection();
                    } catch (IOException e) {
                        throw new IllegalStateException("Unable to get the connection", e);
                    }
                    urlConnection.setDefaultUseCaches(false);
                    try (InputStream is = urlConnection.getInputStream()) {
                        tldScanStreamMethod.invoke(tldLocationCache, url.toString(), null, is);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | IOException e) {
                        throw new IllegalStateException("Unable to invoke the scan method", e);
                    }
                }
            }
        }
    }
}
