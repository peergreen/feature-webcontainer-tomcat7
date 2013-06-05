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
package com.peergreen.webcontainer.tomcat7.internal.core;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.StandardContext;
import org.apache.tomcat.InstanceManager;

import com.peergreen.webcontainer.WebApplication;

/**
 * Allows to define a custom InstanceManager for a context
 * @author Florent Benoit
 */
public class InstanceManagerLifeCycleListener implements LifecycleListener {

    /**
     * Web application
     */
    private final WebApplication webApplication;

    /**
     * Defines the constructor with the given bundle context.
     * @param bundleContext bundle context used to inject data
     */
    public InstanceManagerLifeCycleListener(WebApplication webApplication) {
        this.webApplication = webApplication;
    }


    /**
     * Replace the instance manager when the context is starting
     */
    @Override
    public void lifecycleEvent(LifecycleEvent event) {

        if (Lifecycle.CONFIGURE_START_EVENT.equals(event.getType())) {
            StandardContext standardContext = ((StandardContext) event.getLifecycle());
            InstanceManager instanceManager = new PeergreenInstanceManager(webApplication, standardContext);
            standardContext.setInstanceManager(instanceManager);
            standardContext.getServletContext().setAttribute(InstanceManager.class.getName(), standardContext.getInstanceManager());
        }

    }

}
