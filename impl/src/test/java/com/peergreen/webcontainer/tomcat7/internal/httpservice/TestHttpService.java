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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Container;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardPipeline;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.core.StandardWrapperFacade;
import org.apache.catalina.realm.NullRealm;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.mapper.MappingData;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.peergreen.deployment.model.BundleArtifactManager;
import com.peergreen.webcontainer.tomcat7.internal.InternalTomcat7Service;
import com.peergreen.webcontainer.tomcat7.internal.core.PeergreenContextConfig;

public class TestHttpService {

    private static final String CONTEXT_NAME = "/mycontext";

    private static final String SERLVET_PATH = "/myPath";

    private static final String FULL_SERLVET_PATH = CONTEXT_NAME.concat(SERLVET_PATH);

    @Mock
    private InternalTomcat7Service internalTomcat7Service;

    @Mock
    private Bundle bundle;

    @Mock
    private StandardService service;

    @Mock
    private StandardEngine engine;

    @Mock
    private Request request;
    @Mock
    private Response response;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private HttpServletResponse servletResponse;

    // Cannot be mocked
    private MessageBytes messageBytes;

    @Mock
    private Host defaultHost;

    @Mock
    private HttpContext httpContext;

    @Mock
    private BundleArtifactManager bundleArtifactManager;

    private BasicTomcat7HttpService tomcat7HttpService;

    @BeforeClass
    public void setup() throws IOException {

        // Init Tomcat
        MockitoAnnotations.initMocks(this);
        this.tomcat7HttpService = new BasicTomcat7HttpService(internalTomcat7Service, bundleArtifactManager);

        // setup PG service
        doReturn(defaultHost).when(internalTomcat7Service).getDefaultHost();
        doReturn(new PeergreenContextConfig()).when(internalTomcat7Service).createContextConfig();


        // Here is how the wrapper is contained :
        // Wrapper -> Context -> Host -> Engine -> Service

        // setup host
        doReturn("MyHostName").when(defaultHost).getName();
        doReturn(engine).when(defaultHost).getParent();
        doReturn(new StandardPipeline()).when(defaultHost).getPipeline();

        // setup engine
        doReturn(new StandardPipeline()).when(engine).getPipeline();
        doReturn("MyEngineName").when(engine).getName();
        doReturn(service).when(engine).getService();

        // setup service
        Connector[] connectors = new Connector[0];
        doReturn(connectors).when(service).findConnectors();

        // set base
       Path tmpFile = Files.createTempDirectory("mydirectoryTomcat");
        String path = tmpFile.toString();
        File f = new File(path);
        f.deleteOnExit();
       /*new File(f, "mycontext").mkdirs();
        f.mkdirs();
        */
        doReturn(f.getPath()).when(defaultHost).getAppBase();
        doReturn(f.getPath()).when(engine).getBaseDir();

        // setup http requests
        doReturn("/").when(request).getDecodedRequestURI();
        messageBytes = MessageBytes.newInstance();
        doReturn(messageBytes).when(request).getRequestPathMB();
        doReturn(servletRequest).when(request).getRequest();
        doReturn("GET").when(servletRequest).getMethod();
        doReturn("1.1").when(servletRequest).getProtocol();

        // setup response
        doReturn(servletResponse).when(response).getResponse();



    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testUnregisterAliasBeforeItExists() {
        tomcat7HttpService.unregister(FULL_SERLVET_PATH, bundle);

    }


    @Test(dependsOnMethods="testUnregisterAliasBeforeItExists")
    public void testRegisterServlet() throws ServletException, NamespaceException, IOException, LifecycleException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        // Init servlet
        MyServlet servlet = new MyServlet();

        // Init params
        Dictionary<String, String> initparams = new Hashtable<>();
        initparams.put("mykey1", "florent");
        initparams.put("mykey2", "benoit");

        // Dummy http context
        doReturn(true).when(httpContext).handleSecurity(any(Request.class), any(Response.class));

        // try to register the servlet
        tomcat7HttpService.registerServlet(FULL_SERLVET_PATH, servlet, initparams, httpContext, bundle);

        // Check that servlet is registered in the wrapper
        StandardWrapperFacade wrapperFacade = (StandardWrapperFacade) servlet.getServletConfig();
        Field config = StandardWrapperFacade.class.getDeclaredField("config");
        config.setAccessible(true);
        assertNotNull(config);

        StandardWrapper wrapper = (StandardWrapper) config.get(wrapperFacade);

        assertNotNull(wrapper);
        assertEquals(wrapper.getServlet(), servlet);

        // check path
        assertEquals(wrapper.getName(), SERLVET_PATH);
        // check classloader
        assertEquals(wrapper.getParentClassLoader(), servlet.getClass().getClassLoader());

        Container container = wrapper.getParent();
        StandardContext context = ((StandardContext) container);

        // this context is now available on the host
        doReturn(context).when(defaultHost).findChild(CONTEXT_NAME);


        // should be done by the container
        context.setParent(defaultHost);
        context.setRealm(new NullRealm());

        context.setParentClassLoader(TestHttpService.class.getClassLoader());

        // now, start the context
        context.start();

        // Check context path
        assertEquals(context.getPath(), CONTEXT_NAME);

        // Check injection of init parameters
        assertEquals(servlet.getInitParameter("mykey1"), "florent");
        assertEquals(servlet.getInitParameter("mykey2"), "benoit");

        // Prepare servlet's invocation
        doReturn(wrapper).when(request).getWrapper();
        MappingData mappingData = new MappingData();
        mappingData.wrapper = wrapper;
        doReturn(mappingData).when(request).getMappingData();

        // invoke
        context.getPipeline().getFirst().invoke(request, response);

        assertTrue(servlet.doGetHasBeenCalled());


        // Check handleSecurity method has been called on the given http context
        verify(httpContext).handleSecurity(any(Request.class), any(Response.class));
    }


    /**
     * Register the servlet with the same alias name (it should fails)
     */
    @Test(dependsOnMethods="testRegisterServlet", expectedExceptions=NamespaceException.class)
    public void testDuplicateRegisterServlet() throws ServletException, NamespaceException, IOException, LifecycleException {
        // Init servlet
        MyServlet servlet = new MyServlet();

        // expect namespace exception as the alias is already used.
        tomcat7HttpService.registerServlet(FULL_SERLVET_PATH, servlet, null, null, bundle);
    }


    @Test(dependsOnMethods="testDuplicateRegisterServlet")
    public void testUnregisterAlias() {

        // wrapper is there
        AliasInfo aliasInfo = new AliasInfo(CONTEXT_NAME, SERLVET_PATH);
        HttpServiceStandardContext context = tomcat7HttpService.getStandardContext(aliasInfo);

        // Check that the context is there
        assertNotNull(context);

        // and the wrapper too
        assertTrue(searchWrapper(SERLVET_PATH, context));

        tomcat7HttpService.unregister(FULL_SERLVET_PATH, bundle);

        // Check that the wrapper is no longer here
        assertFalse(searchWrapper(SERLVET_PATH, context));

        // this context shouldn't be available on the host
        doReturn(null).when(defaultHost).findChild(CONTEXT_NAME);

        // as there is no other wrapper, the context should have been unregistered
        context = tomcat7HttpService.getStandardContext(aliasInfo);
        assertNull(context);

    }


    protected boolean searchWrapper(String wrapperName, StandardContext context) {
        Container[] children = context.findChildren();
        boolean found = false;
        int c = 0;
        while (c < children.length && !found) {
            Container child = children[c];
            if (wrapperName.equals(child.getName())) {
                found = true;
            }
            c++;
        }
        return found;
    }


}
