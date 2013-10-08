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

import static org.testng.Assert.assertEquals;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Checks that the alias are valid as specified in the specification.
 * @author Florent Benoit
 */
public class TestHttpServiceAliases {

    private BasicTomcat7HttpService tomcat7HttpService;

    @BeforeClass
    public void setup() {
        tomcat7HttpService = new BasicTomcat7HttpService(null, null);
    }


    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testNullAlias() {
        tomcat7HttpService.getAliasInfo(null);
    }

    @Test
    public void testContextRoot() {
        AliasInfo aliasInfo = tomcat7HttpService.getAliasInfo("/");
        assertEquals(aliasInfo.getContextPath(), "/");
        assertEquals(aliasInfo.getServletPath(), "");
    }


    @Test
    public void testValidAlias() {
        AliasInfo aliasInfo = tomcat7HttpService.getAliasInfo("/toto");
        assertEquals(aliasInfo.getContextPath(), "/toto");
        assertEquals(aliasInfo.getServletPath(), "");

        aliasInfo = tomcat7HttpService.getAliasInfo("/tutu/tata");
        assertEquals(aliasInfo.getContextPath(), "/tutu");
        assertEquals(aliasInfo.getServletPath(), "/tata");

        aliasInfo = tomcat7HttpService.getAliasInfo("/my/name/space");
        assertEquals(aliasInfo.getContextPath(), "/my");
        assertEquals(aliasInfo.getServletPath(), "/name/space");

    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testInvalidStart() {
        tomcat7HttpService.getAliasInfo("myContext/entry");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testInvalidEnd() {
        tomcat7HttpService.getAliasInfo("/myContext/entry/");
    }

}
