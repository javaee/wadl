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
 * ResourceClassGenerator.java
 *
 * Created on June 1, 2006, 5:23 PM
 * 
 */

package com.sun.research.ws.wadl2java;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.research.ws.wadl.*;
import com.sun.research.ws.wadl.util.DSDispatcher;
import com.sun.research.ws.wadl.util.JAXBDispatcher;
import com.sun.research.ws.wadl2java.ast.FaultNode;
import com.sun.research.ws.wadl2java.ast.MethodNode;
import com.sun.research.ws.wadl2java.ast.PathSegment;
import com.sun.research.ws.wadl2java.ast.RepresentationNode;
import com.sun.research.ws.wadl2java.ast.ResourceNode;
import com.sun.research.ws.wadl2java.ast.ResourceTypeNode;
import com.sun.tools.xjc.api.Mapping;
import com.sun.tools.xjc.api.S2JJAXBModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

/**
 * Generator class for nested static classes used to represent web resources
 * @author mh124079
 */
public class ResourceClassGenerator {
    
    private ResourceNode resource;
    private JPackage pkg;
    private S2JJAXBModel s2jModel;
    private JCodeModel codeModel;
    private JFieldVar $jaxbDispatcher;
    private JFieldVar $dsDispatcher;
    private JDefinedClass $class = null;
    private JavaDocUtil javaDoc;
    
    /**
     * Creates a new instance of ResourceClassGenerator
     * @param javaDoc a JavaDocUtil instance for use when generating documentation
     * @param s2jModel the schema2java model to use for element to class mapping lookups
     * @param codeModel code model instance to use when generating code
     * @param pkg package for new classes
     * @param resource the resource element for which to generate a class
     */
    public ResourceClassGenerator(S2JJAXBModel s2jModel, JCodeModel codeModel, 
            JPackage pkg, JavaDocUtil javaDoc, ResourceNode resource) {
        this.resource = resource;
        this.codeModel = codeModel;
        this.javaDoc = javaDoc;
        this.s2jModel = s2jModel;
        this.pkg = pkg;
    }
    
    /**
     * Creates a new instance of ResourceClassGenerator
     * @param javaDoc a JavaDocUtil instance for use when generating documentation
     * @param s2jModel the schema2java model to use for element to class mapping lookups
     * @param codeModel code model instance to use when generating code
     * @param pkg package for new classes
     * @param resource the resource element for which to generate a class
     */
    public ResourceClassGenerator(S2JJAXBModel s2jModel, JCodeModel codeModel, 
            JPackage pkg, JavaDocUtil javaDoc, JDefinedClass clazz) {
        this.resource = null;
        this.codeModel = codeModel;
        this.javaDoc = javaDoc;
        this.s2jModel = s2jModel;
        this.pkg = pkg;
        this.$class = clazz;
    }
    
    /**
     * Get the class for which methods will be generated
     * @return the class or null if no class has yet been generated.
     */
    public JDefinedClass getGeneratedClass() {
        return $class;
    }
    
