package com.peergreen.webcontainer.tomcat7.internal.ruleset;

import org.apache.catalina.Container;
import org.apache.catalina.startup.ConnectorCreateRule;
import org.apache.catalina.startup.ContextRuleSet;
import org.apache.catalina.startup.EngineRuleSet;
import org.apache.catalina.startup.HostRuleSet;
import org.apache.catalina.startup.NamingRuleSet;
import org.apache.catalina.startup.SetAllPropertiesRule;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.apache.tomcat.util.digester.RuleSetBase;
import org.xml.sax.Attributes;
//import org.apache.catalina.ha.ClusterRuleSet;

/**
 * Defines the rules to parse the Tomcat server.xml file
 * Some rules are customized for JOnAS.
 * @author Tomcat team
 * @author Florent Benoit
 */
public class TomcatRuleSet extends RuleSetBase {

    /**
     * Parent class loader use for Tomcat.
     */
    private ClassLoader parentClassLoader = null;


    /**
     * Constructor of the rules of parsing for server.xml.
     * @param parentClassLoader the parent class loader to use
     */
    public TomcatRuleSet(final ClassLoader parentClassLoader) {
        super();
        this.parentClassLoader = parentClassLoader;
    }


    /**
     * Add the rules to the digester object.
     * @param digester object on whcih to define rules
     */
    @Override
    public void addRuleInstances(final Digester digester) {

        // Configure the actions we will be using
        digester.addObjectCreate("Server",
                                 "org.apache.catalina.core.StandardServer",
                                 "className");
        digester.addSetProperties("Server");
        digester.addSetNext("Server",
                            "setServer",
                            "org.apache.catalina.Server");

        digester.addObjectCreate("Server/GlobalNamingResources",
                                 "org.apache.catalina.deploy.NamingResources");
        digester.addSetProperties("Server/GlobalNamingResources");
        digester.addSetNext("Server/GlobalNamingResources",
                            "setGlobalNamingResources",
                            "org.apache.catalina.deploy.NamingResources");


        digester.addObjectCreate("Server/Listener",
                                 null, // MUST be specified in the element
                                 "className");
        digester.addSetProperties("Server/Listener");
        digester.addSetNext("Server/Listener",
                            "addLifecycleListener",
                            "org.apache.catalina.LifecycleListener");

        digester.addObjectCreate("Server/Service",
                                 "org.apache.catalina.core.StandardService",
                                 "className");
        digester.addSetProperties("Server/Service");
        digester.addSetNext("Server/Service",
                            "addService",
                            "org.apache.catalina.Service");

        digester.addObjectCreate("Server/Service/Listener",
                                 null, // MUST be specified in the element
                                 "className");
        digester.addSetProperties("Server/Service/Listener");
        digester.addSetNext("Server/Service/Listener",
                            "addLifecycleListener",
                            "org.apache.catalina.LifecycleListener");

        //Executor
        digester.addObjectCreate("Server/Service/Executor",
                         "org.apache.catalina.core.StandardThreadExecutor",
                         "className");
        digester.addSetProperties("Server/Service/Executor");

        digester.addSetNext("Server/Service/Executor",
                            "addExecutor",
                            "org.apache.catalina.Executor");


        digester.addRule("Server/Service/Connector", new ConnectorCreateRule());
        digester.addRule("Server/Service/Connector", new SetAllPropertiesRule(new String[] {"executor"}));
        digester.addSetNext("Server/Service/Connector", "addConnector", "org.apache.catalina.connector.Connector");




        digester.addObjectCreate("Server/Service/Connector/Listener",
                                 null, // MUST be specified in the element
                                 "className");
        digester.addSetProperties("Server/Service/Connector/Listener");
        digester.addSetNext("Server/Service/Connector/Listener",
                            "addLifecycleListener",
                            "org.apache.catalina.LifecycleListener");


        // Add RuleSets for nested elements
        digester.addRuleSet(new NamingRuleSet("Server/GlobalNamingResources/"));
        digester.addRuleSet(new EngineRuleSet("Server/Service/"));
        digester.addRuleSet(new HostRuleSet("Server/Service/Engine/"));
        digester.addRuleSet(new ContextRuleSet("Server/Service/Engine/Host/"));
        //digester.addRuleSet(new ClusterRuleSet("Server/Service/Engine/Host/Cluster/"));
        digester.addRuleSet(new NamingRuleSet("Server/Service/Engine/Host/Context/"));

        // When the 'engine' is found, set the parentClassLoader.
        digester.addRule("Server/Service/Engine",
                         new SetParentClassLoaderRule(parentClassLoader));
    }
}

/**
 * Class that sets the parent class loader for the top class on the stack.
 * @author take from Catalina.
 */
final class SetParentClassLoaderRule extends Rule {

    /**
     * The parent class loader to be set.
     */
    private ClassLoader parentClassLoader = null;


    /**
     * Construct a new action.
     * @param parentClassLoader The new parent class loader
     */
    public SetParentClassLoaderRule(final ClassLoader parentClassLoader) {
        super();
        this.parentClassLoader = parentClassLoader;
    }

    /**
     * Add the requested parent class loader.
     * @param namespace the namespace URI of the matching element, or an
     *                  empty string if the parser is not namespace aware
     *                  or the element has no namespace
     * @param name the local name if the parser is namespace aware,
     *             or just the element name otherwise
     * @param attributes the attributes.
     * @throws Exception if an error occurs.
     */
    @Override
    public void begin(final String namespace, final String name, final Attributes attributes) throws Exception {
        Container top = (Container) digester.peek();
        top.setParentClassLoader(parentClassLoader);
    }
}
