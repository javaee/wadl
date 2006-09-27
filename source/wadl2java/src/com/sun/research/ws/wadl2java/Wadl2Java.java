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
 * Wadl2Java.java
 *
 * Created on May 11, 2006, 11:35 AM
 *
 */

package com.sun.research.ws.wadl2java;

import com.sun.codemodel.*;
import com.sun.research.ws.wadl.*;
import com.sun.research.ws.wadl2java.ast.FaultNode;
import com.sun.research.ws.wadl2java.ast.MethodNode;
import com.sun.research.ws.wadl2java.ast.RepresentationNode;
import com.sun.research.ws.wadl2java.ast.ResourceNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import com.sun.tools.xjc.api.S2JJAXBModel;
import com.sun.tools.xjc.api.ErrorListener;
import com.sun.tools.xjc.api.SchemaCompiler;
import com.sun.tools.xjc.api.impl.s2j.SchemaCompilerImpl;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * Processes a WADL file and generates client-side stubs for the resources and
 * methods described.
 * @author mh124079
 */
public class Wadl2Java {
    
    private File outputDir;
    private String pkg;
    private S2JJAXBModel s2jModel;
    private JCodeModel codeModel;
    private Map<String, Object> idMap;
    private List<String> processedDocs;
    private JavaDocUtil javaDoc;
    private Unmarshaller u;
    private SchemaCompiler s2j;
    private ErrorListener errorListener;

    /**
     * Creates a new instance of a Wadl2Java processor.
     * @param outputDir the directory in which to generate code.
     * @param pkg the Java package in which to generate code.
     */
    public Wadl2Java(File outputDir, String pkg) {
        this.outputDir = outputDir;
        this.pkg = pkg;
        this.javaDoc = new JavaDocUtil();
        this.processedDocs = new ArrayList<String>();
    }
    
    /**
     * Process the root WADL file and generate code.
     * @param rootDesc the URI of the WADL file to process
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid, a
     * referenced WADL file is invalid, or if the code generator encounters
     * a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     * @throws com.sun.codemodel.JClassAlreadyExistsException if, during code 
     * generation, the WADL processor attempts to create a duplicate
     * class. This indicates a structural problem with the WADL file, e.g. duplicate
     * peer resource entries.
     */
    public void process(URI rootDesc) throws JAXBException, IOException, 
            JClassAlreadyExistsException {
        // read in root WADL file
        JAXBContext jbc = JAXBContext.newInstance( "com.sun.research.ws.wadl", 
                this.getClass().getClassLoader() );
        u = jbc.createUnmarshaller();
        s2j = new SchemaCompilerImpl();
        errorListener = new SchemaCompilerErrorListener();
        s2j.setDefaultPackageName(pkg);
        s2j.setErrorListener(errorListener);
        idMap = new HashMap<String, Object>();
        Application a = processDescription(rootDesc);
        ResourceNode r = buildAst(a, rootDesc);
        
        // generate code
        s2jModel = s2j.bind();
        if (s2jModel != null) {
            codeModel = s2jModel.generateCode(null, errorListener);
            generateEndpointClass(r);
            codeModel.build(outputDir);
        }
    }
    
    /**
     * Unmarshall a WADL file, process any schemas referenced in the WADL file, add 
     * any items with an ID to a global ID map, and follow any references to additional
     * WADL files.
     * @param desc the URI of the description file
     * @return the unmarshalled WADL application element
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if 
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     */
    public Application processDescription(URI desc) 
            throws JAXBException, IOException {
        // check for files that have already been processed to prevent loops
        if (processedDocs.contains(desc.toString()))
            return null;
        processedDocs.add(desc.toString());
        
        // read in WADL file
        System.out.println("Processing: "+desc.toString());
        Application a = (Application)u.unmarshal(desc.toURL());
        
        // process embedded schemas
        Grammars g = a.getGrammars();
        if (g != null) {
            for (Include i: g.getInclude()) {
                URI incl = desc.resolve(i.getHref());
                if (processedDocs.contains(incl.toString()))
                    continue;
                processedDocs.add(incl.toString());
                System.out.println("Processing: "+incl.toString());
                InputSource input = new InputSource(incl.toURL().openStream());
                input.setSystemId(incl.toString());
                s2j.parseSchema(input);
            }
            int embeddedSchemaNo = 0; // used to generate unique system ID
            for (Object any: g.getAny()) {
                if (any instanceof Element) {
                    Element element = (Element)any;
                    s2j.parseSchema(desc.toString()+"#schema"+
                            Integer.toString(embeddedSchemaNo), element);
                    embeddedSchemaNo++;
                }
            }
        }
        buildIDMap(a, desc);
        return a;
    }

