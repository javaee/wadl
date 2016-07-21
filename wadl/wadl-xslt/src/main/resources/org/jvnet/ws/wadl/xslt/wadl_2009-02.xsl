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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
                xmlns:wadl="http://wadl.dev.java.net/2009/02" 
                xmlns:wadljson="http://wadl.dev.java.net/2009/02/json-schema"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                xmlns:a="urn:functions" xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:pxsltu="http://www.oracle.com/XSL/Transform/java/org.jvnet.ws.wadl.xslt.WadlXsltUtils"
                exclude-result-prefixes="a">
   <!-- Make this style sheet schema aware -->
   <!-- Performance really slow   <xsl:import-schema schema-location="wadl_2009-02.xsd" namespace="http://wadl.dev.java.net/2009/02" /> -->
   <!-- This parameter allows this page to be used as a test tool, this value
     should be a URI that can be used to invoke a REST test tool of some kind -->
   <xsl:param name="testButtonUri" as="xsd:string?"/>
   <xsl:param name="jQueryUri" as="xsd:string?"/>
   
   
   <!-- This is a workaround for the fact that elements in a sequence loose there
        original document, going to have to rethink that code but until then 
        at least make this work for simple one document WADL files -->
   <xsl:variable name="root" select="/" />
   
   <!-- The Oracle XSL transformer doesn't support tokenize, so we have to do this
        using a recursive template -->
   <xsl:template name="tokenize">
       <xsl:param name="text" select="."/>
       <xsl:param name="separator" select="'\s+'"/>
       
<!--       <xsl:message>Splitting <xsl:value-of select="$text" /></xsl:message>-->

       <xsl:choose>
         <xsl:when test="not(contains($text,$separator))">
             <xsl:value-of select="$text"/>

          </xsl:when>
         <xsl:otherwise>
             <xsl:copy>
                <xsl:value-of select="normalize-space(substring-before($text, $separator))"/>

                <xsl:call-template name="tokenize">
                  <xsl:with-param name="text" select="substring-after($text, $separator)"/>
                  <xsl:with-param name="separator" select="$separator"/>
               </xsl:call-template>
             </xsl:copy>
          </xsl:otherwise>
      </xsl:choose>
       
   </xsl:template>
   <!-- Resolve element of type nodeName in this or other document based on the
        value of href -->
   <xsl:function name="a:lookupElement" as="node()">
      <xsl:param name="context" as="node()"/>
      <xsl:param name="href" as="xsd:string"/>
      <xsl:param name="nodeName" as="xsd:string"/>

      <xsl:choose>
         <xsl:when test="starts-with($href, '#')">
           <!-- Internal document reference -->
           <xsl:variable name="anchor" select="substring-after($href, '#')"/> 
           <!-- TODO should we have to worry about the root node we are querying in? -->
           <xsl:sequence select="/wadl:application/wadl:*[local-name() = $nodeName and @id = $anchor][1]"/>
         </xsl:when>
         <xsl:when test="contains($href, '#')">
           <!-- external document reference -->
           <xsl:variable name="anchor" select="substring-after($href, '#')"/>
           <xsl:variable name="documentUri" select="substring-before($href, '#')"/>
           <xsl:sequence select="document($documentUri, $context )/wadl:application/wadl:*[local-name() = $nodeName and @id = $anchor][1]"/>
         </xsl:when>
         <xsl:otherwise>
           <xsl:message>Cannot locate <xsl:value-of select='$href'/> </xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:function>
   <!-- Lookup function  that will return a node set that contains just
     the current node as passed in as a parameter or the node it 
     references -->
   <xsl:function name="a:lookupReference" as="node()">
      <xsl:param name="context" as="node()"/>
      
      <!-- resource type thingy -->
      <xsl:variable name="nodeName">
         <xsl:choose>   
            <xsl:when test="local-name(.) = 'resource'">
               <xsl:message>Calling lookupResource with resource, going to ignore that</xsl:message>
           </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="local-name(.)"/>
           </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
   
      <xsl:choose>
         <xsl:when test="@href">
           <xsl:sequence select="a:lookupElement($context,@href,$nodeName)"/>
         </xsl:when>
         <xsl:otherwise>
           <xsl:sequence select="$context"/>
         </xsl:otherwise>
      </xsl:choose>
     
   </xsl:function>
   <!-- Lookup function that will return the context resource object along with
        any resource type parameters that are requried -->
   <xsl:function name="a:lookupResourceReferences" as="node()*">
   
      <xsl:param name="context" as="node()"/>

<!--    <xsl:message>lookupResourceReferences <xsl:value-of select="local-name($context)"/> 
     and root <xsl:value-of select="local-name(root($context))"/>
      </xsl:message> -->

      
      <!-- resource type thingy -->
      <xsl:variable name="nodeName">
         <xsl:choose>
            <xsl:when test="local-name(.) = 'resource'">
               <xsl:value-of select="'resource_type'"/>
           </xsl:when>
            <xsl:otherwise>
               <xsl:message terminate="yes">Wrong version of lookupReference called</xsl:message>
           </xsl:otherwise>
         </xsl:choose>
      </xsl:variable>
   
      <xsl:choose>
         <!-- We need to look at the href attribute  fn:local-name() = $elementToLookup -->
         <xsl:when test="@type">
   
           <xsl:variable name="hrefs">
<!-- Not supported in the XDK             <xsl:value-of select="tokenize(@type, '\s+')"/>-->

                <!-- <xsl:message>Calling tokenize with <xsl:value-of select="@type"/></xsl:message> -->

                <xsl:call-template name="tokenize">
                  <xsl:with-param name="text" select="@type"/>
               </xsl:call-template>
           </xsl:variable> 

<!--           <xsl:message>Result of tokenize 

               <xsl:value-of select="$hrefs"/>           
              <xsl:for-each select="$hrefs">
                Next  <xsl:value-of select="."/>
              </xsl:for-each>
           
           </xsl:message> -->

      
           <!-- Join self with children -->   
           <xsl:sequence select="$context"/>
           <xsl:for-each select="$hrefs">  

              <xsl:variable name="found" select="a:lookupElement($context,.,$nodeName)"/>

