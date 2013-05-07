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
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

/**
 * User: guillaume
 * Date: 06/05/13
 * Time: 16:18
 */
public abstract class ForwardingRule extends Rule {

    protected abstract Rule delegate();

    @Override
    public Digester getDigester() {
        return delegate().getDigester();
    }

    @Override
    public void setDigester(Digester digester) {
        delegate().setDigester(digester);
    }

    @Override
    public String getNamespaceURI() {
        return delegate().getNamespaceURI();
    }

    @Override
    public void setNamespaceURI(String namespaceURI) {
        delegate().setNamespaceURI(namespaceURI);
    }

    @Override
    @Deprecated
    public void begin(Attributes attributes) throws Exception {
        delegate().begin(attributes);
    }

    @Override
    public void begin(String namespace, String name, Attributes attributes) throws Exception {
        delegate().begin(namespace, name, attributes);
    }

    @Override
    @Deprecated
    public void body(String text) throws Exception {
        delegate().body(text);
    }

    @Override
    public void body(String namespace, String name, String text) throws Exception {
        delegate().body(namespace, name, text);
    }

    @Override
    @Deprecated
    public void end() throws Exception {
        delegate().end();
    }

    @Override
    public void end(String namespace, String name) throws Exception {
        delegate().end(namespace, name);
    }

    @Override
    public void finish() throws Exception {
        delegate().finish();
    }
}