    /**
     * Build a map of all method, representation, fault and resource
     * elements that have an ID. These are used to dereference href values
     * when building the ast.
     * @param desc the URI of the WADL file being processed
     * @param a the root element of an unmarshalled WADL document
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if 
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
    */
    protected void buildIDMap(Application a, URI desc) throws JAXBException, IOException {
        // process globally declared items
        for (Object child: a.getResourceOrMethodOrRepresentation()) {
            if (child instanceof Method)
                extractMethodIds((Method)child, desc);
            else if (child instanceof Representation)
                extractRepresentationId((Representation)child, desc);
            else if (child instanceof Fault)
                extractFaultId((Fault)child, desc);
            else if (child instanceof Resource)
                extractResourceIds((Resource)child, desc);
        }
        
        // process resource hierarchy
        if (a.getResources() != null)
            for (Resource r: a.getResources().getResource())
                extractResourceIds(r, desc);
    }
    
    /**
     * Adds the object to the ID map if it is identified and process any file pointed
     * to by href.
     * @param desc The URI of the current file being processed, used when resolving relative paths in href
     * @param id The identifier of o or null if o isn't identified
     * @param href A link to a another element, the document in which the element resides will
     * be recursively processed
     * @param o The object that is being identified or from which the link occurs
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if 
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     */
    protected void processIDHref(URI desc, String id, String href, Object o)
            throws JAXBException, IOException {
        if (id != null && id.length()>0) {
            // if the element has an ID then add it to the id map
            idMap.put(desc.toString()+"#"+id, o);
        }
        else if (href != null && href.startsWith("#") == false) {
            // if the href references another document then unmarshall it
            // and recursively scan it for id and idrefs
            processDescription(getReferencedFile(desc, href));
        }
    }
    
    /**
     * Extract the id from a representation element and add to the
     * representation map.
     * @param file the URI of the current WADL file being processed
     * @param r the representation element
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if 
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     */
    protected void extractRepresentationId(Representation r, URI file) throws JAXBException, IOException {
        processIDHref(file, r.getId(), r.getHref(), r);
    }
    
    /**
     * Extract the id from a fault element and add to the
     * fault map.
     * @param file the URI of the current WADL file being processed
     * @param f the fault element
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if 
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     */
    protected void extractFaultId(Fault f, URI file) throws JAXBException, IOException {
        processIDHref(file, f.getId(), f.getHref(), f);
    }
    
    /**
     * Extract the id from a method element and add to the
     * method map. Also extract the ids from any contained representation or
     * fault elements.
     * @param file the URI of the current WADL file being processed
     * @param m the method element
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if 
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     */
    protected void extractMethodIds(Method m, URI file) throws JAXBException, IOException {
        processIDHref(file, m.getId(), m.getHref(), m);

        if (m.getRequest() != null) {
            for (Representation r: m.getRequest().getRepresentation())
                extractRepresentationId(r, file);
        }
        if (m.getResponse() != null) {
            for (Object child: m.getResponse().getRepresentationOrFault()) {
                if (child instanceof Representation)
                    extractRepresentationId((Representation)child, file);
                else if (child instanceof Fault)
                    extractFaultId((Fault)child, file);
            }
        }
    }
    
