/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jvnet.ws.wadl2java.jaxrs;

import javax.ws.rs.WebApplicationException;

import org.jvnet.ws.wadl.ast.FaultNode;
import org.jvnet.ws.wadl.ast.MethodNode;
import org.jvnet.ws.wadl.ast.RepresentationNode;
import org.jvnet.ws.wadl.ast.ResourceNode;
import org.jvnet.ws.wadl.util.MessageListener;
import org.jvnet.ws.wadl2java.JavaDocUtil;
import org.jvnet.ws.wadl2java.Resolver;
import org.jvnet.ws.wadl2java.Wadl2JavaMessages;
import org.jvnet.ws.wadl2java.common.BaseResourceClassGenerator;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

/**
 * The specific implementation for JAX-RS 2.0 static client
 * @author gdavison
 */ 
public class JAXRS20ResourceClassGenerator 
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
    public JAXRS20ResourceClassGenerator(
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
    public JAXRS20ResourceClassGenerator(
            MessageListener messageListener,
            Resolver resolver, JCodeModel codeModel, 
            JPackage pkg, String generatedPackages, JavaDocUtil javaDoc, JDefinedClass clazz) {
        super(messageListener, resolver, codeModel, pkg, generatedPackages, javaDoc, clazz);
    }

    
    // Accessors for types
        
    
    @Override
    protected JClass clientType() {
        return codeModel.ref("javax.ws.rs.client.Client");
    }

    @Override 
    protected JClass clientFactoryType() {
        return codeModel.ref("javax.ws.rs.client.ClientFactory");
    }

    @Override
    protected String clientFactoryMethod() {
        return "newClient";
    }

    
    @Override
    protected JClass clientResponseClientType() {
        return codeModel.ref("javax.ws.rs.core.Response");
    }

    @Override
    protected JClass genericTypeType() {
        return codeModel.ref("javax.ws.rs.core.GenericType");
    }

    @Override
    protected JClass resourceType() {
        return codeModel.ref("javax.ws.rs.client.WebTarget"); // javax.ws.rs.client.Invocation$Builder");
    }

    @Override
    protected JClass resourceBuilderType() {
        return codeModel.ref("javax.ws.rs.client.Invocation.Builder");
    }

    @Override
    protected String resourceFromClientMethod() {
        return "target";
    }

    @Override
    protected String responseGetEntityMethod() {
        return "readEntity";
    }

    
    @Override  
    protected JVar createRequestBuilderAndAccept(JBlock $methodBody, JVar $resource, RepresentationNode outputRep) {
        JInvocation invokeRequest = $resource.invoke("request");
        // codegen WebResource.Builder resourceBuilder = resource.getRequestBuilder();
        // Add accept headers
        if (outputRep != null && outputRep.getMediaType() != null) {
            invokeRequest.arg(outputRep.getMediaType());
        }

        JVar $resourceBuilder = $methodBody.decl(
            resourceBuilderType(),
            "resourceBuilder", invokeRequest);
        return $resourceBuilder;
    }

    @Override
    protected String buildMethod() {
        return "build";
    }

    

    @Override
    protected JExpression[] createProcessInvocation(MethodNode method, JBlock $methodBody, JVar $resourceBuilder, String methodString, RepresentationNode inputRep, JType returnType, JExpression $returnTypeExpr, JExpression $entityExpr) {
        JInvocation $execute = $resourceBuilder.invoke(buildMethod());

        
        // So we need to invoke the service and get a response back
        // so we can throw any exceptions
        $execute.arg(methodString);
        
        if ($entityExpr!=null)
        {
            JClass $entity = codeModel.ref("javax.ws.rs.client.Entity");
            $execute.arg(
              $entity.staticInvoke("entity")
                    .arg($entityExpr)
                    .arg(JExpr.lit(inputRep.getMediaType()))); // TODO replace with proper call
        }

        // Get the response object back so we can generate the switch based 
        // on it
        JInvocation $invoke  = $execute.invoke("invoke");
        JVar $response = $methodBody.decl(clientResponseClientType(), "response");
        $methodBody.assign($response, $invoke);

        // For a given response process any fault nodes
        generateConditionalForFaultNode(method, $methodBody, $response, returnType, $returnTypeExpr);
        
        // So now we have to get the real answer back from the response
        //
        
        
        // Right need to get entity from the response
        if (clientResponseClientType() == returnType) {
            // In the case when the reponse should be the client response
            // we can return null because
            
            return new JExpression[] {$response};
        }
        else {
            JInvocation $fetchEntity = $response.invoke("readEntity");
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
        // In RS 2.0 we can pass in the response object as they
        // are consistent across the API
        
        caseBody._throw(
           JExpr._new(codeModel.ref(WebApplicationException.class))
                .arg($response));
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
        // Build a constructor
        JMethod $ctor = $exCls.constructor(JMod.PUBLIC);

        JVar $response = $ctor.param(clientResponseClientType(), "response");
        JVar $detail = $ctor.param(detailType, "faultInfo");
        JBlock $ctorBody = $ctor.body();

        // In RS 2.0 the client API is harmonised so we can reuse this
        //
        $ctorBody.directStatement("super(response);");
        $ctorBody.assign($detailField, $detail);
        // Add getter for the body payload
        JMethod $faultInfoGetter = $exCls.method(JMod.PUBLIC, detailType, "getFaultInfo");
        $faultInfoGetter.body()._return($detailField);

        return $exCls;
    }


    
    
    /**
     * This method should create a static private method called CREATE_CLIENT_METHOD that
     * generate the right factory code for this particular implementation
     * @param parentClass The root class to add the method to
     */
    @Override
    protected void generateClientFactoryMethod(JDefinedClass parentClass) {

        JClass $clientConfig = codeModel.ref("javax.ws.rs.core.Configurable");
        
        // These are the template methods that tooling might override
        
        JMethod $custMethod = parentClass.method(
            JMod.PRIVATE | JMod.STATIC, codeModel.VOID, CUSTOMIZE_CLIENT_METHOD);
        $custMethod.param($clientConfig, "cc");
        $custMethod.javadoc().append(Wadl2JavaMessages.CREATE_CLIENT_CUSTOMIZE());

        JMethod $clientInstance = parentClass.method(
            JMod.PRIVATE | JMod.STATIC, clientType(), CREATE_CLIENT_INSTANCE);
        $clientInstance.javadoc().append(Wadl2JavaMessages.CREATE_CLIENT_INSTANCE());
        
        // This is the public method people will call
        //
        
        JMethod $clientFactory = parentClass.method(
            JMod.PUBLIC | JMod.STATIC, clientType(), CREATE_CLIENT_METHOD);
        $clientFactory.javadoc().append(Wadl2JavaMessages.CREATE_CLIENT());

        
        JBlock body = $clientFactory.body();
        // Create configuration
        
        // Invoke the new instance method
        
        JVar client = body.decl(clientType(), "client", JExpr.invoke($clientInstance));
        
        // Invoke customization method
        
        body.invoke($custMethod).arg(
                client.invoke("configuration"));

        // Return a client
        body._return(client);

        
        // Popuplate the create client instance method
        
        JBlock iBody = $clientInstance.body();
        iBody._return(clientFactoryType().staticInvoke(clientFactoryMethod()));
    }
    
}
