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
 * BaseResourceClassGenerator.java
 *
 * Created on June 1, 2006, 5:23 PM
 * 
 */

package org.jvnet.ws.wadl2java.common;
 
import com.sun.codemodel.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import org.jvnet.ws.wadl.Param;
import org.jvnet.ws.wadl.ParamStyle;
import org.jvnet.ws.wadl.ast.*;
import org.jvnet.ws.wadl.util.MessageListener;
import org.jvnet.ws.wadl2java.*;

/**
 * Generator class for nested static classes used to represent web resources.
 *
 * @author mh124079
 */
public abstract class BaseResourceClassGenerator implements ResourceClassGenerator {



    protected static enum MethodType
    {
        JAXB, CLASS, GENERIC_TYPE
    };

    private ResourceNode resource;
    protected JPackage pkg;
    private ElementToClassResolver resolver;
    protected JCodeModel codeModel;
    private JFieldVar $clientReference;
    private JFieldVar $uriBuilder;
    private JFieldVar $templateMatrixParamValMap;
    private JDefinedClass $class = null;
    private JavaDocUtil javaDoc;
    private String generatedPackages;
    private MessageListener messageListener; 
    
    /**
     * Creates a new instance of BaseResourceClassGenerator.
     *
     * @param javaDoc a JavaDocUtil instance for use when generating documentation.
     * @param resolver the schema2java model to use for element to class mapping lookups.
     * @param codeModel code model instance to use when generating code.
     * @param pkg package for new classes.
     * @param resource the resource element for which to generate a class.
     */
    public BaseResourceClassGenerator(
            MessageListener messageListener,
            ElementToClassResolver resolver, JCodeModel codeModel, 
            JPackage pkg, String generatedPackages, JavaDocUtil javaDoc, ResourceNode resource) {
        this.messageListener = messageListener;
        this.resource = resource;
        this.codeModel = codeModel;
        this.javaDoc = javaDoc;
        this.resolver = resolver;
        this.pkg = pkg;
        this.generatedPackages = generatedPackages;
    }
    
    /**
     * Creates a new instance of BaseResourceClassGenerator.
     *
     * @param javaDoc a JavaDocUtil instance for use when generating documentation.
     * @param resolver the schema2java model to use for element to class mapping lookups.
     * @param codeModel code model instance to use when generating code.
     * @param pkg package for new classes.
     * @param clazz the existing class.
     */
    public BaseResourceClassGenerator(
            MessageListener messageListener,
            ElementToClassResolver resolver, JCodeModel codeModel, 
            JPackage pkg, String generatedPackages, JavaDocUtil javaDoc, JDefinedClass clazz) {
        this.messageListener = messageListener;
        this.resource = null;
        this.codeModel = codeModel;
        this.javaDoc = javaDoc;
        this.resolver = resolver;
        this.pkg = pkg;
        this.$class = clazz;
        this.generatedPackages = generatedPackages;
    }



    
    /**
     * Get the class for which methods will be generated.
     *
     * @return the class or null if no class has yet been generated.
     */
    protected JDefinedClass getGeneratedClass() {
        return $class;
    }
 
    //import com.sun.jersey.api.client.Client;
//import com.sun.jersey.api.client.ClientResponse;
//import com.sun.jersey.api.client.GenericType;
//import com.sun.jersey.api.client.WebResource;

    protected abstract JClass clientType();
    protected abstract JClass clientFactoryType();
    protected abstract String clientFactoryMethod();
    protected abstract JClass clientResponseClientType();
    protected abstract JClass genericTypeType();
    protected abstract JClass resourceType();
    protected abstract String resourceFromClientMethod();    
    protected abstract JClass resourceBuilderType();
    protected abstract String buildMethod();
    protected abstract String responseGetEntityMethod();

