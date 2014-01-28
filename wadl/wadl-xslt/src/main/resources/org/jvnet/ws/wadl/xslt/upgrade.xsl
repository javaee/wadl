<?xml version="1.0" encoding="UTF-8"?>
<!--
 The contents of this file are subject to the terms
 of the Common Development and Distribution License
 (the "License").  You may not use this file except
 in compliance with the License.

 You can obtain a copy of the license at
 http://www.opensource.org/licenses/cddl1.php
 See the License for the specific language governing
 permissions and limitations under the License.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:wadl200610="http://research.sun.com/wadl/2006/10">

<!--    <xsl:template match="wadl200610:response/wadl200610:representation[@status]">
        <xsl:call-template name="response-with-status"/>
    </xsl:template>

    <xsl:template match="wadl200610:response/wadl200610:representation">
        <xsl:element namespace="http://wadl.dev.java.net/2009/02" name="response"> 
            <xsl:element namespace="http://wadl.dev.java.net/2009/02" name="representation">
                <xsl:copy-of select="namespace::*"/>
                <xsl:apply-templates select="@*|node()"/>
            </xsl:element>
        </xsl:element> 
    </xsl:template>

    <xsl:template match="wadl200610:response/wadl200610:fault">
        <xsl:call-template name="response-with-status"/>
    </xsl:template>

    <xsl:template name="response-with-status">
         <xsl:element namespace="http://wadl.dev.java.net/2009/02" name="response"> 

             Copy the status attribute up to the response level 
            <xsl:attribute name="status">
                <xsl:value-of select="@status"/>
            </xsl:attribute>

            <xsl:element namespace="http://wadl.dev.java.net/2009/02" name="representation">

                <xsl:copy-of select="namespace::*"/>
                <xsl:for-each select="@*">
                    <xsl:if test="local-name()!='status'">
                        <xsl:apply-templates select="."/>
                    </xsl:if>
                </xsl:for-each>
                <xsl:apply-templates select="node()"/>
            </xsl:element>
        </xsl:element> 
    </xsl:template>-->

    <xsl:template match="wadl200610:*">
        <xsl:choose>
            <!-- So when we have a response, we might need to break it up into
                 child objects depending on how it is defined -->
            <xsl:when test="local-name()='response'">

                <!-- XSL 2.0 version that properly groups response codes -->
<!--                <xsl:for-each-group select="wadl200610:representation|wadl200610:fault" group-by="@status">

                     Create a response element with the speced status code 
                    
                    <xsl:element namespace="http://wadl.dev.java.net/2009/02" name="response"> 

                         Copy the status attribute up to the response level 
                        <xsl:attribute name="status">
                            <xsl:value-of select="current-grouping-key()"/>
                        </xsl:attribute>

                        <xsl:for-each select="current-group()">
                            <xsl:element namespace="http://wadl.dev.java.net/2009/02" name="representation">

                                <xsl:copy-of select="namespace::*"/>
                                <xsl:for-each select="@*">
                                    <xsl:if test="local-name()!='status'">
                                        <xsl:apply-templates select="."/>
                                    </xsl:if>
                                </xsl:for-each>
                                <xsl:apply-templates select="node()"/>
                            </xsl:element>
                        </xsl:for-each>
                    </xsl:element> 
                    
                    
                </xsl:for-each-group>
-->
                
                <xsl:for-each select="wadl200610:representation|wadl200610:fault" >

                    <!-- Create a response element with the speced status code -->
                    
                    <xsl:element namespace="http://wadl.dev.java.net/2009/02" name="response"> 

                        <!-- Copy the status attribute up to the response level -->
                        <xsl:if test="@status">
                            <xsl:attribute name="status">
                                <xsl:value-of select="@status"/>
                            </xsl:attribute>                            
                        </xsl:if>

                        <xsl:element namespace="http://wadl.dev.java.net/2009/02" name="representation">

                            <xsl:copy-of select="namespace::*"/>
                            <xsl:for-each select="@*">
                                <xsl:if test="local-name()!='status'">
                                    <xsl:apply-templates select="."/>
                                </xsl:if>
                            </xsl:for-each>
                            <xsl:apply-templates select="node()"/>
                        </xsl:element>
                    </xsl:element> 
                    
                    
                </xsl:for-each>
                
            </xsl:when>    
            <xsl:otherwise>
                <!-- Just copy over the element with the new namespace -->
                <xsl:element namespace="http://wadl.dev.java.net/2009/02" name="{local-name()}">
                    <xsl:copy-of select="namespace::*"/>
                    <xsl:apply-templates select="@*|node()"/>
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
