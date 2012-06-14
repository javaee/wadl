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

package org.jvnet.ws.wadl.ast;

import com.sun.codemodel.JDefinedClass;
import java.net.URI;
import org.jvnet.ws.wadl.Doc;
import org.jvnet.ws.wadl.Param;
import org.jvnet.ws.wadl.ResourceType;
import org.jvnet.ws.wadl2java.GeneratorUtil;
import java.util.ArrayList;
import java.util.List;
import org.jvnet.ws.wadl.Resource;
import org.xml.sax.Locator;

/**
 * Represents a WADL resource_type
 * @author mh124079
 */
public class ResourceTypeNode extends AbstractNode {
    
    private String interfaceName;
    private List<MethodNode> methods;
    private List<ResourceNode> resources;
    private PathSegment pathSegment;
    private List<Doc> doc;
    private JDefinedClass generatedInterface;
    private ResourceType resourceType;
    
    /**
     * Create a new instance of ResourceTypeNode
     * @param resourceType the unmarshalled JAXB-generated object
     * @param file the URI of the WADL file that contains the resource type element
     * @param idMap a map of URI reference to WADL definition element
     */
    public ResourceTypeNode(ResourceType resourceType, URI file, ElementResolver idMap) throws InvalidWADLException {
        doc = resourceType.getDoc();
        pathSegment = new PathSegment(resourceType, file, idMap);
        interfaceName = GeneratorUtil.makeClassName(resourceType.getId());
        methods = new ArrayList<MethodNode>();
        resources = new ArrayList<ResourceNode>();
        generatedInterface = null;
        this.resourceType = resourceType;
    }
    
    /**
     * Create a new resource and add it as a child
     * @param r the unmarshalled JAXB resource element
     * @param file the URI of the WADL file that contains the resource type element
     * @param idMap a map of URI reference to WADL definition element
     * @return the new resource element
     */
    public ResourceNode addChild(Resource r, URI file, ElementResolver idMap) throws InvalidWADLException {
        ResourceNode n = new ResourceNode(r, this, file, idMap);
        resources.add(n);
        return n;
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
     * Get the methods for this resource type
     * @return a list of methods
     */
    public List<MethodNode> getMethods() {
        return methods;
    }
    
    /**
     * Get the resources for this resource type
     * @return a list of resources
     */
    public List<ResourceNode> getResources() {
        return resources;
    }
    
    public List<Param> getQueryParams() {
        return pathSegment.getQueryParameters();
    }
        
    public List<Param> getHeaderParams() {
        return pathSegment.getHeaderParameters();
    }
        
    public List<Param> getMatrixParams() {
        return pathSegment.getMatrixParameters();
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
    
    
    /**
     * @return The location of the node
     */
    @Override
    public Locator getLocation() {
        return resourceType.sourceLocation();
    }
    
    /**
     * Allow the provided parameter to visit the current node and any
     * child nodes.
     */
    public void visit(NodeVisitor visitor)
    {
        super.visit(visitor);
        
        // resources and methods
        //
        for (ResourceNode node : getResources()) {
            node.visit(visitor); 
        }
        
        for (MethodNode node : getMethods()) {
            node.visit(visitor);
        }

    }
    
    
}
