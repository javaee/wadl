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
package org.jvnet.ws.wadl.ast;
 
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.jvnet.ws.wadl.*;
import org.jvnet.ws.wadl.util.MessageListener;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;


/**
 * Encapsulates the act of build an AST from the URI or a WADL. In this AST
 * any "href" references to both to internal and to external WADL elements
 * are resolved making it easier for the user to follow.
 * 
 * @author gdavison
 */
public class WadlAstBuilder {
    
    /**
     * Allow client to process both internal and external schemas.
     */
    public interface SchemaCallback {
        
        public void processSchema(InputSource is);
        public void processSchema(String uri, Element node);
        
        
    }
    
    private ElementResolver idMap;
    private Map<String, ResourceTypeNode> ifaceMap;
    private MessageListener messageListener;
    private List<String> processedDocs;
    private JAXBContext jbc;
    private SchemaCallback schemaCallback;
    
    /**
     * Create a new instance of the AST builder providing a {@link MessageListener}
     * for informational messages and a {@link SchemaCallback} instance to allow
     * the caller to be provided with schema instances to process.
     *
     * @param schemaCallback used for processing schemas.
     * @param messageListener informal messages listener.
     */
    public WadlAstBuilder(
            SchemaCallback schemaCallback,
            MessageListener messageListener)
    {
        this.idMap = new ElementResolver(
                messageListener);
        this.ifaceMap = new HashMap<String, ResourceTypeNode>();
        this.processedDocs = new ArrayList<String>();
        this.schemaCallback = schemaCallback;
        this.messageListener = messageListener;
    }
  
    
    public Map<String, ResourceTypeNode> getInterfaceMap() {
        return new HashMap<String, ResourceTypeNode>(ifaceMap);
    }
   
    
    private JAXBContext getJAXBContext() throws JAXBException {
        // initialize JAXB runtime
        if (jbc == null) {
            this.jbc = JAXBContext.newInstance( "org.jvnet.ws.wadl",
                this.getClass().getClassLoader() );
        }
        return jbc;
    }    
    

    
    /**
     * Build an abstract tree from an unmarshalled WADL file.
     *
     * @param rootFile the URI of the root WADL file. Other WADL files might be
     * included by reference.
     * @return the resource elements that correspond to the roots of the resource trees.
     * @throws InvalidWADLException when WADL is invalid and cannot be processed.
     * @throws IOException if the specified WADL file cannot be read.
     */
    public ApplicationNode buildAst(URI rootFile) throws InvalidWADLException, IOException {
        try {
            Application a = processDescription(rootFile);
            return buildAst(a,rootFile);
        } catch (JAXBException ex) {
            throw new RuntimeException("Internal error",ex);
        }
    }
    
    
   
    /**
     * Build an abstract tree from an unmarshalled WADL file.
     *
     * @param a the application element of the root WADL file.
     * @param rootFile the URI of the root WADL file. Other WADL files might be
     * included by reference.
     * @return the resource elements that correspond to the roots of the resource trees.
     * @throws InvalidWADLException when WADL is invalid and cannot be processed.
     */
    protected ApplicationNode buildAst(Application a, URI rootFile) throws InvalidWADLException {
        // process resource types in two steps:
        // (i) process resource types in terms of methods
        for (String ifaceId: ifaceMap.keySet()) {
            buildResourceType(ifaceId, a);
        }
        // (ii) process resource type child resources (which may reference
        // resource types located in (i)
        for (String ifaceId: ifaceMap.keySet()) {
            buildResourceTypeTree(ifaceId, a);
        }
        
        List<Resources> rs = a.getResources();
        List<ResourceNode> ns = new ArrayList<ResourceNode>();
        for (Resources r: rs) {
            ResourceNode rootResourcesNode = new ResourceNode(rootFile, a, r); 
            
            //
            
            for (Resource child: r.getResource()) {
                buildResourceTree(rootResourcesNode, child, rootFile);
            }
            ns.add(rootResourcesNode);
        }
        
        return new ApplicationNode(a,ns);
    }
    
