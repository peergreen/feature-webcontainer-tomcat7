/**
 * Copyright 2013 Peergreen S.A.S.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.peergreen.webcontainer.tomcat7.internal.ruleset;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.apache.tomcat.util.digester.FactoryCreateRule;
import org.apache.tomcat.util.digester.ObjectCreateRule;
import org.apache.tomcat.util.digester.ObjectCreationFactory;
import org.apache.tomcat.util.digester.Rule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;

/**
 * User: guillaume
 * Date: 06/05/13
 * Time: 17:14
 */
public class BundleContextDigesterTestCase {

    @Mock
    private ObjectCreationFactory factory;
    @Mock
    private BundleContext bundleContext;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

    }

    @Test
    public void testFactoryCreateWithAwareType() throws Exception {

        Aware aware = new Aware();
        when(factory.createObject(any(Attributes.class))).thenReturn(aware);

        BundleContextDigester digester = new BundleContextDigester(bundleContext);
        digester.addFactoryCreate("Truc/Bidule", factory);

        Rule rule = digester.getRules().rules().get(0);
        rule.begin(null, null, null);
        assertFalse(rule instanceof FactoryCreateRule);
        assertEquals(aware.context, bundleContext);
    }

    @Test
    public void testFactoryCreateWithNotAwareType() throws Exception {

        String notAware = "not BundleContext aware";
        when(factory.createObject(any(Attributes.class))).thenReturn(notAware);

        BundleContextDigester digester = new BundleContextDigester(bundleContext);
        digester.addFactoryCreate("Truc/Bidule", factory);

        Rule rule = digester.getRules().rules().get(0);
        rule.begin(null, null, null);
        assertFalse(rule instanceof FactoryCreateRule);

    }

    @Test
    public void testObjectCreateWithAwareType() throws Exception {

        BundleContextDigester digester = new BundleContextDigester(bundleContext);
        digester.addObjectCreate("Truc/Bidule", Aware.class);

        Rule rule = digester.getRules().rules().get(0);
        rule.begin(null, null, null);
        assertFalse(rule instanceof ObjectCreateRule);

        Aware aware = (Aware) digester.peek();
        assertEquals(aware.context, bundleContext);
    }

    @Test
    public void testObjectCreateWithNotAwareType() throws Exception {

        BundleContextDigester digester = new BundleContextDigester(bundleContext);
        digester.addObjectCreate("Truc/Bidule", NotAware.class);

        Rule rule = digester.getRules().rules().get(0);
        rule.begin(null, null, null);
        assertFalse(rule instanceof ObjectCreateRule);

    }

    @Test
    public void testNormalRuleAreNotEncapsulated() throws Exception {

        BundleContextDigester digester = new BundleContextDigester(bundleContext);
        digester.addRule("Truc/Bidule", new EmptyRule());

        Rule rule = digester.getRules().rules().get(0);
        assertTrue(rule instanceof EmptyRule);

    }


    public static class Aware implements BundleContextAware {

        public BundleContext context;

        @Override
        public void setBundleContext(BundleContext bundleContext) {
            context = bundleContext;
        }
    }

    private static class EmptyRule extends Rule {}

    public static class NotAware {}
}
