<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="html" encoding="UTF-8" indent="yes" />
    <xsl:strip-space elements="*" />
    <xsl:preserve-space elements="screen programlisting" />
    <xsl:param name="ulink.target" />

    <!-- If true, img elements will be output with external urls. If false, a standard placeholder will be used -->
    <xsl:param name="externalImages" select="'true'"/>

    <!-- The start of the url that is used to get the images from the server -->
    <!--<xsl:param name="externalImagesUrlPrefix" select="'http://skynet.usersys.redhat.com:8080/TopicIndex/seam/resource/rest/1/image/get/raw/'"/>-->
    <xsl:param name="externalImagesUrlPrefix" select="'ImageFileDisplay.seam?imageFileId='"/>

    <!-- The end of the url that is used to get the images from the server -->
    <xsl:param name="externalImagesUrlSuffix" select="''"/>

    <!-- Bad hack. Need to remove later. -->
    <xsl:template match="title"></xsl:template>
    <xsl:template match="term"></xsl:template>

    <xsl:template match="/">
        <html>
            <head>
                <link rel="stylesheet" type="text/css" href="stylesheet/docbook.css" />
            </head>
            <body>
                <xsl:apply-templates select="section" />
            </body>
        </html>
    </xsl:template>
    <!--<xsl:template match="article"> <div id="{@id}" class="docbookArticle"> <h1 class="title"> <xsl:value-of select="title" /> </h1> <xsl:apply-templates select="section"/> </div> </xsl:template> -->
    <xsl:template match="section">
        <xsl:variable name="secdepth">
            <xsl:value-of select="count(ancestor::*)" />
        </xsl:variable>
        <xsl:element name="div">
            <xsl:if test="@id">
                <xsl:attribute name="id">
                    <xsl:value-of select="@id" />
                </xsl:attribute>
            </xsl:if>
            <xsl:attribute name="class">section</xsl:attribute>
            <xsl:choose>
                <xsl:when test="$secdepth = 1">
                    <h2 class="docbookSectionTitle">
                        <xsl:value-of select="title" />
                    </h2>
                </xsl:when>
                <xsl:when test="$secdepth = 2">
                    <h3 class="docbookSectionTitle">
                        <xsl:value-of select="title" />
                    </h3>
                </xsl:when>
                <xsl:when test="$secdepth = 3">
                    <h4 class="docbookSectionTitle">
                        <xsl:value-of select="title" />
                    </h4>
                </xsl:when>
                <xsl:when test="$secdepth = 4">
                    <h5 class="docbookSectionTitle">
                        <xsl:value-of select="title" />
                    </h5>
                </xsl:when>
                <!-- Following should never execute. Here as a fail safe. -->
                <xsl:otherwise>
                    <h2 class="docbookSectionTitle">
                        <xsl:value-of select="title" />
                    </h2>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates />
            <div class="docbookFootnotes">
                <hr width="100" align="left" />
                <xsl:apply-templates select="//footnote" mode="footnote"/>
            </div>
        </xsl:element>
    </xsl:template>
    <xsl:template match="varlistentry">
        <xsl:element name="div">
            <xsl:if test="@id">
                <xsl:attribute name="id">
                    <xsl:value-of select="@id" />
                </xsl:attribute>
            </xsl:if>
            <xsl:attribute name="class">docbookVarlistentry</xsl:attribute>
            <h5 class="docbookVarlistentryTerm">
                <xsl:value-of select="term" />
            </h5>
            <xsl:apply-templates />
        </xsl:element>
    </xsl:template>

    <xsl:template match="formalpara">
        <xsl:element name="div">
            <xsl:if test="@id">
                <xsl:attribute name="id">
                    <xsl:value-of select="@id" />
                </xsl:attribute>
            </xsl:if>
            <xsl:attribute name="class">formalpara</xsl:attribute>
            <h5 class="formalParaTitle">
                <xsl:value-of select="title" />
            </h5>
            <xsl:apply-templates />
        </xsl:element>
    </xsl:template>

    <xsl:template match="para">
        <p class="docbookPara">
            <xsl:apply-templates />
        </p>
    </xsl:template>
    <xsl:template match="emphasis">
        <span class="docbookEmphasis">
            <xsl:apply-templates />
        </span>
    </xsl:template>
    <xsl:template match="remark">
        <p class="docbookRemark">
            <xsl:apply-templates />
        </p>
    </xsl:template>
    <xsl:template match="note">
        <div class="docbookNote">
            <h2 class="label docbookNoteTitle">
                <xsl:value-of select="title" />
            </h2>
            <div class="docbookNoteContents">
                <xsl:apply-templates />
            </div>
        </div>
    </xsl:template>
    <xsl:template match="important">
        <div class="important docbookImportant">
            <h2 class="label docbookImportantTitle">
                <xsl:value-of select="title" />
            </h2>
            <div class="docbookImportantContents">
                <xsl:apply-templates />
            </div>
        </div>
    </xsl:template>
    <xsl:template match="warning">
        <div class="warning docbookWarning">
            <h2 class="label docbookWarningTitle">
                <xsl:value-of select="title" />
            </h2>
            <div class="docbookWarningContents">
                <xsl:apply-templates />
            </div>
        </div>
    </xsl:template>
    <xsl:template match="itemizedlist">
        <div class="docbookItemizedList">
            <h6 class="itemizedlistitle docbookItemizedListTitle">
                <xsl:value-of select="title" />
            </h6>
            <ul class="docbookItemizedListContainer itemizedlist">
                <xsl:apply-templates />
            </ul>
        </div>
    </xsl:template>
    <xsl:template match="listitem">
        <li class="docbookListItem">
            <xsl:apply-templates />
        </li>
    </xsl:template>
    <xsl:template match="simplelist">
        <table class="docbookSimpleList">
            <xsl:apply-templates />
        </table>
    </xsl:template>
    <xsl:template match="member">
        <tr>
            <td class="docbookMember">
                <xsl:apply-templates />
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="procedure">
        <div class="docbookProcedure">
            <h6 class="docbookProcedureTitle">
                <xsl:value-of select="title" />
            </h6>
            <ol class="docbookProcedureContainer">
                <xsl:apply-templates />
            </ol>
        </div>
    </xsl:template>
    <xsl:template match="substeps">
        <div class="docbookSubSteps">
            <ol class="docbookSubStepsContainer">
                <xsl:apply-templates />
            </ol>
        </div>
    </xsl:template>
    <xsl:template match="step">
        <li class="docbookStep">
            <h6 class="docbookStepTitle">
                <xsl:value-of select="title" />
            </h6>
            <xsl:apply-templates />
        </li>
    </xsl:template>
    <xsl:template match="screen">
        <pre class="docbookScreen screen">
            <xsl:apply-templates />
        </pre>
    </xsl:template>
    <!-- jwulf 5 July 2012 -->
    <xsl:template match="programlisting">
        <pre class="docbookProgramListing">
            <xsl:apply-templates />
        </pre>
    </xsl:template>
    <!-- ********************** -->
    <!-- Inline tags below this -->
    <!-- ********************** -->
    <xsl:template match="sgmltag">
        <span class="docbookSGMLTag sgmltag-{@class}">
            <xsl:apply-templates />
        </span>
    </xsl:template>
    <xsl:template match="emphasis">
        <span class="docbookEmphasis">
            <xsl:apply-templates />
        </span>
    </xsl:template>
    <!-- jwulf 5 July 2012 -->
    <xsl:template match="firstterm">
        <span class="docbookFirstTerm">
            <xsl:apply-templates />
        </span>
    </xsl:template>
    <!-- jwulf 5 July 2012 -->
    <xsl:template match="literal">
        <span class="docbookLiteral">
            <xsl:apply-templates />
        </span>
    </xsl:template>
    <xsl:template match="interfacename">
        <code class="docbookInterfaceName">
            <xsl:apply-templates />
        </code>
    </xsl:template>
    <xsl:template match="filename">
        <code class="docbookFileName">
            <xsl:apply-templates />
        </code>
    </xsl:template>
    <xsl:template match="classname">
        <code class="docbookClassName">
            <xsl:apply-templates />
        </code>
    </xsl:template>
    <xsl:template match="constant">
        <code class="docbookConstant">
            <xsl:apply-templates />
        </code>
    </xsl:template>
    <xsl:template match="function">
        <code title="docbookFunction" class="function">
            <xsl:apply-templates />
        </code>
    </xsl:template>
    <xsl:template match="parameter">
        <code class="docbookParameter parameter">
            <xsl:apply-templates />
        </code>
    </xsl:template>
    <xsl:template match="replaceable">
        <code class="docbookReplaceable replaceable">
            <xsl:apply-templates />
        </code>
    </xsl:template>
    <xsl:template match="varname">
        <code class="docbookVarname varname">
            <xsl:apply-templates />
        </code>
    </xsl:template>
    <xsl:template match="structfield">
        <code class="docbookStructfield structfield">
            <xsl:apply-templates />
        </code>
    </xsl:template>
    <xsl:template match="systemitem">
        <code title="docbookSystemItem" class="systemitem">
            <xsl:apply-templates />
        </code>
    </xsl:template>
    <xsl:template match="package">
        <span title="docbookPackage" class="package">
            <xsl:apply-templates />
        </span>
    </xsl:template>
    <xsl:template match="command">
        <span class="docbookCommand command">
            <xsl:apply-templates />
        </span>
    </xsl:template>
    <xsl:template match="option">
        <span class="docbookOption option">
            <xsl:apply-templates />
        </span>
    </xsl:template>
    <xsl:template match="userinput">
        <code title="docbookUserInput" class="userinput">
            <xsl:apply-templates />
        </code>
    </xsl:template>
    <xsl:template match="computeroutput">
        <code title="docbookComputerOutput" class="computeroutput">
            <xsl:apply-templates />
        </code>
    </xsl:template>
    <xsl:template match="prompt">
        <code title="docbookPrompt" class="prompt">
            <xsl:apply-templates />
        </code>
    </xsl:template>
    <xsl:template match="subscript">
        <sub title="docbookSubscript">
            <xsl:apply-templates />
        </sub>
    </xsl:template>
    <xsl:template match="superscript">
        <sup title="docbookSuperscript">
            <xsl:apply-templates />
        </sup>
    </xsl:template>
    <!-- jwulf 5 July 2012 -->
    <xsl:template match="code">
        <span class="docbookCode docbookCode">
            <xsl:apply-templates />
        </span>
    </xsl:template>
    <!-- jwulf 5 July 2012 copied from publican xhtml-common -->
    <xsl:template match="ulink" name="ulink">
        <xsl:param name="url" select="@url" />
        <xsl:variable name="link">
            <a xmlns="http://www.w3.org/1999/xhtml">
                <xsl:if test="@id or @xml:id">
                    <xsl:attribute name="id">
                        <xsl:value-of select="(@id|@xml:id)[1]" />
                    </xsl:attribute>
                </xsl:if>
                <xsl:attribute name="href">
                    <xsl:value-of select="$url" />
                </xsl:attribute>
                <xsl:if test="$ulink.target != ''">
                    <xsl:attribute name="target">
                        <xsl:value-of select="$ulink.target" />
                    </xsl:attribute>
                </xsl:if>
                <xsl:if test="@role">
                    <xsl:apply-templates select="." mode="class.attribute">
                        <xsl:with-param name="class" select="@role" />
                    </xsl:apply-templates>
                </xsl:if>
                <xsl:choose>
                    <xsl:when test="count(child::node())=0">
                        <xsl:value-of select="$url" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates />
                    </xsl:otherwise>
                </xsl:choose>
            </a>
        </xsl:variable>
        <xsl:copy-of select="$link" />
    </xsl:template>
    <!-- ******************** -->
    <!-- Meta tags below this -->
    <!-- ******************** -->
    <!-- Do nothing for now -->
    <xsl:template match="indexterm"></xsl:template>

    <xsl:template match="example">
        <xsl:element name="div">
            <xsl:if test="@id">
                <xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute>
            </xsl:if>
            <xsl:attribute name="class">docbookExample</xsl:attribute>
            <h6 class="docbookExampleTitle">
                <xsl:value-of select="title" />
            </h6>
            <xsl:apply-templates />
        </xsl:element>
    </xsl:template>
    <xsl:template match="figure">
        <xsl:element name="div">
            <xsl:if test="@id">
                <xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute>
            </xsl:if>
            <xsl:attribute name="class">docbookFigure</xsl:attribute>
            <xsl:apply-templates />
            <h6 class="docbookFigureTitle">
                <xsl:value-of select="title" />
            </h6>
        </xsl:element>
    </xsl:template>

    <xsl:template match="mediaobject">
        <xsl:element name="div">
            <xsl:if test="@id">
                <xsl:attribute name="id"><xsl:value-of select="@id" /></xsl:attribute>
            </xsl:if>
            <xsl:attribute name="class">docbookMediaObject</xsl:attribute>
            <xsl:attribute name="align">center</xsl:attribute>
            <xsl:apply-templates select="imageobject"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="imageobject">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="imagedata">
        <xsl:variable name="imageid">
            <xsl:call-template name="string-replace-all">
                <xsl:with-param name="text" select="@fileref"/>
                <xsl:with-param name="replace" select="'images/'"/>
                <xsl:with-param name="by" select="''"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="imageidNoExtension">
            <xsl:value-of select="substring-before($imageid,'.')" />
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$externalImages='true'">
                <xsl:element name="img">
                    <xsl:attribute name="src">
                        <xsl:value-of select="$externalImagesUrlPrefix" />
                        <xsl:value-of select="$imageidNoExtension" />
                        <xsl:value-of select="$externalImagesUrlSuffix" />
                    </xsl:attribute>
                    <xsl:if test="../../textobject/phrase">
                        <xsl:attribute name="alt">
                            <xsl:value-of select="../../textobject/phrase" />
                        </xsl:attribute>
                    </xsl:if>
                    <xsl:if test="@width">
                        <xsl:attribute name="width">
                            <xsl:value-of select="@width" />
                        </xsl:attribute>
                    </xsl:if>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <!-- http://www.iconspedia.com/icon/image-12570.html - free license -->
                <img src="data:;base64,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"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="textobject">
        <div>
            <xsl:apply-templates />
        </div>
    </xsl:template>

    <xsl:template match="phrase">
        <div class="docbookPhrase">
            <xsl:apply-templates />
        </div>
    </xsl:template>
    <xsl:template match="citetitle">
        <span class="docbookCiteTitle">
            <xsl:apply-templates />
        </span>
    </xsl:template>

    <xsl:template match="table">
        <xsl:variable name="borderTop">
            <xsl:if test="@frame='all' or @frame='top' or @frame='topbot'">
                border-top-style:solid;
            </xsl:if>
        </xsl:variable>

        <xsl:variable name="borderBottom">
            <xsl:if test="@frame='all' or @frame='bottom' or @frame='topbot'">
                border-bottom-style:solid;
            </xsl:if>
        </xsl:variable>

        <xsl:variable name="borderSides">
            <xsl:if test="@frame='all' or @frame='sides'">
                border-left-style:solid; border-right-style:solid;
            </xsl:if>
        </xsl:variable>

        <h6 class="docbookTableTitle">
            <xsl:value-of select="title" />
        </h6>
        <table class="docbookTable" style="{$borderTop} {$borderBottom} {$borderSides}">
            <xsl:apply-templates />
        </table>
    </xsl:template>

    <xsl:template match="tgroup">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="thead">
        <thead>
            <xsl:apply-templates />
        </thead>
    </xsl:template>

    <xsl:template match="tfoot">
        <tfoot>
            <xsl:apply-templates />
        </tfoot>
    </xsl:template>

    <xsl:template match="tbody">
        <tbody>
            <xsl:apply-templates />
        </tbody>
    </xsl:template>

    <xsl:template match="row">
        <tr>
            <xsl:apply-templates />
        </tr>
    </xsl:template>

    <xsl:template match="entry">
        <td>
            <xsl:apply-templates />
        </td>
    </xsl:template>

    <xsl:template match="thead/row/entry">
        <th>
            <xsl:apply-templates />
        </th>
    </xsl:template>

    <xsl:template match="keycap | mousebutton | keysym">
        <xsl:apply-templates />
        <xsl:if test="following-sibling::node()[1][self::keycap] or following-sibling::node()[1][self::mousebutton] or following-sibling::node()[1][self::keysym]">
            <xsl:text>-</xsl:text>
        </xsl:if>
    </xsl:template>
    <xsl:template match="guimenu">
        <span class="docbookGuiMenu">
            <xsl:apply-templates />
        </span>
        <xsl:if test="following-sibling::node()[1][self::guimenu] or following-sibling::node()[1][self::guisubmenu]  or following-sibling::node()[1][self::guimenuitem]">
            <xsl:text> → </xsl:text>
        </xsl:if>
    </xsl:template>
    <xsl:template match="guisubmenu">
        <span class="docbookGuiSubMenu">
            <xsl:apply-templates />
        </span>
        <xsl:if test="following-sibling::node()[1][self::guimenu] or following-sibling::node()[1][self::guisubmenu]  or following-sibling::node()[1][self::guimenuitem]">
            <xsl:text> → </xsl:text>
        </xsl:if>
    </xsl:template>
    <xsl:template match="guimenuitem">
        <span class="docbookGuiMenuItem">
            <xsl:apply-templates />
        </span>
        <xsl:if test="following-sibling::node()[1][self::guimenu] or following-sibling::node()[1][self::guisubmenu]  or following-sibling::node()[1][self::guimenuitem]">
            <xsl:text> → </xsl:text>
        </xsl:if>
    </xsl:template>
    <xsl:template name="string-replace-all">
        <xsl:param name="text" />
        <xsl:param name="replace" />
        <xsl:param name="by" />
        <xsl:choose>
            <xsl:when test="contains($text, $replace)">
                <xsl:value-of select="substring-before($text,$replace)" />
                <xsl:value-of select="$by" />
                <xsl:call-template name="string-replace-all">
                    <xsl:with-param name="text" select="substring-after($text,$replace)" />
                    <xsl:with-param name="replace" select="$replace" />
                    <xsl:with-param name="by" select="$by" />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$text" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="guibutton">
        <span class="docbookGuiButton">
            <xsl:apply-templates />
        </span>
    </xsl:template>
    <xsl:template match="guilabel">
        <span class="docbookGuiLabel">
            <xsl:apply-templates />
        </span>
    </xsl:template>
    <xsl:template match="footnote">
        <a class="docbookFootnote"><sup><xsl:number level="any" count="footnote" format="[1]"/></sup></a>
    </xsl:template>
    <xsl:template match="footnote" mode="footnote">
        <xsl:apply-templates mode="footnote"/>
        <br />
    </xsl:template>
    <xsl:template match="para" mode="footnote">
        <p class="docbookPara docbookFootnote">
            <sup><xsl:number level="any" count="footnote" format="[1]"/></sup>
            <xsl:text> </xsl:text>
            <xsl:apply-templates />
        </p>
    </xsl:template>
</xsl:stylesheet>