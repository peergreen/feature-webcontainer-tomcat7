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

package com.peergreen.webcontainer.tomcat7.internal.realm;

import static java.lang.String.format;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.peergreen.security.UsernamePasswordAuthenticateService;
import com.peergreen.security.realm.AccountFilter;
import com.peergreen.security.realm.AccountInfo;
import com.peergreen.security.realm.AccountStore;
import com.peergreen.webcontainer.tomcat7.internal.ruleset.BundleContextAware;

/**
 * User: guillaume
 * Date: 06/05/13
 * Time: 11:50
 */
public class AccountStoreRealm extends RealmBase implements BundleContextAware {

    private UsernamePasswordAuthenticateService service;
    private AccountStore store;
    private BundleContext bundleContext;
    private String storeName;
    private ServiceTracker<UsernamePasswordAuthenticateService,UsernamePasswordAuthenticateService> authenticatorTracker;
    private ServiceTracker<AccountStore, AccountStore> storeTracker;

    @Override
    protected String getName() {
        return "Peergreen Adaptive Realm";
    }

    @Override
    protected String getPassword(String username) {
        return null;
    }

    @Override
    protected Principal getPrincipal(final String username) {
        if (store != null) {
            Set<AccountInfo> accounts = store.getAccounts(new AccountFilter() {
                @Override
                public boolean accept(AccountInfo account) {
                    return username.equals(account.getLogin());
                }
            });
            if (accounts.size() == 1) {
                AccountInfo info = accounts.iterator().next();
                return new GenericPrincipal(username, null, new ArrayList<>(info.getRoles()));
            }
        }
        return null;
    }

    @Override
    public Principal authenticate(String username, String credentials) {
        if (service != null) {
            Subject subject = service.authenticate(username, credentials);
            if (subject != null) {
                return getPrincipal(username);
            }
        }
        return null;
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    @Override
    protected void startInternal() throws LifecycleException {
        super.startInternal();

        Filter authenticatorFilter = null;
        Filter storeFilter = null;
        try {
            authenticatorFilter = bundleContext.createFilter(
                    format("(&(objectclass=%s)(%s=%s))",
                            UsernamePasswordAuthenticateService.class.getName(),
                            AccountStore.STORE_NAME,
                            storeName)
            );
            storeFilter = bundleContext.createFilter(
                    format("(&(objectclass=%s)(%s=%s))",
                            AccountStore.class.getName(),
                            AccountStore.STORE_NAME,
                            storeName)
            );
        } catch (InvalidSyntaxException e) {
            throw new LifecycleException(e);
        }
        authenticatorTracker = new ServiceTracker<>(bundleContext, authenticatorFilter, new AuthenticatorTracker());
        authenticatorTracker.open();
        storeTracker = new ServiceTracker<>(bundleContext, storeFilter, new AccountStoreTracker());
        storeTracker.open();
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        authenticatorTracker.close();
        storeTracker.close();
        super.stopInternal();
    }

    private class AuthenticatorTracker implements ServiceTrackerCustomizer<UsernamePasswordAuthenticateService, UsernamePasswordAuthenticateService> {
        @Override
        public UsernamePasswordAuthenticateService addingService(ServiceReference<UsernamePasswordAuthenticateService> reference) {
            service = bundleContext.getService(reference);
            return service;
        }

        @Override
        public void modifiedService(ServiceReference<UsernamePasswordAuthenticateService> reference, UsernamePasswordAuthenticateService service) {

        }

        @Override
        public void removedService(ServiceReference<UsernamePasswordAuthenticateService> reference, UsernamePasswordAuthenticateService service) {
            AccountStoreRealm.this.service = null;
        }
    }

    private class AccountStoreTracker implements ServiceTrackerCustomizer<AccountStore, AccountStore> {

        @Override
        public AccountStore addingService(ServiceReference<AccountStore> reference) {
            store = bundleContext.getService(reference);
            return store;
        }

        @Override
        public void modifiedService(ServiceReference<AccountStore> reference, AccountStore service) {
        }

        @Override
        public void removedService(ServiceReference<AccountStore> reference, AccountStore service) {
            AccountStoreRealm.this.store = null;
        }
    }
}
