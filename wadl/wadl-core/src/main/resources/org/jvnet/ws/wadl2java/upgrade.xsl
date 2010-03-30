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

    <xsl:template match="wadl200610:response">
        <xsl:apply-templates select="node()"/>
    </xsl:template>

    <xsl:template match="wadl200610:representation[@status]">
        <xsl:call-template name="response-with-status"/>
    </xsl:template>

    <xsl:template match="wadl200610:representation">
        <xsl:element namespace="http://wadl.dev.java.net/2009/02" name="response">
            <xsl:element namespace="http://wadl.dev.java.net/2009/02" name="representation">
                <xsl:copy-of select="namespace::*"/>
                <xsl:apply-templates select="@*|node()"/>
            </xsl:element>
        </xsl:element>
    </xsl:template>

    <xsl:template match="wadl200610:fault">
        <xsl:call-template name="response-with-status"/>
    </xsl:template>

    <xsl:template name="response-with-status">
        <xsl:element namespace="http://wadl.dev.java.net/2009/02" name="response">
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
    </xsl:template>

    <xsl:template match="wadl200610:*">
        <xsl:element namespace="http://wadl.dev.java.net/2009/02" name="{local-name()}">
            <xsl:copy-of select="namespace::*"/>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>

