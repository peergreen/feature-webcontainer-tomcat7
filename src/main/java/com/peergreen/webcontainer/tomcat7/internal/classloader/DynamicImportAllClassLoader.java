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
package com.peergreen.webcontainer.tomcat7.internal.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

/**
 * ClassLoader that allows to load all classes exported on the OSGi framework.
 * @author Florent Benoit
 */
public class DynamicImportAllClassLoader extends URLClassLoader {

    /**
     * Classloader of the bundle.
     */
    private final ClassLoader bundleClassLoader;

    public DynamicImportAllClassLoader() {
        super(new URL[0]);
        this.bundleClassLoader = DynamicImportAllClassLoader.class.getClassLoader();
    }

    /**
     * Loads the class with the specified <a href="#name">binary name</a>. This
     * method searches for classes in the same manner as the
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
        return this.bundleClassLoader.loadClass(name);
    }

    /**
     * Finds the resource with the given name. A resource is some data (images,
     * audio, text, etc) that can be accessed by class code in a way that is
     * independent of the location of the code.
     * <p>
     * The name of a resource is a '<tt>/</tt>'-separated path name that
     * identifies the resource.
     * <p>
     * This method will first search the parent class loader for the resource;
     * if the parent is <tt>null</tt> the path of the class loader built-in to
     * the virtual machine is searched. That failing, this method will invoke
     * {@link #findResource(String)} to find the resource.
     * </p>
     * @param name The resource name
     * @return A <tt>URL</tt> object for reading the resource, or <tt>null</tt>
     * if the resource could not be found or the invoker doesn't have adequate
     * privileges to get the resource.
     */
    @Override
    public URL getResource(String name) {
        URL url = this.bundleClassLoader.getResource(name);

        // search with super method
        if (url == null) {
            url = super.getResource(name);
        }
        // return URL
        return url;
    }

    /**
     * Finds all the resources with the given name. A resource is some data
     * (images, audio, text, etc) that can be accessed by class code in a way
     * that is independent of the location of the code.
     * <p>
     * The name of a resource is a <tt>/</tt>-separated path name that
     * identifies the resource.
     * <p>
     * The search order is described in the documentation for
     * {@link #getResource(String)}.
     * </p>
     * @param name The resource name
     * @return An enumeration of {@link java.net.URL <tt>URL</tt>} objects for
     * the resource. If no resources could be found, the enumeration will be
     * empty. Resources that the class loader doesn't have access to will not be
     * in the enumeration.
     * @throws IOException If I/O errors occur
     * @see #findResources(String)
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {

        Enumeration<URL> enumeration = this.bundleClassLoader.getResources(name);

        // Empty enumeration, try with super method
        if (!enumeration.hasMoreElements()) {
            enumeration = super.getResources(name);
        }
        // send the result
        return enumeration;
    }

    /**
     * Returns an input stream for reading the specified resource.
     * <p>
     * The search order is described in the documentation for
     * {@link #getResource(String)}.
     * </p>
     * @param name The resource name
     * @return An input stream for reading the resource, or <tt>null</tt> if the
     * resource could not be found
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream is = this.bundleClassLoader.getResourceAsStream(name);

        // Not found, try with super method
        if (is == null) {
            is = super.getResourceAsStream(name);
        }

        // send the result
        return is;
    }

}
