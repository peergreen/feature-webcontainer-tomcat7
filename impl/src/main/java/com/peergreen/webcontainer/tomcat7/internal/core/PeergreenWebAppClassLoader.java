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

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.catalina.loader.WebappClassLoader;

public class PeergreenWebAppClassLoader extends WebappClassLoader {

    /**
     * Null classloader
     */
    private static final ClassLoader NULL_CLASSLOADER = new NullClassLoader();

    /**
     * Construct a new ClassLoader with no defined repositories and no
     * parent ClassLoader.
     */
    public PeergreenWebAppClassLoader(ClassLoader parent) {
        super(parent);
        // Avoid to use system classloader
        system = NULL_CLASSLOADER;
    }

    /**
     * Clear references.
     */
    @Override
    protected void clearReferences() {
        // Do not clear TC references
    }

    /**
     * Always fails to find something
     */
    private static class NullClassLoader extends ClassLoader {
        /**
         * Loads the class with the specified <a href="#name">binary name</a>.
         * This method searches for classes in the same manner as the
         * {@link #loadClass(String, boolean)} method. It is invoked by the Java
         * virtual machine to resolve class references. Invoking this method is
         * equivalent to invoking {@link #loadClass(String, boolean)
         * <tt>loadClass(name,
         * false)</tt>}. </p>
         * @param name The <a href="#name">binary name</a> of the class
         * @return The resulting <tt>Class</tt> object
         * @throws ClassNotFoundException If the class was not found
         */
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return null;
        }

        /**
         * Finds the resource with the given name. A resource is some data
         * (images, audio, text, etc) that can be accessed by class code in a
         * way that is independent of the location of the code.
         * <p/>
         * <p>
         * The name of a resource is a '<tt>/</tt>'-separated path name that
         * identifies the resource.
         * <p/>
         * <p>
         * This method will first search the parent class loader for the
         * resource; if the parent is <tt>null</tt> the path of the class loader
         * built-in to the virtual machine is searched. That failing, this
         * method will invoke {@link #findResource(String)} to find the
         * resource.
         * </p>
         * @param name The resource name
         * @return A <tt>URL</tt> object for reading the resource, or
         * <tt>null</tt> if the resource could not be found or the invoker
         * doesn't have adequate privileges to get the resource.
         * @since 1.1
         */
        @Override
        public URL getResource(String name) {
            return null;
        }
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {
        Enumeration<URL> resources = super.getResources(name);

        // Only keep distinct elements
        Set<URL> distinct = new HashSet<>();
        distinct.addAll(Collections.list(resources));
        return Collections.enumeration(distinct);
    }
}