    // Functional call outs
    protected abstract JVar createRequestBuilderAndAccept(JBlock $methodBody, JVar $resource, RepresentationNode outputRep);
    protected abstract JExpression createProcessInvocation(MethodNode method, JBlock $methodBody, JVar $resourceBuilder, String methodString, RepresentationNode inputRep, JType returnType, JExpression $returnTypeExpr, JExpression $entityExpr);
    
    
    /**
     * Look for an owner class that has the same name, if it has then fail
     * @return true is this class name is valid
     */
    private boolean validClassName(JClass parentClass, String className) {
        
        while(parentClass!=null) {
            // And inner class cannot have the same name as a sibling class
            if (parentClass.name().equals(className)) {
                return false;
            }
            
            parentClass = parentClass.outer();
            if (parentClass!=null)
            {
                int i=0;
            }
        }
        
        return true;
    }
    
    
    /**
     * Generate a static member class that represents a WADL resource.
     *
     * @param parentClass the parent class for the generated class.
     * @param $global_base_uri a reference to the field that contains the base URI.
     * @return the generated class.
     * @throws com.sun.codemodel.JClassAlreadyExistsException if a class with 
     * the same name already exists.
     */
    @Override
    public JDefinedClass generateClass(JDefinedClass parentClass, 
            JVar $global_base_uri) throws JClassAlreadyExistsException {

        String className = resource.getClassName();
        String originalClassName  = className;
        JClass owner = parentClass;
        while (owner!=null && !validClassName(parentClass, className))
        {
            className = owner.name() + className;
            owner = owner.outer();
        }
        
        // Store the value if it has been updated;
        if (!originalClassName.equals(className)) {
            resource.setClassName(className);
        }
        
        //
        
        JDefinedClass $impl = parentClass._class(JMod.PUBLIC | JMod.STATIC, className); 
        
        
        for (ResourceTypeNode t: resource.getResourceTypes()) {
            $impl._implements(t.getGeneratedInterface());
        }
        javaDoc.generateClassDoc(resource, $impl);
        
        
        // We are going to use Client for this
        
        $clientReference = $impl.field(JMod.PRIVATE,clientType(), "_client"); 
        JClass uriBuilderClass = codeModel.ref(UriBuilder.class);
        $uriBuilder = $impl.field(JMod.PRIVATE, uriBuilderClass, "_uriBuilder");        
        
        
        JClass mapOfStringObject = codeModel.ref(Map.class).narrow(String.class, Object.class);
        JClass hashMapOfStringObject = codeModel.ref(HashMap.class).narrow(String.class, Object.class);
        $templateMatrixParamValMap = $impl.field(JMod.PRIVATE, mapOfStringObject, "_templateAndMatrixParameterValues");

        // Store the base URI
        JVar $uri = $impl.field(JMod.PRIVATE, codeModel.ref(URI.class), "_uri");
        
        // generate constructor with parameters 
        // for the client and each WADL defined path parameter
        JMethod $ctor = $impl.constructor(JMod.PUBLIC);
        JDocComment jdoc = $ctor.javadoc();
        jdoc.append(Wadl2JavaMessages.CREATE_INSTANCE_CLIENT());
        
        // Client reference
        JVar $clientParam = $ctor.param($clientReference.type(), "client");
        JVar $uriParam = $ctor.param(codeModel.ref(java.net.URI.class), "uri");
        
        // Only generate entries for current path segement
        generateParameterForPathSegment(resource.getPathSegment(), $ctor, true, $impl);
        

        // Private constructor for copying
        JMethod $ctorCopy = $impl.constructor(JMod.PRIVATE);
        JVar $clientCopyParam = $ctorCopy.param(clientType(), "client");
        JVar $uriBuilderCopyParam = $ctorCopy.param(uriBuilderClass, "uriBuilder");
        JVar $mapCopyParam = $ctorCopy.param(mapOfStringObject, "map");

        JBlock $ctorCopyBody = $ctorCopy.body();
        $ctorCopyBody.assign($clientReference, $clientCopyParam);


        $ctorCopyBody.assign($uriBuilder, $uriBuilderCopyParam.invoke("clone"));
        $ctorCopyBody.assign($templateMatrixParamValMap, $mapCopyParam);
        
        
        // If this isn't a root node then we need to generate a method
        // on the parent to access just this class
        
        if (resource.getParentResource()!=null) {
    
            boolean outer = !parentClass.fields().containsKey("_client");
            
            // Lower the first character to make it into a method name
            //

            String accessorName = 
                    Character.toLowerCase(className.charAt(0))
                    + className.substring(1);
            
            
            JMethod $accessorMethod = parentClass.method(
                    outer ? JMod.PUBLIC | JMod.STATIC: JMod.PUBLIC, $impl, accessorName);

            javaDoc.generateAccessorDoc(resource, $accessorMethod);

            // If we are at the outermost level then we need to allow users
            // to pass in the client
            JVar $clientAccessorParam = null;
            JVar $baseURIParam = null;
            if (outer) {
                $clientAccessorParam = $accessorMethod.param(clientType(), "client");
                $baseURIParam = $accessorMethod.param(URI.class, "baseURI");
            }
            
            generateParameterForPathSegment(resource.getPathSegment(), $accessorMethod, false, $impl);
            
            JBlock $accessorBody = $accessorMethod.body();
            
            JInvocation invoke = JExpr._new($impl);
            $accessorBody._return(invoke);

            // Pass in client parameeter
            //
            if (!outer) {
                invoke.arg($clientReference); 
                invoke.arg($uriBuilder.invoke("buildFromMap").arg($templateMatrixParamValMap));
            } 
            else {
                invoke.arg($clientAccessorParam);
            }
                    
            // Copy accross value from this method
            //
            
            for (JVar var : $accessorMethod.listParams()) {
                if (outer && "client".equals(var.name())) {
                    continue;
                }
                    
                
                invoke.arg(var);
            }
            
            if (outer) {

                // Generate the version without client or baseURI parameter
                {
                    JMethod $accessorMethodNoClient = parentClass.method(
                            JMod.PUBLIC | JMod.STATIC, $impl, accessorName);

                    javaDoc.generateAccessorDoc(resource, $accessorMethodNoClient);

                    JVar[] originalParams = $accessorMethod.listParams();
                    // Miss of first client parameter
                    for (int counter=2; counter < originalParams.length; counter++){
                        $accessorMethodNoClient.param(
                                originalParams[counter].type(), 
                                originalParams[counter].name());
                    }            

                    JBlock $noClientBody = $accessorMethodNoClient.body();
                    JInvocation $invokeOther = JExpr.invoke($accessorMethod);
                    $noClientBody._return($invokeOther);

                    // Create a client and invoke
                    $invokeOther.arg(
                            clientFactoryType().staticInvoke(clientFactoryMethod()));
                    $invokeOther.arg(
                            $global_base_uri);

                    // Invoke other parameter in order
                    for (JVar next : $accessorMethodNoClient.listParams()) {
                        $invokeOther.arg(next);
                    }
                }
                
                // Generate the version with just the client parameters no
                // baseURI
                {
                    JMethod $accessorMethodNoClient = parentClass.method(
                            JMod.PUBLIC | JMod.STATIC, $impl, accessorName);

                    javaDoc.generateAccessorDoc(resource, $accessorMethodNoClient);

                    JVar[] originalParams = $accessorMethod.listParams();
                    // Miss of first client parameter
                    for (int counter=0; counter < originalParams.length; counter++){
                        if (counter==1)
                            continue; // Skip URI parameters
                        
                        $accessorMethodNoClient.param(
                                originalParams[counter].type(), 
                                originalParams[counter].name());
                    }            

                    JBlock $noClientBody = $accessorMethodNoClient.body();
                    JInvocation $invokeOther = JExpr.invoke($accessorMethod);
                    $noClientBody._return($invokeOther);

                    // Create a client and invoke

                    JVar[] listParams = $accessorMethodNoClient.listParams();

                    // Invoke other parameter in order
                    for (int param = 0; param < listParams.length; param++)
                    {
                        JVar next = listParams[param];
                        $invokeOther.arg(next);
                        // Inset the baseURI param in the sequence
                        if (param==0)
                        {
                            $invokeOther.arg(
                                    $global_base_uri);
                        }
                    }
                }
                
            }
        }
        
        
        // Create a body for the primary constructor
        
        JBlock $ctorBody = $ctor.body();

        
        // code gen _client = client
        //
        $ctorBody.assign($clientReference, $clientParam); 

        
        // Store the uri base value
        $ctorBody.assign($uri, $uriParam);
        
        // Only need to process the current path segment

        $ctorBody.assign($uriBuilder,
                uriBuilderClass.staticInvoke("fromUri").arg($uriParam));
        PathSegment segment = resource.getPathSegment();
        $ctorBody.assign($uriBuilder, 
                $uriBuilder.invoke("path").arg(JExpr.lit(segment.getTemplate())));
        
        
        // codegen: templateAndMatrixParameterValues = new HashMap<String, Object>();
        $ctorBody.assign($templateMatrixParamValMap, JExpr._new(hashMapOfStringObject));
        //for (PathSegment segment: resource.getPathSegments()) {
        {
            for (Param p: segment.getTemplateParameters()) {
                // codegen: templateAndMatrixParameterValues.put(name, value);
                $ctorBody.invoke($templateMatrixParamValMap, "put").arg(JExpr.lit(p.getName())).arg(JExpr.ref(GeneratorUtil.makeParamName(p.getName())));
            }
            for (Param p: segment.getMatrixParameters()) {
                if (p.isRequired()  == Boolean.TRUE) {
                    // codegen: templateAndMatrixParameterValues.put(name, value);
                    $ctorBody.invoke($templateMatrixParamValMap, "put").arg(JExpr.lit(p.getName())).arg(JExpr.ref(GeneratorUtil.makeParamName(p.getName())));
                }
            }
        }

        $class = $impl;
        return $class;
    }

