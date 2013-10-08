/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jvnet.ws.wadl.ast;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;
import org.junit.Test;
import org.jvnet.ws.wadl.util.MessageListener;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;


/**
 * Provide unit tests for the AST
 * @author gdavison
 */
public class WadlAstBuilderTest {
     
    
    /**
     * Whilst investigating a bug, make sure that the AST is properly reporting
     * the number of methods.
     */
    @Test
    public void testSoapUIYahooSearch() throws InvalidWADLException, IOException, URISyntaxException
    {
        WadlAstBuilder builder = new WadlAstBuilder(
                new WadlAstBuilder.SchemaCallback() {

            public void processSchema(InputSource is) {
            }

            public void processSchema(String uri, Element node) {
            }
        },
                new MessageListener() {

            public void warning(String message, Throwable throwable) {
            }

            public void info(String message) {
            }

            public void error(String message, Throwable throwable) {
            }
        });
        
        ApplicationNode an = 
                builder.buildAst(WadlAstBuilderTest.class.getResource("SoapUIYahooSearch.wadl").toURI());
        List<MethodNode> methods = an.getResources().get(0).getChildResources().get(0).getMethods();
        
        // Sanity check that the reference doesn't duplicate the values
        assertThat("Only one method",
                methods.size(), equalTo(1));

        // Check that the status on the upgraded fault message
        // has correctly been upgrade to the 2009 WADL version
        List<RepresentationNode> supportedOutputs = new ArrayList<RepresentationNode>();
        for (List<RepresentationNode> nodeList : methods.get(0).getSupportedOutputs().values()) {
            for (RepresentationNode node : nodeList)
            {
                supportedOutputs.add(node); 
            }
        }
        assertThat("Only one output",
                supportedOutputs.size(), equalTo(1));
        assertThat("Only one fault",
                methods.get(0).getFaults().size(), equalTo(1));
        
    }
    
}
