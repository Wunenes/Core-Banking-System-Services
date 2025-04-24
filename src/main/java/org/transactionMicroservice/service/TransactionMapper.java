package org.transactionMicroservice.service;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.transactionMicroservice.dto.TransactionRequestDTO;
import org.transactionMicroservice.dto.TransactionResponseDTO;
import org.transactionMicroservice.model.Transaction;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionMapper {
    TransactionResponseDTO toResponseDTO(Transaction transaction);

    Transaction toModel(TransactionRequestDTO request);
}
