package com.beangenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Dengjun on 2015/10/23.
 */
public class DataBean {
    public static final int WRITE_NULL = 1; // 属性为空时仍然写入

    private final Set<String> propertyNames;
    private final List<Property> properties;
    int writeMode = 0;
    String clsName;
    int serialType = 0;

    public DataBean(String clsName) {
        this(clsName, 0);
    }

    public DataBean(String clsName, int writeMode) {
        propertyNames = new HashSet<>();
        properties = new ArrayList<>();
        this.writeMode = writeMode;
        this.clsName = clsName;
    }

    public Property addProperty(PropertyType t, String propertyName) {
        return addProperty(t, null, propertyName, writeMode);
    }

    public Property addProperty(PropertyType t, String propertyName, int writeMode) {
        return addProperty(t, null, propertyName, writeMode);
    }

    public Property addProperty(DataBean dataBean, String propertyName) {
        return addProperty(PropertyType.Custom, dataBean, propertyName, writeMode);
    }

    public Property addProperty(DataBean dataBean, String propertyName, int writeMode) {
        return addProperty(PropertyType.Custom, dataBean, propertyName, writeMode);
    }

    public Property addProperty(PropertyType t, DataBean dataBean, String propertyName, int mode) {
        if (!propertyNames.add(propertyName)) {
            throw new RuntimeException("Property already defined: " + propertyName);
        }
        Property property = new Property();
        property.setName(propertyName);
        property.setType(t);
        property.setWriteMode(mode);
        property.setDataBean(dataBean);

        properties.add(property);
        return property;
    }

    public int getSerialType() {
        return serialType;
    }

    public void setSerialType(int serialType) {
        this.serialType = serialType;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public String getClsName() {
        return clsName;
    }

    public void setClsName(String clsName) {
        this.clsName = clsName;
    }
}
