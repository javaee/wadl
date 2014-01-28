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
package org.jvnet.ws.wadl.maven.wadl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.jvnet.ws.wadl.maven.Wadl2JavaMojoTest;

/**
 * Create a new protocol type so that the tests can think that they
 * are resolving the URL from a remote server and not files on disk.
 * This was added in particular to test WADL-50
 * @author gdavison
 */
public class Handler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        
        String relative = u.getPath();
        String pathOnDisk = Wadl2JavaMojoTest.getFilePath(relative);
        
        return new File(pathOnDisk).toURL().openConnection();
        
    }
    
}