    /**
     * Generate a static member class that represents a WADL resource.
     * @param parentClass the parent class for the generated class
     * @return the generated class
     * @throws com.sun.codemodel.JClassAlreadyExistsException if a class with 
     * the same name already exists
     */
    public JDefinedClass generateClass(JDefinedClass parentClass) throws JClassAlreadyExistsException {
        JDefinedClass $impl = parentClass._class(JMod.PUBLIC | JMod.STATIC, resource.getClassName());
        for (ResourceTypeNode t: resource.getResourceTypes()) {
            $impl._implements(t.getGeneratedInterface());
        }
        javaDoc.generateClassDoc(resource, $impl);
        $jaxbDispatcher = $impl.field(JMod.PRIVATE, JAXBDispatcher.class, "jaxbDispatcher");
        $dsDispatcher = $impl.field(JMod.PRIVATE, DSDispatcher.class, "dsDispatcher");
                
        // generate constructor with parameters for each WADL defined path parameter
        JMethod $ctor = $impl.constructor(JMod.PUBLIC);
        JDocComment jdoc = $ctor.javadoc();
        jdoc.append(Wadl2JavaMessages.CREATE_INSTANCE());
        for (PathSegment segment: resource.getPathSegments()) {
            for (Param p: segment.getTemplateParameters()) {
                $ctor.param(GeneratorUtil.getJavaType(p, codeModel, $impl, javaDoc), p.getName());
                javaDoc.generateParamDoc(p, $ctor);
            }
            for (Param p: segment.getMatrixParameters()) {
                $ctor.param(GeneratorUtil.getJavaType(p, codeModel, $impl, javaDoc), p.getName());
                javaDoc.generateParamDoc(p, $ctor);
            }
        }
        $ctor._throws(JAXBException.class);
        JBlock $ctorBody = $ctor.body();
        JInvocation jcInit = codeModel.ref(JAXBContext.class).staticInvoke("newInstance");
        jcInit.arg(JExpr.lit(pkg.name()));
        JVar $jc = $ctorBody.decl(codeModel.ref(JAXBContext.class), "jc", jcInit);
        JClass arrayListOfString = codeModel.ref(ArrayList.class).narrow(String.class);
        JClass listOfString = codeModel.ref(List.class).narrow(String.class);
        JClass arrayListOfListOfString = codeModel.ref(ArrayList.class).narrow(listOfString);
        JClass listOfListOfString = codeModel.ref(List.class).narrow(listOfString);
        JVar $pathSegmentList = $ctorBody.decl(listOfString, "pathSegments", JExpr._new(arrayListOfString ));
        JVar $matrixParamList = $ctorBody.decl(listOfListOfString, "matrixParameters", JExpr._new(arrayListOfListOfString ));
        JVar $matrixParamSet = $ctorBody.decl(listOfString, "matrixParamSet");
        for (PathSegment segment: resource.getPathSegments()) {
            $ctorBody.invoke($pathSegmentList, "add").arg(JExpr.lit(segment.getTemplate()));
            $ctorBody.assign($matrixParamSet, JExpr._new(arrayListOfString));
            for (Param p: segment.getMatrixParameters()) {
                $ctorBody.invoke($matrixParamSet, "add").arg(JExpr.lit(p.getName()));
            }
            $ctorBody.invoke($matrixParamList, "add").arg($matrixParamSet);
        }
        JClass mapOfStringObject = codeModel.ref(HashMap.class).narrow(String.class, Object.class);
        JVar $paramValMap = $ctorBody.decl(mapOfStringObject, "parameterValues", JExpr._new(mapOfStringObject));
        for (PathSegment segment: resource.getPathSegments()) {
            for (Param p: segment.getTemplateParameters()) {
                $ctorBody.invoke($paramValMap, "put").arg(JExpr.lit(p.getName())).arg(JExpr.ref(p.getName()));
            }
            for (Param p: segment.getMatrixParameters()) {
                $ctorBody.invoke($paramValMap, "put").arg(JExpr.lit(p.getName())).arg(JExpr.ref(p.getName()));
            }
        }

        $ctorBody.assign($jaxbDispatcher, JExpr._new(codeModel.ref(JAXBDispatcher.class)).arg($jc).arg($pathSegmentList).arg($matrixParamList).arg($paramValMap));
        $ctorBody.assign($dsDispatcher, JExpr._new(codeModel.ref(DSDispatcher.class)).arg($pathSegmentList).arg($matrixParamList).arg($paramValMap));
        
        // generate second constructor that takes a uri not based on the WADL structure
        $ctor = $impl.constructor(JMod.PUBLIC);
        JVar $uri = $ctor.param(java.net.URI.class, "uri");
        $ctor._throws(JAXBException.class);
        $ctorBody = $ctor.body();
        jcInit = codeModel.ref(JAXBContext.class).staticInvoke("newInstance");
        jcInit.arg(JExpr.lit(pkg.name()));
        $jc = $ctorBody.decl(codeModel.ref(JAXBContext.class), "jc", jcInit);
        $ctorBody.assign($jaxbDispatcher, JExpr._new(codeModel.ref(JAXBDispatcher.class)).arg($jc).arg($uri));
        $ctorBody.assign($dsDispatcher, JExpr._new(codeModel.ref(DSDispatcher.class)).arg($uri));
        $class = $impl;
        return $class;
    }
    
