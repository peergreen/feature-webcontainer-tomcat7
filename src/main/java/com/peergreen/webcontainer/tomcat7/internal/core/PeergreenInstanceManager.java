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

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.catalina.Context;
import org.apache.tomcat.InstanceManager;

import com.peergreen.injection.AnnotatedClass;
import com.peergreen.injection.AnnotatedMember;
import com.peergreen.injection.InjectException;
import com.peergreen.webcontainer.WebApplication;

/**
 * This instance allows to inject resources.
 * It allows to inject BundleContext
 * FIXME: Injection should rely on external services. If EJB are available, @EJB can be used, etc.
 * @author Florent Benoit
 */
public class PeergreenInstanceManager implements InstanceManager {

    private final ClassLoader classLoader;

    Map<String, AnnotatedClass> annotatedClasses;

    public PeergreenInstanceManager(WebApplication webApplication, Context context) {
        if (webApplication != null) {
            this.annotatedClasses = webApplication.getAnnotatedClasses();
        }
        this.classLoader = context.getLoader().getClassLoader();
    }

    @Override
    public void destroyInstance(Object instance) throws InvocationTargetException, IllegalAccessException {
        preDestroy(instance, instance.getClass());
    }

    @Override
    public Object newInstance(String className) throws InvocationTargetException, IllegalAccessException, InstantiationException, ClassNotFoundException, NamingException {
        return newInstance(className, classLoader);
    }

    @Override
    public Object newInstance(final String className, final ClassLoader classLoader) throws IllegalAccessException, NamingException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        Class<?> clazz = classLoader.loadClass(className);
        return newInstance(clazz.newInstance(), clazz);
    }

    @Override
    public void newInstance(Object o)
            throws IllegalAccessException, InvocationTargetException, NamingException {
        newInstance(o, o.getClass());
    }

    private Object newInstance(Object instance, Class<?> clazz) throws InvocationTargetException  {
        if (annotatedClasses == null) {
            return instance;
        }

        Class<?> servletClass = clazz;

        while (servletClass != null) {

            // perform injection on the selected instance
            AnnotatedClass annotatedClass = annotatedClasses.get(servletClass.getName());
            if (annotatedClass != null) {
                List<AnnotatedMember> annotatedMembers = annotatedClass.entries();
                for (AnnotatedMember annotatedMember : annotatedMembers) {
                    if (annotatedMember.hasInjection()) {
                        try {
                            annotatedMember.inject(instance);
                        } catch (InjectException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                }
            }
            servletClass = servletClass.getSuperclass();
        }

        return instance;
    }



    /**
     * Call preDestroy method on the specified instance recursively from deepest superclass to actual class.
     * @param instance object to call preDestroy methods on
     * @param clazz    (super) class to examine for preDestroy annotation.
     * @throws IllegalAccessException if preDestroy method is inaccessible.
     * @throws java.lang.reflect.InvocationTargetException
     *                                if call fails
     */
    protected void preDestroy(Object instance, final Class<?> clazz)
            throws IllegalAccessException, InvocationTargetException {
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != Object.class) {
            preDestroy(instance, superClass);
        }

    }


}
