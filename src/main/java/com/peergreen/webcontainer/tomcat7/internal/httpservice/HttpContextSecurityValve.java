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

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.osgi.service.http.HttpContext;

/**
 * Valve used to check the security through the http context calls.
 * @author Florent Benoit
 */
public class HttpContextSecurityValve extends ValveBase {

    /**
     * Http Context used to check the security.
     */
    private HttpContext httpContext = null;

    /**
     * Build an instance of the valve with the given http context
     * @param httpContext the http context
     */
    public HttpContextSecurityValve(HttpContext httpContext) {
        this.httpContext = httpContext;
    }


    /**
     * Check the security for the given request
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

        // check the security
        if (!httpContext.handleSecurity(request, response)) {
            return;
        }

        // Invoke next valve
        getNext().invoke(request, response);
    }


}
