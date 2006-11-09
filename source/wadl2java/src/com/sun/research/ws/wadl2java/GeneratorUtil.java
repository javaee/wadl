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
 * GeneratorUtil.java
 *
 * Created on June 1, 2006, 5:42 PM
 *
 */

package com.sun.research.ws.wadl2java;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JEnumConstant;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;
import com.sun.research.ws.wadl.Option;
import com.sun.research.ws.wadl.Param;
import com.sun.research.ws.wadl2java.ast.ResourceNode;
import javax.xml.namespace.QName;

/**
 * Utility functions for code generators
 * @author mh124079
 */
public class GeneratorUtil {
    
    /**
     * Make a Java constant name for the supplied WADL parameter. Capitalizes all
     * chars and replaces illegal chars with '_'.
     * @param input the WADL parameter
     * @return a constant name
     */
    public static String makeConstantName(String input) {
        if (input==null || input.length()==0)
            input = "CONSTANT";
        input = input.replaceAll("\\W","_");
        input = input.toUpperCase();
        return input;

    }
    
    /**
     * Maps WADL param types to their respective Java type. For params with
     * child option elements a Java enum is generated, otherwise an existing
     * Java class is used.
     * @param param the WADL parameter
     * @param model the JAXB codeModel instance to use if code generation is required
     * @param parentClass the class in which any generated enums will be placed
     * @param javaDoc a JavaDocUtil instance that will be used for generating
     * JavaDoc comments on any generated enum
     * @return the class of the corresponding Java type
     */
    public static JClass getJavaType(Param param, JCodeModel model, 
            JDefinedClass parentClass, JavaDocUtil javaDoc) {
        if (param.getOption().size() > 0) {
            JDefinedClass $enum;
            try {
                $enum = parentClass._package()._enum(ResourceNode.makeClassName(param.getName()));
                javaDoc.generateEnumDoc(param, $enum);
                for (Option o: param.getOption()) {
                    JEnumConstant c = $enum.enumConstant(makeConstantName(o.getValue()));
                    c.arg(JExpr.lit(o.getValue()));
                    javaDoc.generateEnumConstantDoc(o, c);
                }
                JFieldVar $stringVal = $enum.field(JMod.PRIVATE, String.class, "stringVal");
                JMethod $ctor = $enum.constructor(JMod.PRIVATE);
                JVar $val = $ctor.param(String.class, "v");
                $ctor.body().assign($stringVal, $val);
                JMethod $toString = $enum.method(JMod.PUBLIC, String.class, "toString");
                $toString.body()._return($stringVal);
            } catch (JClassAlreadyExistsException ex) {
                $enum = ex.getExistingClass();
            }
            return $enum;
        } else {
            // map param type to existing Java class
            Class type = String.class;
            QName xmlType = param.getType();
            if (xmlType!=null && xmlType.getNamespaceURI().equals("http://www.w3.org/2001/XMLSchema")) {
                String localPart = xmlType.getLocalPart();
                if (localPart.equals("boolean"))
                    type = Boolean.class;
                else if (localPart.equals("integer"))
                    type = Integer.class;
                else if (localPart.equals("nonPositiveInteger"))
                    type = Integer.class;
                else if (localPart.equals("long"))
                    type = Long.class;
                else if (localPart.equals("nonNegativeInteger"))
                    type = Integer.class;
                else if (localPart.equals("negativeInteger"))
                    type = Integer.class;
                else if (localPart.equals("int"))
                    type = Integer.class;
                else if (localPart.equals("unsignedLong"))
                    type = Long.class;
                else if (localPart.equals("positiveInteger"))
                    type = Integer.class;
                else if (localPart.equals("unsignedInt"))
                    type = Integer.class;
                else if (localPart.equals("unsignedShort"))
                    type = Integer.class;
                else if (localPart.equals("unsignedByte"))
                    type = Byte.class;
                else if (localPart.equals("int"))
                    type = Integer.class;
                else if (localPart.equals("short"))
                    type = Integer.class;
                else if (localPart.equals("byte"))
                    type = Byte.class;
                else if (localPart.equals("float"))
                    type = Float.class;
                else if (localPart.equals("double"))
                    type = Double.class;
                else if (localPart.equals("decimal"))
                    type = Double.class;
                else if (localPart.equals("QName"))
                    type = QName.class;
            }
            return (JClass)model._ref(type);
        }
    }
}