    /**
     * Create an exception class that wraps an element used for indicating a fault
     * condition.
     * @param f the WADL <code>fault</code> element for which to generate the exception class.
     * @return the generated exception class.
     */
    protected JDefinedClass generateExceptionClass(FaultNode f) {
        JDefinedClass $exCls = null;
        String exName = f.getClassName();
        try {
            $exCls = pkg._class( JMod.PUBLIC, exName);
            $exCls._extends(Exception.class);
            Mapping m = s2jModel.get(f.getElement());
            if (m==null)
                System.err.println(Wadl2JavaMessages.ELEMENT_NOT_FOUND(f.getElement().toString()));
            JType detailType = m==null ? codeModel._ref(Object.class) : m.getType().getTypeClass();
            JVar $detailField = $exCls.field(JMod.PRIVATE, detailType, "m_faultInfo");
            JMethod $ctor = $exCls.constructor(JMod.PUBLIC);
            JVar $msg = $ctor.param(String.class, "message");
            JVar $detail = $ctor.param(detailType, "faultInfo");
            JBlock $ctorBody = $ctor.body();
            $ctorBody.directStatement("super(message);");
            $ctorBody.assign($detailField, $detail);
            JMethod $faultInfoGetter = $exCls.method(JMod.PUBLIC, detailType, "getFaultInfo");
            $faultInfoGetter.body()._return($detailField);
        } catch (JClassAlreadyExistsException ex) {
            $exCls = ex.getExistingClass();
        }
        return $exCls;
    }
    
    /**
     * Generate a set of method declarations for a WADL <code>method</code> element.
     * 
     * Generates two Java methods per returned representation type for each request
     * type, one with all optional parameters and one without. I.e. if the WADL method
     * specifies two possible request repreesentation formats and three supported
     * response representation formats, this method will generate twelve Java methods,
     * one for each combination.
     * @param method The WADL <code>method</code> element to process.
     */
    protected void generateMethodDecls(MethodNode method, boolean isAbstract) {

        List<RepresentationNode> supportedInputs = method.getSupportedInputs();
        List<RepresentationNode> supportedOutputs = method.getSupportedOutputs();
        Map<JType, JDefinedClass> exceptionMap = new HashMap<JType, JDefinedClass>();
        for (FaultNode f: method.getFaults()) {
            if (f.getElement()==null) {// skip fault for which there's no XML
                System.err.println(Wadl2JavaMessages.FAULT_NO_ELEMENT());
                continue;
            }
            JDefinedClass generatedException = generateExceptionClass(f);
            Mapping m = s2jModel.get(f.getElement());
            if (m==null)
                System.err.println(Wadl2JavaMessages.ELEMENT_NOT_FOUND(f.getElement().toString()));
            JType faultType = m==null ? codeModel._ref(Object.class) : m.getType().getTypeClass();
            exceptionMap.put(faultType, generatedException);
        }
        if (supportedInputs.size()==0) {
            // no input representations, just query parameters
            // for each output representation
            for (RepresentationNode returnType: supportedOutputs) {
                generateMethodVariants(exceptionMap, method, false, null, returnType, isAbstract);
                if (method.hasOptionalParameters())
                    generateMethodVariants(exceptionMap, method, true, null, returnType, isAbstract);
            }
        } else {
            // for each possible input representation
            for (RepresentationNode inputType: supportedInputs) {
                // for each combination of input and output representation
                for (RepresentationNode returnType: supportedOutputs) {
                    generateMethodVariants(exceptionMap, method, false, inputType, returnType, isAbstract);
                    if (method.hasOptionalParameters())
                        generateMethodVariants(exceptionMap, method, true, inputType, returnType, isAbstract);
                }
            }
        }
    }
    
    /**
     * Get the Java type generated for the specified XML element name.
     * 
     * Note that the specified element must be declared as a top-level element in a
     * schema imported by the WADL file otherwise no such Java type will have been
     * generated and this method will return <code>Object</code>.
     * @param element the name of the XML element.
     * @return the Java type that was generated for the specified element, <code>Object</code>
     * if no matching generated type was found or <code>DataSource</code> if element is null.
     */
    protected JType getTypeFromElement(QName element) {
        JType type = null;
        if (element==null)
            return codeModel._ref(javax.activation.DataSource.class);
        
        Mapping m = s2jModel.get(element);
        if (m==null)
            System.err.println(Wadl2JavaMessages.ELEMENT_NOT_FOUND(element.toString()));
        type = m==null ? codeModel._ref(Object.class) : m.getType().getTypeClass();
        return type;
    }
    
