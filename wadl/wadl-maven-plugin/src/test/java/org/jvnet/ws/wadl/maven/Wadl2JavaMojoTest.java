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

package org.jvnet.ws.wadl.maven;

import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.header.InBoundHeaders;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import javax.ws.rs.WebApplicationException;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.easymock.classextension.EasyMock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.jvnet.ws.wadl.ast.InvalidWADLException;
import static org.jvnet.ws.wadl.matchers.Matchers.contains;
import static org.jvnet.ws.wadl.matchers.Matchers.exists;

/**
 * A bunch of tests for the {@link Wadl2JavaMojo}, that generate for the
 * Jersey 1.x client.
 * 
 * @author Wilfred Springer
 * 
 */
public class Wadl2JavaMojoTest extends AbstractWadl2JavaMojoTest<Client> {

    
    /**
     * @return A new instance of the client customised for automated testing
     */
    @Override
    protected Client createClient()
    {
        ClientConfig config = new DefaultClientConfig();
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        
        return new Client(new TerminatingClientHandler()
        {
            public ClientResponse handle(final ClientRequest cr) throws ClientHandlerException {
// Just for debugging purposes               
//                if (cr.getEntity()!=null) {
//                    ByteArrayOutputStream boas = new ByteArrayOutputStream();
//                    try {
//                        getRequestEntityWriter(cr).writeRequestEntity(boas);
//                    } catch (IOException ex) {
//                        Logger.getLogger(Wadl2JavaMojoTest.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//
//                    String body = boas.toString();
//                }
                
                // Store the request
                _requests.add(
                        new WrapperResponse(){
                            public URI getURI() {
                                return cr.getURI();
                            }

                            public String getMethod() {
                                return cr.getMethod();
                            }
                        });

                ClientResponse resp;
                if (_cannedResponse.size() > 0)
                {  
                    CannedResponse cnr = _cannedResponse.remove(0);
                    InBoundHeaders headers = new InBoundHeaders();
                    headers.putAll(cnr.headers);
                    
                    
                    resp = new ClientResponse(
                            cnr.status,
                            headers,
                            new ByteArrayInputStream(cnr.content),
                            getMessageBodyWorkers());
                }
                else
                {
                    // Generate a generic response for the moment
                    resp = new ClientResponse(
                            200,
                            new InBoundHeaders(),
                            new ByteArrayInputStream("Hello".getBytes()),
                            getMessageBodyWorkers());
                }
                return resp; 
            }
        }, config);        
    }            

    
    /**
     * @return The client class for this implementation
     */
    @Override
    protected Class getClientClass() {
        return Client.class;
    }

    /**
     * @return The response class for this implementation
     */
    @Override
    protected Class getResponseClass() {
        return ClientResponse.class;
    }

    
    /**
     * @return The generic type class for this implementation
     */
    @Override
    protected Class getGenericTypeClass() {
        return GenericType.class;
    }
    

    
    /**
     * @return A regex to verify that the application is being invoked
     */
    @Override
    protected String getReturnStatmentRegex() {
        return "return response.getEntity";
    }
    
    
    
    
    
    
    /**
     * Tests the simple case with an existing sourceDirectory containing no wadl
     * files.
     */
    public void testSmokeTest() throws Exception {
        // Prepare
        Wadl2JavaMojo mojo = getMojo("smoke-test-config.xml");
        File targetDirectory = (File) getVariableValueFromObject(mojo,
                "targetDirectory");
        if (targetDirectory.exists()) {
            assertThat(targetDirectory.delete(), is(equalTo(true)));
        }
        setVariableValueToObject(mojo, "project", _project);

        // Record
        _project.addCompileSourceRoot(targetDirectory.getAbsolutePath());

        // Replay
        EasyMock.replay(_project);
        mojo.execute();

        // Verify
        EasyMock.verify(_project);
        assertThat(targetDirectory, exists());
    }

