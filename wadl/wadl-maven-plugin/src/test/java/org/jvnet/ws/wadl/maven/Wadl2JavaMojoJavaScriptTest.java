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

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebResponseData;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlScript;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import net.sourceforge.htmlunit.corejs.javascript.JavaScriptException;
import net.sourceforge.htmlunit.corejs.javascript.NativeObject;
import org.codehaus.plexus.util.FileUtils;
import org.easymock.classextension.EasyMock;
import org.junit.Test;
import org.jvnet.ws.wadl2java.Wadl2Java;
import org.fest.reflect.field.Invoker;

import static org.fest.reflect.core.Reflection.field;
import static org.fest.reflect.core.Reflection.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.jvnet.ws.wadl.matchers.Matchers.contains;
import static org.jvnet.ws.wadl.matchers.Matchers.exists;

/**
 * Test the generation of javascript clients
 */
public class Wadl2JavaMojoJavaScriptTest 
    extends AbstractWadl2JavaMojoTest {
    

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
    }
    
    @Override
    public void tearDown() throws Exception
    {
        super.setUp();
    }
    
    /**
     * @return a Mojo configured to generate as the JAX-RS static proxy
     */
    @Override
    protected Wadl2JavaMojo getMojo(String pluginXml) throws Exception {
        Wadl2JavaMojo mojo = super.getMojo(pluginXml);
        field("generationStyle").ofType(String.class).in(mojo).set( Wadl2Java.STYLE_JQUERY_JAVASCRIPT);

        Invoker<File> targetDirField = field("targetDirectory").ofType(File.class)
                .in(mojo);
        File originalTarget = targetDirField.get();
        String originalString = originalTarget.toString();
        String original = "generated-sources" + File.separator + "wadl";
        String replacement = "generated-sources" + File.separator + "wadl-javascript";
        File replacementFile = new File(originalString.replace(
                original, replacement));
        targetDirField.set(replacementFile);

        return mojo;
    }
    
    
    
    public Object runGeneratedScript(URL scriptURL, String scriptToExecute) throws IOException
    {
       WebClient client = new WebClient(BrowserVersion.FIREFOX_24);
       client.setWebConnection(new WebConnection() {

            @Override
            public WebResponse getResponse(final WebRequest webRequest) throws IOException {
                
                // Store the request
                _requests.add(
                        new WrapperResponse() {
                            public URI getURI() {
                                try {
                                    return webRequest.getUrl().toURI();
                                } catch (URISyntaxException ex) {
                                    Logger.getLogger(Wadl2JavaMojoJavaScriptTest.class.getName()).log(Level.SEVERE, null, ex);
                                    return null;
                                }
                            }

                            public String getMethod() {
                                return webRequest.getHttpMethod().name();
                            }

                            @Override
                            public String getBodyAsString() {
                                return webRequest.getRequestBody();
                            }
                            
                            
                        });

                // Generate some pre-canned response

                WebResponseData wrd;

                List<NameValuePair> headers = new ArrayList<NameValuePair>();
                // Ensure we don't trip over a CORS issue
                headers.add(new NameValuePair("Access-Control-Allow-Origin", "*"));

                if (webRequest.getHttpMethod().equals(HttpMethod.OPTIONS)) {
                    
                    headers.add(new NameValuePair("Access-Control-Allow-Methods", "PUT, POST, DELETE, GET"));
                    headers.add(new NameValuePair("Access-Control-Allow-Headers", "content-type, content-length"));
                    
                    // Generate a generic response for the moment
                    wrd = new WebResponseData(
                            "".getBytes(),
                            200,
                            "OK",
                            headers);
                    
                }
                else if (_cannedResponse.size() > 0) {
                    
                    CannedResponse cnr = (CannedResponse) _cannedResponse.remove(0);
                    
                    Set<Map.Entry> entries = cnr.headers.entrySet();
                    for (Map.Entry entry : entries)
                    {
                       headers.add(new NameValuePair(entry.getKey().toString(), entry.getValue().toString()));
                    }
                    
                    StringBuilder allowHeaders = new StringBuilder();
                    for (Iterator<Map.Entry> it = entries.iterator(); it.hasNext(); )
                    {
                        allowHeaders.append(it.next().getKey().toString().toLowerCase());
                        if (it.hasNext()) {
                            allowHeaders.append(", ");
                        }
                    }
                    headers.add(new NameValuePair("Access-Control-Allow-Headers", allowHeaders.toString()));


                    wrd = new WebResponseData(cnr.content,
                            cnr.status,
                            Response.Status.fromStatusCode(cnr.status).getReasonPhrase(),
                            headers);

                } else {
                    // Generate a generic response for the moment
                    wrd = new WebResponseData("Hello".getBytes(),
                            200,
                            "OK",
                            headers);
                }

                
                
                WebResponse response = new WebResponse(wrd,webRequest, 100);
                
                return response;
                
                
            }
        });
       
       HtmlPage webpage = client.getPage(this.getClass().getResource("template.html"));
       
       HtmlScript script = (HtmlScript) webpage.getElementById("targetScript");
       script.setAttributeNS(null, "src", scriptURL.toExternalForm());
       
       webpage.initialize();
       
       webpage.executeJavaScript(
               scriptToExecute);
       
       client.waitForBackgroundJavaScript(60000);
       
       return webpage.executeJavaScript("ajaxResult").getJavaScriptResult();
               
    }
    
    
    
    
    
    /**
     * Tests the case in which a valid wadl file exists.
     */
    public void testValidWadlFiles() throws Exception {
        Object result = validWadlFileImpl("ApiSearchYahooCom_NewsSearchServiceV1Client.ApiSearchYahooCom_NewsSearchServiceV1().NewsSearch()"
                + "             .getAsXml('vappid', 'vquery').then(function(data,textStatus,jqXHR)"
                       + "{"
                       + "   console.log('Complete 1 ' + data);"
                       + "   ajaxResult = data;"
                       + "});");

        
        assertThat((String)result, equalTo("Hello"));
        
        assertThat("http://api.search.yahoo.com/NewsSearchService/V1/newsSearch?appid=vappid&query=vquery",
                equalTo(_requests.get(0).getURI().toString()));
    }
    

    /**
     * Tests the case in which a valid wadl file exists.
     */
    public void testValidWadlFilesRequiredParameters() throws Exception {
        
        try
        {
            validWadlFileImpl("ApiSearchYahooCom_NewsSearchServiceV1Client.ApiSearchYahooCom_NewsSearchServiceV1().NewsSearch()"
                + "             .getAsXml().then(function(data,textStatus,jqXHR)"
                       + "{"
                       + "   console.log('Complete 1 ' + data);"
                       + "   ajaxResult = data;"
                       + "});");
            
            assertTrue("Should have thrown exception as missing paramters", false);
        }
        catch (ScriptException jse)
        {
            assertThat(jse.getMessage(), containsString("appid is required"));
        }
        catch (Exception ex)
        {
            assertTrue("Invalid exception type", false);
        }
        
    }
    
    
    private Object validWadlFileImpl(String script) throws Exception {
        // Prepare
        Wadl2JavaMojo mojo = getMojo("valid-wadl-config.xml");
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
        
        //
        EasyMock.verify(_project);
        assertThat(targetDirectory, exists());
        assertThat(targetDirectory, contains("client.js"));
        
        //
        
        URL scriptURL = new File(targetDirectory, "client.js").toURI().toURL();
        Object result =  runGeneratedScript(scriptURL, script);
        return result;
    }
    
    /**
     * Tests the case in which a valid wadl file exists.
     */
    public void testValidWadlFilesWithParameters() throws Exception {
        // Prepare
        Wadl2JavaMojo mojo = getMojo("parameterized-wadl.xml");
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
        assertThat(targetDirectory, contains("client.js"));

        //
        URL scriptURL = new File(targetDirectory, "client.js").toURI().toURL();
        Object result =  runGeneratedScript(scriptURL, 
                "Localhost_JerseySchemaGenExamplesContextRootJerseyClient.Localhost_JerseySchemaGenExamplesContextRootJersey().PathParam1('VParam1').Param2('VParam2')"
                + "             .getAsXml().then(function(data,textStatus,jqXHR)"
                       + "{"
                       + "   console.log('Complete 1 ' + data);"
                       + "   ajaxResult = data;"
                       + "});");

        assertThat("http://localhost:7101/JerseySchemaGen-Examples-context-root/jersey/path/VParam1/VParam2",
                equalTo(_requests.get(0).getURI().toString()));

    }
    
    
    /**
     * Tests the case in which a valid wadl file exists.
     */
    public void testJsonPost() throws Exception {
        // Prepare
        Wadl2JavaMojo mojo = getMojo("jsoninout-wadl.xml");
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
        assertThat(targetDirectory, contains("client.js"));
        
        // Put in a canned response
        _cannedResponse.add(new CannedResponse(200, "application/json", "{ \"hello\" : \"world\" }"));

        //
        URL scriptURL = new File(targetDirectory, "client.js").toURI().toURL();
        NativeObject result =  (NativeObject) runGeneratedScript(scriptURL, 
                "var requestContent = { \"hello\" : \"world\" }\n"
                        + "LocalhostClient.Localhost().Helloworld()"
                + "             .postJson(requestContent).then(function(data,textStatus,jqXHR)"
                       + "{"
                       + "   console.log('Complete 1 ' + data);"
                       + "   ajaxResult = data;"
                       + "}).fail(function(jqXHR, textStatus, errorThrown) {"
                        + "     console.log(errorThrown);\n"
                        + "     console.log(jqXHR.responseText);"
                        + "});"); 

        assertThat(_requests.get(1).getURI().toString(),
                equalTo("http://localhost:9998/helloworld"));

        assertThat(_requests.get(1).getBodyAsString(),
                containsString("{\"hello\":\"world\"}"));
        
        
        assertThat(result.get("hello").toString(),
                equalTo("world"));
        
    }    
}
