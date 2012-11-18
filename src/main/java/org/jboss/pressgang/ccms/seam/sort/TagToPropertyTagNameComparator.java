package org.jboss.pressgang.ccms.seam.sort;

import java.util.Comparator;

import org.jboss.pressgang.ccms.model.PropertyTag;
import org.jboss.pressgang.ccms.model.TagToPropertyTag;

public class TagToPropertyTagNameComparator implements Comparator<TagToPropertyTag>
{

	@Override
	public int compare(final TagToPropertyTag o1, final TagToPropertyTag o2)
	{
		final PropertyTag o1Tag = o1.getPropertyTag() != null ? o1.getPropertyTag(): null;
		final PropertyTag o2Tag = o2.getPropertyTag() != null ? o2.getPropertyTag(): null;
		
		if (o1Tag == null && o2Tag == null)
			return 0;
		
		if (o1Tag == null)
			return -1;
		
		if (o2Tag == null)
			return 1;
		
		return o1Tag.getPropertyTagName().compareTo(o2Tag.getPropertyTagName()); 
	}

}