    /**
     * Tests the simple case with a wadl that is not parsable
     */
    public void testBroken() throws Exception {
        // Prepare
        Wadl2JavaMojo mojo = getMojo("broken-wadl.xml");
        File targetDirectory = (File) getVariableValueFromObject(mojo,
                "targetDirectory");
        if (targetDirectory.exists()) {
            assertThat(targetDirectory.delete(), is(equalTo(true)));
        }
        setVariableValueToObject(mojo, "project", _project);

        // Record
        _project.addCompileSourceRoot(targetDirectory.getAbsolutePath());

        // Replay
        EasyMock.replay(_project);
        try {
            mojo.execute();
        } catch (MojoExecutionException mee) {
            // This is fine
        } catch (Throwable th) {
            assertThat(th, not(instanceOf(MojoExecutionException.class)));
        }
    }


    /**
     * Tests the case in which is method is missing the name attribute
     * WADL bug 49.
     */
    public void testMethodMissingName() throws Exception {
        // Prepare
        Wadl2JavaMojo mojo = getMojo("missing-method-name.xml");
        File targetDirectory = (File) getVariableValueFromObject(mojo,
                "targetDirectory");
        if (targetDirectory.exists()) {
            FileUtils.deleteDirectory(targetDirectory);
        }
        setVariableValueToObject(mojo, "project", _project);

        // Record
        _project.addCompileSourceRoot(targetDirectory.getAbsolutePath());

        // Replay
        EasyMock.replay(_project);
        try
        {
            mojo.execute();
            
            assertTrue("Should have failed with an invalid wadl exception", false);
        }
        catch (Exception ex)
        {
            // All in fine
            assertTrue("Should have failed with an invalid wadl exception", 
                    ex.getCause() instanceof InvalidWADLException); 
        }
    }

    
    /**
     * Test a fixed version of the open patent wadl, that doesn't
     * contain breaks as per testMissingMethodName, this is to exercise
     * WADL-49 and WADL-50 the latter being specifically about relative
     * URI in the base property of resources
     */
    public void testOpenPatentExample() throws Exception {
        // Prepare
        Wadl2JavaMojo mojo = getMojo("open-patent-example.xml");
        File targetDirectory = (File) getVariableValueFromObject(mojo,
                "targetDirectory");
        if (targetDirectory.exists()) {
            FileUtils.deleteDirectory(targetDirectory);
        }
        setVariableValueToObject(mojo, "project", _project);

        // Record
        _project.addCompileSourceRoot(targetDirectory.getAbsolutePath());

        // Replay
        EasyMock.replay(_project);
        mojo.execute();

        // Verify
        EasyMock.verify(_project);
        assertThat(targetDirectory, exists());
        assertThat(targetDirectory, contains("test"));
        
        assertThat(targetDirectory, contains("test/Example_262RestServicesClassification.java"));
        assertThat(targetDirectory, contains("test/Example_262RestServicesFamily.java"));
        assertThat(targetDirectory, contains("test/Example_262RestServicesLegal.java"));
        assertThat(targetDirectory, contains("test/Example_262RestServicesNumberService.java"));
        // Deal with duplicate class name
        assertThat(targetDirectory, contains("test/Example_262RestServicesNumberService2.java"));
        assertThat(targetDirectory, contains("test/Example_262RestServicesPublishedData.java"));
        assertThat(targetDirectory, contains("test/Example_262RestServicesRegister.java"));
        // Relative path version
        
        assertThat(targetDirectory, contains("test/Example_OpenPatentServicesWadlRelativePath.java"));
        
        // Exceptions
        assertThat(targetDirectory, contains("test/FaultException.java"));
        // Enumeration on navigator type
        assertThat(targetDirectory, contains("test/Nav.java"));

        // Check that the generated code compiles
        ClassLoader cl = compile(targetDirectory);

        // Check that the BASE_URI is correct with a / absolute path
        Class $ProxyRoot = cl.loadClass("test.Example_262RestServicesClassification");
        assertEquals(
                URI.create("wadl://example/2.6.2/rest-services/classification/"),
                $ProxyRoot.getDeclaredField("BASE_URI").get($ProxyRoot));
        
        // Verify relative path version
        $ProxyRoot = cl.loadClass("test.Example_OpenPatentServicesWadlRelativePath");
        assertEquals(
                URI.create("wadl://example/open-patent-services/wadl/relativePath/"),
                $ProxyRoot.getDeclaredField("BASE_URI").get($ProxyRoot));

        // Check the enumeration is correct
        Class $Nav = cl.loadClass("test.Nav");
        assertTrue($Nav.isEnum());
        Object conts[] = $Nav.getEnumConstants();
        assertEquals(conts[0].toString(), "prev");
        assertEquals(conts[1].toString(), "next");
        

        // Check the fault is correct
        Class $FE = cl.loadClass("test.FaultException");
        assertTrue($FE.getSuperclass() == WebApplicationException.class);
        assertEquals($FE.getDeclaredFields()[0].getName(), "m_faultInfo");

    }
    
