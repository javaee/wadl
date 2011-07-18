package org.jvnet.ws.wadl.maven;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import static org.jvnet.ws.wadl.matchers.Matchers.*;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.Iterable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.easymock.classextension.EasyMock;


/**
 * A bunch of tests for the {@link Wadl2JavaMojo}.
 * 
 * @author Wilfred Springer
 * 
 */
public class Wadl2JavaMojoTest extends AbstractMojoTestCase {

    /**
     * A mock object representing the active project.
     */
    private MavenProject project;

    // JavaDoc inherited
    @Override
    public void setUp() throws Exception {
        super.setUp();
        project = EasyMock.createMock(MavenProject.class);
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
        setVariableValueToObject(mojo, "project", project);

        // Record
        project.addCompileSourceRoot(targetDirectory.getAbsolutePath());

        // Replay
        EasyMock.replay(project);
        mojo.execute();

        // Verify
        EasyMock.verify(project);
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
        setVariableValueToObject(mojo, "project", project);

        // Record
        project.addCompileSourceRoot(targetDirectory.getAbsolutePath());

        // Replay
        EasyMock.replay(project);
        try {
            mojo.execute();
        }
        catch (MojoExecutionException mee) {
            // This is fine
        }
        catch (Throwable th) {
            assertThat(th, not(instanceOf(MojoExecutionException.class)));
        }
    }
    
    
    /**
     * Tests the case in which a valid wadl file exists.
     */
    public void testValidWadlFiles() throws Exception {
        // Prepare
        Wadl2JavaMojo mojo = getMojo("valid-wadl-config.xml");
        File targetDirectory = (File) getVariableValueFromObject(mojo,
                "targetDirectory");
        if (targetDirectory.exists()) {
            FileUtils.deleteDirectory(targetDirectory);
        }
        setVariableValueToObject(mojo, "project", project);

        // Record
        project.addCompileSourceRoot(targetDirectory.getAbsolutePath());

        // Replay
        EasyMock.replay(project);
        mojo.execute();

        // Verify
        EasyMock.verify(project);
        assertThat(targetDirectory, exists());
        assertThat(targetDirectory, contains("test"));
        assertThat(targetDirectory, contains("test/HttpApiSearchYahooComNewsSearchServiceV1.java"));
        assertThat(targetDirectory, contains("test/Output.java"));
        assertThat(targetDirectory, contains("test/Type.java"));
        assertThat(targetDirectory, contains("test/Sort.java"));
        assertThat(targetDirectory, contains("yahoo/api/ObjectFactory.java"));
        assertThat(targetDirectory, contains("yahoo/api/Error.java"));
        assertThat(targetDirectory, contains("yahoo/yn/ImageType.java"));
        assertThat(targetDirectory, contains("yahoo/yn/ObjectFactory.java"));
        assertThat(targetDirectory, contains("yahoo/yn/ResultSet.java"));
        assertThat(targetDirectory, contains("yahoo/yn/ResultType.java"));

        // Check that the generated code compiles
        compile(targetDirectory);
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
        setVariableValueToObject(mojo, "project", project);

        // Record
        project.addCompileSourceRoot(targetDirectory.getAbsolutePath());

        // Replay
        EasyMock.replay(project);
        mojo.execute();

        // Verify
        EasyMock.verify(project);
        assertThat(targetDirectory, exists());

        // Verify the files are in place
        
        assertThat(targetDirectory, contains("test"));
        assertThat(targetDirectory, contains("test/HttpLocalhost7101JerseySchemaGenExamplesContextRootJersey.java"));
        assertThat(targetDirectory, contains("example/"));
        assertThat(targetDirectory, contains("example/IndirectReturn.java"));
        assertThat(targetDirectory, contains("example/ObjectFactory.java"));
        assertThat(targetDirectory, contains("example/SimpleParam.java"));
        assertThat(targetDirectory, contains("example/SimpleReturn.java"));

        // Check that the generated code compiles
        compile(targetDirectory);
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
        setVariableValueToObject(mojo, "project", project);

        // Record
        project.addCompileSourceRoot(targetDirectory.getAbsolutePath());

        // Replay
        EasyMock.replay(project);
        mojo.execute();

        // Verify
        EasyMock.verify(project);
        assertThat(targetDirectory, exists());
        assertThat(targetDirectory, contains("test"));
        assertThat(targetDirectory, contains("test/HttpApiSearchYahooComNewsSearchServiceV1.java"));
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

    
    
    private void compile(File targetDirectory) {
        // Compile the source
        
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector diagnosticCollector = new DiagnosticCollector();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                diagnosticCollector, 
                null, null);
        
        List<File> files = listFilesRecursively(targetDirectory);

        Iterable<? extends JavaFileObject> compilationUnits1 =
           fileManager.getJavaFileObjectsFromFiles(files);
        compiler.getTask(null, fileManager, null, null, null, compilationUnits1).call();    
       
        assertThat(diagnosticCollector.getDiagnostics().size(), equalTo(0));
    }
    /**
     * @return a list of all java files under the given directory
     */
    private List<File> listFilesRecursively(File root) {
        
        List<File> files = new ArrayList<File>();
        List<File> dirs = new ArrayList<File>();
        dirs.add(root);
        
        for (int i = 0; i < dirs.size(); i++) {
            File dir = dirs.get(i);
            
            files.addAll(Arrays.asList(dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".java");
            }})));
            
            dirs.addAll(Arrays.asList(dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }})));
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
    private Wadl2JavaMojo getMojo(String pluginXml) throws Exception {
        return (Wadl2JavaMojo) lookupMojo("generate", getBasedir()
                + "/src/test/plugin-configs/wadl2java/" + pluginXml);
    }

}