<!--              <xsl:message>Looking up <xsl:value-of select="."/>       
                  Found <xsl:value-of select="local-name($found/node())"/>
                    Id <xsl:value-of select="local-name($found/node()/@id)"/>
              </xsl:message> -->

              <xsl:sequence select="$found/node()"/>
           </xsl:for-each>
   
         </xsl:when>
         <xsl:otherwise>
           <!-- Just return the current object -->
           <xsl:sequence select="$context"/>
         </xsl:otherwise>
      </xsl:choose>
     
   </xsl:function>
   <!-- Look up the list of references -->
   <xsl:function name="a:lookupReferences" as="node()*">
      <xsl:param name="context" as="node()*"/>
   
      <!-- Should be using a recursive element here; but cannot due to XDK bug 14215372
      <xsl:choose>
          When we have run out, just return an empty sequence 
        <xsl:when test="count($context) = 0">
           <xsl:sequence select="/.." />
        </xsl:when>
        <xsl:otherwise>
           <xsl:sequence select="a:lookupReferences(subsequence($context, 2)) | a:lookupReference(first($context))" /> 
        </xsl:otherwise>
      </xsl:choose> -->
      
      <!-- Using this break the schema document imports as the elements are copied -->
      <xsl:for-each select="$context">
        <xsl:variable name="lookedup" select="a:lookupReference(.)/node()"/>
        <!-- check this is how sequence works -->
        <xsl:sequence select="$lookedup"/>
      </xsl:for-each>
      
   </xsl:function>
   <!--
   
   
       Templates
       
       
    -->
   <xsl:template match="wadl:application">
    
      <!-- Header / Body boilerplate -->
      
      <html>
         <header>
            <title>
               <xsl:call-template name="fetchTitle"/>
            </title>
            <style type="text/css">
               <![CDATA[
/* Javadoc style sheet */
/*
Overall document style
*/
body {
    background-color:#ffffff;
    color:#353833;
    font-family:Arial, Helvetica, sans-serif;
    font-size:76%;
    margin:0;
}
a:link, a:visited {
    text-decoration:none;
    color:#4c6b87;
}
a:hover, a:focus {
    text-decoration:none;
    color:#bb7a2a;
}
a:active {
    text-decoration:none;
    color:#4c6b87;
}
a[name] {
    color:#353833;
}
a[name]:hover {
    text-decoration:none;
    color:#353833;
}
pre {
    font-size:1.3em;
}
h1 {
    font-size:1.8em;
}
h2 {
    font-size:1.5em;
}
h3 {
    font-size:1.4em;
}
h4 {
    font-size:1.3em;
}
h5 {
    font-size:1.2em;
}
h6 {
    font-size:1.1em;
}

span.documentation {
   font-size:1.2em;
}

ul {
    list-style-type:disc;
}
code, tt {
    font-size:1.2em;
}
dt code {
    font-size:1.2em;
}
table tr td dt code {
    font-size:1.2em;
    vertical-align:top;
}
sup {
    font-size:.6em;
}
/*
Document title and Copyright styles
*/
.clear {
    clear:both;
    height:0px;
    overflow:hidden;
}
.aboutLanguage {
    float:right;
    padding:0px 21px;
    font-size:.8em;
    z-index:200;
    margin-top:-7px;
}
.legalCopy {
    margin-left:.5em;
}
.bar a, .bar a:link, .bar a:visited, .bar a:active {
    color:#FFFFFF;
    text-decoration:none;
}
.bar a:hover, .bar a:focus {
    color:#bb7a2a;
}
/*.tab {
    background-color:#0066FF;
 /*   background-image:url(resources/titlebar.gif); */
    background-position:left top;
    background-repeat:no-repeat;
    color:#ffffff;
    padding:8px;
    width:5em;
    font-weight:bold;
}*/
/*
Navigation bar styles
*/
.bar {
/*    background-image:url(resources/background.gif); */
    background-repeat:repeat-x;
    color:#FFFFFF;
    padding:.8em .5em .4em .8em;
    height:auto;/*height:1.8em;*/
    font-size:1em;
    margin:0;
}
.topNav {
    background-image:url(resources/background.gif);
    background-repeat:repeat-x;
    color:#FFFFFF;
    float:left;
    padding:0;
    width:100%;
    clear:right;
    height:2.8em;
    padding-top:10px;
    overflow:hidden;
}
.bottomNav {
    margin-top:10px;
 /*   background-image:url(resources/background.gif); */
    background-repeat:repeat-x;
    color:#FFFFFF;
    float:left;
    padding:0;
    width:100%;
    clear:right;
    height:2.8em;
    padding-top:10px;
    overflow:hidden;
}
.subNav {
    background-color:#dee3e9;
    border-bottom:1px solid #9eadc0;
    float:left;
    width:100%;
    overflow:hidden;
}
.subNav div {
    clear:left;
    float:left;
    padding:0 0 5px 6px;
}
ul.navList, ul.subNavList {
    float:left;
    margin:0 25px 0 0;
    padding:0;
}
ul.navList li{
    list-style:none;
    float:left;
    padding:3px 6px;
}
ul.subNavList li{
    list-style:none;
    float:left;
    font-size:90%;
}
.topNav a:link, .topNav a:active, .topNav a:visited, .bottomNav a:link, .bottomNav a:active, .bottomNav a:visited {
    color:#FFFFFF;
    text-decoration:none;
}
.topNav a:hover, .bottomNav a:hover {
    text-decoration:none;
    color:#bb7a2a;
}
.navBarCell1Rev {
/*    background-image:url(resources/tab.gif);*/
    background-color:#a88834;
    color:#FFFFFF;
    margin: auto 5px;
    border:1px solid #c9aa44;
}
/*
Page header and footer styles
*/
.header, .footer {
    clear:both;
    margin:0 20px;
    padding:5px 0 0 0;
}
.indexHeader {
    margin:10px;
    position:relative;
}
.indexHeader h1 {
    font-size:1.3em;
}
.title {
    color:#2c4557;
    margin:10px 0;
}
.subTitle {
    margin:5px 0 0 0;
}
.header ul {
    margin:0 0 25px 0;
    padding:0;
}
.footer ul {
    margin:20px 0 5px 0;
}
.header ul li, .footer ul li {
    list-style:none;
    font-size:1.2em;
}
/*
Heading styles
*/
div.details ul.blockList ul.blockList ul.blockList li.blockList h4, div.details ul.blockList ul.blockList ul.blockListLast li.blockList h4 {
    background-color:#dee3e9;
    border-top:1px solid #9eadc0;
    border-bottom:1px solid #9eadc0;
    margin:0 0 6px -8px;
    padding:6px 1px 1px 5px;
    height:24px
}
ul.blockList ul.blockList ul.blockList li.blockList h3 {
    background-color:#dee3e9;
    border-top:1px solid #9eadc0;
    border-bottom:1px solid #9eadc0;
    margin:0 0 6px -8px;
    padding:2px 5px;
}
ul.blockList ul.blockList li.blockList h3 {
    padding:0;
    margin:5px 0;
}
ul.blockList li.blockList h2 {
    padding:0px 0 5px 0;
}
/*
Page layout container styles
*/
.contentContainer, .sourceContainer, .classUseContainer, .serializedFormContainer, .constantValuesContainer {
    clear:both;
    padding:5px 20px;
    position:relative;
}
.indexContainer {
    margin:10px;
    position:relative;
    font-size:1.0em;
}
.indexContainer h2 {
    font-size:1.1em;
    padding:0 0 3px 0;
}
.indexContainer ul {
    margin:0;
    padding:0;
}
.indexContainer ul li {
    list-style:none;
}
.contentContainer .description dl dt, .contentContainer .details dl dt, .serializedFormContainer dl dt {
    font-size:1.1em;
    font-weight:bold;
    margin:5px 0 0 0;
    color:#4E4E4E;
}
.contentContainer .description dl dd, .contentContainer .details dl dd, .serializedFormContainer dl dd {
    margin:5px 0 10px 20px;
}
.serializedFormContainer dl.nameValue dt {
    margin-left:1px;
    font-size:1.1em;
    display:inline;
    font-weight:bold;
}
.serializedFormContainer dl.nameValue dd {
    margin:0 0 0 1px;
    font-size:1.1em;
    display:inline;
}
/*
List styles
*/
ul.horizontal li {
    display:inline;
    font-size:0.9em;
}

/*
Get ride of those little dots all over the place
*/

li.blockList {
    list-style-type:none; 
}

ul.inheritance {
    margin:0;
    padding:0;
}
ul.inheritance li {
    display:inline;
    list-style:none;
}
ul.inheritance li ul.inheritance {
    margin-left:15px;
    padding-left:15px;
    padding-top:1px;
}
ul.blockList, ul.blockListLast {
    margin:10px 0 10px 0;
    padding:0;
}
ul.blockList li.blockList, ul.blockListLast li.blockList {
    list-style:none;
    margin-bottom:5px;
}
ul.blockList ul.blockList li.blockList, ul.blockList ul.blockListLast li.blockList {
    padding:0px 20px 5px 10px;
    border:1px solid #9eadc0;
    background-color:#f9f9f9; 
}
ul.blockList ul.blockList ul.blockList li.blockList, ul.blockList ul.blockList ul.blockListLast li.blockList {
    padding:0 0 5px 8px;
    background-color:#ffffff;
    border:1px solid #9eadc0;
    border-top:none;
}
ul.blockList ul.blockList ul.blockList ul.blockList li.blockList {
    margin-left:0;
    padding-left:0;
    padding-bottom:5px;
    border:none;
    border-bottom:1px solid #9eadc0;
}
ul.blockList ul.blockList ul.blockList ul.blockList li.blockListLast {
    list-style:none;
    border-bottom:none;
    padding-bottom:0;
}
table tr td dl, table tr td dl dt, table tr td dl dd {
    margin-top:0;
    margin-bottom:1px;
}
/*
Table styles
*/
.contentContainer table, .classUseContainer table, .constantValuesContainer table {
    border-bottom:1px solid #9eadc0;
    width:100%;
}
.contentContainer ul li table, .classUseContainer ul li table, .constantValuesContainer ul li table {
    width:100%;
}
.contentContainer .description table, .contentContainer .details table {
    border-bottom:none;
}
.contentContainer ul li table th.colOne, .contentContainer ul li table th.colFirst, .contentContainer ul li table th.colLast, .classUseContainer ul li table th, .constantValuesContainer ul li table th, .contentContainer ul li table td.colOne, .contentContainer ul li table td.colFirst, .contentContainer ul li table td.colLast, .classUseContainer ul li table td, .constantValuesContainer ul li table td{
    vertical-align:top;
    padding-right:20px;
}
.contentContainer ul li table th.colLast, .classUseContainer ul li table th.colLast,.constantValuesContainer ul li table th.colLast,
.contentContainer ul li table td.colLast, .classUseContainer ul li table td.colLast,.constantValuesContainer ul li table td.colLast,
.contentContainer ul li table th.colOne, .classUseContainer ul li table th.colOne,
.contentContainer ul li table td.colOne, .classUseContainer ul li table td.colOne {
    padding-right:3px;
}
.overviewSummary caption, .packageSummary caption, .contentContainer ul.blockList li.blockList caption, .summary caption, .classUseContainer caption, .constantValuesContainer caption {
    position:relative;
    text-align:left;
    background-repeat:no-repeat;
    color:#FFFFFF;
    font-weight:bold;
    clear:none;
    overflow:hidden;
    padding:0px;
    margin:0px;
}
caption a:link, caption a:hover, caption a:active, caption a:visited {
    color:#FFFFFF;
}

/*.overviewSummary caption span, .packageSummary caption span, .contentContainer ul.blockList li.blockList caption span, .summary caption span, .classUseContainer caption span, .constantValuesContainer caption span {
    white-space:nowrap;
    padding-top:8px;
    padding-left:8px;
    display:block;
    float:left;
 /*   background-image:url(resources/titlebar.gif); */
    height:18px;
}
.overviewSummary .tabEnd, .packageSummary .tabEnd, .contentContainer ul.blockList li.blockList .tabEnd, .summary .tabEnd, .classUseContainer .tabEnd, .constantValuesContainer .tabEnd {
    width:10px;
/*    background-image:url(resources/titlebar_end.gif); */
    background-repeat:no-repeat;
    background-position:top right;
    position:relative;
    float:left;
}*/
ul.blockList ul.blockList li.blockList table {
    margin:0 0 5px 0px;
    width:100%;
}
.tableSubHeadingColor {
    background-color: #EEEEFF;
}
.altColor {
    background-color:#eeeeef;
}
.rowColor {
    background-color:#ffffff;
}
.overviewSummary td, .packageSummary td, .contentContainer ul.blockList li.blockList td, .summary td, .classUseContainer td, .constantValuesContainer td {
    text-align:left;
    padding:3px 3px 3px 7px;
}
th.colFirst, th.colLast, th.colOne, .constantValuesContainer th {
    background:#dee3e9;
    border-top:1px solid #9eadc0;
    border-bottom:1px solid #9eadc0;
    text-align:left;
    padding:3px 3px 3px 7px;
}
td.colOne a:link, td.colOne a:active, td.colOne a:visited, td.colOne a:hover, td.colFirst a:link, td.colFirst a:active, td.colFirst a:visited, td.colFirst a:hover, td.colLast a:link, td.colLast a:active, td.colLast a:visited, td.colLast a:hover, .constantValuesContainer td a:link, .constantValuesContainer td a:active, .constantValuesContainer td a:visited, .constantValuesContainer td a:hover {
    font-weight:bold;
}
td.colFirst, th.colFirst {
    border-left:1px solid #9eadc0;
    white-space:nowrap;
}
td.colLast, th.colLast {
    border-right:1px solid #9eadc0;
}
td.colOne, th.colOne {
    border-right:1px solid #9eadc0;
    border-left:1px solid #9eadc0;
}
table.overviewSummary  {
    padding:0px;
    margin-left:0px;
}
table.overviewSummary td.colFirst, table.overviewSummary th.colFirst,
table.overviewSummary td.colOne, table.overviewSummary th.colOne {
    width:25%;
    vertical-align:middle;
}
table.packageSummary td.colFirst, table.overviewSummary th.colFirst {
    width:25%;
    vertical-align:middle;
}
/*
Content styles
*/
.description pre {
    margin-top:0;
}
.deprecatedContent {
    margin:0;
    padding:10px 0;
}
.docSummary {
    padding:0;
}
/*
Formatting effect styles
*/
.sourceLineNo {
    color:green;
    padding:0 30px 0 0;
}
h1.hidden {
    visibility:hidden;
    overflow:hidden;
    font-size:.9em;
}
.block {
    display:block;
    margin:3px 0 0 0;
}
.strong {
    font-weight:bold;
}