    /**
     * Tests the case in which a valid wadl file exists.
     */
    public void testValidWadlWithCustomization() throws Exception {
        // Prepare
        Wadl2JavaMojo mojo = getMojo("valid-wadl-with-customizations-config.xml");
        File targetDirectory = (File) getVariableValueFromObject(mojo,
                "targetDirectory");
        if (targetDirectory.exists()) {
            FileUtils.deleteDirectory(targetDirectory);
        }
        setVariableValueToObject(mojo, "project", _project);

        // Record
        _project.addCompileSourceRoot(targetDirectory.getAbsolutePath());

        // Replay
        EasyMock.replay(_project);
        mojo.execute();

        // Verify
        EasyMock.verify(_project);
        assertThat(targetDirectory, exists());
        assertThat(targetDirectory, contains("test"));
        assertThat(targetDirectory, contains("test/ApiSearchYahooCom_NewsSearchServiceV1.java"));
        assertThat(targetDirectory, contains("test/Output.java"));
        assertThat(targetDirectory, contains("test/Type.java"));
        assertThat(targetDirectory, contains("test/Sort.java"));
        assertThat(targetDirectory, contains("yahoo/api/ObjectFactory.java"));
        assertThat(targetDirectory, contains("yahoo/api/Error.java"));
        assertThat(targetDirectory, contains("yahoo/yn/ImageType.java"));
        assertThat(targetDirectory, contains("yahoo/yn/ObjectFactory.java"));
        assertThat(targetDirectory, contains("yahoo/yn/ResultSet.java"));
        // Because of the customizations
        assertThat(targetDirectory, contains("yahoo/yn/Result.java"));

        // Check that the generated code compiles
        compile(targetDirectory);
    }

    
    /**
     * Tests the case where when using a customisation that get on the JAXBModel
     * doesn't return anything for an Element an instead we have to look for the 
     * XmlType class. This was causing the sub getEmp method to go missing
     * on the sub class.
     */
    public void testWadlWithNameCustomization() throws Exception {
        // Prepare
        Wadl2JavaMojo mojo = getMojo("wadl-with-name-customization.xml");
        File targetDirectory = (File) getVariableValueFromObject(mojo,
                "targetDirectory");
        if (targetDirectory.exists()) {
            FileUtils.deleteDirectory(targetDirectory);
        }
        setVariableValueToObject(mojo, "project", _project);

        // Record
        _project.addCompileSourceRoot(targetDirectory.getAbsolutePath());

        // Replay
        EasyMock.replay(_project);
        mojo.execute();

        // Verify
        EasyMock.verify(_project);
        assertThat(targetDirectory, exists());
        assertThat(targetDirectory, contains("test"));
        assertThat(targetDirectory, contains("test/Localhost_REST_SanityEmpServiceContextRootResources.java"));
        assertThat(targetDirectory, contains("JAXB_EmployeePack/Emp.java"));
        assertThat(targetDirectory, contains("JAXB_EmployeePack/ObjectFactory.java"));
        assertThat(targetDirectory, contains("JAXB_EmployeePack/EmployeeList.java"));
        

        // Check that the generated code compiles
        compile(targetDirectory);

    
        // Check that the generated code compiles
        ClassLoader cl = compile(targetDirectory);
    
        // Check that we have the expected number of methods
        Class $Sub = cl.loadClass("test.Localhost_REST_SanityEmpServiceContextRootResources$Project1$Name");
        assertNotNull($Sub);


        // Look for the getEmp method that was going missing
        assertNotNull($Sub.getDeclaredMethod("getAsEmp"));
    }
    
    
    

}