    /**
     * Extract the id from a resource element and add to the
     * resource map then recurse into any contained resources.
     * Also extract the ids from any contained method and its
     * representation or fault elements.
     * @param file the URI of the current WADL file being processed
     * @param r the resource element
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if 
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     */
    protected void extractResourceIds(Resource r, URI file) throws JAXBException, IOException {
        processIDHref(file, r.getId(), r.getHref(), r);
        for (Object child: r.getMethodOrResource()) {
            if (child instanceof Method)
                extractMethodIds((Method)child, file);
            else if (child instanceof Resource)
                extractResourceIds((Resource)child, file);
        }
    }
    
    /**
     * Create a class that acts as a container for a hierarchy
     * of static inner classes, one for each resource described by the WADL file.
     * @param root the resource element that corresponds to the root of the resource tree
     * @throws com.sun.codemodel.JClassAlreadyExistsException if, during code 
     * generation, the WADL processor attempts to create a duplicate
     * class. This indicates a structural problem with the WADL file, e.g. duplicate
     * peer resource entries.
     */
    protected void generateEndpointClass(ResourceNode root) 
            throws JClassAlreadyExistsException {
        JDefinedClass impl = codeModel._class(pkg+"."+root.getClassName());
        javaDoc.generateClassDoc(root, impl);
        for (ResourceNode r: root.getChildResources()) {
            generateSubClass(impl, r);
        }
    }
    
    /**
     * Creates an inner static class that represents a resource and its 
     * methods. Recurses the tree of child resources.
     * @param parent the outer class for the static inner class being 
     * generated. This can either be
     * the top level <code>Endpoint</code> class or a nested static inner class 
     * for a parent resource.
     * @param resource the WADL <code>resource</code> element being processed.
     * @throws com.sun.codemodel.JClassAlreadyExistsException if, during code 
     * generation, the WADL processor attempts to create a duplicate
     * class. This indicates a structural problem with the WADL file, 
     * e.g. duplicate peer resource entries.
     */
    protected void generateSubClass(JDefinedClass parent, ResourceNode resource) 
            throws JClassAlreadyExistsException {
        
        ResourceClassGenerator rcGen = new ResourceClassGenerator(s2jModel, 
            codeModel, pkg, javaDoc, resource);
        JDefinedClass impl = rcGen.generateClass(parent);
        
        // generate Java methods for each resource method
        for (MethodNode m: resource.getMethods()) {
            rcGen.generateMethodDecls(m);
        }

        // generate sub classes for each child resource
        for (ResourceNode r: resource.getChildResources()) {
            generateSubClass(impl, r);
        }
    }
    
    /**
     * Build an abstract tree from an unmarshalled WADL file
     * @param a the application element of the root WADL file
     * @param rootFile the URI of the root WADL file. Other WADL files might be
     * included by reference.
     * @return the resource element that corresponds to the root of the resource tree
     */
    protected ResourceNode buildAst(Application a, URI rootFile) {
        Resources r = a.getResources();
        ResourceNode n = new ResourceNode(a, r);

        if (r != null) {
            for (Resource child: r.getResource()) {
                buildResourceTree(n, child, rootFile);
            }
        }
        
        return n;
    }
    
