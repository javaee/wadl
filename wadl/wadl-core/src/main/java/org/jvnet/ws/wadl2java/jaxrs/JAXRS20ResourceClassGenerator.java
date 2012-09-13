/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jvnet.ws.wadl2java.jaxrs;

import com.sun.codemodel.*;
import org.jvnet.ws.wadl.ast.RepresentationNode;
import org.jvnet.ws.wadl.ast.ResourceNode;
import org.jvnet.ws.wadl.util.MessageListener;
import org.jvnet.ws.wadl2java.ElementToClassResolver;
import org.jvnet.ws.wadl2java.JavaDocUtil;
import org.jvnet.ws.wadl2java.common.BaseResourceClassGenerator;

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
            ElementToClassResolver resolver, JCodeModel codeModel, 
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
            ElementToClassResolver resolver, JCodeModel codeModel, 
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
        return codeModel.ref("javax.ws.rs.client.Invocation$Builder");
    }

    @Override
    protected String resourceFromClientMethod() {
        return "target";
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
    protected JInvocation createProcessInvocation(JBlock $methodBody, JVar $resourceBuilder, String methodString, RepresentationNode inputRep, JExpression $returnTypeExpr, JExpression $entityExpr) {
        JInvocation $build = $resourceBuilder.invoke(buildMethod());

        $build.arg(methodString);
        
        if ($entityExpr!=null)
        {
            JClass $entity = codeModel.ref("javax.ws.rs.client.Entity");
            $build.arg(
              $entity.staticInvoke("entity")
                    .arg($entityExpr)
                    .arg(JExpr.lit(inputRep.getMediaType()))); // TODO replace with proper call
        }

        
        JInvocation $invoke  = $build.invoke("invoke");
        
        if ($returnTypeExpr!=null)
        {
            $invoke.arg($returnTypeExpr);
        }

        return $invoke;
   }
    
    
}
