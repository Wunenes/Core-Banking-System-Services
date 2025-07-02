use serde::{Deserialize, Serialize};
use sqlx::types::BigDecimal;
use chrono::{DateTime, Utc};
use uuid::Uuid;
use strum_macros::{Display};
use crate::proto::transaction::TransactionResponse;

#[derive(Debug, sqlx::FromRow, Clone)]
pub struct Transaction {
    pub transaction_reference: String,
    pub from_account: String,
    pub to_account: String,
    pub amount: BigDecimal,
    pub currency_type: String,
    pub transaction_time: DateTime<Utc>,
    pub status: TransactionStatus,
    pub transaction_type: TransactionType,
    pub description: Option<String>,
    pub metadata: Option<String>,
    pub fee_amount: Option<BigDecimal>,
    pub fee_currency: Option<String>,
}

#[derive(Debug, Serialize, Clone, Display, Deserialize, sqlx::Type, PartialEq)]
#[sqlx(type_name = "transaction_status", rename_all = "SCREAMING_SNAKE_CASE")]
pub enum TransactionStatus {
    Pending,
    Completed,
    Failed,
    Cancelled,
}

#[derive(Debug, Copy,  Serialize, Clone, Display, Deserialize, sqlx::Type, PartialEq)]
#[sqlx(type_name = "transaction_type", rename_all = "SCREAMING_SNAKE_CASE")]
pub enum TransactionType {
    Internal,
    External,
    Deposit,
    Withdrawal,
}

impl Transaction {
    pub fn new(
        from_account: String,
        to_account: String,
        amount: BigDecimal,
        currency_type: String,
        transaction_type: TransactionType,
        description: Option<String>,
        metadata: Option<String>,
    ) -> Self {
        Self {
            transaction_reference: Uuid::new_v4().to_string(),
            from_account,
            to_account,
            amount,
            currency_type,
            transaction_time: Utc::now(),
            status: TransactionStatus::Pending,
            transaction_type,
            description,
            metadata,
            fee_amount: None,
            fee_currency: None,
        }
    }

    pub fn to_response(self) -> TransactionResponse {
        TransactionResponse {
            transaction_reference: self.transaction_reference.clone(),
            from_account: self.from_account.clone(),
            to_account: self.to_account.clone(),
            amount: self.amount.to_string(),
            currency_type: self.currency_type.clone(),
            transaction_status: self.status.to_string(),
            transaction_type: self.transaction_type.to_string(),
            description: self.description.clone().unwrap_or_default(),
            metadata: self.metadata.clone().unwrap_or_default(),
            fee_amount: self.fee_amount.clone().unwrap_or_default().to_string(),
            fee_currency: self.fee_currency.clone().unwrap_or_default(),
            timestamp: self.transaction_time.to_rfc3339(),
        }
    }   
}