    /**
     * Generate one or two Java methods for a specified combination of WADL 
     * <code>method</code>,
     * input <code>representation</code> and output <code>representation</code>
     * elements. Always generates one method that works with DataSources and
     * generates an additional method that uses JAXB when XML representations are used
     * and the document element is specified.
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
        generateDSMethodDecl(exceptionMap, method, includeOptionalParams, inputRep, outputRep, isAbstract);
        generateJAXBMethodDecl(exceptionMap, method, includeOptionalParams, inputRep, outputRep, isAbstract);
    }
    
    /**
     * Generate a Java method for a specified combination of WADL <code>method</code>,
     * input <code>representation</code> and output <code>representation</code>
     * elements.
     * @param exceptionMap maps generated types to the corresponding exception class. Used to generate the
     * throws clause for the method and the code to map output types to exceptions when
     * the output type is designated as a fault.
     * @param method the WADL <code>method</code> element for the Java method being generated.
     * @param includeOptionalParams whether to include optional parameters in the method signature or not.
     * @param inputRep the WADL <code>representation</code> element for the request format.
     * @param outputRep the WADL <code>representation</code> element for the response format.
     */
    protected void generateJAXBMethodDecl(Map<JType, JDefinedClass> exceptionMap,
            MethodNode method, boolean includeOptionalParams, RepresentationNode inputRep,
            RepresentationNode outputRep, boolean isAbstract) {
        // check if JAXB can be used with available information
        if ((outputRep != null && outputRep.getElement() == null) || (inputRep != null && inputRep.getElement() == null))
            return;

        // work out the method return type and the type of any input representation
        JType inputType=null, returnType=null;
        if (inputRep != null)
            inputType = getTypeFromElement(inputRep.getElement());
        if (outputRep != null)
            returnType = getTypeFromElement(outputRep.getElement());
        else
            returnType = codeModel.VOID;
        
        // generate a name for the method (http method, "As", output representation) 
        String returnTypeSuffix = outputRep.getElement()==null ? 
            outputRep.getMediaTypeAsClassName() : returnType.name();
        String methodName = method.getName().toLowerCase()+"As"+returnTypeSuffix;
        
        // create the method
        JMethod $genMethod = $class.method(JMod.PUBLIC, returnType, methodName);
        javaDoc.generateMethodDoc(method, $genMethod);
        if (outputRep != null)
            javaDoc.generateReturnDoc(outputRep, $genMethod);
        
        // add throws for any required exceptions
        $genMethod._throws(java.net.URISyntaxException.class);
        for (JDefinedClass $ex: exceptionMap.values()) {
            $genMethod._throws($ex);
        }
        
        // add a parameter for the input representation (if required)
        if (inputType != null) {
            $genMethod.param(inputType, "input");
            javaDoc.generateParamDoc(inputRep, $genMethod);
        }
        
        // add a parameter for each query parameter
        List<Param> params = includeOptionalParams ? 
            method.getQueryParameters() : method.getRequiredParameters();
        for (Param q: params) {
            // skip fixed value query parameters in the method arguments
            if (q.getFixed() != null)
                continue;
            JClass javaType = GeneratorUtil.getJavaType(q, codeModel, $class, javaDoc);
            String paramName = q.getName().equals("input") ? "queryInput" : q.getName();
            q.setName(paramName);
            javaDoc.generateParamDoc(q, $genMethod);
            if (q.isRepeating())
                $genMethod.param(codeModel.ref(List.class).narrow(javaType), q.getName());
            else
                $genMethod.param(javaType, q.getName());
        }
        
        if (!isAbstract) {
            // add the method body
            JBlock $methodBody = $genMethod.body();
            JClass mapOfString = codeModel.ref(HashMap.class).narrow(String.class, Object.class);
            JVar $paramMap = $methodBody.decl(mapOfString, "parameterMap", JExpr._new(mapOfString));
            for (Param q: params) {
                if (!includeOptionalParams && !q.isRequired() && q.getFixed()==null)
                    continue;
                JFieldRef $paramArg = JExpr.ref(q.getName());
                // check that required variables aren't null
                if (q.isRequired() && q.getFixed()==null) {
                    JBlock $throwBlock = $methodBody._if($paramArg.eq(JExpr._null()))._then();
                    $throwBlock._throw(JExpr._new(codeModel.ref(
                            IllegalArgumentException.class)).arg(
                            JExpr.lit(Wadl2JavaMessages.PARAMETER_REQUIRED(q.getName(), methodName))));
                }
                JInvocation addParamToMap = $methodBody.invoke($paramMap, "put");
                if (q.getFixed()!=null)
                    addParamToMap.arg(JExpr.lit(q.getName())).arg(JExpr.lit(q.getFixed()));
                else
                    addParamToMap.arg(JExpr.lit(q.getName())).arg($paramArg);
            }
            generateJAXBDBody(method, exceptionMap, outputRep, returnType, $paramMap, inputRep, $methodBody);
        }
    }

