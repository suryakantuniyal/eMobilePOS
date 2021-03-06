package com.android.emobilepos.models.realms;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import util.StringUtil;

/**
 * Created by guarionex on 7/10/17.
 */

public class CustomerCustomField extends RealmObject {
    @PrimaryKey
    private String id;

    @SerializedName("cust_id")
    @Expose
    @Index
    private String custId;
    @SerializedName("cust_field_id")
    @Expose
    @Index
    private String custFieldId;
    @SerializedName("cust_field_name")
    @Expose
    private String custFieldName;
    @SerializedName("cust_value")
    @Expose
    private String custValue;

    public String getCustId() {
        return custId;
    }

    public void setCustId(String custId) {
        generatePrimaryKey();
        this.custId = custId;
    }

    public String getCustFieldId() {
        return custFieldId;
    }

    public void setCustFieldId(String custFieldId) {
        generatePrimaryKey();
        this.custFieldId = custFieldId;
    }

    public String getCustFieldName() {
        return custFieldName;
    }

    public void setCustFieldName(String custFieldName) {
        this.custFieldName = custFieldName;
    }

    public String getCustValue() {
        return custValue;
    }

    public void setCustValue(String custValue) {
        this.custValue = custValue;
    }

    public void generatePrimaryKey() {
        id = String.format("%s%s", StringUtil.nullStringToEmpty(custId).toUpperCase(), StringUtil.nullStringToEmpty(custFieldId).toUpperCase());
    }

}