    /**
     * Build an abstract resource type based on the methods of a resource type 
     * in a WADL file.
     *
     * @param ifaceId the identifier of the resource type.
     * @param a the application element of the root WADL file.
     * @throws InvalidWADLException when WADL is invalid and cannot be processed.
     */
    protected void buildResourceType(String ifaceId, Application a) throws InvalidWADLException {
        try {
            URI file = new URI(ifaceId.substring(0,ifaceId.indexOf('#')));
            ResourceType type = (ResourceType)idMap.get(ifaceId);
            ResourceTypeNode node = new ResourceTypeNode(type, file, idMap);
            for (Object child: type.getMethodOrResource()) {
                if (child instanceof Method)
                    addMethodToResourceType(node, (Method)child, file);
            }
            ifaceMap.put(ifaceId, node);
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }        
    }
    
    /**
     * Build an abstract resource type tree based on the child resources of a 
     * resource type in a WADL file.
     *
     * @param ifaceId the identifier of the resource type.
     * @param a the application element of the root WADL file.
     * @throws InvalidWADLException when WADL is invalid and cannot be processed.
     */
    protected void buildResourceTypeTree(String ifaceId, Application a) throws InvalidWADLException {
        try {
            URI file = new URI(ifaceId.substring(0,ifaceId.indexOf('#')));
            ResourceType type = (ResourceType)idMap.get(ifaceId);
            ResourceTypeNode node = ifaceMap.get(ifaceId);
            for (Object child: type.getMethodOrResource()) {
                if (child instanceof Resource)
                    addResourceToResourceType(node, (Resource)child, file);
            }
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }        
    }
    
    /**
     * Add a resource and (recursively) its children to a tree starting at the parent.
     * Follow references to resources across WADL file boundaries.
     *
     * @param parent the parent resource in the tree being built.
     * @param resource the WADL resource to process.
     * @param file the URI of the current WADL file being processed.
     * @throws InvalidWADLException when WADL is invalid and cannot be processed.
     */
    protected void buildResourceTree(ResourceNode parent, 
            Resource resource, URI file) throws InvalidWADLException {
        if (resource != null) {
            ResourceNode n = parent.addChild(resource, file, idMap);
            for (String type: resource.getType()) {
                addTypeToResource(n, resource, type, file);
            }
            for (Object child: resource.getMethodOrResource()) {
                if (child instanceof Resource) {
                    Resource childResource = (Resource)child;
                    buildResourceTree(n, childResource, file);
                } else if (child instanceof Method) {
                    Method m = (Method)child;
                    addMethodToResource(n, m, file);
                }
            }
        }
    }
    
    /**
     * Add a type to a resource.
     *
     * <p>Follow references to types across WADL file boundaries.</p>
     * @param href the identifier of the resource_type element to process.
     * @param resourceNode the resource AST node.
     * @param resource the resource object from the model.
     * @param file the URI of the current WADL file being processed.
     * @throws InvalidWADLException when WADL is invalid and cannot be processed.
     */
    protected void addTypeToResource(
            ResourceNode resourceNode, 
            Resource resource,
            String href, 
            URI file) throws InvalidWADLException {
        // dereference resource
        file = getReferencedFile(file, href);
        ResourceTypeNode n = ifaceMap.get(file.toString()+href.substring(href.indexOf('#')));
        
        if (n != null) {
            resourceNode.addResourceType(n);
        } else {
            throw messageStringFromObject(
                    AstMessages.SKIPPING_REFERENCE(href), resource);
        }  
    }
    
    /**
     * Add a method to a resource type.
     *
     * <p>Follow references to methods across WADL file boundaries.</p>
     * @param method the WADL method element to process.
     * @param resource the resource type.
     * @param file the URI of the current WADL file being processed.
     * @throws InvalidWADLException when WADL is invalid and cannot be processed.
     */
    protected void addMethodToResourceType(ResourceTypeNode resource, Method method, 
            URI file) throws InvalidWADLException {
        addMethodToParent(resource, method, file);
    }
    
