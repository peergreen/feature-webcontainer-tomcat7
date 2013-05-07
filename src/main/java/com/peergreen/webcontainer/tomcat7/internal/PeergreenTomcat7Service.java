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

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.loader.WebappLoader;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.tomcat.util.digester.Digester;
import org.osgi.framework.BundleContext;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.peergreen.deployment.DeploymentService;
import com.peergreen.webcontainer.tomcat7.Tomcat7Service;
import com.peergreen.webcontainer.tomcat7.internal.ruleset.BundleContextDigester;
import com.peergreen.webcontainer.tomcat7.internal.ruleset.TomcatRuleSet;

/**
 * Implementation of the Web Container service for Tomcat7
 * @author Florent Benoit
 */
@Component
@Provides
@Instantiate
public class PeergreenTomcat7Service implements Tomcat7Service, InternalTomcat7Service {

    /**
     * Deployment service.
     */
    @Requires
    private DeploymentService deploymentService;

    /**
     * Tomcat server instance.
     */
    private Server server;

    /**
     * BundleContext to be shared/distributed into Tomcat's components.
     */
    private final BundleContext bundleContext;

    public PeergreenTomcat7Service(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Creates a digester instance.
     * @return the customized digester
     */
    protected Digester initializeDigester() {

        // Initialize the digester
        Digester digester = new BundleContextDigester(bundleContext);
        digester.setValidating(false);
        digester.addRuleSet(new TomcatRuleSet(PeergreenTomcat7Service.class.getClassLoader()));

        // Use of the context class loader.
        digester.setUseContextClassLoader(true);
        return digester;
    }

    /**
     * Set the server instance we are configuring.
     * @param server The new server
     */
    public void setServer(final Server server) {
        this.server = server;
    }

    /**
     * Launch the Tomcat instance
     */
    @Validate
    public void start()  {

        // set catalina.base property
        Path tmpFile;
        try {
            tmpFile = Files.createTempDirectory("tomcat");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to get a working directory", e);
        }
        System.setProperty(Globals.CATALINA_BASE_PROP, tmpFile.toFile().getPath());


        // Create the digester for the parsing of the server.xml.
        Digester digester = initializeDigester();

        // Execute the digester for the parsing of the server.xml.
        URL tomcat7ConfigurationURL = PeergreenTomcat7Service.class.getResource("/tomcat7-server.xml");
        InputSource is = new InputSource(tomcat7ConfigurationURL.toExternalForm());
        digester.setClassLoader(this.getClass().getClassLoader());
        digester.push(this);
        try {
            digester.parse(is);
        } catch (IOException | SAXException e) {
            throw new IllegalStateException("Unable to start tomcat", e);
        }

        // Disable registration of the Tomcat URL handler as it is done through OSGi
        Field f;
        try {
            f = WebappLoader.class.getDeclaredField("first");
            f.setAccessible(true);
            f.set(null,  false);
            f.setAccessible(false);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to reset Tomcat URL Handler", e);
        }

        // Init and start thre tomcat instance
        try {
            server.init();
            server.start();
        } catch (LifecycleException e) {
            throw new IllegalStateException("Unable to start Tomcat", e);
        }

    }


    /**
     * Gets the default host
     * @return the default host
     */
    @Override
    public Host getDefaultHost() {
        return getHost(null);
    }


    /**
     * Gets the host by its given name
     * @param hostName the given host to find
     * @return the host with the given name
     */
    public Host getHost(final String hostName) {

        Service[] services = server.findServices();

        // services ?
        if (services.length < 1) {
            throw new IllegalArgumentException("There is no service defined in the tomcat configuration");
        }

        // no host provided
        if (hostName == null || hostName.equals("")) {
            // Take first service
            Service service = services[0];

            Container cont = service.getContainer();
            if (!(cont instanceof Engine)) {
                throw new IllegalArgumentException("Not an engine container");
            }

            Engine engine = (Engine) cont;
            String defaultHost = engine.getDefaultHost();
            if (defaultHost == null) {
                throw new IllegalArgumentException("No default host");
            }
            Container child = engine.findChild(defaultHost);
            // found, return it
            if (child instanceof Host) {
                return (Host) child;
            }
            throw new IllegalArgumentException("No default host");
        }

        // Get all hosts.
        List<Host> hosts = new ArrayList<Host>();
        for (int s = 0; s < services.length; s++) {
            Container cont = services[s].getContainer();
            if (!(cont instanceof Engine)) {
                throw new IllegalArgumentException("Not an engine container");
            }
            Engine engine = (Engine) cont;
            Container child = engine.findChild(hostName);
            if (child instanceof Host) {
                hosts.add((Host) child);
            }
        }

        // error
        if (hosts.size() == 0) {
            // No host found.
            throw new IllegalArgumentException(String.format("No matching host for the given host name %s", hostName));
        }

        // first host found
        return hosts.get(0);
    }

}
