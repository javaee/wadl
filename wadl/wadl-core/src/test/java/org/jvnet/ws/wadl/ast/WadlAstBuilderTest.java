/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jvnet.ws.wadl.ast;

import java.io.IOException;
import java.net.URISyntaxException;
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
        
        // Sanity check that the reference doesn't duplicate the values
        assertThat("Only one method",
                an.getResources().get(0).getChildResources().get(0).getMethods().size(), equalTo(1));
                
    }
    
}
