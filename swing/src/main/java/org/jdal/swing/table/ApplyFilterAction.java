/*
 * Copyright 2008-2011 the original author or authors.
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
package org.jdal.swing.table;


import java.awt.event.ActionEvent;

import org.jdal.swing.View;
import org.jdal.swing.form.FormUtils;

public class ApplyFilterAction extends TablePanelAction {

	public static final String DEFAULT_ICON = "/images/table/22x22/edit-find.png";

	private static final long serialVersionUID = 1L;

	public void init() {
		setIcon(FormUtils.getIcon(getIcon(), DEFAULT_ICON));
	}

	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent arg0) {
		
		View<Object> filterView = getTablePanel().getFilterView();
		if (filterView != null) {
			filterView.update();

			if (filterView.validateView()) {
				getTablePanel().getTable().setFilter(filterView.getModel());
				getTablePanel().getTable().getPaginator().firstPage();
			}
		}
	}
}
