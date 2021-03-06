/*
 * Copyright 2009-2011 the original author or authors.
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
package org.jdal.dao.jpa;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Parameter;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdal.dao.DaoSupport;
import org.jdal.dao.Filter;
import org.jdal.dao.Page;
import org.jdal.dao.PageableDataSource;
import org.jdal.dao.jpa.query.EntityTypeQueryFinder;
import org.jdal.dao.jpa.query.QueryFinder;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * Dao implementation for JPA
 * 
 * @author Jose Luis Martin
 * @see org.jdal.dao.Dao
 * @since 1.1
 */
public class JpaDao<T, PK extends Serializable> extends DaoSupport<T, PK> {
	
	private static final int DEFAULT_DEPTH = 2;
	private static final Log log = LogFactory.getLog(JpaDao.class);
	@PersistenceContext
	private EntityManager em;
	private Class<T> entityClass;
	private Map<String, JpaCriteriaBuilder<?>> criteriaBuilderMap = Collections.synchronizedMap(
			new HashMap<String, JpaCriteriaBuilder<?>>());
	
	private QueryFinder queryFinder;
	private boolean onDeleteSetNull = true;
	
	/**
	 * Default Ctor, When using it, you need to set entityClass 
	 */
	public JpaDao() {
		
	}
	
	/**
	 * Create a new JpaDao for entity class
	 * @param entityClass class to map.
	 */
	public JpaDao(Class<T> entityClass) {
		this.entityClass = entityClass;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public <K> Page<K> getPage(Page<K> page) {
		
		// try named query first
		TypedQuery<K> query = getNamedQuery(page);
		
		if (query == null) // get query from criteria
			query = getCriteriaQuery(page);
	 
		// add range
		query.setMaxResults(page.getPageSize());
		query.setFirstResult(page.getStartIndex());
		
		page.setData(query.getResultList());
		page.setPageableDataSource(((PageableDataSource<K>) this));
		return page;
	}
	
	/**
	 * Build CriteriaQuery using declared JpaCriteriaBuilder in filterMap
	 * @param page
	 * @return a CriteriaQuery from filter
	 */
	@SuppressWarnings("unchecked")
	private <K> CriteriaQuery<K> getCriteria(Page<K> page) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<K> c = (CriteriaQuery<K>) cb.createQuery();
		Filter filter = null;
		if (page.getFilter() instanceof Filter) {
			filter = (Filter) page.getFilter();
			JpaCriteriaBuilder<K> jcb = 
					(JpaCriteriaBuilder<K>) criteriaBuilderMap.get(filter.getFilterName());
			if (jcb != null) {
				if (log.isDebugEnabled())
					log.debug("Found JpaCriteriaBuilder for filter: " + filter.getFilterName());
				// build criteria
				c = jcb.build(c, cb, filter);
			}
			else {
				log.error("No CriteriaBuilder found for filter name [" + filter.getFilterName() + "]");
				c.from(entityClass);
			}
		}
		else {
			c.select((Selection<? extends K>) c.from(getEntityClass()));
		}
		
		return c;
	}
	
