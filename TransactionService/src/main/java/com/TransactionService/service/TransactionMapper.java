package com.TransactionService.service;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import com.TransactionService.dto.TransactionRequestDTO;
import com.TransactionService.dto.TransactionResponseDTO;
import com.TransactionService.model.Transaction;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionMapper {
    TransactionResponseDTO toResponseDTO(Transaction transaction);

    Transaction toModel(TransactionRequestDTO request);
}