    /**
     * For a given path segment generate the correct parameters.
     *
     * @param segment the segment to process.
     * @param method the method we are working on.
     * @param generateBeanDefinitions when {@link true}, bean definitions will be generated.
     * @param contextClass the class we are generating in the context of.
     */
    private void generateParameterForPathSegment(
            PathSegment segment, JMethod method, 
            boolean generateBeanDefinitions, JDefinedClass contextClass) {
        List<Param> matrixParameters = segment.getMatrixParameters();

        for (Param p: segment.getTemplateParameters()) {
            method.param(GeneratorUtil.getJavaType(p, codeModel, contextClass, javaDoc),
                    GeneratorUtil.makeParamName(p.getName()));
            javaDoc.generateParamDoc(p, method);
            if (generateBeanDefinitions) {
                generateBeanProperty(contextClass, matrixParameters, p, false);
            }
        }
        for (Param p: matrixParameters) {
            if (p.isRequired() == Boolean.TRUE) {
                method.param(GeneratorUtil.getJavaType(p, codeModel, contextClass, javaDoc),
                        GeneratorUtil.makeParamName(p.getName()));
                javaDoc.generateParamDoc(p, method);
            }
            if (generateBeanDefinitions) {
                generateBeanProperty(contextClass, matrixParameters,p, false);
            }
        }
    }
    
    
    
    
    /**
     * Create an exception class that wraps an element used for indicating a fault
     * condition.
     *
     * @param f the WADL <code>fault</code> element for which to generate the exception class.
     * @return the generated exception class.
     */
    protected JDefinedClass generateExceptionClass(FaultNode f) {
        JDefinedClass $exCls;
        String exName = f.getClassName();
        try {
            $exCls = generateExceptionClassInternal(exName, f);
        } catch (JClassAlreadyExistsException ex) {
            $exCls = ex.getExistingClass();
        }
        return $exCls;
    }
    
    /**
     * Try to create a new exception class that is relevant for the platform
     * @throws JClassAlreadyExistsException should it already exists
     */
    protected abstract JDefinedClass generateExceptionClassInternal(String exName, FaultNode f) throws JClassAlreadyExistsException;
    