	/**
	 * Create a TypedQuery from a request page
	 * @param page request page
	 * @return new TypedQuery
	 */
	@SuppressWarnings("unchecked")
	private <K> TypedQuery<K> getCriteriaQuery(Page<K> page) {
		CriteriaQuery<K> criteria = getCriteria(page);
		
		CriteriaQuery<Long> countCriteria = (CriteriaQuery<Long>) getCriteria(page);
		CriteriaBuilder cb = em.getCriteriaBuilder();
		
		Root<?> root = countCriteria.getRoots().iterator().next();
		countCriteria.select(cb.count(root));
		
		page.setCount((em.createQuery(countCriteria).getSingleResult())
				.intValue());
		
		
		criteria.orderBy(getOrder(page, criteria));
		
		// Add default select to entity class if none was set.
		if (criteria.getSelection() == null) {
			criteria.select((Selection<? extends K>) root);
		}
		
		return em.createQuery(criteria);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Serializable> getKeys(Page<T> page) {	
		Filter filter = null;
		TypedQuery<Serializable> query = null;
		SingularAttribute<? super T, ?> id = getIdAttribute();
		// try named query
		if (page.getFilter() instanceof Filter) {
			filter = (Filter) page.getFilter();
			String queryString = getQueryString(filter.getFilterName());
			if (queryString != null) {				
				String keyQuery = JpaUtils.getKeyQuery(queryString, id.getName());
				// add Order
				if (page.getSortName() != null)
					keyQuery = JpaUtils.addOrder(keyQuery, page.getSortName(),
						page.getOrder() == Page.Order.ASC);
					
				query = em.createQuery(keyQuery, Serializable.class);
				applyFilter(query, filter);
			}
			else {
				query = getKeyCriteriaQuery(id, page); 
			}
		}
		else {
			query = getKeyCriteriaQuery(id, page);
		}
		
		query.setFirstResult(page.getStartIndex());
		query.setMaxResults(page.getPageSize());
		
		return query.getResultList();
	}

	/**
	 * Gets CriteriaQuery for Page Keys
	 * @param page
	 * @return CriteriaQuery 
	 */
	@SuppressWarnings("unchecked")
	private TypedQuery<Serializable> getKeyCriteriaQuery(SingularAttribute<? super T, ?> id, Page<T> page) {
		CriteriaQuery<Serializable> keyCriteria  = (CriteriaQuery<Serializable>) getCriteria(page);
		Root<T> keyRoot = JpaUtils.findRoot(keyCriteria, getEntityClass());
		keyCriteria.select(keyRoot.<Serializable>get(id.getName()));
		
		return em.createQuery(keyCriteria);
	}
	
	/**
	 * Gets de id attribute from metamodel
	 * @return PK SingularAttribute
	 */
	private SingularAttribute<? super T, ?> getIdAttribute() {
		return getIdAttribute(getEntityClass());
	}
	
	/**
	 * @param class1
	 * @return
	 */
	private <K> SingularAttribute<? super K, ?> getIdAttribute(Class<K> clazz) {
		Type<?> type = em.getMetamodel().entity(clazz).getIdType();
		EntityType<K> entity =  em.getMetamodel().entity(clazz);
		SingularAttribute<? super K, ?> id = entity.getId(type.getJavaType());
		return id;
	}
	
	/**
	 * Get JPA Order from a page order for a CriteriaQuery
	 * @param page request page
	 * @param criteria CriteriaQuery to apply Order on.
	 * @return new Order
	 */
	private Order getOrder(Page<?> page, CriteriaQuery<?> criteria) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		Root<T> root = JpaUtils.findRoot(criteria, getEntityClass());
		String propertyPath = page.getSortName();

		if (log.isDebugEnabled())
			log.debug("Setting order as: " + propertyPath);

		Path<?> path = JpaUtils.getPath(root, propertyPath);

		return page.getOrder() == Page.Order.ASC ? cb.asc(path) : cb.desc(path);
	}
	
	/**
	 * Gets a NamedQuery from page, setup order, params and page result count.
	 * @param page request page
	 * @return a TypedQuery from a NamedQuery
	 */
	@SuppressWarnings("unchecked")
	protected <K> TypedQuery<K> getNamedQuery(Page<K> page) {
		Filter filter = null;
		TypedQuery<K> query = null;
		
		if (page.getFilter() instanceof Filter) {
			filter = (Filter) page.getFilter();
			String queryString = getQueryString(filter.getFilterName());
			if (queryString != null) {
				String countQueryString = JpaUtils.createCountQueryString(queryString);
				TypedQuery<Long> countQuery =  em.createQuery(countQueryString, Long.class);
				applyFilter(countQuery, filter);
				page.setCount(countQuery.getSingleResult().intValue());
				// add Order
				if (page.getSortName() != null)
					queryString = JpaUtils.addOrder(queryString, page.getSortName(),
						page.getOrder() == Page.Order.ASC);
		
				query = (TypedQuery<K>) em.createQuery(queryString);
				applyFilter(query, filter);
			}
		}
		
		return query;
	}
	
