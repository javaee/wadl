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
 * MethodNode.java
 *
 * Created on August 16, 2006, 12:59 PM
 *
 */

package org.jvnet.ws.wadl.ast;
 
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import org.jvnet.ws.wadl.Doc;
import org.jvnet.ws.wadl.Method;
import org.jvnet.ws.wadl.Param;
import org.jvnet.ws.wadl.util.AbstractMultivaluedMap;
import org.xml.sax.Locator;

/**
 * Represents a WADL method
 * @author mh124079
 */
public class MethodNode extends AbstractNode {
    
    private ResourceNode parentResource;
    private ResourceTypeNode parentResourceType;
    private String name;
    private List<Param> queryParams;
    private List<Param> headerParams;
    private List<Param> matrixParams;
    private List<RepresentationNode> supportedInputs;
    private MultivaluedMap<List<Long>, RepresentationNode> supportedOutputs;
    
    // Need to keep the order of these elements
    //
    private MultivaluedMap<List<Long>, FaultNode> faults;
            
    private Method method;
    
    /**
     * Creates a new instance of MethodNode and attach it to a resource
     * @param m the unmarshalled JAXB-generated method object
     * @param r the resource to attach the method to
     */
    public MethodNode(Method m, ResourceNode r) {
        method = m;
        name = m.getName();
        parentResource = r;
        parentResourceType = null;
        queryParams = new ArrayList<Param>();
        queryParams.addAll(parentResource.getQueryParams());
        headerParams = new ArrayList<Param>();
        headerParams.addAll(parentResource.getHeaderParams());
        matrixParams = new ArrayList<Param>();
        matrixParams.addAll(parentResource.getMatrixParams());
        supportedInputs = new ArrayList<RepresentationNode>();
        supportedOutputs = new AbstractMultivaluedMap<List<Long>, RepresentationNode>(
               new java.util.LinkedHashMap<List<Long>, List<RepresentationNode>>()) {};
        faults = new AbstractMultivaluedMap<List<Long>, FaultNode>(
               new java.util.LinkedHashMap<List<Long>, List<FaultNode>>()) {};
        r.getMethods().add(this);
    }
    
    /**
     * Creates a new instance of MethodNode and attach it to a resource type
     * @param m the unmarshalled JAXB-generated method object
     * @param r the resource to attach the method to
     */
    public MethodNode(Method m, ResourceTypeNode r) {
        method = m;
        name = m.getName();
        parentResource = null;
        parentResourceType = r;
        queryParams = new ArrayList<Param>();
        queryParams.addAll(r.getQueryParams());
        headerParams = new ArrayList<Param>();
        headerParams.addAll(r.getHeaderParams());
        matrixParams = new ArrayList<Param>();
        matrixParams.addAll(r.getMatrixParams());
        supportedInputs = new ArrayList<RepresentationNode>();
        supportedOutputs = new AbstractMultivaluedMap<List<Long>, RepresentationNode>(
               new java.util.LinkedHashMap<List<Long>, List<RepresentationNode>>()) {};
        faults = new AbstractMultivaluedMap<List<Long>, FaultNode>(
               new java.util.LinkedHashMap<List<Long>, List<FaultNode>>()) {};
        r.getMethods().add(this);
    }
    
    /**
     * Get the method name
     * @return the method name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the method name
     * @return the method name
     */
    public String getId() {
        return method.getId();
    }

    /**
     * @return The owning resource if this is avaliable, generally only
     * avaliable for fully resolved types
     */
    public ResourceNode getOwningResource() {
        return parentResource;
    }
    
    /**
     * Get all the query parameters
     * @return list of query parameters
     */
    public List<Param> getQueryParameters() {
        return queryParams;
    }
    
    /**
     * Get all the header parameters
     * @return list of header parameters
     */
    public List<Param> getHeaderParameters() {
        return headerParams;
    }

    /**
     * Get all the matrix parameters for this and enclosing resources
     * @return list of header parameters
     */
    public List<Param> getMatrixParameters() {
        return matrixParams;
    }

    /**
     * @param result The filtered output
     * @param toFilter The list of filter
     * @param required Whether the parameter should be required or not
     * @return the value of result to aid chaining
     */
    private List<Param> filterParams(List<Param> result, List<Param> toFilter, boolean required)
    {
        Boolean bRequired = required;
        for (Param p: toFilter) {
            if (p.isRequired()  == bRequired)
                result.add(p);
        }
        return result;
    }
    
    /**
     * Get the parameters marked as required
     * @return list of required parameters
     */
    public List<Param> getRequiredParameters() {
        ArrayList<Param> required = new ArrayList<Param>();
        filterParams(required, getQueryParameters(), true);
        filterParams(required, getHeaderParameters(), true);
        filterParams(required, getMatrixParameters(), true);
        return required;
    }
    
    /**
     * Get the parameters marked as optional
     * @return list of optional parameters
     */
    public List<Param> getOptionalParameters() {
        ArrayList<Param> optional = new ArrayList<Param>();
        filterParams(optional, getQueryParameters(), false);
        filterParams(optional, getHeaderParameters(), false);
        
        // Matrix paramters are dealt with differently so should
        // not be included in this list
//        filterParams(optional, getMatrixParameters(), false);
        return optional;
    }
    
    /**
     * Checks if there are any optional parameters
     * @return true if there are optional parameters, false if not
     */
    public boolean hasOptionalParameters() {
        return getOptionalParameters().size() > 0;
    }
    
    /**
     * Get a list of the supported input types, these correspond to the body of a PUT,
     * or POST request
     * @return a list of representations that can be accepted by the method
     */
    public List<RepresentationNode> getSupportedInputs() {
        return supportedInputs;
    }

    /**
     * Get a multi valued map of output representations that the method supports, these
     * correspond to the body of a GET, POST or PUT response.
     * @return multiValuedMap of supported outputs
     */
    public MultivaluedMap<List<Long>, RepresentationNode> getSupportedOutputs() {
        return supportedOutputs;
    }
    
    /**
     * Get a list of the faults for this method
     * @return list of faults
     */
    public MultivaluedMap<List<Long>, FaultNode> getFaults() {
        return faults;
    }
    
    /**
     * List of child documentation elements
     * @return documentation list, one item per language
     */
    public List<Doc> getDoc() {
        return method.getDoc();
    }

    /**
     * @return The location of the node
     */
    @Override
    public Locator getLocation() {
        return method.sourceLocation();
    }
    
    /**
     * Allow the provided parameter to visit the current node and any
     * child nodes.
     */
    public void visit(NodeVisitor visitor)
    {
        super.visit(visitor);
        
        // Visit inputs, outputs and faults
        //
        for (RepresentationNode node : getSupportedInputs()) {
            node.visit(visitor); 
        }

        for (List<RepresentationNode> nodeList : getSupportedOutputs().values()) {
            for (RepresentationNode node : nodeList)
            {
                node.visit(visitor); 
            }
        }

        for (List<FaultNode> nodeList : getFaults().values()) {
            for (FaultNode node : nodeList)
            {
                node.visit(visitor); 
            }
        }

    }
    

}