.expandButton {
  font-size:0.6em;
  font-weight:bold;
  cursor:pointer;
  margin-right:8px;
  height:19px;
  width:19px;
  visibility:hidden;
  position:relative;top:-2;
}

.clearing {
  clear:both;
}

.testButton {
 font-size:0.8em;
 padding: 0.0em 1em;
 position:relative;top:-2
}
             
             ]]>
            </style>
         </header>
        <head><meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<xsl:choose>
   <xsl:when test="string-length($jQueryUri)>0">
     <script>
       <xsl:attribute name="src"><xsl:value-of select="$jQueryUri"/></xsl:attribute>
       <xsl:text disable-output-escaping="yes"><![CDATA[ ]]></xsl:text> <!-- Just need to ensure the script tag has a distinct close tag -->
     </script>
   </xsl:when>
   <xsl:otherwise>
     <xsl:text disable-output-escaping="yes">
       <![CDATA[<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>]]>
     </xsl:text>
   </xsl:otherwise>
</xsl:choose>

<script>
  function handleHref(urlString)
  {
    if (WADLPreview)
    {
      return WADLPreview.handleClick(urlString);
    }
    {
      return true;
    }
  };
</script>
<script>
  $(document).ready(function() {
  $('.collapsible').hide();
  $('.expandButton').each(function(index,value){
       // we change the margins here to avoid seeing part of the collapsible section
       var h4 = $(this).parent();
       h4.css("margin-bottom", "-6px");
  });
  $('.expandButton').toggle(
      function() 
      {
        var h4 = $(this).parent();
        h4.next('.collapsible').slideDown();
       
        // we change the margins back to their original settings.
        h4.css("margin-bottom", "6px");

        // Change the text to show the section is expanded.
        $(this).text("-");
      },
      function() 
      {
        // Collapse the section.
        var h4 = $(this).parent();
        h4.next('.collapsible').slideUp();

        // we change the margins here to avoid seeing part of the collapsible section
        h4.css("margin-bottom", "-6px");

        // Change the text to show the section is expanded.
        $(this).text("+");
      }
  ); // end toggle
  // We set the expandButton text to collapsed everywhere.
  $('.expandButton').text("+");

  // Make the button visible (this way, it only is visible if JQuery is supported  
  $('.expandButton').css('visibility','visible');

  // We expand the first Resource.
  $('.expandButton:first').click();
}); // end ready
</script>
         </head>
         <body>
            <h2 class="title">
               <xsl:call-template name="fetchTitle"/>
            </h2>
            <div class="contentContainer">
               <xsl:if test="count(wadl:doc)>0">
                 <div class="description">
                     <ul class="blockList">
                        <li class="blockList">
                           <div class="block">
                              <xsl:call-template name="fetchDocumentation"/>
                           </div>
                        </li>
                     </ul>
                  </div>
              </xsl:if>
               <xsl:if test="count(wadl:resources)>1">

                 <div class="summary">
                     <ul class="blockList">
                        <li class="blockList">
                           <!-- ======== Resources Summary ======== -->
                           <ul class="blockList">
                              <li class="blockList">
                                 <xsl:text disable-output-escaping="yes"> <![CDATA[<a name="resources_summary"></a>]]></xsl:text>
                                 <h3>Resources Summary</h3>
                                 <table class="overviewSummary" border="0" cellpadding="3" cellspacing="0"
                                        summary="Resources summary table, listing root URLs and titles">
                                    <caption>
                                       <span>Resources</span>
                                       <span class="tabEnd"></span>
                                    </caption>
                                    <tr>
                                       <th class="colOne" scope="col">URI and Documentation</th>
                                    </tr>
                                    <xsl:for-each select="wadl:resources">
                        <tr class="altColor">
                                          <td class="colOne">
                                             <code>
                                                <strong>
                                                   <a>
                                                      <xsl:attribute name="href">
                                   #<xsl:call-template name="fetchId"/>
                                 </xsl:attribute>
                                                      <xsl:value-of select="@base"/>
                                                   </a>
                                                </strong>
                                             </code>
                                             <div class="block">
                                                <xsl:call-template name="fetchDocumentation"/>
                                             </div>
                                          </td>
                                       </tr>
                     </xsl:for-each>
                                 </table>
                              </li>
                           </ul>
                        </li>
                     </ul>
                  </div>
   
               </xsl:if>
               <div class="details">
                  <ul class="blockList">
                     <li class="blockList">
                        <xsl:if test="count(wadl:grammars/*) > 0">

                     <!-- ========== Grammar Details ========= -->
                 
                     <ul class="blockList">
                              <li class="blockList">
                                 <xsl:text disable-output-escaping="yes"> <![CDATA[<a name="grammar_detail"></a>]]></xsl:text>
                                 <h3>Grammars</h3>
                                 <dl>
                                    <xsl:for-each select="wadl:grammars/wadl:include">
                              <dt>
                                          <code>
                                             <a href="{@href}" onclick="return handleHref('{@href}');">
                                                <xsl:value-of  select="@href"/>
                                             </a>
                                          </code>
                                       </dt>
                              <dd>
                                          <xsl:call-template name="fetchDocumentation"/>
                                       </dd>
                           </xsl:for-each>
                                    <xsl:if test="count(wadl:grammars/xsd:schema) > 0">
                              <dt>[This document contains one or more in-line schemas]</dt>
                           </xsl:if>
                                 </dl>
                              </li>
                           </ul>
                  </xsl:if>
                        <!-- ========== Resources Details ========= -->
                        <ul class="blockList">
                           <li class="blockList">

                              <xsl:text disable-output-escaping="yes"> <![CDATA[<a name="method_detail"></a>]]></xsl:text>
                              <h3>Resources Detail</h3>
                              <xsl:for-each select="wadl:resources">
                       <xsl:call-template name="processResources"/>
                     </xsl:for-each>
                           </li>
                        </ul>
                     </li>
                  </ul>
               </div>
            </div>
         </body>
      </html>
    </xsl:template>
   <!-- Generate a top-level summary of the resources -->
   <xsl:template name="processResources">
   
      <a>
         <xsl:attribute name="name"><xsl:call-template name="fetchId"/></xsl:attribute>
         <xsl:text disable-output-escaping="yes"><![CDATA[ ]]></xsl:text> <!-- Just need to ensure the a tag has a distinct close tag -->
      </a>
      <ul class="blockListLast">
         <li class="blockList">
            <h4>
               <xsl:value-of select="pxsltu:hypernizeURI(@base)" disable-output-escaping="yes"/>
            </h4>
            <!--            <pre>Do we need anything here?</pre> -->
            <div class="block">
               <xsl:call-template name="fetchDocumentation"/>
            </div>
            <!-- Now we need to look up each in turn -->
            <h3>Resources:</h3>
            <xsl:for-each select="wadl:resource">
               <xsl:call-template name="processResource">
                  <xsl:with-param name="parentPath" select="../@base"/>
                  <!-- Deference the resource type if required -->
                  <xsl:with-param name="resourceType" select="."/>
               </xsl:call-template>
             </xsl:for-each>
         </li>
      </ul>
   
   </xsl:template>
   <!-- Display all the resources -->
   <xsl:template name="processResource">
   
     <!-- The parent path, used to derrive the current path -->
     <xsl:param name="parentPath" required="yes"/>
     <!-- This is the current resource to work on, could be a deferenced type
         hence this explicity parameter -->
     <xsl:param name="resourceType" required="yes"/>


     <!-- This allows us to walk back up the tree when we have object deferences -->
     <xsl:param name="resourceObjectPath" select="empty"/> 


