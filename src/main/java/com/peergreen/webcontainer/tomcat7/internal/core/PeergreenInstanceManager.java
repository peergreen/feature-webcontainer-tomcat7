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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Resource;
import javax.naming.NamingException;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.util.Introspection;
import org.apache.tomcat.InstanceManager;
import org.osgi.framework.BundleContext;

/**
 * This instance allows to inject resources.
 * It allows to inject BundleContext
 * FIXME: Injection should rely on external services. If EJB are available, @EJB can be used, etc.
 * @author Florent Benoit
 */
public class PeergreenInstanceManager implements InstanceManager {

    private final BundleContext bundleContext;

    private final ClassLoader classLoader;

    public PeergreenInstanceManager(BundleContext bundleContext, Context context) {
        this.bundleContext = bundleContext;
        this.classLoader = context.getLoader().getClassLoader();
    }

    private final Map<Class<?>, AnnotationCacheEntry[]> annotationCache =
            new WeakHashMap<Class<?>, AnnotationCacheEntry[]>();


    @Override
    public void destroyInstance(Object instance) throws IllegalAccessException, InvocationTargetException {
        preDestroy(instance, instance.getClass());
    }

    @Override
    public Object newInstance(String className) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
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

    private Object newInstance(Object instance, Class<?> clazz) throws IllegalAccessException, InvocationTargetException, NamingException {

        // Search resource
        Field[] fields = Introspection.getDeclaredFields(clazz);
        Method[] methods = Introspection.getDeclaredMethods(clazz);


        for (Field f : fields) {
            Resource resource = f.getAnnotation(Resource.class);
            // needs to inject the bundle context
            if (resource != null && BundleContext.class.equals(f.getType())) {
                f.setAccessible(true);
                f.set(instance, bundleContext);
            }
        }


        for (Method method : methods) {
            Resource resource = method.getAnnotation(Resource.class);
            // needs to inject the bundle context
            if (resource != null && method.getParameterTypes().length == 1 && BundleContext.class.equals(method.getParameterTypes()[0])) {
                method.setAccessible(true);
                method.invoke(instance, bundleContext);
            }

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

        // At the end the postconstruct annotated
        // method is invoked
        AnnotationCacheEntry[] annotations = null;
        synchronized (annotationCache) {
            annotations = annotationCache.get(clazz);
        }
        if (annotations == null) {
            // instance not created through the instance manager
            return;
        }
        for (AnnotationCacheEntry entry : annotations) {
            if (entry.getType() == AnnotationCacheEntryType.PRE_DESTROY) {
                Method preDestroy = getMethod(clazz, entry);
                synchronized (preDestroy) {
                    boolean accessibility = preDestroy.isAccessible();
                    preDestroy.setAccessible(true);
                    preDestroy.invoke(instance);
                    preDestroy.setAccessible(accessibility);
                }
            }
        }
    }


    private static Method getMethod(final Class<?> clazz,
            final AnnotationCacheEntry entry) {
        Method result = null;
        if (Globals.IS_SECURITY_ENABLED) {
            result = AccessController.doPrivileged(
                    new PrivilegedAction<Method>() {
                        @Override
                        public Method run() {
                            Method result = null;
                            try {
                                result = clazz.getDeclaredMethod(
                                        entry.getAccessibleObjectName(),
                                        entry.getParamTypes());
                            } catch (NoSuchMethodException e) {
                                // Should never happen. On that basis don't log
                                // it.
                            }
                            return result;
                        }
            });
        } else {
            try {
                result = clazz.getDeclaredMethod(
                        entry.getAccessibleObjectName(), entry.getParamTypes());
            } catch (NoSuchMethodException e) {
                // Should never happen. On that basis don't log it.
            }
        }
        return result;
    }





    private static final class AnnotationCacheEntry {
        private final String accessibleObjectName;
        private final Class<?>[] paramTypes;
        private final String name;
        private final AnnotationCacheEntryType type;

        public AnnotationCacheEntry(String accessibleObjectName,
                Class<?>[] paramTypes, String name,
                AnnotationCacheEntryType type) {
            this.accessibleObjectName = accessibleObjectName;
            this.paramTypes = paramTypes;
            this.name = name;
            this.type = type;
        }

        public String getAccessibleObjectName() {
            return accessibleObjectName;
        }

        public Class<?>[] getParamTypes() {
            return paramTypes;
        }

        public String getName() {
            return name;
        }
        public AnnotationCacheEntryType getType() {
            return type;
        }
    }

    private static enum AnnotationCacheEntryType {
        FIELD, SETTER, POST_CONSTRUCT, PRE_DESTROY
    }

}
