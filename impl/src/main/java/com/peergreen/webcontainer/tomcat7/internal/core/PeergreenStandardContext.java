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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Loader;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;

import com.peergreen.webcontainer.tomcat7.PeergreenContext;

/**
 * Defines the customized version of StandardContext
 * @author Florent Benoit
 */
public class PeergreenStandardContext extends StandardContext implements PeergreenContext {

    public PeergreenStandardContext() {
        setDelegate(true);
    }


    /**
     * Defines a new loader
     * @param loader the loader for which tunes the loader class
     */
    @Override
    public void setLoader(final Loader loader) {
        // In all cases, update the loader type
        if (loader instanceof WebappLoader) {
            ((WebappLoader) loader).setLoaderClass(PeergreenWebAppClassLoader.class.getName());
        }
        super.setLoader(loader);
    }



    public List<URI> getContextURIs() {
        if (getParent() == null) {
            throw new IllegalStateException("Cannot get context URI if context has not been added on a host");
        }
        List<URI> uris = new ArrayList<>();
        Service service = ((Engine) this.getParent().getParent()).getService();
        Connector[] connectors = service.findConnectors();
        for (Connector connector : connectors) {
            String hostName = ((Host) this.getParent()).getName();
            if (connector.getProxyName() != null) {
                hostName = connector.getProxyName();
            }
            int port = connector.getPort();
            if (connector.getProxyPort() != 0) {
                port = connector.getProxyPort();
            }

            String uri = connector.getScheme().concat("://").concat(hostName).concat(":").concat(String.valueOf(port)).concat(getPath());
            try {
                uris.add(new URI(uri));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Unable to build URI", e);
            }
        }
        return uris;
    }
}