<!--     <xsl:message>Resource Type <xsl:value-of select="local-name($resourceType)" />
     </xsl:message> -->


<!--    <xsl:message>Document root of resource type
    <xsl:value-of select="local-name(.)"/>
    <xsl:value-of select="local-name($resourceType/..)"/>
    <xsl:value-of select="local-name($resourceType/../..)"/>
    <xsl:value-of select="local-name($resourceType/../../..)"/>
    <xsl:value-of select="local-name(root($resourceType))"/>
    </xsl:message> -->

     
     <!-- This is the current resource to work on, could be a deferenced type
         hence this explicity parameter -->
     <xsl:variable name="resourceTypes" select="a:lookupResourceReferences($resourceType)"/>


<!--    <xsl:message>Document root of resource types 
    
        <xsl:for-each select="$resourceTypes/*" >
        
    <xsl:value-of select="local-name(.)"/>
    <xsl:value-of select="local-name(root(.))"/>
    </xsl:for-each>
    </xsl:message> -->
     
     
     <!-- Debug the resource ObjectPath -->
<!--     <xsl:message>Resource Types
       <xsl:for-each select="$resourceTypes/*">
         Node type <xsl:value-of select="local-name(.)" />
         @id and @type <xsl:value-of select="@id" /> <xsl:value-of select="@type" />
       </xsl:for-each>
     </xsl:message> -->
     
     <!-- Work out what the current path is -->
     <xsl:variable name="currentPath">

        <xsl:choose>
            <xsl:when test="ends-with($parentPath, '/') and not(starts-with($resourceType/@path, '/'))">
              <xsl:value-of select="concat($parentPath, $resourceType/@path)"/>
           </xsl:when>
            <xsl:when test="not(ends-with($parentPath, '/')) and starts-with($resourceType/@path, '/')">
              <xsl:value-of select="concat($parentPath, $resourceType/@path)"/>
           </xsl:when>
            <xsl:when test="ends-with($parentPath, '/') and starts-with($resourceType/@path, '/')">
              <xsl:value-of select="concat($parentPath, substring($resourceType/@path,2))"/>
           </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="concat($parentPath,'/', $resourceType/@path)"/>
           </xsl:otherwise>
         </xsl:choose>
     </xsl:variable>
   
   
     <a>
         <xsl:attribute name="href"><xsl:call-template name="fetchId" /></xsl:attribute>
         <xsl:text disable-output-escaping="yes"><![CDATA[ ]]></xsl:text> <!-- Just need to ensure the a tag has a distinct close tag -->
      </a>
     <ul class="blockListLast">
         <li class="blockList">
            <h4><xsl:text disable-output-escaping="yes"> <![CDATA[<div class="clearing"></div>]]></xsl:text><button class="expandButton"><xsl:text disable-output-escaping="yes">&amp;</xsl:text>nbsp;</button> <xsl:value-of select="$resourceType/@path"/>
            </h4>
            <div class="collapsible">
            <tt>
               <xsl:value-of select="pxsltu:hypernizeURI($currentPath)" disable-output-escaping="yes"/>
            </tt>
            <div class="block">
               <xsl:call-template name="fetchDocumentation"/>
            </div>
            <!-- Display the parameters -->
            <dd>
               <dl>
                  <xsl:call-template name="fetchParameters">
                     <xsl:with-param name="context" select="$resourceTypes"/>
                     <!-- We don't want to roll up parameters, just display the ones
                          for the resource, un-like for methods and similar -->
                     <xsl:with-param name="resourceObjectPath" select="empty"/>
                  </xsl:call-template>
               </dl>
            </dd>
            <!-- Process methods -->
            <xsl:if test="count($resourceTypes/*/wadl:method)>0">

        <h3>Methods:</h3>

        <xsl:for-each select="$resourceTypes/*/wadl:method">

	   <xsl:sort select="a:lookupReference(.)/node()/@name"/>
   
           <!-- look up the method, could be a deference -->
           <xsl:variable name="method" select="a:lookupReference(.)/node()"/>    
        
           <xsl:call-template name="processMethod">
                     <xsl:with-param name="method" select="$method"/>
                     <xsl:with-param name="resourceObjectPath" select="$resourceObjectPath"/>
                     <xsl:with-param name="resourceTypes" select="$resourceTypes"/>
                     <xsl:with-param name="currentPath" select="$currentPath"/>
                  </xsl:call-template>
         
        </xsl:for-each>
     </xsl:if>
            <!-- Process sub resources -->
            <xsl:if test="count($resourceTypes/*/wadl:resource)>0">

        <h3>Resources:</h3>
     
         <xsl:for-each select="$resourceTypes/*/wadl:resource">

	    <xsl:sort select="a:lookupReference(.)/node()/@path"/>

            <xsl:call-template name="processResource">
                     <xsl:with-param name="parentPath" select="$currentPath"/>
                     <!-- We don't dereference the resource, this is done in resource types -->
                     <xsl:with-param name="resourceType" select="."/>
                     <xsl:with-param name="resourceObjectPath" select="$resourceTypes/* , $resourceObjectPath"/>
                  </xsl:call-template>
         </xsl:for-each>   
     </xsl:if>
         </div>
         </li>
      </ul>
      
   </xsl:template>
   <!-- Generate an entry for top level methods -->
   <xsl:template name="processMethod">

      <!-- This is the explicity method to work on, might be deferences -->
      <xsl:param name="method" required="yes"/>
 
      <!-- This allows us to walk back up the tree when we have object deferences -->
      <xsl:param name="resourceObjectPath" required="yes"/> 

      <!-- Only walk the current object for responses -->
      <xsl:param name="resourceTypes" required="yes"/> 

      <!-- This allows us to display the tester properly -->
      <xsl:param name="currentPath" required="yes"/> 

      <xsl:variable name="name" select="$method/@name"/>


