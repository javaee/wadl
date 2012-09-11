/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jvnet.ws.wadl2java.jersey;

import com.sun.codemodel.*;
import org.jvnet.ws.wadl.ast.RepresentationNode;
import org.jvnet.ws.wadl.ast.ResourceNode;
import org.jvnet.ws.wadl.util.MessageListener;
import org.jvnet.ws.wadl2java.ElementToClassResolver;
import org.jvnet.ws.wadl2java.JavaDocUtil;
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
    public Jersey1xResourceClassGenerator(
            MessageListener messageListener,
            ElementToClassResolver resolver, JCodeModel codeModel, 
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
        return codeModel.ref("com.sun.jersey.api.client.WebResource$Builder");
    }

    @Override
    protected String resourceFromClientMethod() {
        return "resource";
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
    protected JInvocation postProcessInvocation(JInvocation $execute) {
        // do nothing
        return $execute;
    }
    
}
