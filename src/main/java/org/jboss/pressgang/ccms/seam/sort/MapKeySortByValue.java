package org.jboss.pressgang.ccms.seam.sort;

import java.util.List;
import java.util.Map;

public interface MapKeySortByValue<T, U> 
{
	List<T> sortKeys(final Map<T, U> map);
}