<!--    <xsl:message>Document root of method <xsl:value-of select="local-name(root($method))"/> </xsl:message> -->

   
      <a>
         <xsl:attribute name="name"><xsl:call-template name="fetchId" /></xsl:attribute>
         <xsl:text disable-output-escaping="yes"><![CDATA[ ]]></xsl:text> <!-- Just need to ensure the a tag has a distinct close tag -->
      </a>
      <ul class="blockListLast">
         <li class="blockList">
            <h4><xsl:text disable-output-escaping="yes"> <![CDATA[<div class="clearing"></div>]]></xsl:text><button class="expandButton"><xsl:text disable-output-escaping="yes">&amp;</xsl:text>nbsp;</button><xsl:value-of select="$name"/>
            <!-- testing additions only added if there is a URI avaliable -->
            <xsl:if test="string-length($testButtonUri)>0">
          <div style="float: right">
                  <form method="get" action="{$testButtonUri}">

                     <!-- walk up and down the tree looking for parameters -->

                     <xsl:variable name="unfilteredParams">
                        <xsl:sequence select="$method/wadl:request/wadl:param" />
                        <xsl:sequence select="$resourceTypes/*/wadl:param" />
                        <xsl:sequence select="$resourceObjectPath/wadl:param" />
                     </xsl:variable>


                    
                    <!-- <xsl:message>Unreferenced the parameters
                       <xsl:for-each select="$unfilteredParams/*" >
                          <xsl:value-of select="local-name(.)"/> - <xsl:value-of select="@name"/>
                       </xsl:for-each>
                       </xsl:message> -->


                     <!-- Template in some query parameters -->
                     <xsl:variable name="queryParams"
                                   select="a:lookupReferences($unfilteredParams/*)/wadl:param[@style='query']"/>
                     <xsl:variable name="queryParam" select="string-join($queryParams/@name,'=X&amp;')"/>
                     <!--  Not sure why this doesn't work        select="string-join($queryParams/concat(@name,'=X'),'&amp;')"/> -->
                     <input type="hidden" name="queryParam" value="{$queryParam}"/>
                     <xsl:variable name="queryParamUri">
                   <xsl:choose>
                           <xsl:when test='string-length($queryParam)=0'>
                      </xsl:when>
                           <xsl:otherwise>
                         <xsl:value-of select="concat('?',$queryParam, '=X')"/>
                      </xsl:otherwise>
                        </xsl:choose> 
                </xsl:variable>
                     <!-- Template in some matrix parameters, only on the resource elements as per the WADL
                     spec but under request because of JERSEY-1336 -->
                     
                     
                     
                     <xsl:variable name="matrixParams"
                                   select="a:lookupReferences($unfilteredParams/*)/wadl:param[@style='matrix']"/>


                     <!--<xsl:message>Checking the parameters
                       <xsl:for-each select="$matrixParams" >
                          <xsl:value-of select="local-name(.)"/> - <xsl:value-of select="@name"/>
                       </xsl:for-each>
                       </xsl:message> -->
                       


                     <!-- boolean parameters either exist or not, don't have a value -->
                     <xsl:variable name="matrixParam1"
                                   select="string-join($matrixParams[contains(@type,'boolean')]/@name,'=X;')"/>
                     <xsl:variable name="matrixParam2"
                                   select="string-join($matrixParams[not(contains(@type,'boolean'))]/@name,'=X;')"/>
                     <xsl:variable name="matrixParamUri">
                   <xsl:choose>
                           <xsl:when test='string-length($matrixParam1)=0 and string-length($matrixParam2)=0'>
                         <xsl:value-of select="''"/>
                     </xsl:when>
                           <xsl:when test='string-length($matrixParam1)>0 and string-length($matrixParam2)=0'>
                         <xsl:value-of select="concat(';',$matrixParam1, '=X')"/>
                     </xsl:when>
                           <xsl:when test='string-length($matrixParam1)=0 and string-length($matrixParam2)>0'>
                         <xsl:value-of select="concat(';',$matrixParam2,'=X')"/>
                     </xsl:when>
                           <xsl:otherwise>
                         <xsl:value-of select="concat(';',$matrixParam1,';',$matrixParam2,'=X')"/>
                      </xsl:otherwise>
                        </xsl:choose>
               </xsl:variable>
               
                     <xsl:variable name="uriTemplateTypes" select="string-join($resourceObjectPath/*/wadl:param[@style='template']/concat(@name,'=',@type), ';')"/> 
               
                     <input type="hidden" name="uriTemplateTypes" value="{$uriTemplateTypes}"/>
                     <input type="hidden" name="matrixParamUri" value="{$matrixParamUri}"/>
                     <!-- The other standard parameters -->
                     <input type="hidden" name="uriTemplate" value="{$currentPath}{$matrixParamUri}{$queryParamUri}"/>
                     <input type="hidden" name="method" value="{$name}"/>
                     <xsl:variable name="requestType"
                                   select="a:lookupReferences($method/wadl:request/wadl:representation)/wadl:representation/@mediaType"/>
                     <xsl:variable name="responseType"
                                   select="string-join(a:lookupReferences($method/wadl:response/wadl:representation)/wadl:representation/@mediaType,',')"/>
                     <input type="hidden" name="requestType" value="{$requestType}"/>
                     <input type="hidden" name="responseType" value="{$responseType}"/>
                     <input class="testButton" type="submit" value="Test"/><xsl:text disable-output-escaping="yes">&amp;</xsl:text>nbsp;<xsl:text disable-output-escaping="yes">&amp;</xsl:text>nbsp;

                  </form>
               </div>
       </xsl:if>
            <!-- End of testing additions -->
            </h4>
            <div class="collapsible">
            <!--      <pre>Do we need anything here?</pre> -->
            <div class="block">
               <xsl:call-template name="fetchDocumentation"/>
            </div>
            <!-- Now we need to look up parameters and request/response values in turn -->
            <dl>
               <!-- Request stuff -->
               <xsl:for-each select="$method/wadl:request">
            <dt>
                     <span class="strong">Request:</span>
                  </dt>
            
            <dd>
                     <dl>
                        <xsl:call-template name="fetchParameters">
                           <xsl:with-param name="context">
                              <xsl:sequence select="."/>
                           </xsl:with-param>
                           <xsl:with-param name="resourceObjectPath" select="$resourceObjectPath"/>
                           <xsl:with-param name="resourceTypes" select="$resourceTypes"/>
                        </xsl:call-template>
                        <xsl:call-template name="fetchRepresentations">
                           <xsl:with-param name="context" select="."/>
                           <xsl:with-param name="resourceObjectPath" select="$resourceObjectPath"/>
                        </xsl:call-template>
                     </dl>
                  </dd>
         </xsl:for-each>
               <xsl:for-each select="$method/wadl:response">

            <dt>
                     <span class="strong">
                        Response:
                        <xsl:if test="@status">[<xsl:value-of select="@status"/>]</xsl:if>
                     </span>
                  </dt>
            
            <dd>
                     <dl>
                        <xsl:call-template name="fetchParameters">
                           <xsl:with-param name="context">
                              <xsl:sequence select="."/>
                           </xsl:with-param>
                           <xsl:with-param name="resourceObjectPath" select="$resourceTypes"/>
                        </xsl:call-template>
                        <xsl:call-template name="fetchRepresentations">
                           <xsl:with-param name="context" select="."/>
                           <xsl:with-param name="resourceObjectPath" select="$resourceTypes"/>
                        </xsl:call-template>
                     </dl>
                  </dd>
         </xsl:for-each>
            </dl>
            </div>
         </li>
      </ul>
   
   </xsl:template>
   <!-- Simple function to return the title for the current element
         if it is avaliable -->
   <xsl:template name="fetchTitle">
      <xsl:choose>
         <xsl:when test="wadl:doc/@title">
           <xsl:value-of select="wadl:doc/@title"/>
        </xsl:when>
         <xsl:otherwise>Service</xsl:otherwise>
      </xsl:choose>
    </xsl:template>
   <!-- This code assumes it will be in a dl block -->
   <xsl:template name="fetchParameters">
    

      <!-- This is the object or objects to fetch the parameters for -->
      <xsl:param name="context" required="yes"/> 

      <!-- This allows us to walk back up the tree when we have object deferences -->
      <xsl:param name="resourceObjectPath" required="yes"/> 
    
      <xsl:param name="resourceTypes" select="()" required="no"/>

      <!-- We need to de-reference each parameter before we can test it 
          otherwise we won't know what style it is extracted to here
           so that we don't do this twice in the if and the for-each statement
           later on -->    
      <xsl:variable name="totalParamList" select="$context/*/wadl:param , $resourceTypes/*/wadl:param , $resourceObjectPath/wadl:param"/>
      <xsl:variable name="filteredParamList" select="a:lookupReferences($totalParamList)"/> 

      <xsl:choose>
         <xsl:when test="count($filteredParamList/wadl:param)">
         
            <dt>
               <span class="strong">Parameters:</span>
            </dt>
            
            <xsl:for-each select="$filteredParamList/wadl:param">
              <dd>
                  <code>
                     <xsl:if test="@type"><xsl:value-of select="substring-after(@type, ':')"/><xsl:value-of select="' '"/></xsl:if>
                     <xsl:value-of select="@name"/>
                     <xsl:if test="@default"> = <xsl:value-of select="@default"/> </xsl:if>

                     <xsl:if test="./wadl:option">
                       {<xsl:value-of select="string-join(./wadl:option/@value, '|')"/>}
                     </xsl:if>

                  </code>
                  [
                  <xsl:value-of select="@style"/>
                  ]
                  <xsl:if test="@required eq 'true'">@required<xsl:text disable-output-escaping="yes">&amp;</xsl:text>nbsp;</xsl:if>
                  <xsl:if test="@repeating eq 'true'">@repeating<xsl:text disable-output-escaping="yes">&amp;</xsl:text>nbsp;</xsl:if>
                  <xsl:if test="@fixed eq 'true'">@fixed<xsl:text disable-output-escaping="yes">&amp;</xsl:text>nbsp;</xsl:if>



                  <!-- TODO display something for required and default value -->
                  <xsl:call-template name="fetchDocumentation"/>
               </dd>
            </xsl:for-each>  
         </xsl:when>
         <xsl:otherwise>
