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

import java.io.File;
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
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.loader.WebappLoader;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.tomcat.util.digester.Digester;
import org.xml.sax.InputSource;

import com.peergreen.deployment.DeploymentService;
import com.peergreen.webcontainer.tomcat7.Tomcat7Service;
import com.peergreen.webcontainer.tomcat7.internal.ruleset.TomcatRuleSet;

/**
 * Implementation of the Web Container service for Tomcat7
 * @author Florent Benoit
 */
@Component
@Provides
@Instantiate
public class PeergreenTomcat7Service implements Tomcat7Service {

    @Requires
    private DeploymentService deploymentService;


    private Server server;

    protected Digester initializeDigester() {

        // Initialize the digester
        Digester digester = new Digester();
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
        System.out.println("starting Tomcat7....");

        // set catalina.base
        Path tmpFile;
        try {
            tmpFile = Files.createTempDirectory("tomcat");
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return;
        }
        System.out.println("catalina.base set to " + tmpFile);
        System.setProperty(Globals.CATALINA_BASE_PROP, tmpFile.toFile().getPath());


        try {

        // Create the digester for the parsing of the server.xml.
        Digester digester = initializeDigester();

        // Execute the digester for the parsing of the server.xml.
        // And configure the catalina server.
        File configFile = null;

        URL tomcat7ConfigurationURL = PeergreenTomcat7Service.class.getResource("/tomcat7-server.xml");


        InputSource is = new InputSource(tomcat7ConfigurationURL.toExternalForm());
        //FileInputStream fis = new FileInputStream(configFile);
        //is.setByteStream(fis);
        digester.setClassLoader(this.getClass().getClassLoader());
        digester.push(this);
        digester.parse(is);


        // Disable registration of the Tomcat URL handler as it is done through OSGi
        Field f = WebappLoader.class.getDeclaredField("first");
        f.setAccessible(true);
        f.set(null,  false);
        f.setAccessible(false);



        server.init();
        server.start();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }


//


//        // Set the Domain and the name for each known Engine
//        for (StandardEngine engine : getEngines()) {
//            // WARNING : the order of th two next lines is very important.
//            // The domain must be set in first and the name after.
//            // In the others cases, Tomcat 6 doesn't set correctly these two
//            // properties
//            // because there are somes controls that forbid to have a difference
//            // between
//            // the name and the domain. Certainly a bug !
//            engine.setDomain(getDomainName());
//            engine.setName(getDomainName());
//        }
//
//        // If OnDemand Feature is enabled, the http connector port needs to be changed
//        // And keep-alive feature should be turned-off (to monitor all requests)
//        if (isOnDemandFeatureEnabled()) {
//            Service[] services = getServer().findServices();
//
//            // set name of the first service
//            if (services.length > 0) {
//                services[0].setName(getDomainName());
//            }
//
//            // Get connector of each service
//            for (int s = 0; s < services.length; s++) {
//                Connector[] connectors = services[s].findConnectors();
//                if (connectors.length >= 1) {
//                    // Only for the first connector
//                    Connector connector = connectors[0];
//                    connector.setProperty("maxKeepAliveRequests", "1");
//                    connector.setPort(getOnDemandRedirectPort());
//                    connector.setProxyPort(Integer.parseInt(getDefaultHttpPort()));
//                }
//            }
//        }




//        /
//
//
//        // Start Tomcat server in an execution block
//        IExecution<Void> startExec = new IExecution<Void>() {
//            public Void execute() throws ServiceException {
//                // Finaly start catalina ...
//                if (server instanceof LifecycleMBeanBase) {
//                    try {
//                        ((LifecycleMBeanBase) server).setDomain(getDomainName());
//                        server.init();
//                        ((LifecycleMBeanBase) server).setDomain(getDomainName());
//                        Service[] services = getServer().findServices();
//                        // set name of the first service
//                        if (services.length > 0) {
//                            services[0].setName(getDomainName());
//                        }
//                        ((Lifecycle) server).start();
//                    } catch (Exception e) {
//                        logger.error("Cannot start the Tomcat server", e);
//                        throw new ServiceException("Cannot start the Tomcat server", e);
//                    }
//                }
//
//                return null;
//            }
//        };
//
//        // Execute
//        ExecutionResult<Void> startExecResult = RunnableHelper.execute(getClass().getClassLoader(), startExec);
//
//        // Throw an ServiceException if needed
//        if (startExecResult.hasException()) {
//            logger.error("Cannot start the Tomcat server", startExecResult.getException());
//            throw new ServiceException("Cannot start the Tomcat Server", startExecResult.getException());
//        }

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