    /**
     * Add a child resource to a resource type.
     *
     * <p>Follow references to resources across WADL file boundaries.</p>
     * @param resource the WADL resource element to process.
     * @param type the parent resource type.
     * @param file the URI of the current WADL file being processed.
     * @throws InvalidWADLException when WADL is invalid and cannot be processed.
     */
    protected void addResourceToResourceType(ResourceTypeNode type, Resource resource, 
            URI file) throws InvalidWADLException {
        if (resource != null) {
            ResourceNode n = type.addChild(resource, file, idMap);
            for (String resourceType: resource.getType()) {
                addTypeToResource(n, resource, resourceType,  file);
            }
            for (Object child: resource.getMethodOrResource()) {
                if (child instanceof Resource) {
                    Resource childResource = (Resource)child;
                    buildResourceTree(n, childResource, file);
                } else if (child instanceof Method) {
                    Method m = (Method)child;
                    addMethodToResource(n, m, file);
                }
            }
        }
    }
    
    /**
     * Check if the supplied Response represents an error or not. If any
     * of the possible HTTP status values is >= 400 the Response is considered
     * to represent a fault.
     *
     * @param response the response to check.
     * @return true if the response represents a fault, false otherwise.
     */
    boolean isFaultResponse(Response response) {
        boolean isFault = false;
        for (long status: response.getStatus()) {
            if (status >= 400) {
                isFault = true;
                break;
            }
        }
        return isFault;
    }
    
    /**
     * Add a method to a resource.
     *
     * <p>Follow references to methods across WADL file boundaries.</p>
     * @param method the WADL method element to process.
     * @param resource the resource.
     * @param file the URI of the current WADL file being processed.
     * @throws InvalidWADLException when WADL is invalid and cannot be processed.
     */
    protected void addMethodToResource(ResourceNode resource, Method method, 
            URI file) throws InvalidWADLException {
        addMethodToParent(resource, method, file);
    }
    
    /**
     * Add a method to a resource.
     *
     * <p>Follow references to methods across WADL file boundaries.</p>
     * @param method the WADL method element to process.
     * @param parent the parent object, can be resource or resource type.
     * @param file the URI of the current WADL file being processed.
     * @throws InvalidWADLException when WADL is invalid and cannot be processed.
     */
    private void addMethodToParent(Object parent, Method method, 
            URI file) throws InvalidWADLException {
    
        String href = method.getHref();
        if (href != null && href.length() > 0) {
            
            // Warn the user if we have an element with a href on it that
            // defined one of the other attributes
            if (method.getDoc().size() > 0 
                  || method.getId()!=null
                  || method.getRequest()!=null
                  || method.getResponse().size() > 0) {
                messageListener.warning(null,
                    messageStringFromObject(AstMessages.LONELY_HREF_METHOD(),method));
            }
            
            // dereference resource
            file = getReferencedFile(file, href);
            method = idMap.resolve(file, href, method);
        }
        
        
        if (method != null) {

            if (method.getName()==null || method.getName().length()==0) {
                InvalidWADLException errorMessage = messageStringFromObject(AstMessages.MISSING_METHOD_NAME(), method);
                messageListener.error(
                    null, errorMessage);   
                throw errorMessage;
            }             
            
            MethodNode n;
            if (parent instanceof ResourceNode) {
                n = new MethodNode(method, (ResourceNode)parent);
            }
            else {
                n = new MethodNode(method, (ResourceTypeNode)parent);
            }
            
            Request request = method.getRequest();
            if (request != null) {
                for (Param p: request.getParam()) {
                    href=p.getHref();
                    if (href != null && href.length() > 0) {

                        // Warn the user if we have an element with a href on it that
                        // defined one of the other attributes
                        if (p.getDefault()!=null
                              || p.getDoc().size()> 0
                              || p.getFixed() !=null
                              || p.getId()!=null
                              || p.getLink()!=null
                              || p.getName()!=null
                              || p.getOption().size() > 0
                              || p.getPath() !=null
                              || p.getStyle()!=null
                              || !p.getType().equals(new QName("http://www.w3.org/2001/XMLSchema", "string", "xs"))) {
                            messageListener.warning(null,
                                messageStringFromObject(AstMessages.LONELY_HREF_PARAM(),p));
                        }                        
                        
                        // dereference param
                        file = getReferencedFile(file, href);
                        p = idMap.resolve(file, href, p);
                    }
                    if (p != null) {
                        if (p.getStyle()==ParamStyle.HEADER) {
                            n.getHeaderParameters().add(p);
                        } else if (p.getStyle()==ParamStyle.QUERY) {
                            n.getQueryParameters().add(p);
                        } else {
                            // Should we be ignoring or supporting other
                            // types
                        }
                    }
                }
                for (Representation r: request.getRepresentation()) {
                    addRepresentation(n.getSupportedInputs(), r, file);
                }
            }
            for (Response response: method.getResponse()) {
                boolean isFault = isFaultResponse(response);
                for (Representation o: response.getRepresentation()) {
                    if (isFault) {
                        FaultNode fn = new FaultNode(o);
                        n.getFaults().add(response.getStatus(), fn); 
                    } else {
                        addRepresentation(n.getSupportedOutputs(), o, file);
                    }
                }
            }
        }        
    }

