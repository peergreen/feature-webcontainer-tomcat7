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
package com.peergreen.webcontainer.tomcat7.internal;

import java.util.Dictionary;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

/**
 * Interface used by HttpService implementation in order to perform actions
 * @author Florent Benoit
 */
public interface InternalTomcat7HttpService {

    /**
     * Register the servlet with the given alias and init parameters
     */
    void registerServlet(String alias, Servlet servlet, Dictionary<String, String> initparams, HttpContext context) throws ServletException, NamespaceException;

    /**
     * Unregister the given alias
     * @param alias the alias of the resource/servlet
     */
    void unregister(String alias);

    /**
     * Unregister all the wrappers (resource/servlets)
     */
    void unregisterAll();
}
