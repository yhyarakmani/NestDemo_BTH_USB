package com.cashin.nest.demo.models;

import java.util.List;

public class NestTransaction {

    private int id = 0;
    private int card_pay_type = 0;
    private String card_payload = null;
    private double total = 0.0;
    private NestCurrency currency_obj = null;
    private List<NestTransactionPayment> transaction_payments = null;
    private String uuid = null;
    private String invoice_id = null;
    private String issue_time = null;
    private String issue_date = null;
    private String pdf_path = null;

    public int getId() {
        return id;
    }

    public int getCard_pay_type() {
        return card_pay_type;
    }

    public String getCard_payload() {
        return card_payload;
    }

    public double getTotal() {
        return total;
    }

    public NestCurrency getCurrency_obj() {
        return currency_obj;
    }

    public List<NestTransactionPayment> getTransaction_payments() {
        return transaction_payments;
    }

    public String getUuid() {
        return uuid;
    }

    public String getInvoice_id() {
        return invoice_id;
    }

    public String getIssue_time() {
        return issue_time;
    }

    public String getIssue_date() {
        return issue_date;
    }

    public String getPdf_path() {
        return pdf_path;
    }

}
