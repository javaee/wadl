/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jvnet.ws.wadl2java.jersey;

import com.sun.codemodel.*;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.jvnet.ws.wadl.ast.FaultNode;
import org.jvnet.ws.wadl.ast.MethodNode;
import org.jvnet.ws.wadl.ast.RepresentationNode;
import org.jvnet.ws.wadl.ast.ResourceNode;
import org.jvnet.ws.wadl.util.MessageListener;
import org.jvnet.ws.wadl2java.Resolver;
import org.jvnet.ws.wadl2java.JavaDocUtil;
import org.jvnet.ws.wadl2java.Wadl2JavaMessages;
import org.jvnet.ws.wadl2java.common.BaseResourceClassGenerator;

/**
 * The specific implementation for Jersey 1.x
 * @author gdavison
 */
public class Jersey1xResourceClassGenerator 
   extends BaseResourceClassGenerator {
    
    

    
    /**
     * Creates a new instance of BaseResourceClassGenerator.
     *
     * @param javaDoc a JavaDocUtil instance for use when generating documentation.
     * @param resolver the schema2java model to use for element to class mapping lookups.
     * @param codeModel code model instance to use when generating code.
     * @param pkg package for new classes.
     * @param resource the resource element for which to generate a class.
     */
    public Jersey1xResourceClassGenerator(
            MessageListener messageListener,
            Resolver resolver, JCodeModel codeModel, 
            JPackage pkg, String generatedPackages, JavaDocUtil javaDoc, ResourceNode resource) {
        super(messageListener, resolver, codeModel, pkg, generatedPackages, javaDoc, resource);
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
    public Jersey1xResourceClassGenerator(
            MessageListener messageListener,
            Resolver resolver, JCodeModel codeModel, 
            JPackage pkg, String generatedPackages, JavaDocUtil javaDoc, JDefinedClass clazz) {
        super(messageListener, resolver, codeModel, pkg, generatedPackages, javaDoc, clazz);
    }

    
    // Accessors for types
        
    
    @Override
    protected JClass clientType() {
        return codeModel.ref("com.sun.jersey.api.client.Client");
    }

    @Override
    protected JClass clientFactoryType() {
        return codeModel.ref("com.sun.jersey.api.client.Client");
    }

    @Override
    protected String clientFactoryMethod() {
        return "create";
    }
    
    
    @Override
    protected JClass clientResponseClientType() {
        return codeModel.ref("com.sun.jersey.api.client.ClientResponse");
    }

    
    
    @Override
    protected JClass genericTypeType() {
        return codeModel.ref("com.sun.jersey.api.client.GenericType");
    }


    @Override
    protected JClass resourceType() {
        return codeModel.ref("com.sun.jersey.api.client.WebResource");
    }

    @Override
    protected JClass resourceBuilderType() {
        return codeModel.ref("com.sun.jersey.api.client.WebResource.Builder");
    }

    @Override
    protected String resourceFromClientMethod() {
        return "resource";
    }
    
    @Override
    protected String responseGetEntityMethod() {
        return "getEntity";
    }
    

    @Override  
    protected JVar createRequestBuilderAndAccept(JBlock $methodBody, JVar $resource, RepresentationNode outputRep) {
        // codegen WebResource.Builder resourceBuilder = resource.getRequestBuilder();
        JVar $resourceBuilder = $methodBody.decl(
                resourceBuilderType(),
                "resourceBuilder", 
                $resource.invoke("getRequestBuilder"));
        // Add accept headers
        if (outputRep != null && outputRep.getMediaType() != null) {
            $methodBody.assign($resourceBuilder,
                    $resourceBuilder.invoke("accept")
                    .arg(JExpr.lit(outputRep.getMediaType())));
        }
        return $resourceBuilder;
    }

    @Override
    protected String buildMethod() {
        return "method";
    }

    @Override
    protected JExpression[] createProcessInvocation(MethodNode method, JBlock $methodBody, JVar $resourceBuilder, String methodString, RepresentationNode inputRep, JType returnType, JExpression $returnTypeExpr, JExpression $entityExpr) {
        
        // Store the type
        //
        
        if (inputRep!=null) {
            $methodBody.assign($resourceBuilder,
                    $resourceBuilder.invoke("type")
                    .arg(JExpr.lit(inputRep.getMediaType())));
        }
        
        //
        
        JInvocation $execute = $resourceBuilder.invoke(buildMethod());
        $execute.arg(methodString);
        $execute.arg(JExpr.dotclass(clientResponseClientType()));
        
        
        if ($entityExpr!=null)
        {
            $execute.arg($entityExpr);
        }
        
        // Assign to variable
        //
        
        JVar $response = $methodBody.decl(clientResponseClientType(), "response");
        $methodBody.assign($response, $execute);
        
        // For a given response process any fault nodes
        generateConditionalForFaultNode(method, $methodBody, $response, returnType, $returnTypeExpr);
        
        // Right need to get entity from the response
        if (clientResponseClientType() == returnType) {
            // In the case when the reponse should be the client response
            // we can return null because
            
            return new JExpression[] {$response};
        }
        else {
            JInvocation $fetchEntity = $response.invoke("getEntity");
            if ($returnTypeExpr!=null)
            {
                $fetchEntity.arg($returnTypeExpr);
            }
        
            return new JExpression[] {$fetchEntity, $response};
        }
        
   }

    
    /**
     * Invoked when we need to throw a generic failure exception because
     * we don't have an element mapped.
     */
    protected void generateThrowWebApplicationExceptionFromResponse(JBlock caseBody, JVar $response) {
        // Just for a WebApplicationException in this case
        // with the right status code, in RS 2.0 we can provide
        // the entire reponse
        
        caseBody._throw(
           JExpr._new(codeModel.ref(WebApplicationException.class))
                .arg(
                  codeModel.ref(Response.class).staticInvoke("status")
                    .arg($response.invoke("getClientResponseStatus")).invoke("build")));
    }


    
    /**
     * Try to create a new exception class that is relevant for the platform
     * @throws JClassAlreadyExistsException should it already exists
     */
    protected JDefinedClass generateExceptionClassInternal(String exName, FaultNode f) throws JClassAlreadyExistsException {
        JDefinedClass $exCls = pkg._class( JMod.PUBLIC, exName);
        $exCls._extends(WebApplicationException.class);
        JType rawType = getTypeFromElement(f.getElement());
        JType detailType = rawType==null ? codeModel._ref(Object.class) : rawType;
        JVar $detailField = $exCls.field(JMod.PRIVATE, detailType, "m_faultInfo");
        JVar $responseField = $exCls.field(JMod.PRIVATE, 
                clientResponseClientType(), "m_response");
        // Build a constructor
        JMethod $ctor = $exCls.constructor(JMod.PUBLIC);
        // Remove message
        JVar $response = $ctor.param(clientResponseClientType(), "response");
        JVar $detail = $ctor.param(detailType, "faultInfo");
        JBlock $ctorBody = $ctor.body();

        //
        $ctorBody.directStatement("super(response.getStatus());");
        $ctorBody.assign($detailField, $detail);
        $ctorBody.assign($responseField, $response);
        // Add getter for the body payload
        JMethod $faultInfoGetter = $exCls.method(JMod.PUBLIC, detailType, "getFaultInfo");
        $faultInfoGetter.body()._return($detailField);
        // Add getter for the client response
        JMethod $responseGetter = $exCls.method(JMod.PUBLIC, clientResponseClientType(), "getClientResponse");
        $responseGetter.body()._return($responseField);
        return $exCls;
    }

    /**
     * This method should create a static private method called CREATE_CLIENT_METHOD that
     * generate the right factory code for this particular implementation
     * @param parentClass The root class to add the method to
     */
    @Override
    protected void generateClientFactoryMethod(JDefinedClass parentClass) {

        JClass $clientConfig = codeModel.ref("com.sun.jersey.api.client.config.ClientConfig");
        
        // These are the template methods that tooling might override
        
        JMethod $custMethod = parentClass.method(
            JMod.PRIVATE | JMod.STATIC, codeModel.VOID, CUSTOMIZE_CLIENT_METHOD);
        $custMethod.param($clientConfig, "cc");
        $custMethod.javadoc().append(Wadl2JavaMessages.CREATE_CLIENT_CUSTOMIZE());

        JMethod $clientInstance = parentClass.method(
            JMod.PRIVATE | JMod.STATIC, clientType(), CREATE_CLIENT_INSTANCE);
        $clientInstance.param($clientConfig, "cc");
        $clientInstance.javadoc().append(Wadl2JavaMessages.CREATE_CLIENT_INSTANCE());
        
        // This is the public method people will call
        //
        
        JMethod $clientFactory = parentClass.method(
            JMod.PUBLIC | JMod.STATIC, clientType(), CREATE_CLIENT_METHOD);
        $clientFactory.javadoc().append(Wadl2JavaMessages.CREATE_CLIENT());

        
        JBlock body = $clientFactory.body();
        // Create configuration
        
        JVar clientConfig = body.decl($clientConfig, 
                "cc",
                JExpr._new(codeModel.ref("com.sun.jersey.api.client.config.DefaultClientConfig")));
        
        // Invoke customization method
        
        body.invoke($custMethod).arg(clientConfig);
        
        // Invoke the new instance method
        
        body._return(JExpr.invoke($clientInstance).arg(clientConfig));
        
        // Popuplate the create client instance method
        
        JBlock iBody = $clientInstance.body();
        iBody._return(clientFactoryType().staticInvoke(clientFactoryMethod()).arg(clientConfig));
    }

    
}

