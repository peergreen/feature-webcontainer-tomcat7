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

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.annotation.HandlesTypes;

import org.apache.catalina.startup.ContextConfig;
import org.xml.sax.InputSource;

/**
 * Allows to define a configuration for a Tomcat context.
 * @author Florent Benoit
 */
public class PeergreenContextConfig extends ContextConfig {


    /**
     * Gets the global Web XML source from the Bundle resources.
     */
    @Override
    protected InputSource getGlobalWebXmlSource() {
        //FIXME : should use the config repository to find the resource
        return new InputSource(PeergreenContextConfig.class.getResource("/tomcat7-web.xml").toExternalForm());
    }


    /**
     * Adds the given servlet container initializer.
     * @param sci the servlet container initializer
     */
    public void addServletContainerInitializer(ServletContainerInitializer sci) {
        initializerClassMap.put(sci, new HashSet<Class<?>>());

        HandlesTypes ht = sci.getClass().getAnnotation(HandlesTypes.class);

        if (ht != null) {
            Class<?>[] types = ht.value();
            if (types != null) {
                for (Class<?> type : types) {
                    if (type.isAnnotation()) {
                        handlesTypesAnnotations = true;
                    } else {
                        handlesTypesNonAnnotations = true;
                    }
                    Set<ServletContainerInitializer> scis =
                            typeInitializerMap.get(type);
                    if (scis == null) {
                        scis = new HashSet<ServletContainerInitializer>();
                        typeInitializerMap.put(type, scis);
                    }
                    scis.add(sci);
                }
            }
        }

    }
}