    /**
     * Add a representation to a method's input or output list.
     *
     * <p>Follow references to representations across WADL file boundaries.</p>
     * @param list the list to add the representation to.
     * @param representation the WADL representation element to process.
     * @param file the URI of the current WADL file being processed.
     * @throws InvalidWADLException when WADL is invalid and cannot be processed.
     */
    protected void addRepresentation(List<RepresentationNode> list, Representation representation, 
            URI file) throws InvalidWADLException {
        String href = representation.getHref();
        if (href != null && href.length() > 0) {
            
            // Warn the user if we have an element with a href on it that
            // defined one of the other attributes
            if (representation.getDoc().size() > 0 
                  || representation.getElement()!=null
                  || representation.getId()!=null
                  || representation.getMediaType()!=null
                  || representation.getParam().size() > 0
                  || representation.getProfile().size() > 0) {
                messageListener.warning(null,
                    messageStringFromObject(AstMessages.LONELY_HREF_REPRESENTATION(), representation));
            }

            
            // dereference resource
            file = getReferencedFile(file, href);
            representation = idMap.resolve(file, href, representation);
        }
        if (representation != null) {
            RepresentationNode n = new RepresentationNode(representation);
            list.add(n);
        }
    }
    

    
    
    /**
     * Unmarshall a WADL file, process any schemas referenced in the WADL file, add 
     * any items with an ID to a global ID map, and follow any references to additional
     * WADL files.
     *
     * @param desc the URI of the description file, the description is fetched by
     * converting the URI into a URL and then using a HTTP GET. Use
     * {@link #processDescription(java.net.URI, java.io.InputStream)} to
     * supply the InputStream directly.
     * @return the unmarshalled WADL application element.
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if 
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     */
    protected Application processDescription(URI desc) 
            throws JAXBException, IOException {
        InputStream is = desc.toURL().openStream();
        return processDescription(desc, is);
    }

    /**
     * Unmarshall a WADL file, process any schemas referenced in the WADL file, add
     * any items with an ID to a global ID map, and follow any references to additional
     * WADL files.
     *
     * @param desc the URI of the description file.
     * @param is an input stream from which the description can be read.
     * @return the unmarshalled WADL application element.
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     */
    protected Application processDescription(URI desc, InputStream is)
            throws JAXBException, IOException {

        // check for files that have already been processed to prevent loops
        if (processedDocs.contains(desc.toString()))
            return null;
        processedDocs.add(desc.toString());
        
        // read in WADL file, process with stylesheet to upgrade older versions
        // and then unmarshall the result using JAXB
        messageListener.info(AstMessages.PROCESSING(desc.toString()));

        
        // Upgrade the WADL if required, write to
        // a insternal string as the direct JAXB transform means that
        // we don't get the Locator attribute set.
        StringWriter sw = new StringWriter();
        
        StreamResult result = new StreamResult(sw);
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            StreamSource stylesheet = new StreamSource(
                this.getClass().getResourceAsStream("upgrade.xsl"));
            Transformer t = tf.newTransformer(stylesheet);
            t.transform(new StreamSource(is), result);
        } catch (Exception ex) {
            throw new JAXBException(ex.getMessage(), ex);
        }
        
