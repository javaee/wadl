/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jvnet.ws.wadl2java;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JVar;
import java.net.URI;
import java.util.List;
import org.jvnet.ws.wadl.Param;
import org.jvnet.ws.wadl.ast.MethodNode;
import org.jvnet.ws.wadl.ast.ResourceNode;
import org.jvnet.ws.wadl.ast.ResourceTypeNode;

/**
 * A simplified interface to the resource class generators
 * @author gdavison
 */
public interface ResourceClassGenerator {
//
//    /**
//     * Generate a bean setter and getter for a parameter.
//     *
//     * @param $impl The class or interface to add the bean setter and getter to.
//     * @param p the WADL parameter for which to create the setter and getter.
//     * @param isAbstract controls whether a method body is created {@code false} or not {@code true}. Set to {@code true}
//     * for interface methods, {@code false} for class methods.
//     */
//    void generateBeanProperty(JDefinedClass $impl, List<Param> matrixParameters, Param p, boolean isAbstract);

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
    public void generateEndpointClass(
            URI rootResource, ResourceNode root)
            throws JClassAlreadyExistsException ;

    
//    /**
//     * Generate a static member class that represents a WADL resource.
//     *
//     * @param parentClass the parent class for the generated class.
//     * @param $global_base_uri a reference to the field that contains the base URI.
//     * @return the generated class.
//     * @throws com.sun.codemodel.JClassAlreadyExistsException if a class with
//     * the same name already exists.
//     */
//    JDefinedClass generateClass(JDefinedClass parentClass, JVar $global_base_uri) throws JClassAlreadyExistsException;
//
//    /**
//     * Generate a set of method declarations for a WADL <code>method</code> element.
//     *
//     * <p>Generates two Java methods per returned representation type for each request
//     * type, one with all optional parameters and one without. I.e. if the WADL method
//     * specifies two possible request representation formats and three supported
//     * response representation formats, this method will generate twelve Java methods,
//     * one for each combination.</p>
//     *
//     * @param isAbstract controls whether the generated methods will have a body {@code false}
//     * or not {@code true}.
//     * @param method the WADL <code>method</code> element to process.
//     */
//    void generateMethodDecls(MethodNode method, boolean isAbstract);
    
    
    /**
     * Generate Java interfaces for WADL resource types
     * @throws com.sun.codemodel.JClassAlreadyExistsException if the interface to be generated already exists
     */
    public void generateResourceTypeInterface(ResourceTypeNode n)throws JClassAlreadyExistsException;
    
    
}