<!--            <dt><span class="strong">No Parameters</span></dt> -->
         </xsl:otherwise>
      </xsl:choose>      
    
    </xsl:template>
   <!-- This code assumes it will be in a dl block -->
   <xsl:template name="fetchRepresentations">
    

      <!-- This is the object to fetch the parameters for -->
      <xsl:param name="context" required="yes"/> 

      <!-- This allows us to walk back up the tree when we have object deferences -->
      <xsl:param name="resourceObjectPath" required="yes"/> 


      <!-- We need to de-reference each parameter before we can test it 
          otherwise we won't know what style it is extracted to here
           so that we don't do this twice in the if and the for-each statement
           later on -->    
      <xsl:variable name="totalRepresentationList" select="$context/wadl:representation"/>
      <xsl:variable name="filteredRepresentationList" select="a:lookupReferences($totalRepresentationList)"/> 

      <xsl:choose>
         <xsl:when test="count($filteredRepresentationList/wadl:representation)">
         
            <dt>
               <span class="strong">Representations:</span>
            </dt>
            
            <xsl:for-each select="$filteredRepresentationList/wadl:representation">
              <dd>
                  <code>
                     <xsl:value-of select="@mediaType"/>
                     <xsl:if test="@element"> : 
                         <xsl:call-template name="resolveQName">
                               <xsl:with-param name="name" select="@element" />
                               <xsl:with-param name="context" select="." />
