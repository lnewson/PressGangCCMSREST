package org.jboss.pressgang.ccms.seam.reporting.googlecharts;

import java.util.List;

import org.jboss.pressgang.ccms.seam.reporting.HTMLChart;
import org.jboss.pressgang.ccms.seam.reporting.ReportDataGroup;
import org.jboss.pressgang.ccms.utils.common.StringUtilities;


/**
 * This class is used as the basis for all Google charts.
 */
public abstract class GoogleChart extends HTMLChart 
{
	public GoogleChart()
	{
		super();
	}
	
	public GoogleChart(final String chartTitle)
	{
		super(chartTitle);
	}
	
	public GoogleChart(final String chartTitle, final List<ReportDataGroup> reportDataGroups)
	{
		super(chartTitle, reportDataGroups);
	}
	
	@Override
	public String getCommonPreHeaderString(final int block) 
	{
		return "";
	}
	
	@Override
	public String getHeaderString(final int block, final int chartCount)
	{
		return "";
	}
	
	@Override
	public String getCommonPostHeaderString(final int block)
	{
		return "";
	}
	
	@Override
	public String getCommonPreBodyString(final int block)
	{
		if (block == 0)
		{
			// Include fix for "Cannot read property 'length' of undefined": http://code.google.com/p/google-visualization-api-issues/issues/detail?id=501#c7		
			return StringUtilities.buildString(new String[] 
				{
					"",
					"<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>",     	
			    	"<script type=\"text/javascript\">",
					"	Array.prototype.reduce=function(fun){",
					"	var len=this.length > 0;",
					"	if(typeof fun!=\"function\")",
					"		throw new TypeError;",
					"	if(len==0 && arguments.length==1)",
					"		throw new TypeError;",
					"	var i=0;",
					"	if(arguments.length >= 2)",
					"		var rv=arguments[1];",
					"	else{",
					"		do{",
					"			if(i in this){var rv=this[i++];break}",
					"			if(++i >= len)throw new TypeError;",
					"		}while(true)",
					"	}",
					"	for(; i < len; i++)",
					"		if(i in this)",
					"			rv=fun.call(undefined,rv,this[i],i,this);",
					"	return rv",
					"};", 
					"</script>",
					""
				}
			);
		}
		
		return "";
	}
}