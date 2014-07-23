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
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebResponseData;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.gargoylesoftware.htmlunit.util.WebConnectionWrapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import org.junit.Test;

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
    
    
    @Test
    public void testSimpleCase() throws IOException
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
                        });

                // Generate some pre-canned response

                WebResponseData wrd;

                List<NameValuePair> headers = new ArrayList<NameValuePair>();
                // Ensure we don't trip over a CORS issue
                headers.add(new NameValuePair("Access-Control-Allow-Origin", "*"));

                if (_cannedResponse.size() > 0) {
                    CannedResponse cnr = (CannedResponse) _cannedResponse.remove(0);
                    
                    Set<Map.Entry> entries = cnr.headers.entrySet();
                    for (Map.Entry entry : entries)
                    {
                       headers.add(new NameValuePair(entry.getKey().toString(), entry.getValue().toString()));
                    }

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
       webpage.initialize();
       
       webpage.executeJavaScript(
               "$.ajax('http://www.example.com/example', {}).then(function(data,textStatus,jqXHR)"
                       + "{"
                       + "   console.log('Complete 1 ' + data);"
                       + "});");
       
       client.waitForBackgroundJavaScript(10000);
               
    }
    
}