    /**
     * Generate a Java method for a specified combination of WADL <code>method</code>,
     * input <code>representation</code> and output <code>representation</code>
     * elements.
     * @param exceptionMap maps generated types to the corresponding exception class. Used to generate the
     * throws clause for the method and the code to map output types to exceptions when
     * the output type is designated as a fault.
     * @param method the WADL <code>method</code> element for the Java method being generated.
     * @param includeOptionalParams whether to include optional parameters in the method signature or not.
     * @param inputRep the WADL <code>representation</code> element for the request format.
     * @param outputRep the WADL <code>representation</code> element for the response format.
     */
    protected void generateDSMethodDecl(Map<JType, JDefinedClass> exceptionMap,
            MethodNode method, boolean includeOptionalParams, RepresentationNode inputRep,
            RepresentationNode outputRep, boolean isAbstract) {
        // work out the method return type and the type of any input representation
        JType inputType=null, returnType=null;
        if (inputRep != null)
            inputType = getTypeFromElement(null);
        if (outputRep != null)
            returnType = getTypeFromElement(null);
        else
            returnType = codeModel.VOID;
        
        // generate a name for the method (http method, "As", output representation) 
        String returnTypeSuffix = outputRep.getMediaTypeAsClassName();
        String methodName = method.getName().toLowerCase()+"As"+returnTypeSuffix;
        
        // create the method
        JMethod $genMethod = $class.method(JMod.PUBLIC, returnType, methodName);
        javaDoc.generateMethodDoc(method, $genMethod);
        if (outputRep != null)
            javaDoc.generateReturnDoc(outputRep, $genMethod);

        // add throws for any required exceptions
        $genMethod._throws(java.net.URISyntaxException.class);
        
        // add a parameter for the input representation (if required)
        if (inputType != null) {
            $genMethod.param(inputType, "input");
            javaDoc.generateParamDoc(inputRep, $genMethod);
        }
        
        // add a parameter for each query parameter
        List<Param> params = includeOptionalParams ? 
            method.getQueryParameters() : method.getRequiredParameters();
        for (Param q: params) {
            // skip fixed value query parameters in the method arguments
            if (q.getFixed() != null)
                continue;
            JClass javaType = GeneratorUtil.getJavaType(q, codeModel, $class, javaDoc);
            String paramName = q.getName().equals("input") ? "queryInput" : q.getName();
            q.setName(paramName);
            javaDoc.generateParamDoc(q, $genMethod);
            if (q.isRepeating())
                $genMethod.param(codeModel.ref(List.class).narrow(javaType), q.getName());
            else
                $genMethod.param(javaType, q.getName());
        }
        
        if (!isAbstract) {
            // add the method body
            JBlock $methodBody = $genMethod.body();
            JClass mapOfString = codeModel.ref(HashMap.class).narrow(String.class, Object.class);
            JVar $paramMap = $methodBody.decl(mapOfString, "parameterMap", JExpr._new(mapOfString));
            for (Param q: params) {
                if (!includeOptionalParams && !q.isRequired() && q.getFixed()==null)
                    continue;
                JFieldRef $paramArg = JExpr.ref(q.getName());
                // check that required variables aren't null
                if (q.isRequired() && q.getFixed()==null) {
                    JBlock $throwBlock = $methodBody._if($paramArg.eq(JExpr._null()))._then();
                    $throwBlock._throw(JExpr._new(codeModel.ref(
                            IllegalArgumentException.class)).arg(
                            JExpr.lit(Wadl2JavaMessages.PARAMETER_REQUIRED(q.getName(), methodName))));
                }
                JInvocation addParamToMap = $methodBody.invoke($paramMap, "put");
                if (q.getFixed()!=null)
                    addParamToMap.arg(JExpr.lit(q.getName())).arg(JExpr.lit(q.getFixed()));
                else
                    addParamToMap.arg(JExpr.lit(q.getName())).arg($paramArg);
            }
            generateDSDBody(method, outputRep, returnType, $paramMap, inputRep, $methodBody);
        }
    }