    /**
     * Add a resource and (recursively) its children to a tree starting at the parent.
     * Follow references to resources across WADL file boundaries
     * @param parent the parent resource in the tree being built
     * @param resource the WADL resource to process
     * @param file the URI of the current WADL file being processed
     */
    protected void buildResourceTree(ResourceNode parent, 
            Resource resource, URI file) {
        // check for resource reference
        String href = resource.getHref();
        if (href != null && href.length() > 0) {
            // dereference resource
            if (!href.startsWith("#")) {
                // referecnce to element in another document
                file = getReferencedFile(file, href);
            }
            resource = dereferenceLocalHref(file, href, Resource.class);
        }
        if (resource != null) {
            ResourceNode n = parent.addChild(resource);
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
     * Add a method to a resource.
     * Follow references to methods across WADL file boundaries
     * @param method the WADL method element to process
     * @param resource the resource
     * @param file the URI of the current WADL file being processed
     */
    protected void addMethodToResource(ResourceNode resource, Method method, 
            URI file) {
        String href = method.getHref();
        if (href != null && href.length() > 0) {
            // dereference resource
            if (!href.startsWith("#")) {
                // referecnce to element in another document
                file = getReferencedFile(file, href);
            }
            method = dereferenceLocalHref(file, href, Method.class);
        }
        if (method != null) {
            MethodNode n = new MethodNode(method, resource);
            Request request = method.getRequest();
            if (request != null) {
                for (Param p: request.getParam()) {
                    n.getQueryParameters().add(p);
                }
                for (Representation r: request.getRepresentation()) {
                    addRepresentation(n.getSupportedInputs(), r, file);
                }
            }
            Response response = method.getResponse();
            if (response != null) {
                for (Object o: response.getRepresentationOrFault()) {
                    if (o instanceof Representation) {
                        Representation r = (Representation)o;
                        addRepresentation(n.getSupportedOutputs(), r, file);
                    }
                    else if (o instanceof Fault) {
                        Fault f = (Fault)o;
                        FaultNode fn = new FaultNode(f);
                        n.getFaults().add(fn);
                    }
                }
            }
        }        
    }
    
    /**
     * Add a representation to a method's input or output list.
     * Follow references to representations across WADL file boundaries
     * @param list the list to add the representation to
     * @param representation the WADL representation element to process
     * @param file the URI of the current WADL file being processed
     */
    protected void addRepresentation(List<RepresentationNode> list, Representation representation, 
            URI file) {
        String href = representation.getHref();
        if (href != null && href.length() > 0) {
            // dereference resource
            if (!href.startsWith("#")) {
                // referecnce to element in another document
                file = getReferencedFile(file, href);
            }
            representation = dereferenceLocalHref(file, href, Representation.class);
        }
        if (representation != null) {
            RepresentationNode n = new RepresentationNode(representation);
            list.add(n);
        }
    }
    
    /**
     * Get the referenced file
     * @param currentFile the uri of the file that contains the reference, used 
     * to provide a base for relative paths
     * @param href the reference
     * @return the URI of the referenced file
     */
    protected static URI getReferencedFile(URI currentFile, String href) {
        if (href.startsWith("#"))
            return currentFile;
        URI ref = currentFile.resolve(href.substring(0, href.indexOf('#')));
        return ref;
    }
    
    /**
     * Dereference a href and return the object if it is of the expected type.
     * @return the resolved object
     * @param file the URI of the file in which the referenced element is located, used
     * to absolutize references
     * @param href the reference to resolve
     * @param clazz the class of object expected
     */
    @SuppressWarnings("unchecked")
    protected <T> T dereferenceLocalHref(URI file, String href, Class<T> clazz) {
        Object o = null;
        String id = file.toString()+href.substring(href.indexOf('#'));
        o = idMap.get(id);
        if (o == null) {
            System.err.println("Warning: reference '"+href+"'not found in "+file.toString()+", skipping: ");
            return null;
        }
        else if (!clazz.isInstance(o)) {
            System.err.println("Warning: reference '"+href+"' in "+file.toString()+" identifies the wrong kind of element, skipping: ");
            return null;
        }
        return (T)o;
    }
    

    /**
     * Inner class implementing the JAXB <code>ErrorListener</code> interface to
     * support error reporting from the JAXB infrastructure.
     */
    protected class SchemaCompilerErrorListener implements ErrorListener {
        /**
         * Report a warning
         * @param sAXParseException the exception that caused the warning.
         */
        public void warning(SAXParseException sAXParseException) {
            System.err.println("Warning: "+sAXParseException.getMessage());
        }

        /**
         * Report informative message
         * @param sAXParseException the exception that caused the informative message.
         */
        public void info(SAXParseException sAXParseException) {
            System.err.println("Info: "+sAXParseException.getMessage());
        }

        /**
         * Report a fatal error
         * @param sAXParseException the exception that caused the fatal error.
         */
        public void fatalError(SAXParseException sAXParseException) {
            System.err.println("Fatal Error: "+sAXParseException.getMessage());
        }

        /**
         * Report an error.
         * @param sAXParseException the exception that caused the error.
         */
        public void error(SAXParseException sAXParseException) {
            System.err.println("Error: "+sAXParseException.getMessage());
        }
        
    }

}
