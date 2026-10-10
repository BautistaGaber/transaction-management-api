package com.transactionmanagement.api.transaction.domain.model;

public record Transaction(Long id, double amount, String type, Long parentId) { }
