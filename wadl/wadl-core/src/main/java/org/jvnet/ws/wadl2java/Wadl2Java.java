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
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.api.*;
import com.sun.tools.xjc.api.impl.s2j.SchemaCompilerImpl;
import com.sun.tools.xjc.model.Model;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSType;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.CharBuffer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.RuleFactory;
import org.jvnet.ws.wadl.ast.*;
import org.jvnet.ws.wadl.ast.AbstractNode.NodeVisitor;
import org.jvnet.ws.wadl.util.MessageListener;
import org.jvnet.ws.wadl2java.javascript.JavaScriptGenerator;
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
    public static final String STYLE_JQUERY_JAVASCRIPT = "jqueryJavaScript";
    public static final String STYLE_DEFAULT = STYLE_JERSEY1X;
    public static final Set<String> STYLE_SET = new HashSet<String>() {{
            add(STYLE_JERSEY1X);
            add(STYLE_JAXRS20);
            add(STYLE_JQUERY_JAVASCRIPT);
        }
    };
    public static final Set<String> JAVA_SET = new HashSet<String>() {{
            add(STYLE_JERSEY1X);
            add(STYLE_JAXRS20);
        }
    };
    
    public static final QName JSON_SCHEMA_DESCRIBEDBY
            = new QName("http://wadl.dev.java.net/2009/02/json-schema","describedby");
    
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
        private String generationStyle = STYLE_DEFAULT;
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
                parameters.customizations = new ArrayList<URI>(this.getCustomizations());
                parameters.baseURIToClassName = new HashMap<String, String>(this.getBaseURIToClassName());
                parameters.xjcArguments = ( this.getXjcArguments() == null ? null : new ArrayList<String>(this.getXjcArguments()));
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

        /**
         * @return the codeWriter
         */
        public CodeWriter getCodeWriter() {
            return codeWriter;
        }

        /**
         * @return the customizations
         */
        public List<URI> getCustomizations() {
            return customizations;
        }

        /**
         * @return the xjcArguments
         */
        public List<String> getXjcArguments() {
            return xjcArguments;
        }

        /**
         * @return the pkg
         */
        public String getPkg() {
            return pkg;
        }

        /**
         * @return the generationStyle
         */
        public String getGenerationStyle() {
            return generationStyle;
        }

        /**
         * @return the autoPackage
         */
        public boolean isAutoPackage() {
            return autoPackage;
        }

        /**
         * @return the rootDir
         */
        public URI getRootDir() {
            return rootDir;
        }

        /**
         * @return the baseURIToClassName
         */
        public Map<String, String> getBaseURIToClassName() {
            return baseURIToClassName;
        }

        /**
         * @return the messageListener
         */
        public MessageListener getMessageListener() {
            return messageListener;
        }

    }

    private Parameters parameters;
    private JPackage jPkg;
    private Map<URI, JType> jsonTypes = new HashMap<URI, JType>();
    private S2JJAXBModel s2jModel;
    private URI currentBaseUri;
    private Resolver resolver = new Resolver()
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
                    parameters.getMessageListener().
                            warning("Problem getting hold of the model", th);
                }
            }

            return _model;
        }

        public JType resolve(Object input) {

            if (input instanceof QName)
            {
                QName element = (QName) input;
                
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
                
                return null;
            }
            else if (input instanceof URI){
                URI element = (URI) input;

                // Even then we might fail on this, but finally check the json
                // types
                //

                return jsonTypes.get(element);
            }
            else {
                throw new IllegalArgumentException("input value not of type URI or QName");
            }

        }

        public URI resolveURI(AbstractNode context, String path) {
            return currentBaseUri.resolve(path);
        }

        public boolean isThereJsonMapping() {
            return !jsonTypes.isEmpty();
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
        assert parameters.getCodeWriter()!=null;
        this.javaDoc = new JavaDocUtil();
        
        // Parameter validation
        
        if (!STYLE_SET.contains(parameters.generationStyle)) {
            throw new IllegalArgumentException(
                    Wadl2JavaMessages.INVALID_GENERATION_STYLE(parameters.getGenerationStyle(), STYLE_SET));
        }
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
    public void process(final URI rootDesc) throws JAXBException, IOException,
            JClassAlreadyExistsException, InvalidWADLException {
        
        //
        
        boolean isJava = JAVA_SET.contains(parameters.generationStyle);

        // Just store the current location so we can resolve more easily
        
        currentBaseUri = rootDesc;
        
        // Store a list of JSON schemas
        
        final Set<URI> jsonSchemas = new LinkedHashSet<URI>();
        SchemaCompilerErrorListener errorListener = null;
        
        if (isJava)
        {
            // read in root WADL file
            s2j = new SchemaCompilerImpl();

            errorListener = new SchemaCompilerErrorListener();
            if (!parameters.isAutoPackage())
                s2j.setDefaultPackageName(parameters.getPkg());
            s2j.setErrorListener(errorListener);
        }

        this.astBuilder = new WadlAstBuilder(
                new WadlAstBuilder.SchemaCallback() {

                    public void processSchema(InputSource input) {
                        
                        if (s2j==null)
                        {
                            return;
                        }
                        
                        // Assume that the stream is a buffered stream at this point
                        // and mark a position
                        InputStream is = input.getByteStream();
                        is.mark(8192);
                        
                        // Read the first bytes and look for the xml header
                        //
                        String peakContent = null;
                        
                        try {
                            Reader r = new InputStreamReader(is, "UTF-8");
                            
                            CharBuffer cb = CharBuffer.allocate(20);
                            r.read(cb);
                            cb.flip();
                            peakContent = cb.toString();
                        }
                        catch (IOException e) {
                            throw new RuntimeException("Internal problem pushing back buffer", e);
                        } finally {
                            try {
                                is.reset();
                            } catch (IOException ex) {
                                throw new RuntimeException("Internal problem pushing back buffer", ex);
                            }
                            
                        }
                            
                        // By default assume a xml schema, better guess
                        // because some XML files don't start with <?xml
                        // as per bug WADL-66
                        if (peakContent.matches("^\\s*\\{")) {
                            // We are guessing this is a json type
                            jsonSchemas.add(URI.create(input.getSystemId()));
                        }
                        else { //if (peakContent==null || peakContent.contains("<?xml") || peakContent.startsWith("<")) {
                            s2j.parseSchema(input);
                        } 
                    
                    }

                    public void processSchema(String uri, Element node) {
                        if (s2j==null)
                        {
                            return;
                        }
                        
                        s2j.parseSchema(uri, node);
                    }
                }, parameters.getMessageListener());

        ApplicationNode an = astBuilder.buildAst(rootDesc);
        List<ResourceNode> rs = an.getResources();


        // Override the class name if required
        //

        for (ResourceNode rootResourcesNode : rs) {

            String overrideName = parameters.getBaseURIToClassName().get(
                    rootResourcesNode.getUriTemplate());
            if (overrideName!=null) {
                rootResourcesNode.setClassName(overrideName);
            }
        }


        if (isJava)
        {
            // Apply any customizations
            //
            for (URI customization: parameters.getCustomizations()) {
                URI incl = rootDesc.resolve(customization);
                parameters.getMessageListener().info(Wadl2JavaMessages.PROCESSING(incl.toString()));
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

                // Generate the json classes, first find any references in the 
                // elements themselves
                //

                an.visit(new NodeVisitor() {

                    public void visit(AbstractNode node) {

                        if (node instanceof RepresentationNode) {
                            RepresentationNode rn = (RepresentationNode) node;
                            String uriStr = rn.getOtherAttribute(JSON_SCHEMA_DESCRIBEDBY);
                            if (uriStr!=null)
                            {
                                URI uri = rootDesc.resolve(uriStr);
                                if (uri!=null) {
                                    jsonSchemas.add(uri);
                                }
                            }
                        }

                    }

                });


                // Commented out until we work out what to about JSON Schema
                //

                final AnnotationStyle sa = STYLE_JERSEY1X.equals(parameters.getGenerationStyle()) ?
                        AnnotationStyle.JACKSON1 : AnnotationStyle.JACKSON2;
                GenerationConfig gc = new DefaultGenerationConfig() {
                    public AnnotationStyle getAnnotationStyle() {
                        return sa;
                    }

                };
                AnnotatorFactory af = new AnnotatorFactory();

                SchemaMapper sm = new SchemaMapper(
                        new RuleFactory(
                            gc, 
                            af.getAnnotator(sa),
                            new SchemaStore()), 
                        new SchemaGenerator());



                for (URI jsonSchema : jsonSchemas)
                {
                    String jsonSchemaStr = jsonSchema.toString();
                    String name = jsonSchemaStr.substring(jsonSchemaStr.lastIndexOf('/')+1
                            );
                    String withoutExtension = name.lastIndexOf('.')!=-1
                            ? name.substring(0,name.lastIndexOf('.'))
                            : name;

                    String className = Character.toUpperCase(withoutExtension.charAt(0))
                            + ((withoutExtension.length() > 1 ? withoutExtension.substring(1) : ""));

                    sm.generate(codeModel, 
                            className, parameters.getPkg(), jsonSchema.toURL());

                    // Store this as we would any other json type
                    jsonTypes.put(
                            jsonSchema, 
                            codeModel._getClass(parameters.getPkg() + "." + className));
                }


                // Generate the resource interface
                //
                jPkg = codeModel._package(parameters.getPkg());
            }
        }
        else
        {
            codeModel = new JCodeModel();
        }

        
        Map<String, ResourceTypeNode> ifaceMap = astBuilder.getInterfaceMap();
        for (String id: ifaceMap.keySet()) {
            ResourceTypeNode n = ifaceMap.get(id);

            ResourceClassGenerator rcGen = createGeneratorForResourceType();
            rcGen.generateResourceTypeInterface(n);
        }


        for (ResourceNode r: rs)
        {
            ResourceClassGenerator rcg = createGeneratorForResource(r);
            rcg.generateEndpointClass(rootDesc, r); 
        }


        // Reuse the codemodel abstract for the moment
        codeModel.build(parameters.getCodeWriter());
        
 
        // If we have gotten this far as we have recorded a fatal error then
        // we should wrap and re-throw it
        if (errorListener!=null && errorListener.hasFatalErrorOccured())
        {
            throw new JAXBException(
                    Wadl2JavaMessages.JAXB_PROCESSING_FAILED(),
                    errorListener.getFirstFatalError());
        } 
    }

    private void applyXjcArguments(Options options) {
        if(parameters.getXjcArguments() != null) {
            try {
                String[] args = parameters.getXjcArguments().toArray(new String[0]);
                for(int i=0;i<args.length;i++) {
                    options.parseArgument(args, i);
                }
            } catch (BadCommandLineException e) {
                throw new RuntimeException(e);
            }
        }
    }

    
    
    private ResourceClassGenerator createGeneratorForResource(ResourceNode resource) {
        
        if (parameters.getGenerationStyle()==null || STYLE_JERSEY1X.equals(parameters.getGenerationStyle())) {
            return  new Jersey1xResourceClassGenerator(
                    parameters,
                resolver,
                codeModel, jPkg, generatedPackages, javaDoc, resource);
        }
        else if (STYLE_JAXRS20.equals(parameters.getGenerationStyle())) {
            return  new JAXRS20ResourceClassGenerator(
                    parameters,
                resolver,
                codeModel, jPkg, generatedPackages, javaDoc, resource);
        }
        else if (STYLE_JQUERY_JAVASCRIPT.equals(parameters.getGenerationStyle())) {
            return  new JavaScriptGenerator(
                    parameters,
                    codeModel);
        }
        else{
            throw new IllegalStateException("Invalid generation style");
        }
    }

    private ResourceClassGenerator createGeneratorForResourceType() {
        if (parameters.getGenerationStyle()==null || STYLE_JERSEY1X.equals(parameters.getGenerationStyle())) {
            return new Jersey1xResourceClassGenerator(parameters,
                    resolver,
                    codeModel, jPkg, generatedPackages, javaDoc);
        }
        else if (STYLE_JAXRS20.equals(parameters.getGenerationStyle())) {
            return  new JAXRS20ResourceClassGenerator(
                    parameters,
                    resolver,
                    codeModel, jPkg, generatedPackages, javaDoc);
        }
        else if (STYLE_JQUERY_JAVASCRIPT.equals(parameters.getGenerationStyle())) {
            return  new JavaScriptGenerator(
                    parameters,
                    codeModel);
        }
        else {
            throw new IllegalStateException("Invalid generation style");
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
            parameters.getMessageListener().warning(
                    Wadl2JavaMessages.WARNING(sAXParseException.getMessage()),
                    sAXParseException);
        }

        /**
         * Report informative message
         * @param sAXParseException the exception that caused the informative message.
         */
        public void info(SAXParseException sAXParseException) {
            parameters.getMessageListener().info(
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

            parameters.getMessageListener().error(
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
            parameters.getMessageListener().error(
                    Wadl2JavaMessages.ERROR(
                            sAXParseException.getMessage(),
                            sAXParseException.getLineNumber(),
                            sAXParseException.getColumnNumber(),
                            sAXParseException.getSystemId()),
                    sAXParseException);
        }

    }

}