	/**
	 * Apply filter to parametriced Query
	 * @param query the query to apply filter on
	 * @param filter Filter to apply
	 */
	private void applyFilter(Query query, Filter filter) {
		Map<String, Object> parameterMap = filter.getParameterMap();
		for (Parameter<?> p : query.getParameters()) {
			if (parameterMap.containsKey(p.getName())) {
				query.setParameter(p.getName(), parameterMap.get(p.getName()));
			}
			else {
				throw new InvalidDataAccessApiUsageException("Parameter " + p.getName() +
						"was not found in filter " + filter.getFilterName() + 
				". Review NamedQuery or Filter.");
			}
		}
 	}

	/**
	 * Gets query string fro named query using configured QueryFinder, if it's null
	 * use EntityTypeQueryFinder as defaults (looks for anntations on entity class).
	 * @param name query name
	 * @return query string.
	 */
	protected String getQueryString(String name) {
		if (queryFinder == null)
			queryFinder = new EntityTypeQueryFinder(em.getMetamodel().entity(getEntityClass()));
		
		return queryFinder.find(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<T> findByNamedQuery(String queryName, Map<String, Object> queryParams) {
		TypedQuery<T> query = em.createNamedQuery(queryName, getEntityClass());
		if (queryParams != null) {
			for (Map.Entry<String, ?> entry : queryParams.entrySet()) {
				query.setParameter(entry.getKey(), entry.getValue());
			}
		}
		return query.getResultList();
	}

	/**
	 * {@inheritDoc}
	 */
	public T get(PK id) {
		return em.find(getEntityClass(), id);
	}


	/**
	 * {@inheritDoc}
	 */
	public List<T> getAll() {
		CriteriaQuery<T> q = em.getCriteriaBuilder().createQuery(getEntityClass());
		q.from(getEntityClass());
		
		return em.createQuery(q).getResultList();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void delete(T entity) {
		if (!em.contains(entity))
			entity = em.merge(entity);
		
		if (onDeleteSetNull)
			nullReferences(entity);
		
		em.remove(entity);
	}

	/**
	 * {@inheritDoc}
	 */
	public void deleteById(PK id) {
		delete(get(id));
		
	}
	
	/**
	 * Null References on one to many and one to one associations.
	 * Will only work if association has annotated with a mappedBy attribute.
	 * 
	 * @param entity entity
	 */
	private void nullReferences(T entity) {
		EntityType<T> type = em.getMetamodel().entity(getEntityClass());

		if (log.isDebugEnabled()) 
			log.debug("Null references on entity " + type.getName());
		
		for (Attribute<?, ?> a :  type.getAttributes()) {
			if (PersistentAttributeType.ONE_TO_MANY == a.getPersistentAttributeType() ||
					PersistentAttributeType.ONE_TO_ONE == a.getPersistentAttributeType()) {
				Object association =  PropertyAccessorFactory.forDirectFieldAccess(entity)
						.getPropertyValue(a.getName());
				if (association != null) {
					EntityType<?> associationType = null;
					if (a.isCollection()) {
						associationType = em.getMetamodel().entity(
								((PluralAttribute<?, ?, ?>)a).getBindableJavaType());
					}
					else {
						associationType = em.getMetamodel().entity(a.getJavaType());

					}
					
					String mappedBy = JpaUtils.getMappedBy(a);
					if (mappedBy != null) {
						Attribute<?,?> aa = associationType.getAttribute(mappedBy);
						if (PersistentAttributeType.MANY_TO_ONE == aa.getPersistentAttributeType()) {
							if (log.isDebugEnabled()) {
								log.debug("Null ManyToOne reference on " + 
										associationType.getName() + "." + aa.getName());
							}
							for (Object o : (Collection<?>) association) {
								PropertyAccessorFactory.forDirectFieldAccess(o).setPropertyValue(aa.getName(), null);
							}
						}
						else if (PersistentAttributeType.ONE_TO_ONE == aa.getPersistentAttributeType()) {
							if (log.isDebugEnabled()) {
								log.debug("Null OneToOne reference on " + 
										associationType.getName() + "." + aa.getName());
							}
							PropertyAccessorFactory.forDirectFieldAccess(association).setPropertyValue(aa.getName(), null);
						}
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean exists(PK id) {
		return id != null && get(id) != null;
	}


	/**
	 * {@inheritDoc}
	 */
	public T initialize(T entity) {
		initialize(entity, DEFAULT_DEPTH);
		return entity;
	}

	/**
	 * {@inheritDoc}
	 */
	public T initialize(T entity, int depth) {
		JpaUtils.initialize(em, entity, depth);
		return entity;
	}

	/**
	 * {@inheritDoc}
	 */
	public T save(T entity) {
		T persistentEntity;
	
		if (isNew(entity)) {
			em.persist(entity);
			persistentEntity = entity;
		}
		else {
			persistentEntity = em.merge(entity);
		}
		
		return persistentEntity;
			
	}

	/**
	 * Test if entity is New
	 * @param entity
	 * @return true if entity is new, ie not detached
	 */
	@SuppressWarnings("unchecked")
	protected boolean isNew(T entity) {
		SingularAttribute<?, ?> id = getIdAttribute(entity.getClass());
		// try field
		PropertyAccessor pa = PropertyAccessorFactory.forDirectFieldAccess(entity);
		PK key = (PK) pa.getPropertyValue(id.getName());
		if (key == null)
			key = (PK) PropertyAccessorFactory.forBeanPropertyAccess(entity).getPropertyValue(id.getName());
		
		
		return key == null || !exists(key, entity.getClass());
	}
	
	/**
	 * @param key
	 * @param class1
	 * @return
	 */
	private boolean exists(PK key, Class<? extends Object> clazz) {
		return get(key, clazz) != null;
	}

	/**
	 * @return entity class
	 */
	public Class<T> getEntityClass() {
		return entityClass;
	}

	/**
	 * @return the criteriaBuilderMap
	 */
	public Map<String, JpaCriteriaBuilder<?>> getCriteriaBuilderMap() {
		return criteriaBuilderMap;
	}

	/**
	 * @param criteriaBuilderMap the criteriaBuilderMap to set
	 */
	public void setCriteriaBuilderMap(Map<String, JpaCriteriaBuilder<T>> criteriaBuilderMap) {
		this.criteriaBuilderMap.clear();
		this.criteriaBuilderMap.putAll(criteriaBuilderMap);
	}

	/**
	 * @return the queryFinder
	 */
	public QueryFinder getQueryFinder() {
		return queryFinder;
	}

	/**
	 * @param queryFinder the queryFinder to set
	 */
	public void setQueryFinder(QueryFinder queryFinder) {
		this.queryFinder = queryFinder;
	}

	/**
	 * {@inheritDoc}
	 */
	public <E> E get(PK id, Class<E> clazz) {
		return em.find(clazz, id);
	}

	/**
	 * {@inheritDoc}
	 */
	public <E> List<E> getAll(Class<E> clazz) {
		CriteriaQuery<E> q = em.getCriteriaBuilder().createQuery(clazz);
		q.from(clazz);
		
		return em.createQuery(q).getResultList();
	}

	/**
	 * @return the em
	 */
	public EntityManager getEntityManager() {
		return em;
	}

	/**
	 * @param em the em to set
	 */
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

	/**
	 * @param entityClass the entityClass to set
	 */
	public void setEntityClass(Class<T> entityClass) {
		this.entityClass = entityClass;
	}

	/**
	 * @return the onDeleteSetNull
	 */
	public boolean isOnDeleteSetNull() {
		return onDeleteSetNull;
	}

	/**
	 * @param onDeleteSetNull the onDeleteSetNull to set
	 */
	public void setOnDeleteSetNull(boolean onDeleteSetNull) {
		this.onDeleteSetNull = onDeleteSetNull;
	}

}
