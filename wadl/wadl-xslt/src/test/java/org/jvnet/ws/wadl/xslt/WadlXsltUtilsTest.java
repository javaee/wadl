package org.jvnet.ws.wadl.xslt;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class WadlXsltUtilsTest {

    @Test
    public void hypernizUriTest() {
        
        String uri = "http://one/long/string";
        String result = WadlXsltUtils.hypernizeURI(uri);
        
        // Check that the URI will now properly hypernate
        assertThat(5, equalTo(result.split("&").length));
    }

    
    @Test
    public void checkTemplatesExistTest() {
        
        
        // Check that the URI will now properly hypernate
        assertThat(WadlXsltUtils.getUpgradeTransformAsStream(), notNullValue());
        assertThat(WadlXsltUtils.getWadlSummaryTransform(), notNullValue());
    }
    
}
