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

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import static org.codehaus.plexus.PlexusTestCase.getBasedir;
import org.easymock.classextension.EasyMock;

/**
 *
 * @author gdavison
 */
public abstract class AbstractWadl2JavaMojoTest extends AbstractMojoTestCase {
    
    
    /**
     * A class to store a canned response to a client request
     */
    
    protected static class CannedResponse
    {
        public final int status;
        public final MultivaluedMap headers;
        public final byte[] content;
        
        public CannedResponse(int status, String contentType, String content)
        {
            this.status = status;
            headers = new MultivaluedHashMap();
            headers.add("Content-Type", contentType);
            try
            {
                this.content = content.getBytes("UTF-8");
            } catch (Exception ex) {
                throw new RuntimeException("Problem converting text", ex);
            }
            
            headers.add("Content-Length", Integer.toString(this.content.length));
            
        }
    }
    
    /**
     * A class that wraps the response for the given implementation
     */
    
    protected interface WrapperResponse
    {
       public URI getURI();
       public String getMethod();
       public String getBodyAsString();
    }
    
    
    /**
     * A mock object representing the active project.
     */
    protected MavenProject _project;
    /**
     * Store a list of client request
     */
    protected List<WrapperResponse> _requests = new ArrayList<WrapperResponse>();
    /**
     * Store a list of canned responses
     */
    protected List<CannedResponse> _cannedResponse = new ArrayList<CannedResponse>();

    
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        _project = EasyMock.createMock(MavenProject.class);
        // Register the package for the stream handler
        System.setProperty("java.protocol.handler.pkgs", "org.jvnet.ws.wadl.maven");
        // Configure a mock click so we can capture any request
        _requests.clear();
        _cannedResponse.clear();
    }

    @Override
    protected void tearDown() throws Exception {
        //compiled code
        super.tearDown();
        _project = null;
        _requests.clear();
        _cannedResponse.clear();
    }
    
    
    // Helper methods
    

    /**
     * A convenience method for getting a link to the resources dir
     *
     * @param subpath
     *            The sub path under the resources directory
     * @return The full path to this file
     */
    public static String getFilePath(String subpath) {
        return getBasedir() + "/src/test/resources/" + subpath;
    }

   /**
     * @return a list of all java files under the given directory
     */
    protected List<File> listFilesRecursively(File root) {

        List<File> files = new ArrayList<File>();
        List<File> dirs = new ArrayList<File>();
        dirs.add(root);

        for (int i = 0; i < dirs.size(); i++) {
            File dir = dirs.get(i);

            files.addAll(Arrays.asList(dir.listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.endsWith(".java");
                }
            })));

            dirs.addAll(Arrays.asList(dir.listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return new File(dir, name).isDirectory();
                }
            })));
        }

        return files;
    }

    /**
     * A convenience method for getting a configured {@lik Wadl2JavaMojo}
     * instance.
     * 
     * @param pluginXml
     *            The name of the configuration file, configuring the
     *            {@link Wadl2JavaMojo}. The operation will search for a file
     *            named <code>pluginXml</code> in
     *            <code>${basedir}/src/test/plugin-configs/wadl2java</code>.
     * @return A configured instance of the {@link Wadl2JavaMojo}.
     * @throws Exception
     *             If an instance of the {@link Wadl2JavaMojo} cannot be
     *             constructed from the file name passed in.
     */
    protected Wadl2JavaMojo getMojo(String pluginXml) throws Exception {
        return (Wadl2JavaMojo) lookupMojo("generate", getBasedir()
                + "/src/test/plugin-configs/wadl2java/" + pluginXml);
    }    
    
}
