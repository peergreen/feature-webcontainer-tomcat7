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

import javax.servlet.ServletContext;

import org.osgi.service.http.HttpContext;

import com.peergreen.webcontainer.tomcat7.internal.core.PeergreenStandardContext;

/**
 * Tomcat context customized for the Http Service.
 * @author Florent Benoit
 */
public class HttpServiceStandardContext extends PeergreenStandardContext {

    /**
     * Wrapped Http Context.
     */
    private final HttpContext httpContext;

    public HttpServiceStandardContext(HttpContext httpContext) {
        super();
        this.httpContext = httpContext;
    }

    /**
     * Use a specific Servlet Context.
     */
    @Override
    public ServletContext getServletContext() {
        if (context == null) {
            this.context = new HttpServiceServletContext(this);
        }
        return context;
    }

    /**
     * @return the wrapped HttpContext.
     */
    protected HttpContext getHttpContext() {
        return httpContext;
    }


}
