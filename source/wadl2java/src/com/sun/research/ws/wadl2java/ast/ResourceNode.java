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
 * ResourceNode.java
 *
 * Created on August 16, 2006, 12:58 PM
 *
 */

package com.sun.research.ws.wadl2java.ast;

import com.sun.research.ws.wadl.Application;
import com.sun.research.ws.wadl.Doc;
import com.sun.research.ws.wadl.Param;
import com.sun.research.ws.wadl.Resource;
import com.sun.research.ws.wadl.Resources;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a WADL resource
 * @author mh124079
 */
public class ResourceNode {
    
    private ResourceNode parentResource;
    private String className;
    private PathSegment pathSegment;
    private List<ResourceNode> childResources;
    private List<MethodNode> methods;
    private List<ResourceTypeNode> types;
    private List<Doc> doc;
    
    /**
     * Creates a new instance of ResourceNode
     * @param app the unmarshalled JAXB-generated application object
     * @param resources the unmarshalled JAXB-generated resources object
     */
    public ResourceNode(Application app, Resources resources) {
        doc = app.getDoc();
        parentResource = null;
        className = "Endpoint";
        pathSegment = new PathSegment(
                resources==null ? "" : resources.getBase());
        childResources = new ArrayList<ResourceNode>();
        methods = new ArrayList<MethodNode>();
        types = new ArrayList<ResourceTypeNode>();
    }
    
    /**
     * Create a new instance of ResourceNode and attach it as a child of an existing
     * resource
     * @param resource the unmarshalled JAXB-generated resource object
     * @param parent the parent resource to attach the new resource to
     */
    public ResourceNode(Resource resource, ResourceNode parent) {
        doc = resource.getDoc();
        parentResource = parent;
        pathSegment = new PathSegment(resource);
        className = makeClassName(pathSegment.getTemplate());
        childResources = new ArrayList<ResourceNode>();
        methods = new ArrayList<MethodNode>();        
        types = new ArrayList<ResourceTypeNode>();
    }
    
    /**
     * Create a new resource and add it as a child
     * @param r the unmarshalled JAXB resource element
     * @return the new resource element
     */
    public ResourceNode addChild(Resource r) {
        ResourceNode n = new ResourceNode(r, this);
        childResources.add(n);
        return n;
    }
    
    /**
     * Convenience function for generating a suitable Java class name for this WADL
     * resource
     * @return a suitable name
     */
    public String getClassName() {
        return className;
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
     * Get the child resources
     * @return a list of child resources
     */
    public List<ResourceNode> getChildResources() {
        return childResources;
    }
    
    /**
     * Get the path segment for this resource
     * @return the path segment
     */
    public PathSegment getPathSegment() {
        return pathSegment;
    }
    
    /**
     * Get the method for this resource
     * @return a list of methods
     */
    public List<MethodNode> getMethods() {
        return methods;
    }
    
    public void addResourceType(ResourceTypeNode n) {
        types.add(n);
        methods.addAll(n.getMethods());
    }
    
    /**
     * Get the types for this resource
     * @return a list of resource types
     */
    public List<ResourceTypeNode> getResourceTypes() {
        return types;
    }
    
    /**
     * Get the parent resource
     * @return the parent resource or null if there isn't one.
     */
    public ResourceNode getParentResource() {
        return parentResource;
    }
    
    /**
     * Get a list of path segments for this resource and its ancestors. The order of
     * segments is the reverse of the ancestor list. I.e. root resource, child of root,
     * ..., parent of resource, resource.
     * @return list of path segments
     */
    public List<PathSegment> getPathSegments() {
        List<PathSegment> list = new ArrayList<PathSegment>();
        ResourceNode n = this;
        while (n != null) {
            list.add(0, n.getPathSegment());
            n = n.getParentResource();
        }
        return list;
    }
    
    /**
     * Get a list of query parameters for this resource and its types. The order of
     * parameters is the reverse of the ancestor list. I.e. root resource, child of root,
     * ..., parent of resource, resource.
     * @return list of query parameters
     */
    public List<Param> getQueryParams() {
        List<Param> params = new ArrayList<Param>();
        params.addAll(getPathSegment().getQueryParameters());
        for (ResourceTypeNode type: getResourceTypes()) {
            params.addAll(type.getQueryParams());
        }
        return params;
    }
    
    /**
     * List of child documentation elements
     * @return documentation list, one item per language
     */
    public List<Doc> getDoc() {
        return doc;
    }
}
