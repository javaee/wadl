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
package org.jvnet.ws.wadl2java;

import javax.xml.namespace.QName;

import com.sun.codemodel.JType;

/**
 * This is a simple interface to replace reference to the S2JJAXModel
 * class that is passed around to resolve QName element references to type
 * references.
 * 
 * @author gdavison
 */
public interface ElementToClassResolver {
   
    /**
     * @param element The element to resolve.
     *
     * @return the java type that is used to represent this element, might
     *   actually be a XmlType rather than a XmlElement
     */
    public JType resolve (QName element);
}
