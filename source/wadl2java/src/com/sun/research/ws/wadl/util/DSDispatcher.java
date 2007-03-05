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
 * JAXBDispatcher.java
 *
 * Created on May 1, 2006, 9:57 AM
 *
 */

package com.sun.research.ws.wadl.util;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import javax.activation.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;

/**
 * A wrapper for JAX-WS <code>Dispatch<DataSource></code> containing methods used by code
 * generated for WADL methods.
 * @author mh124079
 */
public class DSDispatcher {
    
    Dispatch<DataSource> d;
    
    /**
     * Creates a new instance of JAXBDispatcher
     */
    public DSDispatcher() {
        ServiceManager sm = ServiceManager.newInstance();
        d = sm.createDSDispatch("http://127.0.0.1/");
    }
    
    /**
     * Perform a HTTP GET on the resource
     * @return the unmarshalled resource representation.
     * @param url the URL of the resource
     * @param expectedMimeType the MIME type that will be used in the HTTP Accept header
     */
    public DataSource doGET(String url, Map<String, Object> httpHeaders, String expectedMimeType) {
        Map<String, Object> requestContext = d.getRequestContext();
        requestContext.put(MessageContext.HTTP_REQUEST_METHOD, "GET");
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("Accept", Arrays.asList(expectedMimeType));
        for(String key: httpHeaders.keySet())
            headers.put(key, Arrays.asList(httpHeaders.get(key).toString()));
        requestContext.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
        requestContext.put(Dispatch.ENDPOINT_ADDRESS_PROPERTY, url);
        return d.invoke(null);
    }

    /**
     * Perform a HTTP POST on the resource
     * 
     * @return the unmarshalled resource representation.
     * @param url the URL of the resource
     * @param input the body of the POST request
     * @param inputMimeType the MIME type of the body of the POST request
     * @param expectedMimeType the MIME type that will be used in the HTTP Accept header
     */
    public DataSource doPOST(DataSource input, String inputMimeType, String url, Map<String, Object> httpHeaders, String expectedMimeType) {
        Map<String, Object> requestContext = d.getRequestContext();
        requestContext.put(MessageContext.HTTP_REQUEST_METHOD, "POST");
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("Accept", Arrays.asList(expectedMimeType));
        if (inputMimeType != null)
            headers.put("Content-Type", Arrays.asList(inputMimeType));
        for(String key: httpHeaders.keySet())
            headers.put(key, Arrays.asList(httpHeaders.get(key).toString()));
        requestContext.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
        requestContext.put(Dispatch.ENDPOINT_ADDRESS_PROPERTY, url);
        return d.invoke(input);
    }

    /**
     * Perform a HTTP PUT on the resource
     * 
     * @return the unmarshalled resource representation.
     * @param url the URL of the resource
     * @param input the body of the POST request
     * @param inputMimeType the MIME type of the body of the POST request
     * @param expectedMimeType the MIME type that will be used in the HTTP Accept header
     */
    public DataSource doPUT(DataSource input, String inputMimeType, String url, Map<String, Object> httpHeaders,  String expectedMimeType) {
        Map<String, Object> requestContext = d.getRequestContext();
        requestContext.put(MessageContext.HTTP_REQUEST_METHOD, "PUT");
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("Accept", Arrays.asList(expectedMimeType));
        if (inputMimeType != null)
            headers.put("Content-Type", Arrays.asList(inputMimeType));
        for(String key: httpHeaders.keySet())
            headers.put(key, Arrays.asList(httpHeaders.get(key).toString()));
        requestContext.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
        requestContext.put(Dispatch.ENDPOINT_ADDRESS_PROPERTY, url);
        return d.invoke(input);
    }

}
