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

package org.jvnet.ws.wadl.ast;

import org.jvnet.ws.wadl2java.Wadl2JavaMessages;
import org.xml.sax.Locator;

/**
 * Thrown the the WADL is invalid and cannot be processed
 * @author gdavison
 */
public class InvalidWADLException extends java.lang.Exception {
    
    private static final Locator NULL_LOCATOR = new Locator() {
        public String getPublicId() {
            return null;
        }

        public String getSystemId() {
            return null;
        }

        public int getLineNumber() {
            return -1;
        }

        public int getColumnNumber() {
            return -1;
        }
    };
    
    private Locator _locator;
    
    /**
     * Constructs an instance of <code>InvalidWADLException</code> with the specified detail message
     * and location
     * @param msg the detail message.
     */
    public InvalidWADLException(String msg, Locator locator) {
        super(locator==null ? msg : Wadl2JavaMessages.FILE(
              msg,
              locator.getLineNumber(), locator.getColumnNumber(), locator.getSystemId()));                    

        // Use a null locator is one isn't provided
        _locator = locator !=null ? locator : NULL_LOCATOR;
    }
    
    
    /**
     * @return the line number for the problem, can be -1
     */
    public int getLineNumber() {
        return _locator.getLineNumber();
    }

    /**
     * @return the column for the problem, can be -1
     */
    public int getColumnNumber() {
        return _locator.getColumnNumber();
    }

    /**
     * @return the system id of the location, can be used as the location
     * of the file being processed if avaliable.
     */
    public String getSystemId() {
        return _locator.getSystemId();
    }

}
