package com.knowledgepixels.nanodash.component;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.io.Serializable;
import java.util.*;

public class ActivityPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private Map<String, Map<String, String>> typeMonthValueMap = new HashMap<>();

    public ActivityPanel(String markupId, ApiResponse response) {
        super(markupId);

        List<Entity> list = new ArrayList<>();
        Set<String> types = new HashSet<>();
        for (ApiResponseEntry e : response.getData()) {
            String type = e.get("type");
            if (!types.contains(type)) {
                list.add(new Entity(type));
                types.add(type);
            }
            if (!typeMonthValueMap.containsKey(type)) {
                typeMonthValueMap.put(type, new HashMap<>());
            }
            typeMonthValueMap.get(type).put(e.get("month"), e.get("npCount"));
        }

        List<IColumn<Entity, String>> columns = new ArrayList<>();
        columns.add(new Column("type"));
        columns.add(new Column("2023-11"));
        columns.add(new Column("2023-12"));
        columns.add(new Column("2024-01"));
        columns.add(new Column("2024-02"));
        columns.add(new Column("2024-03"));
        columns.add(new Column("2024-04"));
        columns.add(new Column("2024-05"));
        columns.add(new Column("2024-06"));
        columns.add(new Column("2024-07"));
        columns.add(new Column("2024-08"));
        columns.add(new Column("2024-09"));
        columns.add(new Column("2024-10"));

        DataTable<Entity, String> table = new DataTable<Entity, String>("table", columns, new EntityProvider(list), 10);
        table.addBottomToolbar(new NavigationToolbar(table));
        table.addTopToolbar(new HeadersToolbar<String>(table, null));
        add(table);
    }

    private class Entity implements Serializable {

        private static final long serialVersionUID = 1L;

        public String type;

        public Entity(String type) {
            this.type = type;
        }

        public String getValue(String month) {
            if (!typeMonthValueMap.containsKey(type)) return "";
            if (!typeMonthValueMap.get(type).containsKey(month)) return "";
            return typeMonthValueMap.get(type).get(month);
        }

    }

    private class Column extends AbstractColumn<Entity, String> {

        private static final long serialVersionUID = 1L;

        private String title;

        public Column(String title) {
            this(new Model<String>(title));
        }

        public Column(Model<String> titleModel) {
            super(titleModel);
            this.title = titleModel.getObject();
            titleModel.setObject(titleModel.getObject().replaceFirst("^20", ""));
        }

        @Override
        public void populateItem(Item<ICellPopulator<Entity>> cellItem, String componentId, IModel<Entity> rowModel) {
            Entity e = rowModel.getObject();
            if (title.equals("type")) {
                cellItem.add(new NanodashLink(componentId, e.type));
            } else {
                String v = e.getValue(title);
                cellItem.add(new Label(componentId, v));
                try {
                    int i = Integer.parseInt(v);
                    if (i >= 100) {
                        cellItem.add(new AttributeAppender("class", " high"));
                    } else if (i >= 10) {
                        cellItem.add(new AttributeAppender("class", " med"));
                    } else if (i >= 1) {
                        cellItem.add(new AttributeAppender("class", " low"));
                    }
                } catch (NumberFormatException ex) {
                }
            }
        }

    }

    private class EntityProvider implements IDataProvider<Entity> {

        private static final long serialVersionUID = 1L;

        private List<Entity> list;

        public EntityProvider(List<Entity> list) {
            this.list = list;
        }

        @Override
        public Iterator<? extends Entity> iterator(long first, long count) {
            return list.iterator();
        }

        @Override
        public long size() {
            return list.size();
        }

        @Override
        public IModel<Entity> model(Entity object) {
            return new Model<Entity>(object);
        }

    }

}
