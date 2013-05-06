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
package com.peergreen.webcontainer.tomcat7.internal.processor;

import org.osgi.framework.BundleContext;

import com.peergreen.deployment.Processor;
import com.peergreen.deployment.ProcessorContext;
import com.peergreen.deployment.ProcessorException;
import com.peergreen.webcontainer.WebApplication;
import com.peergreen.webcontainer.tomcat7.internal.TomcatWebApplication;
import com.peergreen.webcontainer.tomcat7.internal.classloader.DynamicImportAllClassLoader;
import com.peergreen.webcontainer.tomcat7.internal.core.InstanceManagerLifeCycleListener;
import com.peergreen.webcontainer.tomcat7.internal.core.PeergreenContextConfig;
import com.peergreen.webcontainer.tomcat7.internal.core.PeergreenStandardContext;

/**
 * WAR scanner.
 * @author Florent Benoit
 */
public class WebApplicationDeployerProcessor implements Processor<WebApplication> {

    private final BundleContext bundleContext;

    public WebApplicationDeployerProcessor(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
  }


    @Override
    public void handle(WebApplication webApplication, ProcessorContext processorContext) throws ProcessorException {

        // deploy the given web application
        System.out.println("Deploying the web application with context " + webApplication.getContextPath());

        // Creates the context
        final PeergreenStandardContext context = new PeergreenStandardContext();

        // sets the path to the war file
        context.setDocBase(webApplication.getURI().getPath());

        // sets the context of the application
        context.setPath(webApplication.getContextPath());

        // unpack the war if not yet unpacked
        context.setUnpackWAR(true);

        // add the context config
       PeergreenContextConfig contextConfig = new PeergreenContextConfig();
        context.addLifecycleListener(contextConfig);

        // Set the PG Instance Manager
        context.addLifecycleListener(new InstanceManagerLifeCycleListener(bundleContext));

        // Sets the parent class loader with the OSGi dynamic import classloader
        context.setParentClassLoader(new DynamicImportAllClassLoader());

        TomcatWebApplication tomcatWebApplication = new TomcatWebApplication();
        tomcatWebApplication.setContext(context);

        // add tomcat web application
        processorContext.addFacet(TomcatWebApplication.class, tomcatWebApplication);

    }


}
