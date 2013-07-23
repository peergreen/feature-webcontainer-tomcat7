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

import java.net.URI;

import org.apache.catalina.Host;
import org.apache.felix.ipojo.annotations.Requires;

import com.peergreen.deployment.ProcessorContext;
import com.peergreen.deployment.ProcessorException;
import com.peergreen.deployment.facet.endpoint.Endpoints;
import com.peergreen.deployment.processor.Phase;
import com.peergreen.deployment.processor.Processor;
import com.peergreen.webcontainer.tomcat7.internal.InternalTomcat7Service;
import com.peergreen.webcontainer.tomcat7.internal.TomcatWebApplication;

/**
 * WAR scanner.
 * @author Florent Benoit
 */
@Processor
@Phase("START")
public class TomcatWebApplicationStartProcessor {

    private final InternalTomcat7Service tomcat7Service;

    public TomcatWebApplicationStartProcessor(@Requires InternalTomcat7Service tomcat7Service) {
        this.tomcat7Service = tomcat7Service;
    }

    public void handle(TomcatWebApplication tomcatWebApplication, ProcessorContext processorContext) throws ProcessorException {

        // Gets the host
        Host host = tomcat7Service.getDefaultHost();

        // Starts the context
        host.addChild(tomcatWebApplication.getContext());

        // add the context Endpoint
        Endpoints endpoints = processorContext.getArtifact().as(Endpoints.class);
        for (URI uri : tomcatWebApplication.getContext().getContextURIs()) {
            endpoints.register(uri, "HttpContext");
        }

    }

}
