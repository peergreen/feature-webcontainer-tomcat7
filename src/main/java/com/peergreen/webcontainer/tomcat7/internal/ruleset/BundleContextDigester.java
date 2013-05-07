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

import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.FactoryCreateRule;
import org.apache.tomcat.util.digester.ObjectCreateRule;
import org.apache.tomcat.util.digester.Rule;
import org.osgi.framework.BundleContext;
import org.xml.sax.Attributes;

/**
 * User: guillaume
 * Date: 06/05/13
 * Time: 16:08
 */
public class BundleContextDigester extends Digester {

    private final BundleContext bundleContext;

    public BundleContextDigester(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public void addRule(final String pattern, final Rule rule) {
        if ((rule instanceof ObjectCreateRule) || (rule instanceof FactoryCreateRule)) {
            // Wrap the rule to be able to inject a BundleContext to the new instance
            super.addRule(pattern, new ForwardingRule() {
                @Override
                protected Rule delegate() {
                    return rule;
                }

                @Override
                public void begin(String namespace, String name, Attributes attributes) throws Exception {
                    super.begin(namespace, name, attributes);
                    Object o = getDigester().peek();
                    if (o instanceof BundleContextAware) {
                        BundleContextAware bca = (BundleContextAware) o;
                        bca.setBundleContext(bundleContext);
                    }
                }
            });
        } else {
            super.addRule(pattern, rule);
        }
    }

}
