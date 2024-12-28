package com.microtomcat.jmx;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

public class MBeanRegistry {
    private final MBeanServer mBeanServer;
    
    public MBeanRegistry() {
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
        System.out.println("Created MBeanRegistry with server: " + mBeanServer);
    }
    
    public void registerMBean(Object mbean, String name) throws JMException {
        System.out.println("Attempting to register MBean: " + name);
        System.out.println("MBean class: " + mbean.getClass().getName());
        
        ObjectName objectName = new ObjectName("com.microtomcat:type=" + name);
        System.out.println("Created ObjectName: " + objectName);
        
        if (!mBeanServer.isRegistered(objectName)) {
            mBeanServer.registerMBean(mbean, objectName);
            System.out.println("Successfully registered MBean: " + objectName);
        } else {
            System.out.println("MBean already registered: " + objectName);
        }
    }
    
    public void unregisterMBean(String name) throws JMException {
        ObjectName objectName = new ObjectName("com.microtomcat:type=" + name);
        if (mBeanServer.isRegistered(objectName)) {
            mBeanServer.unregisterMBean(objectName);
            System.out.println("Unregistered MBean: " + objectName);
        }
    }
} 