/*
 * Copyright 2009-2014 Jose Luis Martin.
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
package org.jdal.aop.config;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdal.annotation.AnnotatedElementAccessor;
import org.jdal.annotation.SerializableProxy;
import org.jdal.aop.ProxyUtils;
import org.jdal.aop.SerializableAopProxy;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * BeanPostProcessor that process {@link org.jdal.annotation.SerializableProxy} annotation 
 * for replacing fields or properties with serializable proxies.
 * 
 * @author Jose Luis Martin
 * @since 2.0
 */
public class SerializableAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter 
	implements BeanFactoryAware {
	
	private static final Log log = LogFactory.getLog(SerializableAnnotationBeanPostProcessor.class);
	private ConfigurableListableBeanFactory beanFactory;

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		
		// Check for already serializable, infraestructure or serializable proxy targets.
		if (bean instanceof SerializableAopProxy || 
				bean instanceof AopInfrastructureBean || 
				beanName.startsWith(ProxyUtils.TARGET_NAME_PREFIX))
			return bean;
		
		SerializableProxy ann = AnnotationUtils.findAnnotation(bean.getClass(), SerializableProxy.class);
		
		if (ann != null) {
			if (log.isDebugEnabled())
				log.debug("Creating serializable proxy for bean [" + beanName + "]");
			
			boolean proxyTargetClass = !beanFactory.getType(beanName).isInterface() || ann.proxyTargetClass();
			return ProxyUtils.createSerializableProxy(bean, proxyTargetClass, ann.useCache(), beanFactory, beanName);
		}

		List<AnnotatedElement> elements = AnnotatedElementAccessor.findAnnotatedElements(SerializableProxy.class, bean.getClass());
		
		for (AnnotatedElement element : elements) {
			Field f = (Field) element;
			Object value = AnnotatedElementAccessor.getValue(element, bean);
			if (value != null && !(value instanceof SerializableAopProxy)) {
				ann = AnnotationUtils.getAnnotation(element, SerializableProxy.class);
				boolean proxyTargetClass = !f.getType().isInterface() || ann.proxyTargetClass();
				Object proxy = getProxy(value, proxyTargetClass, ann.useCache(), 
						getDependencyDescriptor(element), beanName);
				if (proxy != null)
					AnnotatedElementAccessor.setValue(element, bean, proxy);
			}
				
		}
		
		return bean;
	}
	
	/**
	 * Create a serializable proxy from given object.
	 * @param value object to proxy
	 * @return a new serializable proxy
	 */
	protected Object getProxy(Object target, boolean proxyTargetClass, boolean useCache,
			DependencyDescriptor descriptor, String beanName) {
		
		return ProxyUtils.createSerializableProxy(target, proxyTargetClass, useCache, 
				beanFactory, descriptor, beanName);
	}

	public BeanFactory getBeanFactory() {
		return this.beanFactory;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	protected DependencyDescriptor getDependencyDescriptor(AnnotatedElement ae) {
		return new DependencyDescriptor((Field) ae, false);
	}
}
