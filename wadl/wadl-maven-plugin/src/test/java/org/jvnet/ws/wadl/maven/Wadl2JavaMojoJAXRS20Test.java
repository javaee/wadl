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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.util.concurrent.Future;
import javax.ws.rs.ProcessingException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Configuration;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;

import org.fest.reflect.field.Invoker;
import static org.fest.reflect.core.Reflection.field;


/**
 * @author gdavison
 */
public class Wadl2JavaMojoJAXRS20Test
        extends AbstractJavaWadl2JavaMojoTest<Client> {

    @Override
    protected Client createClient() {

        JerseyClient client = (JerseyClient) ClientBuilder.newClient();
        ClientConfig cc = client.getConfiguration();
        cc.register(new MoxyJsonFeature()); 
        cc.connectorProvider(new ConnectorProvider()
        {
            public Connector getConnector(Client client, Configuration config)
            {
                return  new Connector() {
                    public String getName() {
                        return "TestConnector";
                    }

                    public ClientResponse apply(final ClientRequest cr) throws ProcessingException {

                        // Store the request
                        _requests.add(
                                new WrapperResponse() {
                                    public URI getURI() {
                                        return cr.getUri();
                                    }

                                    public String getMethod() {
                                        return cr.getMethod();
                                    }

                                    @Override
                                    public String getBodyAsString() {
                                        throw new UnsupportedOperationException("Not supported yet.");
                                    }
                                });

                        // Generate some pre-canned response

                        ClientResponse resp;

                        if (_cannedResponse.size() > 0) {
                            CannedResponse cnr = (CannedResponse) _cannedResponse.remove(0);

                            resp = new ClientResponse(
                                    Response.Status.fromStatusCode(cnr.status),
                                    cr);

                            resp.headers(cnr.headers);
                            resp.setEntityStream(new ByteArrayInputStream(cnr.content));

                        } else {
                            // Generate a generic response for the moment
                            resp = new ClientResponse(
                                    Response.Status.OK,
                                    cr);
                            resp.setEntityStream(new ByteArrayInputStream("Hello".getBytes()));
                        }

                        return resp;

                    }

                    public Future<?> apply(ClientRequest cr, AsyncConnectorCallback acc) {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                    public void close() {
                        // Do nothing;
                    }
                };
            }
        });
                
                
                


        return client;
    }


    /**
     * @return a Mojo configured to generate as the JAX-RS static proxy
     */
    @Override
    protected Wadl2JavaMojo getMojo(String pluginXml) throws Exception {
        Wadl2JavaMojo mojo = super.getMojo(pluginXml);
        field("generationStyle").ofType(String.class).in(mojo).set("jaxrs20");

        Invoker<File> targetDirField = field("targetDirectory").ofType(File.class)
                .in(mojo);
        File originalTarget = targetDirField.get();
        String originalString = originalTarget.toString();
        String original = "generated-sources" + File.separator + "wadl";
        String replacement = "generated-sources" + File.separator + "wadl-jaxrs20";
        File replacementFile = new File(originalString.replace(
                original, replacement));
        targetDirField.set(replacementFile);

        return mojo;
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
        return Response.class;
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
        return "return response.readEntity";
    }

}
