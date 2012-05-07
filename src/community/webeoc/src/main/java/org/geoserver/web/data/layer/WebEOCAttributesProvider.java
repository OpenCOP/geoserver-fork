/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geoserver.web.wicket.GeoServerDataProvider;

public class WebEOCAttributesProvider extends GeoServerDataProvider<AttributeDescription> {

    List<AttributeDescription> attributes = new ArrayList<AttributeDescription>();

    static final Property<AttributeDescription> NAME = new BeanProperty<AttributeDescription>(
            "name", "name");

    static final Property<AttributeDescription> BINDING = new BeanProperty<AttributeDescription>(
            "binding", "binding");

    static final PropertyPlaceholder<AttributeDescription> LON = new PropertyPlaceholder<AttributeDescription>("lon");
    static final PropertyPlaceholder<AttributeDescription> LAT = new PropertyPlaceholder<AttributeDescription>("lat");
    static final PropertyPlaceholder<AttributeDescription> LAST_UPDATED = new PropertyPlaceholder<AttributeDescription>("lastUpdated");

    // List of selectable bindings, pretty much AttributeDescription.BINDINGS
    // less geometry bindings
    static final List BINDINGS = Arrays.asList(String.class, Boolean.class, Integer.class,
            Long.class, Float.class, Double.class, Date.class, Time.class, Timestamp.class);

    public WebEOCAttributesProvider() {
    }

    public void addNewAttribute(AttributeDescription attribute) {
        attributes.add(attribute);
    }

    @Override
    protected List<AttributeDescription> getItems() {
        return attributes;
    }

    @Override
    protected List<Property<AttributeDescription>> getProperties() {
        return Arrays.asList(NAME, BINDING, LON, LAT, LAST_UPDATED);
    }

    public void removeAll(List<AttributeDescription> removed) {
        this.attributes.removeAll(removed);
    }

    public List<AttributeDescription> getAttributes() {
        return attributes;
    }

}