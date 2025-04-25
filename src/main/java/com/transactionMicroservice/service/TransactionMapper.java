package com.transactionMicroservice.service;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import com.transactionMicroservice.dto.TransactionRequestDTO;
import com.transactionMicroservice.dto.TransactionResponseDTO;
import com.transactionMicroservice.model.Transaction;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionMapper {
    TransactionResponseDTO toResponseDTO(Transaction transaction);

    Transaction toModel(TransactionRequestDTO request);
}
