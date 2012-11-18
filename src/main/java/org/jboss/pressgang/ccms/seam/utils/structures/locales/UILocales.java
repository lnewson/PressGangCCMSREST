package org.jboss.pressgang.ccms.seam.utils.structures.locales;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.jboss.pressgang.ccms.model.Filter;
import org.jboss.pressgang.ccms.rest.v1.constants.CommonFilterConstants;

import org.jboss.pressgang.ccms.restserver.utils.EntityUtilities;
import org.jboss.seam.Component;

public class UILocales
{
	private List<UILocale> locales = new ArrayList<UILocale>();
	
	public UILocales()
	{
	    final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
		final List<String> locales = EntityUtilities.getLocales(entityManager);
		for (final String locale : locales)
		{
			getLocales().add(new UILocale(locale, false));
		}
	}

	public List<UILocale> getLocales()
	{
		return locales;
	}

	public void setLocales(final List<UILocale> locales)
	{
		this.locales = locales;
	}
	
	public void loadLocaleCheckboxes(final Filter filter)
	{
		if (filter == null) return;
		
		for (final UILocale uiLocale : locales)
		{
			final List<Integer> localeStates = filter.hasLocale(uiLocale.getName());
			
			/*
			 * find out if this locale is already selected
			 */
			boolean selected = false;
			boolean selectedNot = false;

			for (final Integer localeState : localeStates)
			{
				if (localeState == CommonFilterConstants.NOT_MATCH_TAG_STATE)
					selected = selectedNot = true;
				else if (localeState == CommonFilterConstants.MATCH_TAG_STATE)
					selected = true;
			}
			
			uiLocale.setNotSelected(selectedNot);
			uiLocale.setSelected(selected);
		}
	}
}
