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
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.api.*;
import com.sun.tools.xjc.api.impl.s2j.SchemaCompilerImpl;
import com.sun.tools.xjc.model.Model;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSType;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Generated;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import org.jvnet.ws.wadl.Param;
import org.jvnet.ws.wadl.ast.*;
import org.jvnet.ws.wadl.util.MessageListener;
import org.jvnet.ws.wadl2java.common.BaseResourceClassGenerator;
import org.jvnet.ws.wadl2java.jaxrs.JAXRS20ResourceClassGenerator;
import org.jvnet.ws.wadl2java.jersey.Jersey1xResourceClassGenerator;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;


/**
 * Processes a WADL file and generates client-side stubs for the resources and
 * methods described.
 *
 * @author mh124079
 */
public class Wadl2Java {
    
    // Generation Styles
    public static final String STYLE_JERSEY1X = "jersey1x";
    public static final String STYLE_JAXRS20 = "jaxrs20";

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
        private List<String> xjcArguments;
        private String pkg;
        private String generationStyle = STYLE_JERSEY1X;
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


        @Override
        public Parameters clone()
        {
            try {
                Parameters parameters = (Parameters)super.clone();
                parameters.customizations = new ArrayList<URI>(this.customizations);
                parameters.baseURIToClassName = new HashMap<String, String>(this.baseURIToClassName);
                parameters.xjcArguments = ( this.xjcArguments == null ? null : new ArrayList<String>(this.xjcArguments));
                return parameters;
            } catch (CloneNotSupportedException ex) {
                Logger.getLogger(Wadl2Java.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

        /**
         * @param codeWriter the code writer used to write out the Java files.
         * @return {@code this}.
         */
        public Parameters setCodeWriter(CodeWriter codeWriter) {
            this.codeWriter = codeWriter;
            return this;
        }

        /**
         * @param generationStyle the generation style, currently unused.
         * @return {@code this}.
         */
        public Parameters setGenerationStyle(String generationStyle) {
            this.generationStyle = generationStyle;
            return this;
        }

        /**
         * @param customizations A list of JAX-B customization files.
         * @return {@code this}.
         */
        public Parameters setCustomizations(List<URI> customizations) {
            this.customizations = new ArrayList<URI>(
                    customizations); // Copy
            return this;
        }

        /**
         * @param customizations A list of JAX-B customization files.
         * @return {@code this}.
         */
        public Parameters setCustomizationsAsFiles(List<File> customizations) {
            this.customizations = convertToURIList(customizations); // Copy
            return this;
        }

        /**
         * @param files A list of files.
         * @return A list of URI for those files.
         */
        private static List<URI> convertToURIList(List<File> files) {
            List<URI> copy = new ArrayList<URI>();
            for (File file : files) {
                copy.add(file.toURI());
            }
            return copy;
        }

        /**
         * @param pkg The Java package in which to generate the code.
         * @return {@code this}.
         */
        public Parameters setPkg(String pkg) {
            this.pkg = pkg;
            return this;
        }

        /**
         * @param autoPackage Whether to use JAX-B auto-package generation.
         * @return {@code this}.
         */
        public Parameters setAutoPackage(boolean autoPackage) {
            this.autoPackage = autoPackage;
            return this;
        }

        /**
         * @param rootDir The root directory of the generation.
         * @return {@code this}.
         */
        public Parameters setRootDir(URI rootDir) {
            this.rootDir = rootDir;
            return this;
        }


        /**
         * @param map A map of template strings to class names.
         * @return {@code this}.
         */
        public Parameters setCustomClassNames(Map<String, String> map) {
            baseURIToClassName = new HashMap<String, String>(map);
            return this;
        }


        public Parameters setMessageListener(MessageListener ml) {
            messageListener = ml;
            return this;
        }

        /**
         * @param xjcArguments arguments
         */
        public Parameters setXjcArguments(List<String> xjcArguments) {
            this.xjcArguments = xjcArguments;
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
    private JavaDocUtil javaDoc;
    private SchemaCompiler s2j;
    private String generatedPackages = "";
    private WadlAstBuilder astBuilder;

    /**
     * Creates a new instance of a Wadl2Java processor.
     */
    public Wadl2Java(Parameters parameters) {
        this.parameters = parameters.clone();
        assert parameters.codeWriter!=null;
        this.javaDoc = new JavaDocUtil();
    }


    /**
     * Creates a new instance of a Wadl2Java processor.
     *
     * @param outputDir the directory in which to generate code.
     * @param pkg the Java package in which to generate code.
     * @param autoPackage whether to use JAXB auto package name generation
     * @param customizations a list of JAXB customization files
     * @throws java.io.IOException TODO.
     */
    public Wadl2Java(File outputDir, String pkg, boolean autoPackage,
                     List<File> customizations, List<String> xjcArguments) throws IOException {
        this(new Parameters()
                .setRootDir(outputDir.toURI())
                .setCodeWriter(new FileCodeWriter(outputDir))
                .setPkg(pkg)
                .setAutoPackage(autoPackage)
                .setCustomizationsAsFiles(customizations)
                .setXjcArguments(xjcArguments));
    }

    /**
     * Process the root WADL file and generate code.
     *
     * @param rootDesc the URI of the WADL file to process
     * @throws javax.xml.bind.JAXBException if the WADL file is invalid, a
     * referenced WADL file is invalid, or if the code generator encounters
     * a problem.
     * @throws java.io.IOException if the specified WADL file cannot be read.
     * @throws com.sun.codemodel.JClassAlreadyExistsException if, during code 
     * generation, the WADL processor attempts to create a duplicate
     * class. This indicates a structural problem with the WADL file, e.g. duplicate
     * peer resource entries.
     * @throws org.jvnet.ws.wadl.ast.InvalidWADLException TODO.
     */
    public void process(URI rootDesc) throws JAXBException, IOException,
            JClassAlreadyExistsException, InvalidWADLException {

        // read in root WADL file
        s2j = new SchemaCompilerImpl();

        SchemaCompilerErrorListener errorListener = new SchemaCompilerErrorListener();
        if (!parameters.autoPackage)
            s2j.setDefaultPackageName(parameters.pkg);
        s2j.setErrorListener(errorListener);

        this.astBuilder = new WadlAstBuilder(
                new WadlAstBuilder.SchemaCallback() {

                    public void processSchema(InputSource input) {
                        s2j.parseSchema(input);
                    }

                    public void processSchema(String uri, Element node) {
                        s2j.parseSchema(uri, node);
                    }
                },
                parameters.messageListener);

        ApplicationNode an = astBuilder.buildAst(rootDesc);
        List<ResourceNode> rs = an.getResources();


        // Override the class name if required
        //

        for (ResourceNode rootResourcesNode : rs) {

            String overrideName = parameters.baseURIToClassName.get(
                    rootResourcesNode.getUriTemplate());
            if (overrideName!=null) {
                rootResourcesNode.setClassName(overrideName);
            }
        }


        // Apply any customizations
        //
        for (URI customization: parameters.customizations) {
            URI incl = rootDesc.resolve(customization);
            parameters.messageListener.info(Wadl2JavaMessages.PROCESSING(incl.toString()));
            InputSource input = new InputSource(incl.toURL().openStream());
            input.setSystemId(incl.toString());
            s2j.parseSchema(input);
        }


        // generate code
        applyXjcArguments(s2j.getOptions());
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

    private void applyXjcArguments(Options options) {
        if(parameters.xjcArguments != null) {
            try {
                String[] args = parameters.xjcArguments.toArray(new String[0]);
                for(int i=0;i<args.length;i++) {
                    options.parseArgument(args, i);
                }
            } catch (BadCommandLineException e) {
                throw new RuntimeException(e);
            }
        }
    }

    
    
    private ResourceClassGenerator createGeneratorForResource(ResourceNode resource) {
        
        if (parameters.generationStyle==null || STYLE_JERSEY1X.equals(parameters.generationStyle)) {
            return  new Jersey1xResourceClassGenerator(
                parameters.messageListener,
                resolver,
                codeModel, jPkg, generatedPackages, javaDoc, resource);
        }
        else if (STYLE_JAXRS20.equals(parameters.generationStyle)) {
            return  new JAXRS20ResourceClassGenerator(
                parameters.messageListener,
                resolver,
                codeModel, jPkg, generatedPackages, javaDoc, resource);
        }
        else{
            throw new IllegalStateException("Invalid generation style");
        }
    }

    private ResourceClassGenerator createGeneratorForResourceType(JDefinedClass iface) {
        if (parameters.generationStyle==null || STYLE_JERSEY1X.equals(parameters.generationStyle)) {
            return new Jersey1xResourceClassGenerator(parameters.messageListener,
                    resolver,
                    codeModel, jPkg, generatedPackages, javaDoc, iface);
        }
        else if (STYLE_JAXRS20.equals(parameters.generationStyle)) {
            return  new JAXRS20ResourceClassGenerator(
                parameters.messageListener,
                    resolver,
                    codeModel, jPkg, generatedPackages, javaDoc, iface);
        }
        else {
            throw new IllegalStateException("Invalid generation style");
        }
    }
    
    
    /**
     * Generate Java interfaces for WADL resource types
     * @throws com.sun.codemodel.JClassAlreadyExistsException if the interface to be generated already exists
     */
    protected void generateResourceTypeInterfaces()
            throws JClassAlreadyExistsException{

        Map<String, ResourceTypeNode> ifaceMap = astBuilder.getInterfaceMap();
        for (String id: ifaceMap.keySet()) {
            ResourceTypeNode n = ifaceMap.get(id);
            JDefinedClass iface = jPkg._class(JMod.PUBLIC, n.getClassName(), ClassType.INTERFACE);
            n.setGeneratedInterface(iface);
            javaDoc.generateClassDoc(n, iface);
            ResourceClassGenerator rcGen = createGeneratorForResourceType(iface);
            // generate Java methods for each resource method
            for (MethodNode m: n.getMethods()) {
                rcGen.generateMethodDecls(m, true);
            }
            List<Param> matrixParams = n.getMatrixParams();
            // generate bean properties for matrix parameters
            for (Param p: matrixParams) {
                rcGen.generateBeanProperty(iface, matrixParams, p, true);
            }
        }
    }


    /**
     * Create a class that acts as a container for a hierarchy
     * of static inner classes, one for each resource described by the WADL file.
     *
     * @param rootResource the root URI to the WADL so we can generate the required annotations
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
        JDefinedClass impl;

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
                | Modifier.FINAL, URI.class, "BASE_URI");

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
                .init(
                   codeModel.ref(URI.class).staticInvoke("create").arg(JExpr.lit(root.getUriTemplate())));

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
                + "\n                    originalURI = java.net.URI.create(found);"
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
     *
     * @param parent the outer class for the static inner class being 
     * generated. This can either be a top level class or a nested static 
     * inner class for a parent resource.
     * @param resource the WADL <code>resource</code> element being processed.
     * @param $base_uri The root URI for this resource class
     * @throws com.sun.codemodel.JClassAlreadyExistsException if, during code
     * generation, the WADL processor attempts to create a duplicate
     * class. This indicates a structural problem with the WADL file, 
     * e.g. duplicate peer resource entries.
     */
    protected void generateSubClass(JDefinedClass parent, JVar $global_base_uri, ResourceNode resource)
            throws JClassAlreadyExistsException {

        ResourceClassGenerator rcGen = createGeneratorForResource(resource);
        
        JDefinedClass impl = rcGen.generateClass(parent, $global_base_uri);

        // generate Java methods for each resource method
        for (MethodNode m: resource.getMethods()) {
            rcGen.generateMethodDecls(m, false);
        } 


        // generate sub classes for each child resource
        for (ResourceNode r: resource.getChildResources()) {
            generateSubClass(impl,$global_base_uri, r);
        }
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
