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

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import com.peergreen.deployment.DelegateHandlerProcessor;
import com.peergreen.deployment.ProcessorException;
import com.peergreen.deployment.resource.builder.RequirementBuilder;
import com.peergreen.webcontainer.WebApplicationLifeCycle;
import com.peergreen.webcontainer.tomcat7.Tomcat7Service;
import com.peergreen.webcontainer.tomcat7.internal.TomcatWebApplication;

@Component
@Provides
@Instantiate
public class TomcatWebApplicationStartDelegateInternalProcessor extends DelegateHandlerProcessor<TomcatWebApplication> {

    public TomcatWebApplicationStartDelegateInternalProcessor(@Requires Tomcat7Service tomcat7Service) throws ProcessorException {
        super(new TomcatWebApplicationStartProcessor(tomcat7Service), TomcatWebApplication.class);
    }

    @Override
    @Bind(optional=false)
    public void bindRequirementBuilder(RequirementBuilder requirementBuilder) {
        super.bindRequirementBuilder(requirementBuilder);
    }


    @Validate
    protected void addRequirements() {

        // Execute only on Tomcat Applications
        addRequirement(getRequirementBuilder().buildFacetRequirement(this, TomcatWebApplication.class));

        // Execute at the START phase
        addRequirement(getRequirementBuilder().buildPhaseRequirement(this, WebApplicationLifeCycle.START.toString()));

    }

}