    /**
     * Invoked when we need to throw a generic failure exception because
     * we don't have an element mapped.
     */
    protected abstract void generateThrowWebApplicationExceptionFromResponse(JBlock caseBody, JVar $response);

    
    /**
     * Generates the switch block based on status code that will
     * throw exceptions for a specified failure
     */
    protected void generateConditionalForFaultNode(MethodNode method, JBlock $methodBody, JVar $response) {
        // Right do we have any fault objects to deal with here
        //
        
        Set<List<Long>> statusCodes = method.getFaults().keySet();
        if (statusCodes.size() > 0)
        {
            // So we need to generate a switch statement of some kind
            //
            
            JSwitch sw = $methodBody._switch(
                    $response.invoke("getStatus"));
            
            for (List<Long> statusCode : statusCodes) {

                JCase last = null;
                
                // Create a case statement for each code
                //
                for(Iterator<Long> it = statusCode.iterator(); it.hasNext();)
                {
                    int code = it.next().intValue();
                    last = sw._case(JExpr.lit(code));
                }
                
                if (last!=null)
                {
                    JBlock caseBody = last.body();
                    
                    // For the moment just proces the first fault
                    //
                    
                    FaultNode fn = method.getFaults().getFirst(statusCode);
                    if (fn.getElement()!=null) {
                        
                        
                        JType rawType = (JClass)getTypeFromElement(fn.getElement());
                        JClass exception = generateExceptionClass(fn);

                        caseBody._throw(
                        JExpr._new(exception)
                                .arg($response)
                                .arg($response.invoke(responseGetEntityMethod())
                                        .arg(toClassLiteral(rawType)))
                                );
                    } else {
                        
                        generateThrowWebApplicationExceptionFromResponse(
                           caseBody, $response);
                        
                        
                        caseBody._break();
                    }
                }
            }                
            
        }
    }
    
        
    /**
     * Generate a set of method declarations for a WADL <code>method</code> element.
     * 
     * <p>Generates two Java methods per returned representation type for each request
     * type, one with all optional parameters and one without. I.e. if the WADL method
     * specifies two possible request representation formats and three supported
     * response representation formats, this method will generate twelve Java methods,
     * one for each combination.</p>
     * 
     * @param isAbstract controls whether the generated methods will have a body {@code false}
     * or not {@code true}.
     * @param method the WADL <code>method</code> element to process.
     */
    @Override
    public void generateMethodDecls(MethodNode method, boolean isAbstract) {

        List<RepresentationNode> supportedInputs = method.getSupportedInputs();
        List<RepresentationNode> supportedOutputs = method.getSupportedOutputs();
        Map<JType, JDefinedClass> exceptionMap = new HashMap<JType, JDefinedClass>();
        for (List<FaultNode> fl: method.getFaults().values()) {
            for (FaultNode f : fl){
                if (f.getElement()==null) {// skip fault for which there's no XML
                    messageListener.info(Wadl2JavaMessages.FAULT_NO_ELEMENT());
                    continue;
                }
                JDefinedClass generatedException = generateExceptionClass(f);
                JType rawType = getTypeFromElement(f.getElement());
                JType faultType = rawType==null ? codeModel._ref(Object.class) : rawType;
                exceptionMap.put(faultType, generatedException);
            }
        }
        if (supportedInputs.isEmpty()) {
            // no input representations, just query parameters
            // for each output representation
            if (supportedOutputs.isEmpty()) {
                generateMethodVariants(exceptionMap, method, false, null, null, isAbstract);
                if (method.hasOptionalParameters())
                    generateMethodVariants(exceptionMap, method, true, null, null, isAbstract);
                
            } else {
                for (RepresentationNode returnType: supportedOutputs) {
                    generateMethodVariants(exceptionMap, method, false, null, returnType, isAbstract);
                    if (method.hasOptionalParameters())
                        generateMethodVariants(exceptionMap, method, true, null, returnType, isAbstract);
                }
            }
        } else {
            // for each possible input representation
            for (RepresentationNode inputType: supportedInputs) {
                // for each combination of input and output representation
                if (supportedOutputs.size() == 0) {
                    generateMethodVariants(exceptionMap, method, false, inputType, null, isAbstract);
                    if (method.hasOptionalParameters())
                        generateMethodVariants(exceptionMap, method, true, inputType, null, isAbstract);
                    
                } else {
                    for (RepresentationNode returnType: supportedOutputs) {
                        generateMethodVariants(exceptionMap, method, false, inputType, returnType, isAbstract);
                        if (method.hasOptionalParameters())
                            generateMethodVariants(exceptionMap, method, true, inputType, returnType, isAbstract);
                    }
                }
            }
        }
    }
    
    /**
     * Get the Java type generated for the specified XML element name.
     * 
     * <p>Note that the specified element must be declared as a top-level element in a
     * schema imported by the WADL file otherwise no such Java type will have been
     * generated and this method will return {@link Object}.</p>
     * @param element the name of the XML element.
     * @return the Java type that was generated for the specified element or {@code null}
     * if no matching generated type was found.
     */
    protected JType getTypeFromElement(QName element) {
        JType type = resolver.resolve(element);
        if (type==null)
            messageListener.info(Wadl2JavaMessages.ELEMENT_NOT_FOUND(element.toString()));
        return type;
    }
    
    /**
     * Generate one or two Java methods for a specified combination of WADL 
     * <code>method</code>,
     * input <code>representation</code> and output <code>representation</code>
     * elements. Always generates one method that works with DataSources and
     * generates an additional method that uses JAXB when XML representations are used
     * and the document element is specified.
     * 
     * @param isAbstract controls whether the generated methods will have a body {@code false}
     * or not {@code true}.
     * @param exceptionMap maps generated types to the corresponding exception class. Used to generate the
     * throws clause for the method and the code to map output types to exceptions when
     * the output type is designated as a fault.
     * @param method the WADL <code>method</code> element for the Java method being generated.
     * @param includeOptionalParams whether to include optional parameters in the method signature or not.
     * @param inputRep the WADL <code>representation</code> element for the request format.
     * @param outputRep the WADL <code>representation</code> element for the response format.
     */
    protected void generateMethodVariants(Map<JType, JDefinedClass> exceptionMap,
            MethodNode method, boolean includeOptionalParams, RepresentationNode inputRep,
            RepresentationNode outputRep, boolean isAbstract) {
        generateMethodDecl(exceptionMap, method, includeOptionalParams, inputRep, outputRep, MethodType.JAXB, isAbstract);
        generateMethodDecl(exceptionMap, method, includeOptionalParams, inputRep, outputRep, MethodType.GENERIC_TYPE, isAbstract);
        generateMethodDecl(exceptionMap, method, includeOptionalParams, inputRep, outputRep, MethodType.CLASS, isAbstract);
    }
    
