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
import java.net.URI;
import org.jvnet.ws.wadl.ast.AbstractNode;

/**
 * This is a simple interface to replace reference to the S2JJAXModel
 * class that is passed around to resolve QName element or URI references 
 * to type references.
 * 
 * @author gdavison
 */
public interface Resolver {
    
    /**
     * @param element The element to resolve, can be a QName in the case
     *   of a JAX-B xml definition or a URI in the case of a JSON-Schema
     *   reference
     *
     * @return the java type that is used to represent this element, might
     *   actually be a XmlType rather than a XmlElement
     */
    public JType resolve (Object element);
    
    
    /**
     * @param context The object that we are loading relative to
     * @return A URI relative to the base URI of the document that context
     *   is loaded from
     */
    
    public URI resolveURI(AbstractNode context, String path);
    
    
    /**
     * Do we need to configure the client for JSON
     * @return true if required
     */
    public boolean isThereJsonMapping();
}
