package com.android.emobilepos.models;

/**
 * Created by guari_000 on 11/15/2015.
 */
public class GroupTax {
    private String taxName;
    private String taxRate;
    private String prTax;

    public String getTaxName() {
        return taxName;
    }

    public void setTaxName(String taxName) {
        this.taxName = taxName;
    }

    public String getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(String taxRate) {
        this.taxRate = taxRate;
    }

    public String getPrTax() {
        return prTax;
    }

    public void setPrTax(String prTax) {
        this.prTax = prTax;
    }
}
