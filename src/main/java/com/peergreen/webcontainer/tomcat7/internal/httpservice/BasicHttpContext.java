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
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * Implementation of {@link HttpContext} OSGi interface.
 * @author Florent Benoit
 */
public class BasicHttpContext implements HttpContext {

    /**
     * Bundle which has called the HTTP service.
     */
    private final Bundle bundle;

    /**
     * Build a context on the given bundle.
     * @param bundle the bundle that obtains the HttpService reference
     */
    public BasicHttpContext(Bundle bundle) {
        this.bundle = bundle;
    }


    /**
     * Handles security for the specified request.
     *
     * <p>
     * The Http Service calls this method prior to servicing the specified
     * request. This method controls whether the request is processed in the
     * normal manner or an error is returned.
     *
     * <p>
     * If the request requires authentication and the Authorization header in
     * the request is missing or not acceptable, then this method should set the
     * WWW-Authenticate header in the response object, set the status in the
     * response object to Unauthorized(401) and return {@code false}. See also
     * RFC 2617: <i>HTTP Authentication: Basic and Digest Access Authentication
     * </i> (available at http://www.ietf.org/rfc/rfc2617.txt).
     *
     * <p>
     * If the request requires a secure connection and the {@code getScheme}
     * method in the request does not return 'https' or some other acceptable
     * secure protocol, then this method should set the status in the response
     * object to Forbidden(403) and return {@code false}.
     *
     * <p>
     * When this method returns {@code false}, the Http Service will send the
     * response back to the client, thereby completing the request. When this
     * method returns {@code true}, the Http Service will proceed with servicing
     * the request.
     *
     * <p>
     * If the specified request has been authenticated, this method must set the
     * {@link #AUTHENTICATION_TYPE} request attribute to the type of
     * authentication used, and the {@link #REMOTE_USER} request attribute to
     * the remote user (request attributes are set using the
     * {@code setAttribute} method on the request). If this method does not
     * perform any authentication, it must not set these attributes.
     *
     * <p>
     * If the authenticated user is also authorized to access certain resources,
     * this method must set the {@link #AUTHORIZATION} request attribute to the
     * {@code Authorization} object obtained from the
     * {@code org.osgi.service.useradmin.UserAdmin} service.
     *
     * <p>
     * The servlet responsible for servicing the specified request determines
     * the authentication type and remote user by calling the
     * {@code getAuthType} and {@code getRemoteUser} methods, respectively, on
     * the request.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @return {@code true} if the request should be serviced, {@code false} if
     *         the request should not be serviced and Http Service will send the
     *         response back to the client.
     * @throws java.io.IOException may be thrown by this method. If this occurs,
     *         the Http Service will terminate the request and close the socket.
     */
    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // return true for all requests
        return true;
    }

    /**
     * Maps a resource name to a URL.
     *
     * <p>
     * Called by the Http Service to map a resource name to a URL. For servlet
     * registrations, Http Service will call this method to support the
     * {@code ServletContext} methods {@code getResource} and
     * {@code getResourceAsStream}. For resource registrations, Http Service
     * will call this method to locate the named resource. The context can
     * control from where resources come. For example, the resource can be
     * mapped to a file in the bundle's persistent storage area via
     * {@code bundleContext.getDataFile(name).toURL()} or to a resource in the
     * context's bundle via {@code getClass().getResource(name)}
     *
     * @param name the name of the requested resource
     * @return URL that Http Service can use to read the resource or
     *         {@code null} if the resource does not exist.
     */
    @Override
    public URL getResource(String name) {
        // redirect to the caller bundle
        return bundle.getResource(name);
    }


    /**
     * Maps a name to a MIME type.
     *
     * Called by the Http Service to determine the MIME type for the name. For
     * servlet registrations, the Http Service will call this method to support
     * the {@code ServletContext} method {@code getMimeType}. For resource
     * registrations, the Http Service will call this method to determine the
     * MIME type for the Content-Type header in the response.
     *
     * @param name determine the MIME type for this name.
     * @return MIME type (e.g. text/html) of the name or {@code null} to
     *         indicate that the Http Service should determine the MIME type
     *         itself.
     */
    @Override
    public String getMimeType(String name) {
        // the http service will determine the MIME type itself
        return null;
    }
}