    /**
     * Generate a name for the method.
     *
     * @param method the WADL <code>method</code> element for the Java method being generated.
     * @param inputRep the WADL <code>representation</code> element for the request format.
     * @param outputRep the WADL <code>representation</code> element for the response format.
     * @param returnType a reference to the Java return type.
     * @return a suitable method name.
     */
    protected String getMethodName(MethodNode method, RepresentationNode inputRep, RepresentationNode outputRep,
            JType returnType) {
        StringBuilder buf = new StringBuilder();
        buf.append(method.getName().toLowerCase());
        if (inputRep != null) {
            if (inputRep.getId() != null) {
                buf.append(inputRep.getId().substring(0,1).toUpperCase());
                buf.append(inputRep.getId().substring(1).toLowerCase());
            } else {
                buf.append(inputRep.getMediaTypeAsClassName());
            }
        }
        if (returnType != null) {
            
            if (returnType == clientResponseClientType()) {
                // Don't both appending anything
            }
            // If we have mutliple supported content types, then we need to
            // differential by content type
            else if (method.getSupportedOutputs().size() > 1 && outputRep!=null) {
                buf.append(returnType.name());
                buf.append("As");
                buf.append(outputRep.getMediaTypeAsClassName());
            }
            else {
                buf.append("As");
                buf.append(returnType.name());
            }
            
            
        } else if (outputRep != null) {
            buf.append("As");
            buf.append(outputRep.getMediaTypeAsClassName());
        }
// Duplicate code, ends up adding inputRep twice
//        } else if (inputRep != null) {
//            buf.append(inputRep.getMediaTypeAsClassName());
//        }
        return buf.toString();
    }
    
