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
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpContext;

/**
 * This class allows to wrap HttpService resources in a servlet.
 * @author Florent Benoit
 */
public class WrappingResourceInServlet extends HttpServlet {

    /**
     * Seial version UID.
     */
    private static final long serialVersionUID = 4740539360592241634L;

    private static final int BUFFER = 4096;

    private final String name;

    private final HttpContext httpContext;

    public WrappingResourceInServlet(String name, HttpContext httpContext) {
        this.name = name;
        this.httpContext = httpContext;
    }

    /**
     * Called by the server (via the <code>service</code> method) to allow a
     * servlet to handle a GET request.
     * <p>
     * Overriding this method to support a GET request also automatically
     * supports an HTTP HEAD request. A HEAD request is a GET request that
     * returns no body in the response, only the request header fields.
     * <p>
     * When overriding this method, read the request data, write the response
     * headers, get the response's writer or output stream object, and finally,
     * write the response data. It's best to include content type and encoding.
     * When using a <code>PrintWriter</code> object to return the response, set
     * the content type before accessing the <code>PrintWriter</code> object.
     * <p>
     * The servlet container must write the headers before committing the
     * response, because in HTTP the headers must be sent before the response
     * body.
     * <p>
     * Where possible, set the Content-Length header (with the
     * {@link javax.servlet.ServletResponse#setContentLength} method), to allow
     * the servlet container to use a persistent connection to return its
     * response to the client, improving performance. The content length is
     * automatically set if the entire response fits inside the response buffer.
     * <p>
     * When using HTTP 1.1 chunked encoding (which means that the response has a
     * Transfer-Encoding header), do not set the Content-Length header.
     * <p>
     * The GET method should be safe, that is, without any side effects for
     * which users are held responsible. For example, most form queries have no
     * side effects. If a client request is intended to change stored data, the
     * request should use some other HTTP method.
     * <p>
     * The GET method should also be idempotent, meaning that it can be safely
     * repeated. Sometimes making a method safe also makes it idempotent. For
     * example, repeating queries is both safe and idempotent, but buying a
     * product online or modifying data is neither safe nor idempotent.
     * <p>
     * If the request is incorrectly formatted, <code>doGet</code> returns an
     * HTTP "Bad Request" message.
     * @param req an {@link HttpServletRequest} object that contains the request
     * the client has made of the servlet
     * @param resp an {@link HttpServletResponse} object that contains the
     * response the servlet sends to the client
     * @exception IOException if an input or output error is detected when the
     * servlet handles the GET request
     * @exception ServletException if the request for the GET could not be
     * handled
     * @see javax.servlet.ServletResponse#setContentType
     */
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        String requestedName = req.getPathInfo();
        if (requestedName == null) {
            requestedName = "";
        } else if (requestedName.startsWith("/")) {
            requestedName = requestedName.substring(1);
        }

        String resourcePath = this.name.concat("/").concat(requestedName);

        URL url = this.httpContext.getResource(resourcePath);

        if (url == null) {
            // Send error
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // open the connection
        URLConnection urlConnection = url.openConnection();
        urlConnection.setDefaultUseCaches(false);

        // Encoding
        String contentEncoding = urlConnection.getContentEncoding();
        resp.setCharacterEncoding(contentEncoding);

        // Content type
        String contentType = urlConnection.getContentType();
        resp.setContentType(contentType);

        // COntent length
        int contentlength = urlConnection.getContentLength();
        resp.setContentLength(contentlength);

        // deliver resource content if there is content
        if (contentlength >= 0) {
            try (InputStream inputStream = urlConnection.getInputStream();
                    OutputStream outputStream = resp.getOutputStream()) {
                byte buffer[] = new byte[BUFFER];
                int read;
                while ((read = inputStream.read(buffer, 0, buffer.length)) != -1) {
                    outputStream.write(buffer, 0, read);
                    outputStream.flush();
                }
            }
        }
    }

    /**
     * Called by the server (via the <code>service</code> method) to allow a
     * servlet to handle a POST request. The HTTP POST method allows the client
     * to send data of unlimited length to the Web server a single time and is
     * useful when posting information such as credit card numbers.
     * <p>
     * When overriding this method, read the request data, write the response
     * headers, get the response's writer or output stream object, and finally,
     * write the response data. It's best to include content type and encoding.
     * When using a <code>PrintWriter</code> object to return the response, set
     * the content type before accessing the <code>PrintWriter</code> object.
     * <p>
     * The servlet container must write the headers before committing the
     * response, because in HTTP the headers must be sent before the response
     * body.
     * <p>
     * Where possible, set the Content-Length header (with the
     * {@link javax.servlet.ServletResponse#setContentLength} method), to allow
     * the servlet container to use a persistent connection to return its
     * response to the client, improving performance. The content length is
     * automatically set if the entire response fits inside the response buffer.
     * <p>
     * When using HTTP 1.1 chunked encoding (which means that the response has a
     * Transfer-Encoding header), do not set the Content-Length header.
     * <p>
     * This method does not need to be either safe or idempotent. Operations
     * requested through POST can have side effects for which the user can be
     * held accountable, for example, updating stored data or buying items
     * online.
     * <p>
     * If the HTTP POST request is incorrectly formatted, <code>doPost</code>
     * returns an HTTP "Bad Request" message.
     * @param req an {@link HttpServletRequest} object that contains the request
     * the client has made of the servlet
     * @param resp an {@link HttpServletResponse} object that contains the
     * response the servlet sends to the client
     * @exception IOException if an input or output error is detected when the
     * servlet handles the request
     * @exception ServletException if the request for the POST could not be
     * handled
     * @see javax.servlet.ServletOutputStream
     * @see javax.servlet.ServletResponse#setContentType
     */
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        // redirect to doGet request
        this.doGet(req, resp);
    }

}
