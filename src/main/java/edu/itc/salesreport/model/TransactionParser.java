package edu.itc.salesreport.model;

public interface TransactionParser {
    SaleTransaction parse(String line) throws InvalidRowException;
}