package com.numbergame.gamenumber.mapper;

import com.numbergame.gamenumber.dto.response.TransactionResponse;
import com.numbergame.gamenumber.entity.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionType(transaction.getTransactionType())
                .turnsAdded(transaction.getTurnsAdded())
                .amount(transaction.getAmount())
                .paymentMethod(transaction.getPaymentMethod())
                .paymentStatus(transaction.getPaymentStatus())
                .transactionRef(transaction.getTransactionRef())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    public List<TransactionResponse> toResponseList(List<Transaction> transactions) {
        return transactions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}