        InputSource inputSource = new InputSource(new StringReader(sw.toString()));
        inputSource.setSystemId(desc.toString());
        Application a = (Application)
                getJAXBContext().createUnmarshaller().
                unmarshal(inputSource);
        
        // process embedded schemas
        Grammars g = a.getGrammars();
        if (g != null) {
            for (Include i: g.getInclude()) {
                URI incl = desc.resolve(i.getHref());
                if (processedDocs.contains(incl.toString()))
                    continue;
                processedDocs.add(incl.toString());
                messageListener.info(AstMessages.PROCESSING(incl.toString()));
                InputSource input = new InputSource(
                        new BufferedInputStream(incl.toURL().openStream()));
                input.setSystemId(incl.toString());
                schemaCallback.processSchema(input);
            }
            int embeddedSchemaNo = 0; // used to generate unique system ID
            for (Object any: g.getAny()) {
                if (any instanceof Element) {
                    Element element = (Element)any;
                    schemaCallback.processSchema(desc.toString()+"#schema"+
                            Integer.toString(embeddedSchemaNo), element);
                    embeddedSchemaNo++;
                }
            }
        }
        buildIDMap(a, desc);
        return a;
    }

    /**
     * Build a map of all method, param, representation, fault and resource_type
     * elements that have an ID. These are used to dereference href values
     * when building the ast.
     *
     * @param desc the URI of the WADL file being processed.
     * @param a the root element of an unmarshalled WADL document.
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if 
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
    */
    @SuppressWarnings("unchecked")
    protected void buildIDMap(Application a, URI desc) throws JAXBException, IOException {
        // process globally declared items
        // warn if any are missing id properties
        //
        for (Object child: a.getResourceTypeOrMethodOrRepresentation()) {
            if (child instanceof Method) {

                Method m = (Method)child;
                if (m.getId()==null) {
                    messageListener.warning(null,
                        messageStringFromObject(AstMessages.MISSING_ID_METHOD(), m));
                } 
                
                extractMethodIds(m, desc);
            }
            else if (child instanceof ResourceType) {

                ResourceType r = (ResourceType)child;
                if (r.getId()==null) {
                    messageListener.warning(null,
                        messageStringFromObject(AstMessages.MISSING_ID_RESOURCE_TYPE(),r));
                }
                
                extractResourceTypeIds(r, desc);
            }
            else if (child instanceof Representation) {

                Representation r = (Representation)child;
                if (r.getId()==null) {
                    messageListener.warning(null,
                        messageStringFromObject(AstMessages.MISSING_ID_REPRESENTATION(),r));
                }
                
                extractRepresentationId(r, desc);
            }
            else {

                Param r = (Param)child;
                if (r.getId()==null) {
                    messageListener.warning(null,
                        messageStringFromObject(AstMessages.MISSING_ID_PARAM(),r));
                }
                
                extractParamId(r, desc);
            }
        }
        
        // process resource hierarchy
        if (a.getResources() != null)
            for (Resources rs: a.getResources())
                for (Resource r: rs.getResource())
                    extractResourceIds(r, desc);
    }
    
    /**
     * Convert a given message and JAX-B object into a string with location 
     * information so that the user can correlate then back to the original file
     */
    public static InvalidWADLException messageStringFromObject(String message, Object obj)
    {
        // Lets see if we can get hold of a locator object
        //


        try {
            java.lang.reflect.Method m = obj.getClass().getDeclaredMethod("sourceLocation");
            Locator l = (Locator) m.invoke(obj);
            if (l!=null) { 
                return new InvalidWADLException(
                        message, l);
            }
        }
        catch (NoSuchMethodException nsme) {
            
        }
        catch (Exception ex) {
            // Shouldn't happen; but it might I guess if the model
            // is extended
            Logger.getLogger(WadlAstBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // Return original message
        
        return new InvalidWADLException("[Error working out location] " + message, null);
    }
    
    /**
     * Adds the object to the ID map if it is identified and process any file pointed
     * to by href.
     *
     * @param desc The URI of the current file being processed, used when resolving relative paths in href.
     * @param id The identifier of o or null if o isn't identified.
     * @param href A link to a another element, the document in which the element resides will
     * be recursively processed.
     * @param o The object that is being identified or from which the link occurs.
     * @return a unique identifier for the element or null if not identified.
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if 
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     */
    protected String processIDHref(URI desc, String id, String href, Object o)
            throws JAXBException, IOException {
        String uniqueId = idMap.addReference(desc, id, o);
        if (href != null && href.startsWith("#") == false) {
            // if the href references another document then unmarshall it
            // and recursively scan it for id and idrefs
            processDescription(getReferencedFile(desc, href));
        }
        return uniqueId;
    }
    
    /**
     * Extract the id from a representation element and add to the
     * representation map.
     *
     * @param file the URI of the current WADL file being processed.
     * @param r the representation element.
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if 
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     */
    protected void extractRepresentationId(Representation r, URI file) throws JAXBException, IOException {        
        processIDHref(file, r.getId(), r.getHref(), r);
        for (Param p: r.getParam())
            extractParamId(p, file);
    }
    
    /**
     * Extract the id from a param element and add to the
     * representation map.
     *
     * @param file the URI of the current WADL file being processed.
     * @param p the param element.
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if 
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     */
    protected void extractParamId(Param p, URI file) throws JAXBException, IOException {
        processIDHref(file, p.getId(), p.getHref(), p);
    }
    
    /**
     * Extract the id from a method element and add to the
     * method map. Also extract the ids from any contained representation or
     * fault elements.
     *
     * @param file the URI of the current WADL file being processed.
     * @param m the method element.
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if 
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     */
    protected void extractMethodIds(Method m, URI file) throws JAXBException, IOException {
        processIDHref(file, m.getId(), m.getHref(), m);

        if (m.getRequest() != null) {
            for (Param p: m.getRequest().getParam())
                extractParamId(p, file);
            for (Representation r: m.getRequest().getRepresentation())
                extractRepresentationId(r, file);
        }
        for (Response resp: m.getResponse()) {
            for (Param p: resp.getParam())
                extractParamId(p, file);
            for (Representation r: resp.getRepresentation()) {
                extractRepresentationId(r, file);
            }
        }
    }
    
    /**
     * Extract the id from a resource element and add to the
     * resource map then recurse into any contained resources.
     * Also extract the ids from any contained param, method and its
     * representation or fault elements.
     *
     * @param file the URI of the current WADL file being processed.
     * @param r the resource element.
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if 
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     */
    protected void extractResourceIds(Resource r, URI file) throws JAXBException, IOException {
        processIDHref(file, r.getId(), null, r);
        for (String type: r.getType()) {
            processIDHref(file, null, type, r);
        }
        for (Param p: r.getParam())
            extractParamId(p, file);
        for (Object child: r.getMethodOrResource()) {
            if (child instanceof Method)
                extractMethodIds((Method)child, file);
            else if (child instanceof Resource)
                extractResourceIds((Resource)child, file);
        }
    }
    
    /**
     * Extract the id from a resource_type element and add to the
     * resource map.
     *
     * <p>Also extract the ids from any contained method and its param,
     * representation or fault elements.</p>
     * @param file the URI of the current WADL file being processed.
     * @param r the resource_type element.
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if 
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     */
    protected void extractResourceTypeIds(ResourceType r, URI file) throws JAXBException, IOException {
        String id = processIDHref(file, r.getId(), null, r);
        if (id != null)
            ifaceMap.put(id, null);
        for (Param p: r.getParam())
            extractParamId(p, file);
        for (Object child: r.getMethodOrResource()) {
            if (child instanceof Method)
                extractMethodIds((Method)child, file);
            else if (child instanceof Resource)
                extractResourceIds((Resource)child, file);
        }
    }
    
    
    
    
    /**
     * Get the referenced file, currentFile will be returned if href is a
     * fragment identifier, otherwise href is resolved against currentFile.
     *
     * @param currentFile the uri of the file that contains the reference, used 
     * to provide a base for relative paths.
     * @param href the reference.
     * @return the URI of the referenced file.
     */
    protected static URI getReferencedFile(URI currentFile, String href) {
        if (href.startsWith("#"))
            return currentFile;
        // href references another file
        return currentFile.resolve(href.substring(0, href.indexOf('#')));
    }
    
}
