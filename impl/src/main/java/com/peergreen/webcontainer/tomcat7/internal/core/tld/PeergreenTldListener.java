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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.TldConfig;
import org.apache.jasper.servlet.JspServlet;

/**
 * Adds in webapplication the TLDs that have been registered.
 * @author Florent Benoit
 */
public class PeergreenTldListener implements LifecycleListener {

    /**
     * As the TldConfig class is final, wrap it for use it.
     */
    private final TldConfig tldConfig;

    /**
     * List of tldURLs
     */
    private Collection<URL> tldURLs = null;

    /**
     * Tld Scan Stream method.
     */
    private Method tldScanStreamMethod = null;

    /**
     * The URLs to use
     * @param tldURLs the TLD urls
     */
    public PeergreenTldListener(final Collection<URL> tldURLs) {
        this.tldURLs = tldURLs;

        // make our own instance
        this.tldConfig = new TldConfig();

        try {
            this.tldScanStreamMethod = tldConfig.getClass().getDeclaredMethod("tldScanStream", InputStream.class);
            // private method
            this.tldScanStreamMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to find the given method", e);
        }

    }

    /**
     * Notify the tld scanner depending on the events.
     * @param event the lifecycle event
     */
    @Override
    public void lifecycleEvent(final LifecycleEvent event) {
        Context context = null;
        // Identify the context we are associated with
        try {
            context = (Context) event.getLifecycle();
        } catch (ClassCastException e) {
            return;
        }

        if (event.getType().equals(Lifecycle.AFTER_INIT_EVENT)) {
            // Init our internal tldconfig object
            tldConfig.lifecycleEvent(event);
        } else if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
            // needs to analyze the OSGi TLD resources
            if (tldURLs != null) {
                for (URL url : tldURLs) {
                    try {
                        URLConnection urlConnection = url.openConnection();
                        urlConnection.setDefaultUseCaches(false);
                        tldScanStreamMethod.invoke(tldConfig, urlConnection.getInputStream());
                    } catch (InvocationTargetException | IOException | IllegalAccessException | IllegalArgumentException e) {
                        throw new IllegalStateException("Unable to add the TLD", e);
                    }
                }
            }

            String[] listeners = tldConfig.getTldListeners();
            if (listeners != null) {
                for (String listener : listeners) {
                    context.addApplicationListener(listener);
                }
            }
            addTldIntoCache(context);
        } else if (event.getType().equals(Lifecycle.STOP_EVENT)) {
            // Stop our internal tldconfig object
            tldConfig.lifecycleEvent(event);
        }
    }

    /**
     * Adds listener on JSP in order to register the TLD.
     * @param context the context on which the TLDs will be added
     */
    protected void addTldIntoCache(final Context context) {

        // Get all children
        Container[] childs = context.findChildren();
        // For each wrapper that is a JspServlet, register the TLd listener
        if (childs != null) {
            for (Container child : childs) {
                Wrapper wrapper = (Wrapper) child;
                String servletClass = wrapper.getServletClass();
                if (JspServlet.class.getName().equals(servletClass)) {
                    wrapper.addInstanceListener(new PeergreenTldCacheListener(tldURLs));
                }
            }
        }
    }

}
