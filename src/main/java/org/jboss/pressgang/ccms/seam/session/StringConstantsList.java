package org.jboss.pressgang.ccms.seam.session;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.framework.EntityQuery;

import org.jboss.pressgang.ccms.restserver.entity.StringConstants;

import java.util.Arrays;

@Name("stringConstantsList")
public class StringConstantsList extends EntityQuery<StringConstants> 
{

	/** Serializable version identifier */
	private static final long serialVersionUID = -3452081004524609845L;

	private static final String EJBQL = "select stringConstants from StringConstants stringConstants";

	private static final String[] RESTRICTIONS = {
			"lower(stringConstants.constantName) like lower(concat('%',#{stringConstantsList.stringConstants.constantName},'%'))",
			"lower(stringConstants.constantValue) like lower(concat('%',#{stringConstantsList.stringConstants.constantValue},'%'))", };

	private StringConstants stringConstants = new StringConstants();

	public StringConstantsList() {
		setEjbql(EJBQL);
		setRestrictionExpressionStrings(Arrays.asList(RESTRICTIONS));
		setMaxResults(25);
	}

	public StringConstants getStringConstants() {
		return stringConstants;
	}
}