    /**
     * Generate a Java method for a specified combination of WADL <code>method</code>,
     * input <code>representation</code> and output <code>representation</code>
     * elements.
     *
     * @param methodType {@link MethodType}
     * @param isAbstract controls whether the generated methods will have a body {@code false}
     * or not {@code true}.
     * @param exceptionMap maps generated types to the corresponding exception class. Used to generate the
     * throws clause for the method and the code to map output types to exceptions when
     * the output type is designated as a fault.
     * @param method the WADL <code>method</code> element for the Java method being generated.
     * @param includeOptionalParams whether to include optional parameters in the method signature or not.
     * @param inputRep the WADL <code>representation</code> element for the request format.
     * @param outputRep the WADL <code>representation</code> element for the response format.
     */
    protected void generateMethodDecl(Map<JType, JDefinedClass> exceptionMap,
            MethodNode method, boolean includeOptionalParams, RepresentationNode inputRep,
            RepresentationNode outputRep, 
            MethodType methodType,
            boolean isAbstract) {
        
        boolean isJAXB = methodType == MethodType.JAXB;
        // check if JAXB can be used with available information
        if (isJAXB) {
            if ((outputRep != null && outputRep.getElement() == null) || (inputRep != null && inputRep.getElement() == null))
                return;
        }

        // work out the method return type and the type of any input representation
        JType inputType=null;
        JType returnType;
        boolean wrapInputTypeInJAXBElement = false;
        boolean genericReturnType = false;
        if (isJAXB) {
            if (inputRep != null) {
                inputType = getTypeFromElement(inputRep.getElement());
                
                if (inputType instanceof JDefinedClass) {

                    boolean isRootElement = false;

                    // The version of code model that comes with JAX-B
                    // doesn't include accessor methods, to get around this
                    // I am using reflection otherwise the classloading situation
                    // becomes difficult
                    try
                    {
                        Field annotationClassField = JAnnotationUse.class.getDeclaredField("clazz");
                        annotationClassField.setAccessible(true);
                        Field annotationField = JDefinedClass.class.getDeclaredField("annotations");
                        annotationField.setAccessible(true);
                        List<JAnnotationUse> annotations = (List<JAnnotationUse>) annotationField.get(inputType);
                        found : for (JAnnotationUse use : annotations)
                        {
                            JClass annotationClass = (JClass)annotationClassField.get(use);
                            if (annotationClass.fullName().equals(XmlRootElement.class.getName()))
                            {
                                isRootElement = true;
                                break found;
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        // Ignore for the moment
                        messageListener.warning("Internal error", ex);
                    }
                    
                    wrapInputTypeInJAXBElement = !isRootElement;
                }
                
                if (inputType == null)
                    return;
            }
            if (outputRep != null) {
                returnType = getTypeFromElement(outputRep.getElement());
                if (returnType == null)
                    return;
            }
            else {
                // Default to just response
                returnType = clientResponseClientType();
            }
        }
        else {
            
            // We need to map to a generic return type, but we have to
            // create the method first
            //
            
            if (inputRep != null) {
                inputType = codeModel.ref(Object.class);
            }
            if (outputRep != null) {
                genericReturnType = true;
                returnType =codeModel.ref(Object.class);
            }
            else {
                genericReturnType = true;
                returnType =codeModel.ref(Object.class);
                // Don't allow void return type otherwise the user
                // cannot get back status messages, perhaps future enhancement
                // should just reutrn a void version; but doesn't seem like
                // a common use case
                // returnType = codeModel.VOID;
            }
        }
        
        // generate a name for the method 
        String methodName = getMethodName(method, inputRep, outputRep, isJAXB ? returnType : null);
        
        // create the method
        JMethod $genMethod = $class.method(JMod.PUBLIC, returnType, methodName);
        javaDoc.generateMethodDoc(method, $genMethod);
        if (outputRep != null)
            javaDoc.generateReturnDoc(outputRep, $genMethod);

        
        // add throws for any required exceptions, Client throws far fewer exception
        for (JDefinedClass $ex: exceptionMap.values()) {
            $genMethod._throws($ex);
        }
        
        // add a parameter for the input representation (if required)
        if (inputType != null) {
            $genMethod.param(inputType, "input");
            javaDoc.generateParamDoc(inputRep, $genMethod);
        }
        
        // add a parameter for each query parameter
        List<Param> params = method.getRequiredParameters();
        if (includeOptionalParams)
            params.addAll(method.getOptionalParameters());
        for (Param q: params) {
            // skip fixed value parameters in the method arguments
            if (q.getFixed() != null)
                continue;
            // Skip matrix parameters as they are defined at the resource level
            if (q.getStyle() == ParamStyle.MATRIX)
                continue;
            
            JClass javaType = GeneratorUtil.getJavaType(q, codeModel, $class, javaDoc);
            String paramName = q.getName().equals("input") ? "queryInput" : q.getName();
            q.setName(paramName);
            javaDoc.generateParamDoc(q, $genMethod);
            if (q.isRepeating() == Boolean.TRUE)
                $genMethod.param(codeModel.ref(List.class).narrow(javaType), GeneratorUtil.makeParamName(q.getName()));
            else
                $genMethod.param(javaType, GeneratorUtil.makeParamName(q.getName()));
        }
        
        // Update the return type with a generic parameter if required
        // then add another method parameter
        //
        
        JVar $genericMethodParameter = null;
        if (genericReturnType) {
            JTypeVar $genericParameter = $genMethod.generify("T");
            $genMethod.type($genericParameter);
            JClass baseParameter = methodType == MethodType.CLASS ? codeModel.ref(Class.class)
                        : genericTypeType();
            JClass specificParameter = baseParameter.narrow($genericParameter);
            $genericMethodParameter = $genMethod.param(
                    specificParameter, 
                    "returnType");
        }

        // We need to just process the matrix parameters on the immediate
        // resource segment
        List<Param> matrixOnOwningResource = Collections.EMPTY_LIST;
        if (method.getOwningResource()!=null)
        {
            matrixOnOwningResource = method.getOwningResource().getPathSegment().getMatrixParameters();
        }
        
        // Top part of the method body
        
        if (!isAbstract) {
            // add the method body
            JBlock $methodBody = $genMethod.body();
            JClass mapOfString = codeModel.ref(HashMap.class).narrow(String.class, Object.class);
                       
            // codegen : UriBuilder localUriBuilder = $uriBuilder.clone();
            JVar $localUriBuilder = $methodBody.decl(
                    $uriBuilder.type(),
                    "localUriBuilder", 
                    $uriBuilder.invoke("clone"));

            // Process query parmaeters
            // codegen : localUriBuilder = localUriBuilder.queryParam(...);
            for (Param q: params) {
                if (!includeOptionalParams && q.isRequired() == Boolean.FALSE && q.getFixed()==null)
                    continue;

                // Only process matrix parameters on current path segment
                //
                boolean matrix = q.getStyle() == ParamStyle.MATRIX;
                if (matrix && !matrixOnOwningResource.contains(q)) {
                    continue;
                }
                
                if (q.getStyle() == ParamStyle.QUERY
                    || matrix)
                {
                    JExpression toSet;
                    if (q.getFixed()!=null)
                        toSet = JExpr.lit(q.getFixed());
                    else {
                        String paramName = GeneratorUtil.makeParamName(q.getName());
                        
                        // Now if there is a parameter that matched
                        if (matrix) {
                            // We need to read a value from the _templateMatrixParamValMap 
                            // structure as they are part of the path and not directly referenced
                            // we think that matrix parameters at the request/method
                            // level are invalid in the current WADL specification
                            
                            toSet =  $templateMatrixParamValMap.invoke("get").arg(JExpr.lit(q.getName()));
                        }
                        // Assuming query parameters that are passed in directly
                        else {
                        
                            JFieldRef $paramArg = JExpr.ref(paramName);
                            // check that required variables aren't null
                            toSet = $paramArg;
                        }
                        
                        
                        //
                        if (q.isRequired() == Boolean.TRUE) {
                            JBlock $throwBlock = $methodBody._if(toSet.eq(JExpr._null()))._then();
                            $throwBlock._throw(JExpr._new(codeModel.ref(
                                    IllegalArgumentException.class)).arg(
                                    JExpr.lit(Wadl2JavaMessages.PARAMETER_REQUIRED(q.getName(), methodName))));
                        }
                        
                    }

                    // codegen replace parameters
                    $methodBody.
                        assign($localUriBuilder, 
                                $localUriBuilder.invoke(
                                matrix ? "replaceMatrixParam" : "replaceQueryParam")
                                .arg(q.getName())
                                .arg(q.isRepeating() ? JExpr.cast(codeModel.ref(Object[].class), toSet.invoke("toArray")) :  toSet));
                }
            }
            
            // Build the resource from the client, TODO extract path parameters
            //
            
            // codegen WebResource resource = uriBuilder.buildFromMap(_templateMatrixParameterValue);
            JVar $resource = $methodBody.decl(
                    resourceType(),
                    "resource", 
                    $clientReference.invoke(resourceFromClientMethod()).arg(
                    $localUriBuilder.invoke("buildFromMap").
                       arg($templateMatrixParamValMap)));
            
            // Create the resouce builder entry
            //
            JVar $resourceBuilder = createRequestBuilderAndAccept($methodBody, $resource, outputRep);
            
            //
            
            for (Param q: params) {
                if (!includeOptionalParams && q.isRequired() == Boolean.FALSE && q.getFixed()==null)
                    continue;
                if (q.getStyle() == ParamStyle.HEADER)
                {

                    JFieldRef $paramArg = JExpr.ref(GeneratorUtil.makeParamName(q.getName()));
                    // check that required variables aren't null
                    if (q.isRequired() == Boolean.TRUE && q.getFixed()==null) {
                        JBlock $throwBlock = $methodBody._if($paramArg.eq(JExpr._null()))._then();
                        $throwBlock._throw(JExpr._new(codeModel.ref(
                                IllegalArgumentException.class)).arg(
                                JExpr.lit(Wadl2JavaMessages.PARAMETER_REQUIRED(q.getName(), methodName))));
                    }

                    JExpression toSet;
                    if (q.getFixed()!=null)
                        toSet = JExpr.lit(q.getFixed());
                    else
                        toSet = $paramArg;
                    
                    // resourceBuilder = resourceBuilder.header(...);
                    // if not null
                    $methodBody._if(toSet.ne(JExpr._null()))._then().                
                        assign($resourceBuilder,
                            $resourceBuilder.invoke("header")
                            .arg(q.getName())
                            .arg(q.isRepeating() ? toSet.invoke("toArray") :  toSet));
                    
                }
            }
            
            
            // Now deal with the method body
            
            generateBody(method,isJAXB, exceptionMap, outputRep, 
                    $genericMethodParameter, wrapInputTypeInJAXBElement, inputType, returnType, $resourceBuilder, inputRep, $methodBody);
        }
    }

    
    
    private JExpression toClassLiteral(JType type)
    {
        if (type instanceof JClass) {
            return JExpr.dotclass((JClass)type);
        }
        else if (type instanceof JPrimitiveType) {
            return JExpr.dotclass(
                        ((JPrimitiveType)type).boxify());
        }
        else {
            // I guess panic a little bit at this point
            return JExpr.dotclass(codeModel.ref(Object.class));
        }
    }
    
    
    /**
     * Generate a method body that uses a JAXBDispatcher, used when the payloads are XML.
     *
     * @param method the method to generate a body for.
     * @param isJAXB, whether we are generating a generic of JAXB version.
     * @param exceptionMap the generated exceptions that the method can raise.
     * @param outputRep the output representation.
     * @param $genericMethodParameter TODO.
     * @param wrapInputTypeInJAXBElement If the JAX-B element is not @XmlRootElement we have to do more.
     * @param returnType the type of the method return.
     * @param $resourceBuilder TODO.
     * @param inputRep the input representation.
     * @param $methodBody a reference to the method body in which to generate code.
     */
    protected void generateBody(final MethodNode method, 
            final boolean isJAXB,
            final Map<JType, 
            JDefinedClass> exceptionMap, final RepresentationNode outputRep, 
            final JVar $genericMethodParameter,
            final boolean wrapInputTypeInJAXBElement,
            final JType inputType, 
            final JType returnType, 
            final JVar $resourceBuilder, 
            final RepresentationNode inputRep, final JBlock $methodBody)
    {
        // This code is quite different for jersey and jax-rs 2.0
        // so we are going to collect these values as required and
        // then let each generator sort it out
        //
        
        String methodString = method.getName();
        JExpression $returnTypeExpr = null;
        JExpression $entityExpr = null;
        
        // Content type
        //
        
        
//        JInvocation $execute = $resourceBuilder.invoke(buildMethod());
//        $execute.arg(method.getName());
        
        
        // Return type if required
        
        if ($genericMethodParameter!=null) {
            $returnTypeExpr = JExpr.ref("returnType");
//            $execute.arg(JExpr.ref("returnType"));
        }
        else if (returnType!=null) {
            $returnTypeExpr = toClassLiteral(returnType);
//            $execute.arg(toClassLiteral(returnType));
        }

        // Assume we can't be that picky about HTTP methods as there could
        // be content types
        
        if (true) {
//        if (method.getName().equals("POST") || method.getName().equals("PUT")) {
            if (inputRep == null) {
                // Do nothing
                //
            } else {
                
                
                if (wrapInputTypeInJAXBElement) {
                    // So this is not a XmlRootElement but we can wrap it with
                    // the correct JAXBElment to make the code just function
                    // new JAXBElement(new QName(inputType..),returnType.class, input)
                    
                    JExpression jaxbe = JExpr._new(
                        codeModel.ref(JAXBElement.class))
                            .arg(
                                JExpr._new(
                                    codeModel.ref(QName.class))
                                       .arg(inputRep.getElement().getNamespaceURI())
                                       .arg(inputRep.getElement().getLocalPart()))
                            .arg(
                                toClassLiteral(inputType))
                            .arg(JExpr.ref("input"));
                    
                    $entityExpr = jaxbe;
                    
                }
                else {
                    $entityExpr = JExpr.ref("input");
                }
            }
        }
        
        // Allow JAX-RS to tag on
        JExpression $execute = createProcessInvocation(
               method, $methodBody, $resourceBuilder, methodString, inputRep, returnType, $returnTypeExpr, $entityExpr);
        
       
        // Generic variant always need to return the result
        if (outputRep != null || !isJAXB || !returnType.equals(codeModel.VOID))
            $methodBody._return(
                    $execute);
        else {
            // This is fine as we already have executed something
            // need to modify RS-2.0 code to suit
            // $methodBody.add($execute);
        }
    }


    /**
     * Generate a bean setter and getter for a parameter.
     *
     * @param $impl The class or interface to add the bean setter and getter to.
     * @param p the WADL parameter for which to create the setter and getter.
     * @param isAbstract controls whether a method body is created {@code false} or not {@code true}. Set to {@code true}
     * for interface methods, {@code false} for class methods.
     */
    @Override
    public void generateBeanProperty(JDefinedClass $impl, List<Param> matrixParameters, Param p, boolean isAbstract) {
        JClass rawType = GeneratorUtil.getJavaType(p, codeModel, $impl, javaDoc);
        JType propertyType = p.isRepeating() ? codeModel.ref(List.class).narrow(rawType) : rawType;
        
        String paramName = GeneratorUtil.makeParamName(p.getName());
        String propertyName = paramName.substring(0,1).toUpperCase()+paramName.substring(1);
        
        // getter
        JMethod $getter = $impl.method(JMod.PUBLIC, propertyType, "get"+propertyName);
        JDocComment jdoc = $getter.javadoc();
        jdoc.append("Get "+p.getName());
        javaDoc.generateReturnDoc(p, $getter);
        if (!isAbstract) {
            JBlock $getterBody = $getter.body();
            // codegen: return ((Type) templateAndMatrixParameterValues.get("name"));
            $getterBody._return(JExpr.cast(propertyType, $templateMatrixParamValMap.invoke("get").arg(JExpr.lit(p.getName()))));
        }

        
        // setter
        JMethod $setter = $impl.method(JMod.PUBLIC, $impl, "set"+propertyName);
        jdoc = $setter.javadoc();
        jdoc.append("Duplicate state and set "+p.getName());
        $setter.param(propertyType, paramName);
        javaDoc.generateParamDoc(p, $setter);
        if (!isAbstract) {


            JBlock $setterBody = $setter.body();
            
            // Verify parameter is not null
            if (p.isRequired() == Boolean.TRUE) {
                JBlock $throwBlock = $setterBody._if(JExpr.ref(paramName).eq(JExpr._null()))._then();
                $throwBlock._throw(JExpr._new(codeModel.ref(
                        IllegalArgumentException.class)).arg(
                        JExpr.lit(Wadl2JavaMessages.PARAMETER_REQUIRED(p.getName(), $setter.name()))));
            }
            
            
            // Copy the map containing the parameters
            JClass mapOfStringObject = codeModel.ref(Map.class).narrow(String.class, Object.class);
            JClass hashMapOfStringObject = codeModel.ref(HashMap.class).narrow(String.class, Object.class);
            JVar $copyMap = $setterBody.decl(mapOfStringObject, "copyMap");
            $setterBody.assign($copyMap,
                   JExpr._new(hashMapOfStringObject).arg(
                        $templateMatrixParamValMap));

            
            // Make a copy of URI builder so we can process any marix parameters
            // as required
            JVar $localUriBuilder = $setterBody.decl(
                    $uriBuilder.type(),
                    "copyUriBuilder", 
                    $uriBuilder.invoke("clone"));


            // Update the value in the map, if this value is repeating
            // we need to take a copy
            JFieldRef parameter = JExpr.ref(paramName);
            JExpression sourceValue;
            if (p.isRepeating()) {
                // Create an unmodifiable copy
                sourceValue = codeModel.ref(Collections.class).staticInvoke("unmodifiableList")
                        .arg(JExpr._new(
                            codeModel.ref(ArrayList.class).narrow(rawType)).arg(parameter));
            }
            else {
               sourceValue = parameter;
            }
                
            $setterBody.invoke($copyMap, "put").arg(JExpr.lit(p.getName())).arg(sourceValue);
            
            // Copy of matrix parmaters, can this be duplicated in some way
            // with the code use in the invokcation functions
            
            for (Param q: matrixParameters) {

                JExpression toSet;
                if (q.getFixed()!=null)
                    toSet = JExpr.lit(q.getFixed());
                else {
//                    String paramName = GeneratorUtil.makeParamName(q.getName());

                    // We need to read a value from the _templateMatrixParamValMap 
                    // structure as they are part of the path and not directly referenced
                    // we think that matrix parameters at the request/method
                    // level are invalid in the current WADL specification

                    toSet =  $copyMap.invoke("get").arg(JExpr.lit(q.getName()));
                    if (q.isRepeating()) {
                        toSet = JExpr.cast(codeModel.ref(List.class), toSet);
                    }
                 }

                // check to see if the map contains this key, if so then
                // set the value, assume the data is clean at this point
                // otherwise set a null value to make sure we clean all
                // set values
                
                JConditional containsIf = $setterBody._if($copyMap.invoke("containsKey").arg(q.getName()));
                containsIf._then().                
                       assign($localUriBuilder, 
                        $localUriBuilder.invoke(
                            "replaceMatrixParam")
                        .arg(q.getName())
                        .arg(q.isRepeating() ? JExpr.cast(codeModel.ref(Object[].class),toSet.invoke("toArray")) :  toSet));
                containsIf._else().
                       assign($localUriBuilder, 
                        $localUriBuilder.invoke(
                            "replaceMatrixParam")
                        .arg(q.getName())
                        .arg(JExpr.cast(codeModel.ref(Object[].class),JExpr._null())));
            }
            
            
            
            // Allows chained method settings
            // codegen: return new <this>(_client,_uriBuilder,copy);
            $setterBody._return(JExpr._new($impl)
                 .arg($clientReference)
                 .arg($localUriBuilder)
                 .arg($copyMap));
        }
    }
}
