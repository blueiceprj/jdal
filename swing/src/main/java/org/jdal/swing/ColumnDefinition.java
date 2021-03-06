/*
 * Copyright 2009-2016 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdal.swing;


import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jdal.beans.MessageSourceWrapper;
import org.jdal.beans.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

/**
 * A Column definition for configuring ListTableModel with
 * inner beans in context.
 * 
 * @author Jose Luis Martin - (jlm@joseluismartin.info)
 * @since 1.1
 */
public class ColumnDefinition {

	private String name;
	private String displayName;
	private boolean editable;
	private Integer width;
	private String sortProperty;
	private TableCellRenderer renderer;
	private TableCellEditor editor;
	private Class<?> clazz;
	private boolean sortable = true;
	
	private MessageSourceWrapper messageSourceWrapper = new MessageSourceWrapper();
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return the displayName
	 */
	public String getDisplayName() {
		if (displayName == null) {
			return PropertyUtils.toHumanReadable(name);
		}
		
		if (messageSourceWrapper.hasMessage(displayName)) {
			return messageSourceWrapper.getMessage(displayName);
		}
		
		return displayName;
	}
	
	/**
	 * @param displayName the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	/**
	 * @return the editable
	 */
	public boolean isEditable() {
		return editable;
	}
	
	/**
	 * @param editable the editable to set
	 */
	public void setEditable(boolean editable) {
		this.editable = editable;
	}
	
	/**
	 * @return the sortProperty
	 */
	public String getSortProperty() {
		return sortProperty;
	}
	
	/**
	 * @param sortProperty the sortProperty to set
	 */
	public void setSortProperty(String sortProperty) {
		this.sortProperty = sortProperty;
	}
	
	/**
	 * @return the renderer
	 */
	public TableCellRenderer getRenderer() {
		return renderer;
	}
	
	/**
	 * @param renderer the renderer to set
	 */
	public void setRenderer(TableCellRenderer renderer) {
		this.renderer = renderer;
	}
	
	/**
	 * @return the width
	 */
	public Integer getWidth() {
		return width;
	}
	
	/**
	 * @param width the width to set
	 */
	public void setWidth(Integer width) {
		this.width = width;
	}
	
	/**
	 * @return the editor
	 */
	public TableCellEditor getEditor() {
		return editor;
	}
	
	/**
	 * @param editor the editor to set
	 */
	public void setEditor(TableCellEditor editor) {
		this.editor = editor;
	}
	
	/**
	 * @return the clazz
	 */
	public Class<?> getClazz() {
		return clazz;
	}
	
	/**
	 * @param clazz the clazz to set
	 */
	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}
	
	/**
	 * @return the sortable
	 */
	public boolean isSortable() {
		return sortable;
	}
	
	/**
	 * @param sortable the sortable to set
	 */
	public void setSortable(boolean sortable) {
		this.sortable = sortable;
	}
	
	@Autowired
	public void setMessageSource(MessageSource messageSource) {
		this.messageSourceWrapper.setMessageSource(messageSource);
	}
	
	public MessageSource getMessageSource() {
		return messageSourceWrapper.getMessageSource();
	}
	
}
