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

package org.jvnet.ws.wadl2java;

import com.sun.codemodel.*;
import com.sun.codemodel.writer.FileCodeWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import org.jvnet.ws.wadl.*;
import org.jvnet.ws.wadl2java.ast.FaultNode;
import org.jvnet.ws.wadl2java.ast.MethodNode;
import org.jvnet.ws.wadl2java.ast.RepresentationNode;
import org.jvnet.ws.wadl2java.ast.ResourceNode;
import org.jvnet.ws.wadl2java.ast.ResourceTypeNode;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import com.sun.tools.xjc.api.S2JJAXBModel;
import com.sun.tools.xjc.api.ErrorListener;
import com.sun.tools.xjc.api.Mapping;
import com.sun.tools.xjc.api.SchemaCompiler;
import com.sun.tools.xjc.api.TypeAndAnnotation;
import com.sun.tools.xjc.api.impl.s2j.SchemaCompilerImpl;
import com.sun.tools.xjc.model.Model;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSType;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import javax.annotation.Generated;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.util.JAXBResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

/**
 * Processes a WADL file and generates client-side stubs for the resources and
 * methods described.
 * @author mh124079
 */
public class Wadl2Java {
    
    
    
    /**
     * A parameter object to make it easier to extend this class without
     * having to add more constructors or parameters. Each setter is chained
     * so that it can use used as the first line in a constructor.
     */
    public static class Parameters
      implements Cloneable
    {
        private CodeWriter codeWriter;
        private List<URI> customizations;
        private String pkg;
        private String generationStyle;
        private boolean autoPackage;
        private URI rootDir;
        private Map<String, String> baseURIToClassName = Collections.EMPTY_MAP;
        private MessageListener messageListener = new MessageListener() {
            public void warning(String message, Throwable throwable) {
                System.err.println(
                         Wadl2JavaMessages.LOGGER_WARNING(
                            message!=null ? message : throwable.getMessage()));
            }

            public void info(String message) {
                System.err.println(
                         Wadl2JavaMessages.LOGGER_INFO(message));
            }

            public void error(String message, Throwable throwable) {
                System.err.println(
                         Wadl2JavaMessages.LOGGER_ERROR(
                            message!=null ? message : throwable.getMessage()));
            }
        };

        
        public Parameters clone()
        {
            try {
                Parameters parameters = (Parameters)super.clone();
                parameters.customizations = new ArrayList<URI>(this.customizations);
                parameters.baseURIToClassName = new HashMap<String, String>(this.baseURIToClassName);
                return parameters;
            } catch (CloneNotSupportedException ex) {
                Logger.getLogger(Wadl2Java.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
        
        /**
         * @param the code writer used to write out the Java files
         * @return this 
         */
        public Parameters setCodeWriter(CodeWriter codeWriter) {
            this.codeWriter = codeWriter;
            return this;
        }

        /**
         * @param the generation style, currently unused
         * @return this 
         */
        public Parameters setGenerationStyle(String generationStyle) {
            this.generationStyle = generationStyle;
            return this;
        }

        /**
         * @param A list of JAX-B customization files
         * @return this 
         */
        public Parameters setCustomizations(List<URI> customizations) {
            this.customizations = new ArrayList(
                    customizations); // Copy
            return this;
        }

        /**
         * @param A list of JAX-B customization files
         * @return this 
         */
        public Parameters setCustomizationsAsFiles(List<File> customizations) {
            this.customizations = convertToURIList(customizations); // Copy
            return this;
        }

        /**
         * @param files A list of files
         * @return A list of URI for those files
         */
        private static List<URI> convertToURIList(List<File> files) {
            List<URI> copy = new ArrayList<URI>();
            for (File file : files) {
                copy.add(file.toURI());
            }
            return copy;
        }
        
        
        /**
         * @param The Java package in which to generate the code
         * @return this 
         */
        public Parameters setPkg(String pkg) {
            this.pkg = pkg;
            return this;
        }

        /**
         * @param Whether to use JAX-B auto-package generation
         * @return this 
         */
        public Parameters setAutoPackage(boolean autoPackage) {
            this.autoPackage = autoPackage;
            return this;
        }

        /**
         * @param The root directory of the generation
         * @return this 
         */
        public Parameters setRootDir(URI rootDir) {
            this.rootDir = rootDir;
            return this;
        }

    
        /**
         * @param A map of template strings to class names
         * @return this 
         */
        public Parameters setCustomClassNames(Map<String, String> map) {
            baseURIToClassName = new HashMap<String, String>(map);
            return this;
        }
        
        
        public Parameters setMessageListener(MessageListener ml) {
            messageListener = ml;
            return this;
        }
    }    
    
    private Parameters parameters;
    private JPackage jPkg;
    private S2JJAXBModel s2jModel;
    private ElementToClassResolver resolver = new ElementToClassResolver()
    {
        private Model _model;
        
        private Model getModel()
        {
           if (_model==null)
           {
               try
               {
                   Field $model = s2jModel.getClass().getDeclaredField("model");
                   $model.setAccessible(true);
                   _model = (Model) $model.get(s2jModel);
               }
               catch (Throwable th)
               {
                   parameters.messageListener.
                           warning("Problem getting hold of the model", th);
               }
           }
           return _model;
        }
        
        
        public JType resolve(QName element) {
            
            // Use the default method to resolve an element
            
            Mapping map = s2jModel.get(element);
            if (map!=null)
            {
                return map.getType().getTypeClass();
            }
            
            // This can fail with certain customizations where the element
            // and type class are seperate, so instead look up the 
            // type class directly from the model
            //
            
            Model model = getModel();
            XSElementDecl xelement =  model.schemaComponent.getElementDecl(element.getNamespaceURI(), element.getLocalPart());
            if (xelement!=null)
            {
                XSType type = xelement.getType();
                if (type!=null && type.isGlobal())
                {
                    QName qname = new QName(type.getTargetNamespace(), type.getName());
                    TypeAndAnnotation taa = s2jModel.getJavaType(qname);
                    if (taa!=null)
                    {
                        return taa.getTypeClass();
                    }
                }
            }

            // Even then we might fail on this
            //
            
            return null;
        }
    };

    private JCodeModel codeModel;
    private ElementResolver idMap;
    private Map<String, ResourceTypeNode> ifaceMap;
    private List<String> processedDocs;
    private JavaDocUtil javaDoc;
    private JAXBContext jbc;
    private SchemaCompiler s2j;
    private SchemaCompilerErrorListener errorListener;
    private String generatedPackages = "";

    /**
     * Creates a new instance of a Wadl2Java processor.
     */
    public Wadl2Java(Parameters parameters) {
        this.parameters = parameters.clone();
        assert parameters.codeWriter!=null;
        this.javaDoc = new JavaDocUtil();
        this.processedDocs = new ArrayList<String>();
        this.idMap = new ElementResolver(
                parameters.messageListener);
        this.ifaceMap = new HashMap<String, ResourceTypeNode>();
    }
    

    /**
     * Creates a new instance of a Wadl2Java processor.
     * @param outputDir the directory in which to generate code.
     * @param pkg the Java package in which to generate code.
     * @param autoPackage whether to use JAXB auto package name generation
     * @param customizations a list of JAXB customization files
     */
    public Wadl2Java(File outputDir, String pkg, boolean autoPackage, 
            List<File> customizations) throws IOException {
        this(new Parameters()
                .setRootDir(outputDir.toURI())
                .setCodeWriter(new FileCodeWriter(outputDir))
                .setPkg(pkg)
                .setAutoPackage(autoPackage)
                .setCustomizationsAsFiles(customizations));
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
            JClassAlreadyExistsException, InvalidWADLException {

        // read in root WADL file
        s2j = new SchemaCompilerImpl();
        
        errorListener = new SchemaCompilerErrorListener();
        if (!parameters.autoPackage)
            s2j.setDefaultPackageName(parameters.pkg);
        s2j.setErrorListener(errorListener);
        Application a = processDescription(rootDesc);
        List<ResourceNode> rs = buildAst(a, rootDesc);
        
        // generate code
        s2jModel = s2j.bind();
        if (s2jModel != null) {
            codeModel = s2jModel.generateCode(null, errorListener);
            Iterator<JPackage> packages = codeModel.packages();
            StringBuilder buf = new StringBuilder();
            while(packages.hasNext()) {
                JPackage genPkg = packages.next();
                if (!genPkg.isDefined("ObjectFactory"))
                    continue;
                if (buf.length() > 0)
                    buf.append(':');
                buf.append(genPkg.name());
            }
            generatedPackages = buf.toString();
            jPkg = codeModel._package(parameters.pkg);
            generateResourceTypeInterfaces();
            for (ResourceNode r: rs)
                generateEndpointClass(rootDesc, r);
            codeModel.build(parameters.codeWriter);
        }
        
        // If we have gotten this far as we have recorded a fatal error then
        // we should wrap and re-throw it
        
        if (errorListener.hasFatalErrorOccured())
        {
            throw new JAXBException(
                    Wadl2JavaMessages.JAXB_PROCESSING_FAILED(),
                    errorListener.getFirstFatalError());
        }
    }
    
    /**
     * Unmarshall a WADL file, process any schemas referenced in the WADL file, add 
     * any items with an ID to a global ID map, and follow any references to additional
     * WADL files.
     * @param desc the URI of the description file, the description is fetched by
     * converting the URI into a URL and then using a HTTP GET. Use
     * {@link #processDescription(java.net.URI, java.io.InputStream)} to
     * supply the InputStream directly
     * @return the unmarshalled WADL application element
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if 
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     */
    public Application processDescription(URI desc) 
            throws JAXBException, IOException {
        InputStream is = desc.toURL().openStream();
        return processDescription(desc, is);
    }

    /**
     * Unmarshall a WADL file, process any schemas referenced in the WADL file, add
     * any items with an ID to a global ID map, and follow any references to additional
     * WADL files.
     * @param desc the URI of the description file
     * @param is an input stream from which the description can be read
     * @return the unmarshalled WADL application element
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid or if
     * the code generator encounters a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     */
    public Application processDescription(URI desc, InputStream is)
            throws JAXBException, IOException {

        // check for files that have already been processed to prevent loops
        if (processedDocs.contains(desc.toString()))
            return null;
        processedDocs.add(desc.toString());
        
        // read in WADL file, process with stylesheet to upgrade older versions
        // and then unmarshall the result using JAXB
        parameters.messageListener.info(Wadl2JavaMessages.PROCESSING(desc.toString()));

        
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
                parameters.messageListener.info(Wadl2JavaMessages.PROCESSING(incl.toString()));
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
        for (URI customization: parameters.customizations) {
            URI incl = desc.resolve(customization);
            parameters.messageListener.info(Wadl2JavaMessages.PROCESSING(incl.toString()));
            InputSource input = new InputSource(incl.toURL().openStream());
            input.setSystemId(incl.toString());
            s2j.parseSchema(input);
        }
        buildIDMap(a, desc);
        return a;
    }

    /**
     * Build a map of all method, param, representation, fault and resource_type
     * elements that have an ID. These are used to dereference href values
     * when building the ast.
     * @param desc the URI of the WADL file being processed
     * @param a the root element of an unmarshalled WADL document
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
                    parameters.messageListener.warning(null,
                        messageStringFromObject(Wadl2JavaMessages.MISSING_ID_METHOD(), m));
                } 
                
                extractMethodIds(m, desc);
            }
            else if (child instanceof ResourceType) {

                ResourceType r = (ResourceType)child;
                if (r.getId()==null) {
                    parameters.messageListener.warning(null,
                        messageStringFromObject(Wadl2JavaMessages.MISSING_ID_RESOURCE_TYPE(),r));
                }
                
                extractResourceTypeIds(r, desc);
            }
            else if (child instanceof Representation) {

                Representation r = (Representation)child;
                if (r.getId()==null) {
                    parameters.messageListener.warning(null,
                        messageStringFromObject(Wadl2JavaMessages.MISSING_ID_REPRESENTATION(),r));
                }
                
                extractRepresentationId(r, desc);
            }
            else {

                Param r = (Param)child;
                if (r.getId()==null) {
                    parameters.messageListener.warning(null,
                        messageStringFromObject(Wadl2JavaMessages.MISSING_ID_PARAM(),r));
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
            Logger.getLogger(Wadl2Java.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // Return original message
        
        return new InvalidWADLException("[Error working out location] " + message, null);
    }
    
    /**
     * Adds the object to the ID map if it is identified and process any file pointed
     * to by href.
     * @param desc The URI of the current file being processed, used when resolving relative paths in href
     * @param id The identifier of o or null if o isn't identified
     * @param href A link to a another element, the document in which the element resides will
     * be recursively processed
     * @param o The object that is being identified or from which the link occurs
     * @return a unique identifier for the element or null if not identified
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
     * @param file the URI of the current WADL file being processed
     * @param r the representation element
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
     * @param file the URI of the current WADL file being processed
     * @param p the param element
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
     * @param file the URI of the current WADL file being processed
     * @param m the method element
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
     * @param file the URI of the current WADL file being processed
     * @param r the resource element
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
     * Also extract the ids from any contained method and its param,
     * representation or fault elements.
     * @param file the URI of the current WADL file being processed
     * @param r the resource_type element
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
     * Generate Java interfaces for WADL resource types
     * @throws com.sun.codemodel.JClassAlreadyExistsException if the interface to be generated already exists
     */
    protected void generateResourceTypeInterfaces() 
            throws JClassAlreadyExistsException {
        for (String id: ifaceMap.keySet()) {
            ResourceTypeNode n = ifaceMap.get(id);
            JDefinedClass iface = jPkg._class(JMod.PUBLIC, n.getClassName(), ClassType.INTERFACE);
            n.setGeneratedInterface(iface);
            javaDoc.generateClassDoc(n, iface);
            ResourceClassGenerator rcGen = new ResourceClassGenerator(parameters.messageListener,
                    resolver, 
                codeModel, jPkg, generatedPackages, javaDoc, iface);
            // generate Java methods for each resource method
            for (MethodNode m: n.getMethods()) {
                rcGen.generateMethodDecls(m, true);
            }
            // generate bean properties for matrix parameters
            for (Param p: n.getMatrixParams()) {
                rcGen.generateBeanProperty(iface, p, true);
            }
        }
    }
    
    /**
     * Create a class that acts as a container for a hierarchy
     * of static inner classes, one for each resource described by the WADL file.
     * @param the root URI to the WADL so we can generate the required annotations
     * @param root the resource element that corresponds to the root of the resource tree
     * @throws com.sun.codemodel.JClassAlreadyExistsException if, during code 
     * generation, the WADL processor attempts to create a duplicate
     * class. This indicates a structural problem with the WADL file, e.g. duplicate
     * peer resource entries.
     */
    protected void generateEndpointClass(
            URI rootResource, ResourceNode root) 
            throws JClassAlreadyExistsException {
        
        int counter=0; //
        JDefinedClass impl = null;
        
        // It is possible for multiple resources to have the same
        // root, so in that case we genreate the name sequentially
        //
        do
        {
            String proposedName = counter++ ==0 ?
                    root.getClassName() : root.getClassName() + counter;
            
            try
            {
                impl = jPkg._class(JMod.PUBLIC, proposedName);
                // Store the name for later
                root.setClassName(proposedName);
            }
            catch (JClassAlreadyExistsException ex)
            { 
                // So we try again
                impl = null;
            }
                
        }
        while (impl==null);
        
                
        // Put a Generated annotation on the class for later regeneration
        // by tooling
        if (rootResource!=null) {
            JAnnotationUse annUse = impl.annotate(Generated.class); 
            JAnnotationArrayMember array = annUse.paramArray("value");
            array.param("wadl|" + rootResource.toString()); 
            
            // Process any of the binding files if avaliable
            //
            
            URI packagePath = UriBuilder.fromUri(parameters.rootDir)
                    .path(parameters.pkg.replace(".", "/") + "/").build();
            
            
            for (URI customization : parameters.customizations) {
                array.param("customization|" + packagePath.relativize(customization));
            }
            
            //
            
            annUse.param("comments",
                    "wadl2java, http://wadl.java.net");
            
            // Output date
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime(new Date());
            annUse.param("date",
                    DatatypeConverter.printDateTime(
                       gc));
            
            
        }

        // Create a static final field that contains the root URI
        //

        JFieldVar $base_uri = impl.field(
            Modifier.PUBLIC
                | Modifier.STATIC
                | Modifier.FINAL, String.class, "BASE_URI");

        $base_uri.javadoc().append("The base URI for the resource represented by this proxy");

        // Generate the subordinate classes
        //
        
        for (ResourceNode r: root.getChildResources()) {
            generateSubClass(impl, $base_uri, r);
        }

        // Populate the BASE_URI field in a static init block at the
        // end of the file to make things a bit tidier.
        
        JBlock staticInit = impl.init();
        
        JVar $originalURI = staticInit.decl($base_uri.type(), "originalURI")
                .init(JExpr.lit(root.getUriTemplate()));
        
        staticInit.directStatement(
                  "// Look up to see if we have any indirection in the local copy"
                + "\n        // of META-INF/java-rs-catalog.xml file, assuming it will be in the"
                + "\n        // oasis:name:tc:entity:xmlns:xml:catalog namespace or similar duck type"
                + "\n        java.io.InputStream is = " + impl.name() + ".class.getResourceAsStream(\"/META-INF/jax-rs-catalog.xml\");"
                + "\n        if (is!=null) {"
                + "\n            try {"
                + "\n                // Ignore the namespace in the catalog, can't use wildcard until"
                + "\n                // we are sure we have XPath 2.0"
                + "\n                String found = javax.xml.xpath.XPathFactory.newInstance().newXPath().evaluate("
                + "\n                    \"/*[name(.) = 'catalog']/*[name(.) = 'uri' and @name ='\" + originalURI +\"']/@uri\", "
                + "\n                    new org.xml.sax.InputSource(is)); "
                + "\n                if (found!=null && found.length()>0) {"
                + "\n                    originalURI = found;"
                + "\n                }"
                + "\n                "
                + "\n            }"
                + "\n            catch (Exception ex) {"
                + "\n                ex.printStackTrace();"
                + "\n            }"
                + "\n            finally {"
                + "\n                try {"
                + "\n                    is.close();"
                + "\n                } catch (java.io.IOException e) {"
                + "\n                }"
                + "\n            }"
                + "\n        }"); 
        staticInit.assign($base_uri, $originalURI); 

    }
    
    /**
     * Creates an inner static class that represents a resource and its 
     * methods. Recurses the tree of child resources.
     * @param parent the outer class for the static inner class being 
     * generated. This can either be a top level class or a nested static 
     * inner class for a parent resource.
     * @param resource the WADL <code>resource</code> element being processed.
     * @param $base_uri The root URI for this resource class
     * @param isroot is this the
     * @throws com.sun.codemodel.JClassAlreadyExistsException if, during code 
     * generation, the WADL processor attempts to create a duplicate
     * class. This indicates a structural problem with the WADL file, 
     * e.g. duplicate peer resource entries.
     */
    protected void generateSubClass(JDefinedClass parent, JVar $base_uri, ResourceNode resource) 
            throws JClassAlreadyExistsException {
        
        ResourceClassGenerator rcGen = new ResourceClassGenerator(
                parameters.messageListener,
                resolver, 
            codeModel, jPkg, generatedPackages, javaDoc, resource);
        JDefinedClass impl = rcGen.generateClass(parent, $base_uri);
        
        // generate Java methods for each resource method
        for (MethodNode m: resource.getMethods()) {
            rcGen.generateMethodDecls(m, false);
        }
        
 
        // generate sub classes for each child resource
        for (ResourceNode r: resource.getChildResources()) {
            generateSubClass(impl, $base_uri, r);
        }
    }
    
    /**
     * Build an abstract tree from an unmarshalled WADL file
     * @param a the application element of the root WADL file
     * @param rootFile the URI of the root WADL file. Other WADL files might be
     * included by reference.
     * @return the resource elements that correspond to the roots of the resource trees
     */
    public List<ResourceNode> buildAst(Application a, URI rootFile) throws InvalidWADLException {
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
            
            // Override the class name if required
            //
            
            String overrideName = parameters.baseURIToClassName.get(
                    rootResourcesNode.getUriTemplate());
            if (overrideName!=null) {
                rootResourcesNode.setClassName(overrideName);
            }
            
            //
            
            for (Resource child: r.getResource()) {
                buildResourceTree(rootResourcesNode, child, rootFile);
            }
            ns.add(rootResourcesNode);
        }
        
        return ns;
    }
    
    /**
     * Build an abstract resource type based on the methods of a resource type 
     * in a WADL file
     * @param ifaceId the identifier of the resource type
     * @param a the application element of the root WADL file
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
     * resource type in a WADL file
     * @param ifaceId the identifier of the resource type
     * @param a the application element of the root WADL file
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
     * Follow references to resources across WADL file boundaries
     * @param parent the parent resource in the tree being built
     * @param resource the WADL resource to process
     * @param file the URI of the current WADL file being processed
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
     * Follow references to types across WADL file boundaries
     * @param href the identifier of the resource_type element to process
     * @param resourceNode the resource AST node
     * @param resource the resource object from the model.
     * @param file the URI of the current WADL file being processed
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
            throw Wadl2Java.messageStringFromObject(
                    Wadl2JavaMessages.SKIPPING_REFERENCE(href), resource);
        }  
    }
    
    /**
     * Add a method to a resource type.
     * Follow references to methods across WADL file boundaries
     * @param method the WADL method element to process
     * @param resource the resource type
     * @param file the URI of the current WADL file being processed
     */
    protected void addMethodToResourceType(ResourceTypeNode resource, Method method, 
            URI file) throws InvalidWADLException {
        addMethodToParent(resource, method, file);
    }
    
    /**
     * Add a child resource to a resource type.
     * Follow references to resources across WADL file boundaries
     * @param resource the WADL resource element to process
     * @param type the parent resource type
     * @param file the URI of the current WADL file being processed
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
     * @param response the response to check
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
     * Follow references to methods across WADL file boundaries
     * @param method the WADL method element to process
     * @param resource the resource
     * @param file the URI of the current WADL file being processed
     */
    protected void addMethodToResource(ResourceNode resource, Method method, 
            URI file) throws InvalidWADLException {
        addMethodToParent(resource, method, file);
    }
    
    /**
     * Add a method to a resource.
     * Follow references to methods across WADL file boundaries
     * @param method the WADL method element to process
     * @param parent the parent object, can be resource or resource type
     * @param file the URI of the current WADL file being processed
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
                parameters.messageListener.warning(null,
                    messageStringFromObject(Wadl2JavaMessages.LONELY_HREF_METHOD(),method));
            }
            
            // dereference resource
            file = getReferencedFile(file, href);
            method = idMap.resolve(file, href, method);
        }
        
        
        if (method != null) {

            if (method.getName()==null || method.getName().length()==0) {
                InvalidWADLException errorMessage = messageStringFromObject(Wadl2JavaMessages.MISSING_METHOD_NAME(), method);
                parameters.messageListener.error(
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
                            parameters.messageListener.warning(null,
                                messageStringFromObject(Wadl2JavaMessages.LONELY_HREF_PARAM(),p));
                        }                        
                        
                        // dereference param
                        file = getReferencedFile(file, href);
                        p = idMap.resolve(file, href, p);
                    }
                    if (p != null) {
                        if (p.getStyle()==ParamStyle.HEADER)
                            n.getHeaderParameters().add(p);
                        else
                            n.getQueryParameters().add(p);
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
                        n.getFaults().add(fn);
                    } else {
                        addRepresentation(n.getSupportedOutputs(), o, file);
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
                parameters.messageListener.warning(null,
                    messageStringFromObject(Wadl2JavaMessages.LONELY_HREF_REPRESENTATION(), representation));
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
     * Get the referenced file, currentFile will be returned if href is a
     * fragment identifier, otherwise href is resolved against currentFile.
     * @param currentFile the uri of the file that contains the reference, used 
     * to provide a base for relative paths
     * @param href the reference
     * @return the URI of the referenced file
     */
    protected static URI getReferencedFile(URI currentFile, String href) {
        if (href.startsWith("#"))
            return currentFile;
        // href references another file
        URI ref = currentFile.resolve(href.substring(0, href.indexOf('#')));
        return ref;
    }
    
    /**
     * Inner class implementing the JAXB <code>ErrorListener</code> interface to
     * support error reporting from the JAXB infrastructure.
     */
    protected class SchemaCompilerErrorListener implements ErrorListener {
        
        private Throwable firstFatalError;
        
        /**
         * @return Whether a fatal error has occurred;
         */
        public boolean hasFatalErrorOccured(){
            return firstFatalError!=null;
        }

        /**
         * @return The first fatal error to have occurred.
         */
        public Throwable getFirstFatalError(){
            return firstFatalError;
        }

        
        /**
         * Report a warning
         * @param sAXParseException the exception that caused the warning.
         */
        public void warning(SAXParseException sAXParseException) {
            parameters.messageListener.warning(
                    Wadl2JavaMessages.WARNING(sAXParseException.getMessage()),
                    sAXParseException);
        }

        /**
         * Report informative message
         * @param sAXParseException the exception that caused the informative message.
         */
        public void info(SAXParseException sAXParseException) {
            parameters.messageListener.info(
                Wadl2JavaMessages.INFO(
                    sAXParseException.getMessage(),
                    sAXParseException.getLineNumber(),
                    sAXParseException.getColumnNumber(),
                    sAXParseException.getSystemId()));
        }

        /**
         * Report a fatal error
         * @param sAXParseException the exception that caused the fatal error.
         */
        public void fatalError(SAXParseException sAXParseException) {
            if (firstFatalError==null)
            {
                firstFatalError = sAXParseException;
            }
            
            parameters.messageListener.error(
                    Wadl2JavaMessages.ERROR_FATAL( 
                        sAXParseException.getMessage(),
                        sAXParseException.getLineNumber(),
                        sAXParseException.getColumnNumber(),
                        sAXParseException.getSystemId()),
                    sAXParseException);
        }

        /**
         * Report an error.
         * @param sAXParseException the exception that caused the error.
         */
        public void error(SAXParseException sAXParseException) {
            parameters.messageListener.error(
                    Wadl2JavaMessages.ERROR( 
                        sAXParseException.getMessage(),
                        sAXParseException.getLineNumber(),
                        sAXParseException.getColumnNumber(),
                        sAXParseException.getSystemId()),
                    sAXParseException);
        }
        
    }

}
