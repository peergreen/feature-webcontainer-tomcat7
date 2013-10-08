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

import java.net.URL;
import java.util.Collection;

import org.apache.catalina.startup.ContextConfig;
import org.apache.felix.ipojo.annotations.Requires;

import com.peergreen.deployment.ProcessorContext;
import com.peergreen.deployment.ProcessorException;
import com.peergreen.deployment.processor.Phase;
import com.peergreen.deployment.processor.Processor;
import com.peergreen.webcontainer.WebApplication;
import com.peergreen.webcontainer.tomcat7.TomcatWebApplication;
import com.peergreen.webcontainer.tomcat7.internal.DefaultTomcatWebApplication;
import com.peergreen.webcontainer.tomcat7.internal.InternalTomcat7Service;
import com.peergreen.webcontainer.tomcat7.internal.core.InstanceManagerLifeCycleListener;
import com.peergreen.webcontainer.tomcat7.internal.core.PeergreenStandardContext;
import com.peergreen.webcontainer.tomcat7.internal.core.tld.PeergreenTldListener;

/**
 * WAR scanner.
 * @author Florent Benoit
 */
@Processor
@Phase("INIT")
public class WebApplicationDeployerProcessor {


    private final InternalTomcat7Service tomcat7Service;

    public WebApplicationDeployerProcessor(@Requires InternalTomcat7Service tomcat7Service) {
        this.tomcat7Service = tomcat7Service;
    }

    public void handle(WebApplication webApplication, ProcessorContext processorContext) throws ProcessorException {

        // Creates the context
        final PeergreenStandardContext context = new PeergreenStandardContext();

        // Uses of Peergreen naming
        context.setUseNaming(false);

        // Sets the docbase
        context.setDocBase(webApplication.getUnpackedDirectory().getPath());
        // Do not unpack the war (already done)
        context.setUnpackWAR(false);

        // sets the context of the application
        context.setPath(webApplication.getContextPath());

        // add the context config
        ContextConfig contextConfig = tomcat7Service.createContextConfig();
        context.addLifecycleListener(contextConfig);

        // Sets PG Tld Listener
        Collection<URL> tldUrls = webApplication.getExtraTlds();
        if (tldUrls != null && tldUrls.size() > 0) {
            context.addLifecycleListener(new PeergreenTldListener(tldUrls));
        }

        // Set the PG Instance Manager
        context.addLifecycleListener(new InstanceManagerLifeCycleListener(webApplication));

        // Sets the parent classloader
        context.setParentClassLoader(webApplication.getClassLoader());

        TomcatWebApplication tomcatWebApplication = new DefaultTomcatWebApplication();
        tomcatWebApplication.setContext(context);

        // add tomcat web application
        processorContext.addFacet(TomcatWebApplication.class, tomcatWebApplication);

    }


}
