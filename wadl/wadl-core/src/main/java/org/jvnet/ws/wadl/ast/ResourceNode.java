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

package org.jvnet.ws.wadl.ast;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.jvnet.ws.wadl.Application;
import org.jvnet.ws.wadl.Doc;
import org.jvnet.ws.wadl.Param;
import org.jvnet.ws.wadl.Resource;
import org.jvnet.ws.wadl.Resources;
import org.jvnet.ws.wadl2java.GeneratorUtil;
import org.xml.sax.Locator;

/**
 * Represents a WADL resource.
 *
 * @author mh124079
 */
public class ResourceNode extends AbstractNode {
    
    private Object rootElement;
    private ResourceNode parentResource;
    private String className;
    private PathSegment pathSegment;
    private List<ResourceNode> childResources;
    private List<MethodNode> methods;
    private List<ResourceTypeNode> types;
    private List<Doc> doc;
    
    /**
     * Creates a new instance of ResourceNode.
     *
     * @param app the unmarshalled JAXB-generated application object.
     * @param resources the unmarshalled JAXB-generated resources object.
     */
    public ResourceNode(URI baseURI, Application app, Resources resources) {
        doc = app.getDoc();
        parentResource = null;
        rootElement = resources;
        
        // So we have the base string, which is a simple anyURI type
        // but we need to test to see if this is an relative one or
        // not
        String baseString = resources.getBase();
        URI base = URI.create(baseString);
        if (!base.isAbsolute())
        {
            UriBuilder builder = UriBuilder.fromUri(baseURI);
            String originalPath = baseURI.getPath();

            // Absolute URI path on the given server
            if (baseString.startsWith("/")) {
                builder.replacePath(baseString);
            }
            else {
                // If the path doesn't end in a /, normal for most WADL references
                // then you need to hack it back to the / to get relative URI to 
                // work properly
                if (!originalPath.endsWith("/") && originalPath.lastIndexOf('/')!=-1) {
                    builder.replacePath(
                            originalPath.substring(0,originalPath.lastIndexOf('/')));
                }
                
                builder.path(baseString);
            }
                
            
            // Build it up
            base = builder.build();
        }
        
        className = createClassNameFromBase(base.toString());
        pathSegment = new PathSegment(
                resources==null ? "" : base.toString());
        childResources = new ArrayList<ResourceNode>();
        methods = new ArrayList<MethodNode>();
        types = new ArrayList<ResourceTypeNode>();
    }

    /**
     * @return A simplified name with both the protocol and the port removed.
     */
    public static String createClassNameFromBase(String base) {

        try {
            URL url = new URL(base);
            String simpifiedURL =
                    url.getHost() + ((url.getPath().length() > 0 && !url.getPath().equals("/")) ? "_"
                    + url.getPath() : "");
            return GeneratorUtil.makeClassName(simpifiedURL);
        }
        catch (MalformedURLException me) {
            return GeneratorUtil.makeClassName(base);
        }
        
    }
    
    
    /**
     * Create a new instance of ResourceNode and attach it as a child of an existing
     * resource.
     *
     * @param resource the unmarshalled JAXB-generated resource object.
     * @param parent the parent resource to attach the new resource to.
     * @param file the URI of the WADL file that contains the resource element.
     * @param idMap a map of URI reference to WADL definition element.
     * @throws InvalidWADLException when WADL is invalid and cannot be processed.
     */
    public ResourceNode(Resource resource, ResourceNode parent, URI file, ElementResolver idMap) throws InvalidWADLException {
        doc = resource.getDoc();
        rootElement = resource;
        parentResource = parent;
        pathSegment = new PathSegment(resource, file, idMap);
        className = GeneratorUtil.makeClassName(getUriTemplate());
        childResources = new ArrayList<ResourceNode>();
        methods = new ArrayList<MethodNode>();        
        types = new ArrayList<ResourceTypeNode>();
    }

    
    /**
     * Create a new instance of ResourceNode and attach it as a child of an existing
     * resource type.
     *
     * @param resource the unmarshalled JAXB-generated resource object.
     * @param parent the parent resource to attach the new resource to.
     * @param file the URI of the WADL file that contains the resource element.
     * @param idMap a map of URI reference to WADL definition element.
     * @throws InvalidWADLException when WADL is invalid and cannot be processed.
     */
    public ResourceNode(Resource resource, ResourceTypeNode parent, URI file, ElementResolver idMap) throws InvalidWADLException {
        doc = resource.getDoc();
        rootElement = resource;
        parentResource = null;
        pathSegment = new PathSegment(resource, file, idMap);
        childResources = new ArrayList<ResourceNode>();
        methods = new ArrayList<MethodNode>();        
        types = new ArrayList<ResourceTypeNode>();
    }
    
    /**
     * Create a new resource and add it as a child.
     *
     * @param r the unmarshalled JAXB resource element.
     * @param file the URI of the WADL file that contains the resource element.
     * @param idMap a map of URI reference to WADL definition element.
     * @return the new resource element.
     * @throws InvalidWADLException when WADL is invalid and cannot be processed.
     */
    public ResourceNode addChild(Resource r, URI file, ElementResolver idMap) throws InvalidWADLException {
        ResourceNode n = new ResourceNode(r, this, file, idMap);
        childResources.add(n);
        return n;
    }
    
