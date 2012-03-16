package org.jvnet.ws.wadl.maven;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import java.io.DataInputStream;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import static org.jvnet.ws.wadl.matchers.Matchers.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.lang.Iterable;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
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
        } catch (MojoExecutionException mee) {
            // This is fine
        } catch (Throwable th) {
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
        assertThat(targetDirectory, contains("test/ApiSearchYahooCom_NewsSearchServiceV1.java"));
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
        ClassLoader cl = compile(targetDirectory);

        // Check that the BASE_URI is correct
        Class $ProxyRoot = cl.loadClass("test.ApiSearchYahooCom_NewsSearchServiceV1");
        assertEquals(
                "http://api.search.yahoo.com/NewsSearchService/V1/",
                $ProxyRoot.getDeclaredField("BASE_URI").get($ProxyRoot));
        
        // Check that we have the expected number of methods
        Class $NewsSearch = cl.loadClass("test.ApiSearchYahooCom_NewsSearchServiceV1$NewsSearch");
        assertNotNull($NewsSearch);

        Class $Type = cl.loadClass("test.Type");
        Class $Sort = cl.loadClass("test.Sort");
        Class $Output = cl.loadClass("test.Output");

        // Constructors
        assertNotNull($NewsSearch.getConstructor());
        assertNotNull($NewsSearch.getConstructor(Client.class));


        // Check that we have two methods of the right name and parameters
        assertNotNull($NewsSearch.getDeclaredMethod("getAsResultSet", String.class, String.class));
        assertNotNull($NewsSearch.getDeclaredMethod("getAsApplicationXml", String.class, String.class, Class.class));
        assertNotNull($NewsSearch.getDeclaredMethod("getAsApplicationXml", String.class, String.class, GenericType.class));

        assertNotNull($NewsSearch.getDeclaredMethod("getAsResultSet", String.class, String.class,
                $Type, Integer.class, Integer.class, $Sort, String.class, $Output, String.class));
        assertNotNull($NewsSearch.getDeclaredMethod("getAsApplicationXml", String.class, String.class,
                $Type, Integer.class, Integer.class, $Sort, String.class, $Output, String.class, Class.class));
        assertNotNull($NewsSearch.getDeclaredMethod("getAsApplicationXml", String.class, String.class,
                $Type, Integer.class, Integer.class, $Sort, String.class, $Output, String.class, GenericType.class));


    }

    
    /**
     * Tests the case in which a valid wadl file exists, and we have a catalog
     * file with which to override the BASE_URI with
     */
    public void testValidWadlFilesWithCatalog() throws Exception {
        // Prepare
        Wadl2JavaMojo mojo = getMojo("valid-wadl-config-with-catalog.xml");
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
 
        // Create a catalog file
        
        File metainf = new File(targetDirectory, "META-INF/");
        metainf.mkdirs();
        File catalog = new File(metainf, "jax-rs-catalog.xml");
        PrintStream ps = new PrintStream(
                new FileOutputStream(catalog));
        try
        {
            ps.println("<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">");
            ps.println("  <uri name=\"http://api.search.yahoo.com/NewsSearchService/V1/\"" );
            ps.println("          uri=\"http://otherhost/\"/> ");
            ps.println("</catalog>");
        }
        finally
        {
            ps.close();
        }
        
        
        // Check that the generated code compiles
        ClassLoader cl = compile(targetDirectory);

        // Check that the BASE_URI is correct
        Class $ProxyRoot = cl.loadClass("test.ApiSearchYahooCom_NewsSearchServiceV1");
        assertEquals(
                "http://otherhost/",
                $ProxyRoot.getDeclaredField("BASE_URI").get($ProxyRoot));

    }

    /**
     * Tests the case in which a valid wadl file exists, and it it contains a method that returns
     * just text/plain
     */
    public void testNonJAXBWadlFiles() throws Exception {
        final String expectedClassName = "Localhost";
        final String pluginXmlFilename = "nonjaxb-wadl.xml";

        assertNonJAXBWadlFiles(expectedClassName, pluginXmlFilename);
    }

    /**
     * Tests the hello world file works with custom class names providing less cryptic and
     * non-changing class names for bug bug WADL-43
     * 
     * 
     * @throws Exception
     */
    public void testNonJAXBWadlFilesCustomClassName() throws Exception {
        final String expectedClassName = "MyApiClient";
        final String pluginXmlFilename = "nonjaxb-wadl-custom-names.xml";

        assertNonJAXBWadlFiles(expectedClassName, pluginXmlFilename);
    }

    /**
     * Tests the case in which a valid wadl file exists, and it it contains a method that returns
     * just text/plain
     * 
     * @param expectedClassName
     * @param pluginXmlFilename
     * @throws Exception
     * @throws IllegalAccessException
     * @throws IOException
     * @throws MojoExecutionException
     * @throws MojoFailureException
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     */
    private void assertNonJAXBWadlFiles(final String expectedClassName, final String pluginXmlFilename)
            throws Exception {
        // Prepare
        Wadl2JavaMojo mojo = getMojo(pluginXmlFilename);
        File targetDirectory = (File) getVariableValueFromObject(mojo, "targetDirectory");
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
        assertThat(targetDirectory, contains("test/" + expectedClassName + ".java"));

        // Check that the generated code compiles
        ClassLoader cl = compile(targetDirectory);

        // Check that we have the expected number of methods
        Class $Helloworld = cl.loadClass("test." + expectedClassName + "$Helloworld");
        assertNotNull($Helloworld);

        // Constructors
        assertNotNull($Helloworld.getConstructor());
        assertNotNull($Helloworld.getConstructor(Client.class));

        // Check that we have two methods of the right name and parameters
        assertNotNull($Helloworld.getDeclaredMethod("getAsTextPlain", Class.class));
        assertNotNull($Helloworld.getDeclaredMethod("getAsTextPlain", GenericType.class));
    }

    
    
    
    
    
    /**
     * Tests the case where the methods produce application/xml but don't
     * have a matching schema. This results in duplicate methods being produced.
     * 
     * See WADL issue 41
     */
    public void testJAXBMissingSchema() throws Exception {
        // Prepare
        Wadl2JavaMojo mojo = getMojo("jaxb-missing-schema.xml");
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
        assertThat(targetDirectory, contains("test/Localhost_Project1Jersey.java"));

        // Check that the generated code compiles
        ClassLoader cl = compile(targetDirectory);



        // Check that we have the expected number of methods
        Class $Helloworld = cl.loadClass("test.Localhost_Project1Jersey$Put");
        assertNotNull($Helloworld);

        // Constructors
        assertNotNull($Helloworld.getConstructor());
        assertNotNull($Helloworld.getConstructor(Client.class));

        // Check that we have two methods of the right name and parameters
        assertNotNull($Helloworld.getDeclaredMethod("getAsApplicationXml", Class.class));
        assertNotNull($Helloworld.getDeclaredMethod("getAsApplicationXml", GenericType.class));

        assertNotNull($Helloworld.getDeclaredMethod("putApplicationXml", Object.class, Class.class));
        assertNotNull($Helloworld.getDeclaredMethod("putApplicationXml", Object.class, GenericType.class));

        // Verify that in both cases the method is actually invoked, in
        // liu of functional tests for the moment
        File proxyFile = new File(targetDirectory, "test/Localhost_Project1Jersey.java");
        DataInputStream input = new DataInputStream(new FileInputStream(proxyFile));
        byte data[] = new byte[(int) proxyFile.length()];
        input.readFully(data);
        String contents = new String(data);
        Matcher matcher = Pattern.compile("return resourceBuilder.method").matcher(contents);
        // Make sure we only have four return methods of this kind
        matcher.find();
        matcher.find();
        matcher.find();
        assertTrue(matcher.find());
        assertFalse(matcher.find());
    }

    /**
     * Tests the case in which a method has multiple representations
     */
    public void testValidMutlipleContentTypes() throws Exception {
        // Prepare
        Wadl2JavaMojo mojo = getMojo("multiple-contenttypes-wadl.xml");
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

        // Verify
        EasyMock.verify(project);
        assertThat(targetDirectory, exists());
        assertThat(targetDirectory, contains("test"));
        assertThat(targetDirectory, contains("test/Localhost.java"));

        // Check that the generated code compiles
        ClassLoader cl = compile(targetDirectory);



        // Check that we have the expected number of methods
        Class $Helloworld = cl.loadClass("test.Localhost$Helloworld");
        assertNotNull($Helloworld);

        // Constructors
        assertNotNull($Helloworld.getConstructor());
        assertNotNull($Helloworld.getConstructor(Client.class));

        // Check that we have two methods of the right name and parameters
        assertNotNull($Helloworld.getDeclaredMethod("getSomeClassAsApplicationXml"));
        assertNotNull($Helloworld.getDeclaredMethod("getAsApplicationXml", Class.class));
        assertNotNull($Helloworld.getDeclaredMethod("getAsApplicationXml", GenericType.class));

        assertNotNull($Helloworld.getDeclaredMethod("getSomeClassAsApplicationJson"));
        assertNotNull($Helloworld.getDeclaredMethod("getAsApplicationJson", Class.class));
        assertNotNull($Helloworld.getDeclaredMethod("getAsApplicationJson", GenericType.class));

    }

    /**
     * Test the case where we have the types generating as @XmlType not
     * @XmlRootElement so we need to generate some extra boilerplate code.
     */
    public void testHelloBeanInputOuputJAXB() throws Exception {
        runBeanInputOuputJAXB(false);
    }
    /**
     * Test the case where we have the types generating as 
     * @XmlRootElement so we don't need to generate boilerplate code.
     */
    public void testHelloBeanInputOuputJAXBSimpleXJC() throws Exception {
        runBeanInputOuputJAXB(true);
    }
    
    /**
     * Test the case where we have the types generating as @XmlType not
     * @XmlRootElement so we need to generate some extra boilerplate code.
     */
    private void runBeanInputOuputJAXB(boolean simplexjc) throws Exception {
    
        
        // Prepare
        Wadl2JavaMojo mojo = getMojo(simplexjc ? "hellobean-wadl-simplexjc.xml" : "hellobean-wadl.xml");
        

        
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
        assertThat(targetDirectory, contains("test/WwwExampleCom_Resource.java"));
        assertThat(targetDirectory, contains("com/example/beans/Bean.java"));

        // Check that the generated code compiles
        ClassLoader cl = compile(targetDirectory);

        // Just check that the base class works
        Class $ProxyRoot = cl.loadClass("test.WwwExampleCom_Resource");
        
        // Load the source for the Proxy and check we generated the correct new JAXBElement lines
        String contents ="";
        File resource = new File(targetDirectory,"test/WwwExampleCom_Resource.java" );
        FileInputStream fis = new FileInputStream(resource);
        try {
            byte data[] = new byte[(int)resource.length()];
            fis.read(data);
            contents = new String(data);
        }
        finally
        {
            fis.close();
        }
        
        // Make sure we wrap the @XmlType as a JAXBElement with the right
        // namespace
        
        boolean containsBinding = contents.contains("new JAXBElement(new QName(\"http://example.com/beans\", \"bean\"), com.example.beans.Bean.class, input)");
        if (simplexjc) {
            assertFalse(
                 containsBinding);            
        }
        else {
            assertTrue(
                 containsBinding);            
        }
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
        assertThat(targetDirectory, contains("test/Localhost_JerseySchemaGenExamplesContextRootJersey.java"));
        assertThat(targetDirectory, contains("example/"));
        assertThat(targetDirectory, contains("example/IndirectReturn.java"));
        assertThat(targetDirectory, contains("example/ObjectFactory.java"));
        assertThat(targetDirectory, contains("example/SimpleParam.java"));
        assertThat(targetDirectory, contains("example/SimpleReturn.java"));



        // Check that the generated code compiles
        ClassLoader cl = compile(targetDirectory);

        // Check top level accessor
        Class $Root = cl.loadClass("test.Localhost_JerseySchemaGenExamplesContextRootJersey");
        assertNotNull($Root.getDeclaredMethod("pathParam1", Client.class, String.class));

        // Check that we have the expected number of methods
        Class $PathParam1 = cl.loadClass("test.Localhost_JerseySchemaGenExamplesContextRootJersey$PathParam1");
        assertNotNull($PathParam1);

        // Constructors
        assertNotNull($PathParam1.getConstructor(String.class));
        assertNotNull($PathParam1.getConstructor(Client.class, String.class));

        // Check that we have two methods of the right name and parameters
        assertNotNull($PathParam1.getDeclaredMethod("getParam1"));
        assertNotNull($PathParam1.getDeclaredMethod("setParam1", String.class));

        // Check accessors
        assertNotNull($PathParam1.getDeclaredMethod("param2", String.class));


        // Go on level down
        Class $PathParam2 = $PathParam1.getDeclaredClasses()[0];
        assertThat($PathParam2.getSimpleName(), equalTo("Param2"));
        assertNotNull($PathParam2);

        // Constructors
        assertNotNull($PathParam2.getConstructor(String.class, String.class));
        assertNotNull($PathParam2.getConstructor(Client.class, String.class, String.class));

        // Check that we have two methods of the right name and parameters
        assertNotNull($PathParam2.getDeclaredMethod("getParam1"));
        assertNotNull($PathParam2.getDeclaredMethod("setParam1", String.class));
        assertNotNull($PathParam2.getDeclaredMethod("getParam2"));
        assertNotNull($PathParam2.getDeclaredMethod("setParam2", String.class));


        // Check that we have two methods of the right name and parameters
        assertNotNull($PathParam2.getDeclaredMethod("getAsSimpleReturn"));
        assertNotNull($PathParam2.getDeclaredMethod("getAsApplicationXml", Class.class));
        assertNotNull($PathParam2.getDeclaredMethod("getAsApplicationXml", GenericType.class));
    }

    
    /**
     * Verify as per WADL-37 that query and header parameters are not inhereted
     * from parent resources
     */
    public void testValidWadlFilesWithNestedParameters() throws Exception {
        // Prepare
        Wadl2JavaMojo mojo = getMojo("nested-wadl.xml");
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
        assertThat(targetDirectory, contains("test/Nested.java"));
 
        // Check that the generated code compiles
        ClassLoader cl = compile(targetDirectory);

        // Check top level accessor
        Class $Root = cl.loadClass("test.Nested$Root");

        // Check that we have the expected number of methods
        Class $Sub = cl.loadClass("test.Nested$Root$Sub");
        assertNotNull($Sub);


        // Check that we have two methods of the right name and parameters
        // Should only have four string parametes as per WADL-32 the
        // root resource parametes are not inhereted
        assertNotNull($Sub.getDeclaredMethod("getAs", String.class, String.class, String.class, String.class, Class.class));
        assertNotNull($Sub.getDeclaredMethod("getAs", String.class, String.class,String.class, String.class, GenericType.class));

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

    private ClassLoader compile(File targetDirectory) throws MalformedURLException {
        // Compile the source

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector diagnosticCollector = new DiagnosticCollector();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                diagnosticCollector,
                null, null);

        List<File> files = listFilesRecursively(targetDirectory);

        Iterable<? extends JavaFileObject> compilationUnits1 =
                fileManager.getJavaFileObjectsFromFiles(files);
        boolean success = compiler.getTask(null, fileManager, null, null, null, compilationUnits1).call();
        assertTrue("Compilation failed for some reason", success);

        assertThat(diagnosticCollector.getDiagnostics().size(), equalTo(0));

        // Create an return a stuitable class loader
        //

        return new URLClassLoader(new java.net.URL[]{targetDirectory.toURI().toURL()});
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
    private Wadl2JavaMojo getMojo(String pluginXml) throws Exception {
        return (Wadl2JavaMojo) lookupMojo("generate", getBasedir()
                + "/src/test/plugin-configs/wadl2java/" + pluginXml);
    }
}
