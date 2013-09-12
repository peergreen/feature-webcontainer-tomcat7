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
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.naming.NamingException;

import org.apache.catalina.Context;
import org.apache.tomcat.InstanceManager;

import com.peergreen.metadata.adapter.AnnotatedClass;
import com.peergreen.metadata.adapter.AnnotatedMember;
import com.peergreen.metadata.adapter.InjectException;
import com.peergreen.metadata.adapter.LifeCycleCallbackException;
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
        if (annotatedClasses == null) {
            return;
        }
        preDestroy(instance, instance.getClass());
    }

    @Override
    public Object newInstance(String className) throws InvocationTargetException, IllegalAccessException, InstantiationException, ClassNotFoundException, NamingException {
        return newInstance(className, classLoader);
    }

    @Override
    public Object newInstance(final String className, final ClassLoader classLoader) throws IllegalAccessException, NamingException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        // Build
        Object o = execute(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Class<?> clazz = classLoader.loadClass(className);
                return clazz.newInstance();
            }
        }, classLoader);

        // Configure
        newInstance(o);
        return o;
    }

    @Override
    public void newInstance(Object o)
            throws IllegalAccessException, InvocationTargetException, NamingException {
        newInstance(o, o.getClass());
    }

    private Object newInstance(final Object instance, final Class<?> clazz) throws InvocationTargetException  {
        if (annotatedClasses == null) {
            return instance;
        }

        return execute(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
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
                // call the post construct
                postConstruct(instance, clazz);
                return instance;
            }
        }, classLoader);
    }

    /**
     * Call postConstruct method on the specified instance recursively from deepest superclass to actual class.
     *
     * @param instance object to call postconstruct methods on
     * @param clazz    (super) class to examine for postConstruct annotation.
     * @throws InvocationTargetException if postConstruct call fails
     */
    protected void postConstruct(final Object instance, final Class<?> clazz) throws InvocationTargetException {

        execute(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Class<?> superClass = clazz.getSuperclass();
                if (superClass != Object.class) {
                    postConstruct(instance, superClass);
                }

                // Get current class
                AnnotatedClass annotatedClass = annotatedClasses.get(clazz.getName());
                if (annotatedClass != null) {
                    // Calls the PostConstruct methods if any
                    annotatedClass.callback(PostConstruct.class.getName(), instance);
                }

                return null;
            }
        }, classLoader);
    }

    /**
     * Call preDestroy method on the specified instance recursively from deepest superclass to actual class.
     * @param instance object to call preDestroy methods on
     * @param clazz    (super) class to examine for preDestroy annotation.
     * @throws InvocationTargetException if preDestroy method call fails
     */
    protected void preDestroy(final Object instance, final Class<?> clazz) throws InvocationTargetException {

        execute(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Class<?> superClass = clazz.getSuperclass();
                if (superClass != Object.class) {
                    preDestroy(instance, superClass);
                }

                // Get current class
                AnnotatedClass annotatedClass = annotatedClasses.get(clazz.getName());
                if (annotatedClass != null) {
                    // Calls the PreDestroy methods if any
                    annotatedClass.callback(PreDestroy.class.getName(), instance);
                }

                return null;
            }
        }, classLoader);
    }

    private static <T> T execute(Callable<T> callable, ClassLoader loader) throws InvocationTargetException {
        ClassLoader contextClassLoader = null;
        try {
            contextClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(loader);

            return callable.call();
        } catch (Exception e) {
            throw new InvocationTargetException(e, "Cannot execute the given Callable with TCCL set to " + loader);
        } finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
    }


}
