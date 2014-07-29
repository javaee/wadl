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
package org.jvnet.ws.wadl2java.javascript;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jvnet.ws.wadl.Param;
import org.jvnet.ws.wadl.ParamStyle;
import org.jvnet.ws.wadl.ast.MethodNode;
import org.jvnet.ws.wadl.ast.RepresentationNode;
import org.jvnet.ws.wadl.ast.ResourceNode;
import org.jvnet.ws.wadl.ast.ResourceTypeNode;
import org.jvnet.ws.wadl2java.ResourceClassGenerator;
import org.jvnet.ws.wadl2java.Wadl2Java;
import org.jvnet.ws.wadl2java.common.BaseResourceClassGenerator;

/**
 * Generate JavaScript from WADL
 */
public class JavaScriptGenerator
        implements ResourceClassGenerator {

    protected JCodeModel codeModel;
    private Wadl2Java.Parameters parameters;

    public JavaScriptGenerator(Wadl2Java.Parameters parameters, JCodeModel codeModel) {
        this.parameters = parameters;
        this.codeModel = codeModel;
    }

    @Override
    public void generateEndpointClass(URI rootResource, ResourceNode root) throws JClassAlreadyExistsException {
        try {
            try (Writer writer = parameters.getCodeWriter().openSource(codeModel._package(""), "client.js")) {
                BlockTracker bt = new BlockTracker(new PrintWriter(writer));
                final String name = root.getClassName() + "Client";

                try (Block assignment = bt.var(name)) {
                    try (Block rootScript = bt.block()) {

                        try (Block rootURI = bt.jsonValue("rootUri")) {

                            bt.literal(root.getUriTemplate().toString());
                        }

                        // Create a function that creates the root context
                        // with a return statement in it
                        try (Block resourceNode = bt.jsonValue(root.getClassName())) {

                            try (Block function = bt.function()) {

                                try (Block path = bt.var("path0")) {
                                    bt.reference(name + ".rootUri");
                                }

                                try (Block ret = bt.ret()) {
                                    generateForResource(bt, root, 1);
                                }
                            }
                        }

                    }
                }

                int i = 0;

            }
        } catch (IOException ex) {
            Logger.getLogger(JavaScriptGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }

        int i = 0;
    }

    private void generateForResource(final BlockTracker bt, ResourceNode parent, int depth) {

        try (Block jsonBlock = bt.block()) {
            String pathVar = "path" + (depth - 1);

            // For each resource generate a function to return the next block
            //
            for (ResourceNode child : parent.getChildResources()) {

                try (Block jsonForResource = bt.jsonValue(child.getClassName())) {
                    
                    List<Param> templateParameters = child.getPathSegment().getTemplateParameters();

                    String names[] = new String[templateParameters.size()];
                    for (int i = 0; i < templateParameters.size(); i++) {
                        Param next = templateParameters.get(i);
                        names[i] = next.getName();
                    }
                    

                    try (Block resourceFunction = bt.function(names)) {

                        // TODO build relavent path
                        //
                        try (Block assignment = bt.var("path" + depth)) {

                            bt.reference("path" + (depth - 1));
                            bt.reference("+");
                            
                            String uriTemplate = child.getUriTemplate();
                            String parenturiTemplate = child.getParentResource().getUriTemplate();
                            
                            if (uriTemplate.startsWith("/")) {
                                if (parenturiTemplate.endsWith("/")) {
                                    bt.literal(uriTemplate.substring(1));
                                }
                                else {
                                    bt.literal(uriTemplate);
                                }
                            }
                            else {
                                if (parenturiTemplate.endsWith("/")) {
                                    bt.literal(uriTemplate);
                                }
                                else {
                                    bt.literal("/");
                                    bt.reference("+");
                                    bt.literal(uriTemplate);
                                }
                            }
                            

                        }
                        
                        // Perform path subs
                        //
                        
                        for (final Param param : templateParameters)
                        {
                            try (Block assignment = bt.assignment("path" + depth)){
                                bt.invokefunction("path" + (depth), "replace",
                                        new Parameter() {
                                            @Override
                                            public void build() {

                                                bt.reference("/\\{" + param.getName() + ".*?\\}/");

                                            }
                                        },
                                        new Parameter() {

                                            @Override
                                            public void build() {
                                                bt.reference(param.getName());
                                            }
                                        });
                            }
                            
                        }

                        try (Block ret = bt.ret()) {
                            generateForResource(bt, child, depth + 1);
                        }

                    }
                }
            }

            // Process methods
            for (final MethodNode child : parent.getMethods()) {

                generateMethodDecls(bt, pathVar, child, false);

            }
        }
    }

    /**
     * Generate a set of method declarations for a WADL <code>method</code>
     * element.
     *
     * <p>
     * Generates two Java methods per returned representation type for each
     * request type, one with all optional parameters and one without. I.e. if
     * the WADL method specifies two possible request representation formats and
     * three supported response representation formats, this method will
     * generate twelve Java methods, one for each combination.</p>
     *
     * @param isAbstract controls whether the generated methods will have a body
     * {@code false} or not {@code true}.
     * @param method the WADL <code>method</code> element to process.
     */
    protected void generateMethodDecls(BlockTracker bt, String pathVar, MethodNode method, boolean isAbstract) {

        // TODO refactor this so that the code in BaseResourceClassGenerator is 
        // following the same pattern
        List<RepresentationNode> supportedInputs = method.getSupportedInputs();
        List<RepresentationNode> supportedOutputs = new ArrayList<RepresentationNode>();
        for (List<RepresentationNode> nodeList : method.getSupportedOutputs().values()) {
            for (RepresentationNode node : nodeList) {
                supportedOutputs.add(node);
            }
        }

//        Map<JType, JDefinedClass> exceptionMap = new HashMap<JType, JDefinedClass>();
//        for (List<FaultNode> fl: method.getFaults().values()) {
//            for (FaultNode f : fl){
//                if (f.getElement()==null) {// skip fault for which there's no XML
//                    parameters.getMessageListener().info(Wadl2JavaMessages.FAULT_NO_ELEMENT());
//                    continue;
//                }
//                JDefinedClass generatedException = generateExceptionClass(f);
//                JType rawType = getTypeFromElement(f.getElement());
//                JType faultType = rawType==null ? codeModel._ref(Object.class) : rawType;
//                exceptionMap.put(faultType, generatedException);
//            }
//        }
        if (supportedInputs.isEmpty()) {
            // no input representations, just query parameters
            // for each output representation
            if (supportedOutputs.isEmpty()) {
                generateMethodVariants(bt, pathVar, method, null, null, isAbstract);

            } else {
                for (RepresentationNode returnType : supportedOutputs) {
                    generateMethodVariants(bt, pathVar, method, null, returnType, isAbstract);
                }
            }
        } else {
            // for each possible input representation
            for (RepresentationNode inputType : supportedInputs) {
                // for each combination of input and output representation
                if (supportedOutputs.size() == 0) {
                    generateMethodVariants(bt, pathVar, method, inputType, null, isAbstract);

                } else {

                    // If there is a matcing output just generate that
                    //
                    RepresentationNode matchingReturn = null;
                    findMatching:
                    for (RepresentationNode returnType : supportedOutputs) {
                        if (inputType.getMediaType().equals(returnType.getMediaType())) {
                            matchingReturn = returnType;
                            break findMatching;
                        }
                    }

                    // Only generate one method if we have one matching return type
                    //
                    if (matchingReturn != null) {
                        generateMethodVariants(bt, pathVar, method, inputType, matchingReturn, isAbstract);
                    } else {
                        for (RepresentationNode returnType : supportedOutputs) {
                            generateMethodVariants(bt, pathVar, method, inputType, returnType, isAbstract);
                        }
                    }
                }
            }
        }
    }

    /**
     * Generate one or two Java methods for a specified combination of WADL
     * <code>method</code>, input <code>representation</code> and output
     * <code>representation</code> elements. Always generates one method that
     * works with DataSources and generates an additional method that uses
     * JAXB_MAPPING when XML representations are used and the document element
     * is specified.
     *
     * @param isAbstract controls whether the generated methods will have a body
     * {@code false} or not {@code true}.
     * @param method the WADL <code>method</code> element for the Java method
     * being generated.
     * @param inputRep the WADL <code>representation</code> element for the
     * request format.
     * @param outputRep the WADL <code>representation</code> element for the
     * response format.
     */
    protected void generateMethodVariants(
            final BlockTracker bt,
            final String pathVar,
            final MethodNode method, final RepresentationNode inputRep,
            final RepresentationNode outputRep, boolean isAbstract) {

        String methodName = BaseResourceClassGenerator.getMethodName(method, inputRep, outputRep, null, null);

        List<Param> required = method.getRequiredParameters();
        List<Param> optional = method.getOptionalParameters();
//        List<Param> all = new ArrayList<>();
//        all.addAll(required);
//        all.addAll(optional);

        List<Param> queryParameters = new ArrayList<Param>();

        List<String> names = new ArrayList<>();
        
        StringBuilder sb = new StringBuilder("Required: ");
        for (Param param : required) {
            
            names.add(param.getName());
            
            sb.append(param.getName());
            sb.append(" ");

            if (param.getStyle() == ParamStyle.QUERY) {
                queryParameters.add(param);
            }

        }
        
        // If we have a data body we need a parmeter for this
        if (inputRep != null && inputRep.getMediaType() != null) {
            names.add("requestEntity");
            
            sb.append("Entity: requestEntity ");
        }
        
        
        sb.append("Optional: ");
        for (Param param : optional) {

            names.add(param.getName());

            sb.append(param.getName());
            sb.append(" ");

            if (param.getStyle() == ParamStyle.QUERY) {
                queryParameters.add(param);
            }
        }
        bt.inlineComment(sb.toString());
        
        
        
        
        //

        try (Block jsonForResource = bt.jsonValue(methodName)) {
            try (Block methodFunction = bt.function(names.toArray(new String[names.size()]))) {

                // Validate required parameters
                //
                for (Param param : required) {
                    try (Block check = bt.parameterNotDefined(param)) {
                        try (Block throwx = bt.throwValue()) {
                            bt.reference("'Parameter " + param.getName() + " is required';");
                        }
                    }
                }

                // Process query paramters
                final StringBuilder queryExtras = new StringBuilder();
                if (queryParameters.size() > 0)
                {
                    try (Block v = bt.var("queryPart")) {
                        try (Block emptyJson = bt.block()) {
                        }
                    }
                    
                    for (Param query : queryParameters) {
                        
                        try (Block pd = bt.parameterDefined(query)) {
                            try (Block jsonAssignment = bt.assignment("queryPart." + query.getName())) {
                                bt.reference(query.getName());
                            }
                        }

                    }
                    
                    try (Block updatePath = bt.assignment(pathVar)) {
                        
                        bt.reference(pathVar);
                        bt.reference(" + '?' + ");
                        bt.reference("$.param(queryPart)");
                    }
                }

                //
                try (Block ret = bt.ret()) {

                    bt.invokefunction("$", "ajax",
                            new Parameter() {

                                @Override
                                public void build() {
                                    bt.reference(pathVar);
                                }

                            },
                            new Parameter() {

                                @Override
                                public void build() {
                                    try (Block jsonBlock = bt.block()) {

                                        try (Block value = bt.jsonValue("type")) {
                                            bt.literal(method.getName());
                                        }

                                        if (inputRep != null && inputRep.getMediaType() != null) {
                                            try (Block value = bt.jsonValue("contentType")) {
                                                bt.literal(inputRep.getMediaType());
                                            }

                                            try (Block value = bt.jsonValue("data")) {
                                                bt.reference("requestEntity");
                                            }

                                        }

                                        if (outputRep != null && outputRep.getMediaType() != null) {
                                            try (Block value = bt.jsonValue("headers")) {

                                                try (Block headers = bt.block()) {

                                                    try (Block acceptValue = bt.jsonValue("Accept")) {
                                                        bt.literal(outputRep.getMediaType());
                                                    }

                                                }
                                            }

                                        }

                                    }
                                }

                            });
                }

            }
        }

    }

    @Override
    public void generateResourceTypeInterface(ResourceTypeNode n) throws JClassAlreadyExistsException {
        // NOOP, doesn't make sense in context of JavaScript
    }

    public interface Block
            extends AutoCloseable {

        public void close();
    }

    public interface Parameter {

        public void build();
    }

    private static class BlockTracker {

        public BlockTracker(PrintWriter pw) {
            this.pw = pw;
        }

        int depth;
        PrintWriter pw;

        public Block assignment(String name) {
            pw.print(name);
            pw.print(" = ");

            return new Block() {

                @Override
                public void close() {
                    endLine();
                }

            };
        }

        public Block var(String name) {
            pw.print("var ");
            pw.print(name);
            pw.print(" = ");

            return new Block() {

                @Override
                public void close() {
                    endLine();
                }

            };
        }

        public BlockTracker inlineComment(String comment) {
            pw.print("// ");
            pw.print(comment);
            pw.println();
            return this;
        }

        public BlockTracker literal(String value) {
            // TODO escape this value
            pw.print("\"");
            pw.print(value);
            pw.print("\"");
            return this;
        }

        public BlockTracker reference(String value) {
            pw.print(value);
            return this;
        }

        public Block jsonValue(String name) {
            pw.print(name);
            pw.print(" : ");

            return new Block() {

                @Override
                public void close() {
                    pw.println(",");
                }

            };
        }

        public Block function(String... arguments) {
            pw.print("function(");

            for (int arg = 0; arg < arguments.length; arg++) {
                pw.print(arguments[arg]);
                if (arg < arguments.length - 1) {
                    pw.print(", ");
                }
            }
            pw.println(") ");

            return block();
        }

        public void invokefunction(String variable, String function, Parameter... parameters) {

            pw.print(variable);
            pw.print(".");
            pw.print(function);
            pw.print("(");

            for (int param = 0; param < parameters.length; ++param) {
                parameters[param].build();
                if (param < parameters.length - 1) {
                    pw.print(", ");
                }
            }

            pw.print(")");

        }

        public Block block() {
            pw.println("{");
            depth++;

            return new Block() {

                @Override
                public void close() {
                    pw.println("\n}");
                    depth--;
                    if (depth < 0) {
                        throw new IllegalStateException("Ended pop too much out of a block");
                    }
                }

            };
        }

        public void endLine() {
            pw.println(";");
        }

        public Block ret() {
            pw.print("return ");

            return new Block() {

                @Override
                public void close() {
                    endLine();
                }

            };
        }

        public Block throwValue()
        {
            pw.print("throw");
            
            return new Block() {

                @Override
                public void close() {
                    endLine();
                }

            };
        }
        
        public Block parameterNotDefined(Param param) {
            pw.println("if (" + param.getName() + " === undefined || " + param.getName() + " === null)");
            return block();
        }
        
        public Block parameterDefined(Param param) {
            pw.println("if (" + param.getName() + " !== undefined && " + param.getName() + " !== null)");
            return block();
        }

    }
}