    /**
     * Generate a method body that uses a JAXBDispatcher, used when the payloads are XML
     * @param method the method to generate a body for
     * @param exceptionMap the generated exceptions that the method can raise
     * @param outputRep the output representation
     * @param returnType the type of the method return
     * @param $paramMap a reference to the parameterMap variable in the generated code
     * @param inputRep the input representation
     * @param $methodBody a reference to the method body in which to generate code
     */
    protected void generateJAXBDBody(final MethodNode method, final Map<JType, 
            JDefinedClass> exceptionMap, final RepresentationNode outputRep, 
            final JType returnType, final JVar $paramMap, 
            final RepresentationNode inputRep, final JBlock $methodBody) {
        JInvocation $executeMethod = $jaxbDispatcher.invoke("do"+method.getName());
        if (method.getName().equals("POST") || method.getName().equals("PUT")) {
            $executeMethod.arg(JExpr.ref("input"));
            $executeMethod.arg(JExpr.lit(inputRep.getMediaType()));
        }
        $executeMethod.arg($paramMap);
        $executeMethod.arg(JExpr.lit(outputRep.getMediaType()));
        JExpression $retVal = $methodBody.decl(codeModel.ref(Object.class), "retVal", $executeMethod);

        // check type of returned object and throw generated exception if
        // it matches a fault declared in the description
        for (JType faultType: exceptionMap.keySet()) {
            JDefinedClass matchingException = exceptionMap.get(faultType);
            JBlock $throwBlock = $methodBody._if(JExpr.invoke(JExpr.dotclass(faultType.boxify()), "isInstance").arg($retVal))._then();
            $throwBlock._throw(JExpr._new(matchingException).arg(JExpr.lit(Wadl2JavaMessages.INVOCATION_FAILED())).arg(JExpr.cast(faultType,$retVal)));
        }
        
        $methodBody._return(JExpr.cast(returnType,$retVal));
    }

    /**
     * Generate a method body that uses a DSDispatcher, used when the payloads are not XML
     * @param method the method to generate a body for
     * @param outputRep the output representation
     * @param returnType the type of the method return
     * @param $paramMap a reference to the parameterMap variable in the generated code
     * @param inputRep the input representation
     * @param $methodBody a reference to the method body in which to generate code
     */
    protected void generateDSDBody(final MethodNode method, 
            final RepresentationNode outputRep, final JType returnType, 
            final JVar $paramMap, final RepresentationNode inputRep, 
            final JBlock $methodBody) {
        JInvocation $executeMethod = $dsDispatcher.invoke("do"+method.getName());
        if (method.getName().equals("POST") || method.getName().equals("PUT")) {
            $executeMethod.arg(JExpr.ref("input"));
            $executeMethod.arg(JExpr.lit(inputRep.getMediaType()));
        }
        $executeMethod.arg($paramMap);
        $executeMethod.arg(JExpr.lit(outputRep.getMediaType()));
        JExpression $retVal = $methodBody.decl(codeModel.ref(javax.activation.DataSource.class), "retVal", $executeMethod);
        
        $methodBody._return($retVal);
    }}
