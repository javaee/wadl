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
 * ResourceTypeNode.java
 *
 * Created on August 16, 2006, 12:58 PM
 *
 */

package com.sun.research.ws.wadl2java.ast;

import com.sun.codemodel.JDefinedClass;
import com.sun.research.ws.wadl.Doc;
import com.sun.research.ws.wadl.Param;
import com.sun.research.ws.wadl.ResourceType;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a WADL resource_type
 * @author mh124079
 */
public class ResourceTypeNode {
    
    private String interfaceName;
    private List<MethodNode> methods;
    private PathSegment pathSegment;
    private List<Doc> doc;
    private JDefinedClass generatedInterface;
    
    /**
     * Create a new instance of ResourceNode and attach it as a child of an existing
     * resource
     * @param resource the unmarshalled JAXB-generated resource object
     * @param parent the parent resource to attach the new resource to
     */
    public ResourceTypeNode(ResourceType resourceType) {
        doc = resourceType.getDoc();
        pathSegment = new PathSegment(resourceType);
        interfaceName = makeClassName(resourceType.getId());
        methods = new ArrayList<MethodNode>();
        generatedInterface = null;
    }
    
    /**
     * Convenience function for generating a suitable Java class name for this WADL
     * resource
     * @return a suitable name
     */
    public String getClassName() {
        return interfaceName;
    }
    
    /**
     * Utility function for generating a suitable Java class name from an arbitrary
     * string. Replaces any characters not allowed in an class name with '_'.
     * @param input the string
     * @return a string suitable for use as a Java class name
     */
    public static String makeClassName(String input) {
        if (input==null || input.length()==0)
            return("Index");
        StringBuffer buf = new StringBuffer();
        for(String segment: input.split("[^a-zA-Z0-9]")) {
            if (segment.length()<1)
                continue;
            buf.append(segment.substring(0,1).toUpperCase());
            buf.append(segment.substring(1));
        }
        return buf.toString();
    }
    
    /**
     * Get the methods for this resource
     * @return a list of methods
     */
    public List<MethodNode> getMethods() {
        return methods;
    }
    
    public List<Param> getQueryParams() {
        return pathSegment.getQueryParameters();
    }
        
    /**
     * List of child documentation elements
     * @return documentation list, one item per language
     */
    public List<Doc> getDoc() {
        return doc;
    }

    public void setGeneratedInterface(JDefinedClass iface) {
        generatedInterface = iface;
    }
    
    public JDefinedClass getGeneratedInterface() {
        return generatedInterface;
    }
}
