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
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;

/**
 * A wrapper for JAX-WS <code>Dispatch<Object></code> containing methods used by code
 * generated for WADL methods.
 * @author mh124079
 */
public class JAXBDispatcher {
    
    Dispatch<Object> d;
    
    /**
     * Creates a new instance of JAXBDispatcher
     * @param pathSegments a list of path segments that will be concatenated to form the URI of the
     * resource. Embedded parameters are represented as {<i>name</i>} where <i>name</i>
     * is the name of the parameter. Each path segment has a corresponding list of
     * matrix URI parameters in the same position in the matrixParams list.
     * @param matrixParams a set of matrix parameters that will be added to their corresponding path
     * segments to form the URI of the resource. Each entry in the list is a set of
     * matrix parameters for the path segment in the same list position.
     * @param paramValues a map of parameter names to values. Values may be of any class, the object's
     * toString method is used to produce a stringified value when embedded in the
     * resource's URI.
     * @param jc a JAXBContext that will be used to marshall requests and unmarshall responses.
     */
    public JAXBDispatcher(JAXBContext jc, List<String> pathSegments, List<List<String>> matrixParams, Map<String, Object> paramValues) {
        ServiceManager sm = ServiceManager.newInstance();
        String url = URIUtil.buildURI(pathSegments, matrixParams, paramValues);
        d = sm.createJAXBDispatch(jc,url);
    }
    
    /**
     * Creates a new instance of JAXBDispatcher
     * 
     * 
     * @param jc a JAXBContext that will be used to marshall requests and unmarshall responses.
     * @param uri the URI of the endpoint
     */
    public JAXBDispatcher(JAXBContext jc, URI uri) {
        ServiceManager sm = ServiceManager.newInstance();
        d = sm.createJAXBDispatch(jc,uri.toString());
    } 

    
    /**
     * Perform a HTTP GET on the resource
     * @param queryParams a set of query parameters that will be passed using the URI in the HTTP GET
     * @param expectedMimeType the MIME type that will be used in the HTTP Accept header
     * @return the unmarshalled resource representation.
     */
    public Object doGET(Map<String, Object> queryParams, String expectedMimeType) {
        Map<String, Object> requestContext = d.getRequestContext();
        requestContext.put(MessageContext.HTTP_REQUEST_METHOD, "GET");
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("Accept", Arrays.asList(expectedMimeType));
        requestContext.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
        String queryString = URIUtil.buildQueryString(queryParams); 
        requestContext.put(MessageContext.QUERY_STRING, queryString);
        return d.invoke(null);
    }

    /**
     * Perform a HTTP POST on the resource
     * @param input the body of the POST request
     * @param inputMimeType the MIME type of the body of the POST request
     * @param queryParams a set of query parameters that will be passed using the URI in the HTTP GET
     * @param expectedMimeType the MIME type that will be used in the HTTP Accept header
     * @return the unmarshalled resource representation.
     */
    public Object doPOST(Object input, String inputMimeType, Map<String, Object> queryParams, String expectedMimeType) {
        Map<String, Object> requestContext = d.getRequestContext();
        requestContext.put(MessageContext.HTTP_REQUEST_METHOD, "POST");
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("Accept", Arrays.asList(expectedMimeType));
        headers.put("Content-Type", Arrays.asList(inputMimeType));
        requestContext.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
        String queryString = URIUtil.buildQueryString(queryParams); 
        requestContext.put(MessageContext.QUERY_STRING, queryString);
        return d.invoke(input);
    }


    /**
     * Perform a HTTP PUT on the resource
     * @param input the body of the POST request
     * @param inputMimeType the MIME type of the body of the POST request
     * @param queryParams a set of query parameters that will be passed using the URI in the HTTP GET
     * @param expectedMimeType the MIME type that will be used in the HTTP Accept header
     * @return the unmarshalled resource representation.
     */
    public Object doPUT(Object input, String inputMimeType, Map<String, Object> queryParams, String expectedMimeType) {
        Map<String, Object> requestContext = d.getRequestContext();
        requestContext.put(MessageContext.HTTP_REQUEST_METHOD, "PUT");
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("Accept", Arrays.asList(expectedMimeType));
        headers.put("Content-Type", Arrays.asList(inputMimeType));
        requestContext.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
        String queryString = URIUtil.buildQueryString(queryParams); 
        requestContext.put(MessageContext.QUERY_STRING, queryString);
        return d.invoke(input);
    }
}
