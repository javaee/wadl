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

/**
 * 
 */

package org.jvnet.ws.wadl2java;

/**
 * Thrown the the WADL is invalid and cannot be processed
 * @author mh124079
 */
public class InvalidWADLException extends java.lang.Exception {
    
    
    /**
     * Constructs an instance of <code>UnresolvableReferenceException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public InvalidWADLException(String msg) {
        super(msg);
    }
}