    /**
     * Convenience function for generating a suitable Java class name for this WADL
     * resource.
     *
     * @return a suitable name.
     */
    public String getClassName() {
        if (className==null)
        {
            className = GeneratorUtil.makeClassName(getUriTemplate());
        }
        return className;
    }

    /**
     * @return The template for this resource.
     */
    public String getUriTemplate() {
        return pathSegment.getTemplate();
    }
    
    
    /**
     * @return A template that contains all parent resources; but not
     *   the base URI. This path will start with a '/' in order to 
     *   make the code we generate less complicated
     */
    public String getAllResourceUriTemplate() {
        StringBuilder sb = new StringBuilder("/");
        List<PathSegment> segments = getPathSegments();
        for(int segement = 1; segement < segments.size(); segement++) {
            String pathSegment = segments.get(segement).getTemplate();
            boolean bufferEndsInSlash = sb.length() > 0 ? sb.charAt(sb.length()-1)=='/': false;
            boolean pathSegmentStartsWithSlash = pathSegment.startsWith("/");
            
            if (pathSegmentStartsWithSlash && bufferEndsInSlash) {
                // We only need the one slash, so remove one from the 
                // pathSegement
                sb.append(pathSegment, 1, pathSegment.length());
            }
            else if (pathSegmentStartsWithSlash ^ bufferEndsInSlash) {
                // Only one has a slash so it is fine to append
                sb.append(pathSegment);
            }
            else {
                // Neither has one so add a slash in
                sb.append('/');
                sb.append(pathSegment);
            }
        }
        
        return sb.toString();
    }
    

    /**
     * Override the default generated class name.
     *
     * @param className class name.
     */
    public void setClassName(String className) {
        // TODO perform some kind of validation
        this.className = className;
    }

    /**
     * Get the child resources.
     *
     * @return a list of child resources.
     */
    public List<ResourceNode> getChildResources() {
        return childResources;
    }
    
    /**
     * Get the path segment for this resource.
     *
     * @return the path segment.
     */
    public PathSegment getPathSegment() {
        return pathSegment;
    }
    
    /**
     * Get the method for this resource.
     *
     * @return a list of methods.
     */
    public List<MethodNode> getMethods() {
        return methods;
    }
    
    /**
     * Add a new base type for this resource - adds any methods, query or
     * matrix parameters specified on the type to those specified for
     * this resource.
     *
     * @param n the abstract resource type node to add.
     */
    public void addResourceType(ResourceTypeNode n) {
        types.add(n);
        methods.addAll(n.getMethods());
        childResources.addAll(n.getResources());
        pathSegment.getQueryParameters().addAll(n.getQueryParams());
        // ?? No header params
        pathSegment.getMatrixParameters().addAll(n.getMatrixParams());
    }
    
    /**
     * Get the types for this resource.
     *
     * @return a list of resource types.
     */
    public List<ResourceTypeNode> getResourceTypes() {
        return types;
    }
    
    /**
     * Get the parent resource.
     *
     * @return the parent resource or null if there isn't one.
     */
    public ResourceNode getParentResource() {
        return parentResource;
    }
    
    /**
     * Get a list of path segments for this resource and its ancestors. The order of
     * segments is the reverse of the ancestor list. I.e. root resource, child of root,
     * ..., parent of resource, resource.
     *
     * @return list of path segments.
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
     * Get a list of query parameters for this resource and its types.
     *
     * @return list of query parameters
     */
    public List<Param> getQueryParams() {
        ArrayList<Param> completeList = new ArrayList<Param>();
        completeList.addAll(getPathSegment().getQueryParameters());
// Removed as per WADL-32
//        if (getParentResource() != null)
//            completeList.addAll(getParentResource().getQueryParams());
        return completeList;
    }
    
    /**
     * Get a list of header parameters for this resource and its types.
     *
     * @return list of header parameters
     */
    public List<Param> getHeaderParams() {
        ArrayList<Param> completeList = new ArrayList<Param>();
        completeList.addAll(getPathSegment().getHeaderParameters());
// Removed as per WADL-32
//        if (getParentResource() != null)
//            completeList.addAll(getParentResource().getHeaderParams());
        return completeList;
    }

    /**
     * Get a list of header parameters for this resource and its types.
     *
     * @return list of header parameters
     */
    public List<Param> getMatrixParams() {
        ArrayList<Param> completeList = new ArrayList<Param>();
        completeList.addAll(getPathSegment().getMatrixParameters());
        // Included per WADL-32
        if (getParentResource() != null)
            completeList.addAll(getParentResource().getMatrixParams());
        return completeList;
    }    
    
    
    /**
     * List of child documentation elements.
     *
     * @return documentation list, one item per language.
     */
    public List<Doc> getDoc() {
        return doc;
    }


    /**
     * @return The location of the node.
     */
    @Override
    public Locator getLocation() {
        return rootElement instanceof Application ?
                ((Resources)rootElement).sourceLocation():
                ((Resource)rootElement).sourceLocation();
    }
    
    /**
     * Allow the provided parameter to visit the current node and any
     * child nodes.
     *
     * @param visitor {@link NodeVisitor} instance.
     */
    public void visit(NodeVisitor visitor)
    {
        super.visit(visitor);
        
        // Visit resources, resourcetypes, and methods
        //
        for (ResourceTypeNode node : getResourceTypes()) {
            node.visit(visitor); 
        }

        for (ResourceNode node : getChildResources()) {
            node.visit(visitor); 
        }

        for (MethodNode node : getMethods()) {
            node.visit(visitor); 
        }
    }
}
