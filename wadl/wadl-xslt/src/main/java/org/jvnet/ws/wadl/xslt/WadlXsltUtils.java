package org.jvnet.ws.wadl.xslt;

import java.io.InputStream;

/**
 * A collection of helper utilities for accessing common XSL scripts and 
 * functions for use in them.
 *
 */
public class WadlXsltUtils {

    /**
     * @return A transform that upgrades the current WADL to the '2009 
     *   standard
     */
    public static InputStream getUpgradeTransformAsStream() {
        return WadlXsltUtils.class.getResourceAsStream("upgrade.xsl");
    }

    /**
     * A the moment this template requires the Oracle XDK; but in future the ideal
     * is to move towards a vanilla XSLT document without any specific parser or
     * java dependencies
     * 
     * @return A template that will transform a WADL into a user readable
     *   HTML document.
     */
    public static InputStream getWadlSummaryTransform() {
        return WadlXsltUtils.class.getResourceAsStream("wadl_2009-02.xsl");
    }
    
    /**
     * @todo Figure out how to do this is XSLT
     * @param uri The input URI
     * @return A String where each / has been replaced with a / and a soft
     *   hyphen
     */
    public static String hypernizeURI(String uri) {
        // &shy and <wbr> didn't work; but this is apparently font related
        String result = uri.replaceAll("/", "/&#8203;");
        return result;
    }
}
