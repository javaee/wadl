/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.php
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */

/*
 * ServiceManager.java
 *
 * Created on June 1, 2006, 2:35 PM
 *
 */

package com.sun.research.ws.wadl.util;

import javax.activation.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.http.HTTPBinding;

/**
 * A singleton that manages a JAX-WS Service instance and a set of dynamically
 * created ports. Used by JAXBDispatcher and DSDispatcher to prevent creation
 * of multiple Service instances.
 * @author mh124079
 */
public class ServiceManager {
    
    private static ServiceManager singleton = null;
    private Service s = null;
    private String nsURI = null;
    private int portNumber = 0;
    
    /** Creates a new instance of ServiceManager */
    private ServiceManager() {
        nsURI = new String("urn:sun:wadl");
        QName serviceName = new QName("wadl",nsURI);
        s = Service.create(serviceName);
   }
    
    /**
     * Create or return the singleton instance.
     * @return the singleton instance
     */
    public static ServiceManager newInstance() {
        if (singleton == null)
            singleton = new ServiceManager();
        return singleton;
    }
    
    /**
     * Create a JAX-WS Dispatch&lt;Object&gt;
     * @param jc JAXB context used for marshalling and unmarshalling XML data
     * @param url The URI of the resource
     * @return the created Dispatch instance
     */
    public Dispatch<Object> createJAXBDispatch(JAXBContext jc, String url) {
        QName portName = new QName("wadl_port"+Integer.toString(portNumber),nsURI);
        s.addPort(portName, HTTPBinding.HTTP_BINDING, url);
        portNumber++;
        return s.createDispatch(portName, jc, Service.Mode.PAYLOAD);        
    }
    
    /**
     * Create a JAX-WS Dispatch&lt;DataSource&gt;
     * @param url The URI of the resource
     * @return the created Dispatch instance
     */
    public Dispatch<DataSource> createDSDispatch(String url) {
        QName portName = new QName("wadl_port"+Integer.toString(portNumber),nsURI);
        s.addPort(portName, HTTPBinding.HTTP_BINDING, url);
        portNumber++;
        return s.createDispatch(portName, DataSource.class, Service.Mode.MESSAGE);        
    }
}
