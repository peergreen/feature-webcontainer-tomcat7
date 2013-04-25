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
package com.peergreen.webcontainer.tomcat7.internal.handler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerSetter;

/**
 * The Tomcat URL handler needs to be registered though OSGi service.
 * @author Florent Benoit
 */
@Component
@Provides(properties= {@StaticServiceProperty(name="url.handler.protocol", type="java.lang.String", value="jndi")})
@Instantiate
public class DirContextURLStreamHandlerService extends DirContextURLStreamHandler implements URLStreamHandlerService {

    /**
     * Keeps the URL Stream handler setter.
     */
    private URLStreamHandlerSetter urlStreamHandlerSetter;


    /**
     * @see "java.net.URLStreamHandler.openConnection"
     */
    @Override
    public URLConnection openConnection(final URL url) throws IOException {
        return super.openConnection(url);
    }

    /**
     * Parse a URL. This method is called by the {@code URLStreamHandler} proxy,
     * instead of {@code java.net.URLStreamHandler.parseURL}, passing a
     * {@code URLStreamHandlerSetter} object.
     *
     * @param realHandler The object on which {@code setURL} must be invoked for
     *        this URL.
     * @see "java.net.URLStreamHandler.parseURL"
     */
    @Override
    public void parseURL(final URLStreamHandlerSetter urlStreamHandlerSetter,
                         final URL url,
                         final String spec,
                         final int start,
                         final int limit) {
        this.urlStreamHandlerSetter = urlStreamHandlerSetter;
        super.parseURL(url, spec, start, limit);
    }

    /**
     * @see "java.net.URLStreamHandler.toExternalForm"
     */
    @Override
    public String toExternalForm(final URL url) {
        return super.toExternalForm(url);
    }

    /**
     * @see "java.net.URLStreamHandler.equals(URL, URL)"
     */
    @Override
    public boolean equals(final URL u1, final URL u2) {
        return super.equals(u1, u2);
    }

    /**
     * @see "java.net.URLStreamHandler.getDefaultPort"
     */

    @Override
    public int getDefaultPort() {
        return super.getDefaultPort();
    }

    /**
     * @see "java.net.URLStreamHandler.getHostAddress"
     */
    @Override
    public InetAddress getHostAddress(final URL url) {
        return super.getHostAddress(url);
    }

    /**
     * @see "java.net.URLStreamHandler.hashCode(URL)"
     */
    @Override
    public int hashCode(final URL url) {
        return super.hashCode(url);
    }

    /**
     * @see "java.net.URLStreamHandler.hostsEqual"
     */
    @Override
    public boolean hostsEqual(URL u1, URL u2) {
        return super.hostsEqual(u1, u2);
    }

    /**
     * @see "java.net.URLStreamHandler.sameFile"
     */
    @Override
    public boolean sameFile(URL u1, URL u2) {
        return super.sameFile(u1, u2);
    }



	/**
	 * This method calls
	 * <code>urlStreamHandlerSetter.setURL(URL,String,String,int,String,String)</code>.
	 *
	 * @see "java.net.URLStreamHandler.setURL(URL,String,String,int,String,String)"
	 * @deprecated This method is only for compatibility with handlers written
	 *             for JDK 1.1.
	 */
	@Deprecated
    @Override
    protected void setURL(final URL u,
                          final String proto,
                          final String host,
                          final int port,
			final String file, final String ref) {
	    urlStreamHandlerSetter.setURL(u, proto, host, port, file, ref);
	}

	/**
	 * This method calls
	 * <code>urlStreamHandlerSetter.setURL(URL,String,String,int,String,String,String,String)</code>.
	 *
	 * @see "java.net.URLStreamHandler.setURL(URL,String,String,int,String,String,String,String)"
	 */
	@Override
    protected void setURL(final URL u,
                          final String proto,
                          final String host,
                          final int port,
                          final String auth,
                          final String user,
                          final String path,
                          final String query,
                          final String ref) {
	    urlStreamHandlerSetter.setURL(u, proto, host, port, auth, user, path, query, ref);
	}
}
