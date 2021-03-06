/*
 * Copyright 2009-2015 the original author or authors.
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
package org.jdal.vaadin;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdal.annotation.SerializableProxy;
import org.jdal.vaadin.annotation.UiMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

/**
 * {@link UiRequestMapping} implementation that look for {@link UI UIs} by bean name.
 * Parse {@link UiMapping} annotations in spring context UI types and match urls using 
 * an {@link AntPathMatcher}.
 * 
 * @author Jose Luis Martin
 * @since 2.1
 */
@SerializableProxy	
public class UrlBeanNameUiMapping implements UiRequestMapping, Serializable {
	
	private static final Log log = LogFactory.getLog(UrlBeanNameUiMapping.class);
	/** translate paths to bean names */
	private Map<String, String> urlMap = new ConcurrentHashMap<String, String>();;
	/** Used to match url paths */
	private transient PathMatcher pathMatcher  = new AntPathMatcher();
	
	@Override
	public UI getUi(VaadinRequest request) {
		ApplicationContext ctx = VaadinUtils.getApplicationContext();
		String beanName = getBeanNameFromRequest(request);
		
		if (beanName != null && ctx.containsBean(beanName)) 
			return VaadinUtils.getApplicationContext().getBean(beanName, UI.class);
		
		return null;
	}

	/**
	 * Try to match a ant url pattern in url mapping and return the UI bean name
	 * @param request vaadin request
	 * @return the bean name for request, null if none.
	 */
	protected String getBeanNameFromRequest(VaadinRequest request) {
		String beanName = null;
		String pathInfo = request.getPathInfo();
		
		if (this.pathMatcher == null)
			this.pathMatcher = new AntPathMatcher();
		
		for (String pattern : this.urlMap.keySet())  {
			if (log.isDebugEnabled())
				log.debug("Matching pattern [" + pattern + "] over path info [" + pathInfo + "]");
			
			if (this.pathMatcher.match(pattern, request.getPathInfo())) {
				beanName = this.urlMap.get(pattern);
				if (log.isDebugEnabled())
					log.debug("Matching success. Using bean name  [" + beanName + "]");
				
				break;
			}
		}
			
		if (beanName == null)
			beanName = request.getPathInfo();
		
		return beanName;
	}
	
	/**
	 * Init th url map parsing {@link UiMapping} annotations
	 * @param ctx ApplicationContext
	 */
	@Autowired
	public void init(ApplicationContext ctx) {
		Map<String, Object> uis = ctx.getBeansWithAnnotation(UiMapping.class);
		
		for (String name : uis.keySet()) {
			Object ui = uis.get(name);
			if (ui instanceof UI) {
				UiMapping ann = AnnotationUtils.findAnnotation(ui.getClass(), UiMapping.class);
				if (ann != null) {
					if (log.isDebugEnabled())
						log.debug("Mapping UI [" + ui.getClass().getName() + "] to request path [" + ann.value() + "]");
					this.urlMap.put(ann.value(), name);
				}
			}
		}
	}
	
	/**
	 * Lookup UI class from application context.
	 * @param request request
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Class<?extends UI> getUiClass(VaadinRequest request) {
		ApplicationContext ctx = VaadinUtils.getApplicationContext();
		String beanName = getBeanNameFromRequest(request);
		
		if (beanName != null && ctx.containsBean(beanName))
			return (Class<? extends UI>) ctx.getType(beanName);
		
		return null;
	}

}
