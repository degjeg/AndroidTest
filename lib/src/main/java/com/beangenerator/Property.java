package com.beangenerator;

/**
 * Created by Dengjun on 2015/10/23.
 */
public class Property {
    private PropertyType type;
    private String name;

    /**
     * @see DataBean.WRITE_NULL
     * @see DataBean.WRITE_EMPTY
     */
    private int writeMode;
    private DataBean dataBean;

    public PropertyType getType() {
        return type;
    }

    public void setType(PropertyType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWriteMode() {
        return writeMode;
    }

    public void setWriteMode(int writeMode) {
        this.writeMode = writeMode;
    }

    public DataBean getDataBean() {
        return dataBean;
    }

    public void setDataBean(DataBean dataBean) {
        this.dataBean = dataBean;
    }
}
