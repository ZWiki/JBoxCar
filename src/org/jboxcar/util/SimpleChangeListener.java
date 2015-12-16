package org.jboxcar.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class SimpleChangeListener<T> {
	private PropertyChangeSupport pcs;
	private T value;
	
	public SimpleChangeListener() {
		pcs = new PropertyChangeSupport(this);
	}
	
	public SimpleChangeListener(T initialValue) {
		this();
		value = initialValue;
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	public T getValue() {
		return value;
	}
	
	public void setValue(T newValue) {
		T oldValue = this.value;
		this.value = newValue;
		pcs.firePropertyChange("value", oldValue, newValue);
	}

}