<!--                               <xsl:with-param name="document" select="root($context)"/> -->
                                <!-- For the moment just support single document imports
                                     as you cannot get a document from a element in a sequence -->
                                <xsl:with-param name="document" select="$root"/>
                        </xsl:call-template>
                      </xsl:if>
                      <xsl:if test="@wadljson:describedby"> : 
                         <a href="{@wadljson:describedby}" onclick="return handleHref('{@wadljson:describedby}');">
                         <!--<a href="{@wadljson:describedby}">-->
                            <xsl:value-of select="@wadljson:describedby" />
                         </a>
                      </xsl:if>
                  </code>
                  <xsl:text disable-output-escaping="yes">&amp;</xsl:text>nbsp;
                  <xsl:call-template name="fetchDocumentation"/>
                  <!-- Display the parameters -->
                  <dl>
                     <xsl:call-template name="fetchParameters">
                       <xsl:with-param name="context">
                          <xsl:sequence select="."/>
                       </xsl:with-param>
                       <xsl:with-param name="resourceObjectPath" select="$resourceObjectPath"/>
                     </xsl:call-template>
                 </dl>
               </dd>
            </xsl:for-each>  
         </xsl:when>
         <xsl:otherwise>
            <dt>
               <span class="strong">No Representation</span>
            </dt>
         </xsl:otherwise>
      </xsl:choose>      
    
    </xsl:template>
   <!-- Work out whether a element can be referenced -->
   <xsl:template name="resolveQName">

       <xsl:param name="name" as="xsd:string"/>
       <xsl:param name="context" as="node"/>
       <xsl:param name="document" as="node()"/>

