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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.catalina.core.ApplicationContext;
import org.osgi.service.http.HttpContext;

/**
 * Defines the servlet context by calling the specified {@link HttpContext}
 * @author Florent Benoit
 */
public class HttpServiceServletContext extends ApplicationContext {

    /**
     * Wrapped Http context.
     */
    private final HttpContext httpContext;

    public HttpServiceServletContext(final HttpServiceStandardContext standardContext) {
        super(standardContext);
        this.httpContext = standardContext.getHttpContext();
    }

    /**
     * Returns the MIME type of the specified file, or null if the MIME type is
     * not known. The MIME type is determined by calling
     * {@link HttpContext#getMimeType(String)}. If this method returns
     * <code>null</code>, the {@link ApplicationContext#getMimeType(String)}
     * method is called.
     * @param file a String specifying the name of a file
     * @return a String specifying the file's MIME type
     */
    @Override
    public String getMimeType(final String file) {
        final String mime = this.httpContext.getMimeType(file);
        if (mime != null) {
            return mime;
        }

        return super.getMimeType(file);
    }


    /**
     * Returns a URL to the resource that is mapped to a specified path. The
     * path must begin with a "/" and is interpreted as relative to the current
     * context root.
     * <p>
     * This method allows the servlet container to make a resource available to
     * servlets from any source. Resources can be located on a local or remote
     * file system, in a database, or in a <code>.war</code> file.
     * <p>
     * The servlet container must implement the URL handlers and
     * <code>URLConnection</code> objects that are necessary to access the
     * resource.
     * <p>
     * This method returns <code>null</code> if no resource is mapped to the
     * pathname.
     * <p>
     * Some containers may allow writing to the URL returned by this method
     * using the methods of the URL class.
     * <p>
     * The resource content is returned directly, so be aware that requesting a
     * <code>.jsp</code> page returns the JSP source code. Use a
     * <code>RequestDispatcher</code> instead to include results of an
     * execution.
     * <p>
     * This method has a different purpose than
     * <code>java.lang.Class.getResource</code>, which looks up resources based
     * on a class loader. This method does not use class loaders.
     *
     * @param path
     *            a <code>String</code> specifying the path to the resource
     * @return the resource located at the named path, or <code>null</code> if
     *         there is no resource at that path
     * @exception MalformedURLException
     *                if the pathname is not given in the correct form
     */
    @Override
    public URL getResource(final String path) throws MalformedURLException {
        return this.httpContext.getResource(path);
    }


    /**
     * Returns the resource located at the named path as an
     * <code>InputStream</code> object.
     * <p>
     * The data in the <code>InputStream</code> can be of any type or length.
     * The path must be specified according to the rules given in
     * <code>getResource</code>. This method returns <code>null</code> if no
     * resource exists at the specified path.
     * <p>
     * Meta-information such as content length and content type that is
     * available via <code>getResource</code> method is lost when using this
     * method.
     * <p>
     * The servlet container must implement the URL handlers and
     * <code>URLConnection</code> objects necessary to access the resource.
     * <p>
     * This method is different from
     * <code>java.lang.Class.getResourceAsStream</code>, which uses a class
     * loader. This method allows servlet containers to make a resource
     * available to a servlet from any location, without using a class loader.
     *
     * @param path
     *            a <code>String</code> specifying the path to the resource
     * @return the <code>InputStream</code> returned to the servlet, or
     *         <code>null</code> if no resource exists at the specified path
     */
    @Override
    public InputStream getResourceAsStream(final String path) {

        // Get resource
        URL url;
        try {
            url = this.getResource(path);
        } catch (MalformedURLException e) {
            // unable to find the resource
            return null;
        }

        // Red stream
        if (url != null) {
            try {
                return url.openStream();
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

}
