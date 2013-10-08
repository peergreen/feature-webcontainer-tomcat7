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

import com.peergreen.deployment.ProcessorContext;
import com.peergreen.deployment.ProcessorException;
import com.peergreen.deployment.processor.Phase;
import com.peergreen.deployment.processor.Processor;
import com.peergreen.webcontainer.tomcat7.PeergreenContext;
import com.peergreen.webcontainer.tomcat7.TomcatWebApplication;

/**
 * Stop the given web application.
 * @author Florent Benoit
 */
@Processor
@Phase("STOP")
public class TomcatWebApplicationStopProcessor {


    public void handle(TomcatWebApplication tomcatWebApplication, ProcessorContext processorContext) throws ProcessorException {

        PeergreenContext context = tomcatWebApplication.getContext();
        if (context != null) {
            context.getParent().removeChild(context);
        }
    }

}