<!--      <xsl:message>Resolve qName type <xsl:value-of select="$qname"/></xsl:message>  -->



       <xsl:variable name="prefix" as="xsd:string" select="substring-before($name, ':')" />
       <xsl:variable name="string-localname" as="xsd:string" select="substring-after($name, ':')" />
       
       <!-- If the prefix is invalid the resolve-QNAme function throws and exception and becuase we 
            are not allowed to have nice things such as exception handling we have to check
            first that the prefix is avaliable -->
       <xsl:variable name="qname" select="if (contains(string-join(in-scope-prefixes(.), '|'), $prefix)) then resolve-QName($name,$context) else QName('', $string-localname)"  as="xsd:QName" />

       <xsl:variable name="ns-uri" select="namespace-uri-from-QName($qname)"/>
       <xsl:variable name="localname" select="local-name-from-QName($qname)"/> 


<!--       <xsl:message>namespace <xsl:value-of select="$ns-uri"/></xsl:message> 
       <xsl:message>localname <xsl:value-of select="$localname"/></xsl:message> -->
       
      
<!--        <xsl:message>Looking at document element <xsl:value-of select="local-name($document)"/>
        
         <xsl:for-each select="$document/*">
            <xsl:value-of select="local-name($document)"/>
         </xsl:for-each>

        
        </xsl:message>  -->



      <xsl:choose>
         <xsl:when test="$ns-uri='http://www.w3.org/2001/XMLSchema' or $ns-uri='http://www.w3.org/2001/XMLSchema-instance'">
               <a href="http://www.w3.org/TR/xmlschema-2/#{$localname}" onclick="return handleHref('http://www.w3.org/TR/xmlschema-2/#{$localname}');">
               <!--<a href="http://www.w3.org/TR/xmlschema-2/#{$localname}">-->
               <xsl:value-of select="$localname"/>
            </a>
   <!--            <xsl:message>
                  Found in XML Schema
               </xsl:message>-->
           </xsl:when>
         <!-- TODO do some testing of this, this is for xml schema that live in the current document -->
         <xsl:when test="/wadl:application/wadl:grammars/xsd:schema[@targetNamespace = $ns-uri]/xsl:element[@name = $localname]">
               <!--<xsl:message>
                  Found in Embedded schema
               </xsl:message> -->
               <a href="#{$localname}" onclick="return handleHref('#{$localname}');">              
               <!--<a href="#{$localname}">-->
               <xsl:value-of select="$localname"/>
            </a>
           </xsl:when>
         <!-- Possibly incrediably expensive query for find out which schema file this is in -->
         <!-- Put back when bug 14215372 is resolved        
        see also bug 14215407 	-->
         <xsl:when test="document($document/wadl:application/wadl:grammars/wadl:include/@href, $document)/xsd:schema[@targetNamespace = $ns-uri or (not(@targetNamespace) and $ns-uri eq '')] /xsd:element[@name = $localname]">
   
              <xsl:variable name="documentURI" 
                            select="$document/wadl:application/wadl:grammars/wadl:include[document(@href, $document)/xsd:schema[@targetNamespace = $ns-uri  or (not(@targetNamespace) and $ns-uri eq '')]/xsd:element[@name = $localname]]/@href" />
   
<!--               <xsl:message>
                  Found in External Schema <xsl:value-of select="$documentURI"/>#<xsl:value-of select="$localname"/>
               </xsl:message>-->

               <a href="{$documentURI}#{$localname}" onclick="return handleHref('{$documentURI}#{$localname}');" title="{$ns-uri}:{$localname}">
               <!--<a href="#{$localname}" title="{$ns-uri}:{$localname}toto">-->
               <xsl:value-of select="$localname"/>
            </a> 
           </xsl:when>
         <xsl:otherwise>
           
<!--               <xsl:message>Checking out external grammars</xsl:message>
               <xsl:for-each select="$document//wadl:application/wadl:grammars/wadl:include">
                  <xsl:message><xsl:value-of select="@href"/></xsl:message>
                  <xsl:variable name="externalDoc" select="document(@href,$document)"/>
                  <xsl:message><xsl:value-of select="$externalDoc/xsd:schema/@targetNamespace"/></xsl:message>
                  
                  
               </xsl:for-each>
    -->       
               <xsl:value-of select="$qname"/>
           </xsl:otherwise>
      </xsl:choose>    
    </xsl:template>
   <!-- Simple function to return the id for the current element or generate
         it if required -->
   <xsl:template name="fetchId">
       <xsl:choose>
         <xsl:when test="@id">
           <xsl:value-of select="@id"/>
        </xsl:when>
         <xsl:otherwise><xsl:value-of select="generate-id()"/></xsl:otherwise>
      </xsl:choose>
    </xsl:template>
   <!-- Simple function to return documents, html and all for the current element -->
   <xsl:template name="fetchDocumentation">
       <span class="documentation">
        <xsl:choose>
           <!-- If we have child elements then copy them in -->
           <xsl:when test="count(wadl:doc/*|wadl:doc/text()) > 0">
             <xsl:copy-of select="wadl:doc/*|wadl:doc/text()"/>
          </xsl:when>
           <!-- Otherwise use the title -->
           <xsl:when test="wadl:doc/@title">
             <xsl:value-of select="wadl:doc/@title"/>
          </xsl:when>
           <!-- Or just leave a blank -->
           <xsl:otherwise></xsl:otherwise>
        </xsl:choose>
      </span>
    </xsl:template>
</xsl:stylesheet>
