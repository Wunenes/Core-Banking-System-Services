use thiserror::Error;
use tonic::Status;

#[derive(Error, Debug)]
pub enum TransactionError {
    #[error("Database error: {0}")]
    Database(#[from] sqlx::Error),

    #[error("Account not found: {0}")]
    AccountNotFound(String),

    #[error("Insufficient funds in account {account}: current balance {balance}, requested amount {amount} {currency}")]
    InsufficientFunds {
        account: String,
        balance: String,
        amount: String,
        currency: String,
    },

    #[error("Ineligible account {account}: {reason}")]
    IneligibleAccount {
        account: String,
        reason: String,
    },

    #[error("Transaction not found: {0}")]
    TransactionNotFound(String),

    #[error("Invalid currency: {0}")]
    InvalidCurrency(String),

    #[error("Encryption error: {0}")]
    Encryption(String),

    #[error("Authentication error: {0}")]
    Authentication(String),

    #[error("Invalid amount: {0}")]
    InvalidAmount(String),

    #[error("Internal error: {0}")]
    Internal(String),
}

impl From<TransactionError> for Status {
    fn from(error: TransactionError) -> Self {
        match error {
            TransactionError::Database(e) => Status::internal(e.to_string()),
            TransactionError::AccountNotFound(msg) => Status::not_found(msg),
            TransactionError::InsufficientFunds { account, balance, amount, currency } => {
                Status::failed_precondition(format!(
                    "Insufficient funds in account {}: balance {} {}, requested {} {}",
                    account, balance, currency, amount, currency
                ))
            }
            TransactionError::IneligibleAccount { account, reason } => {
                Status::failed_precondition(format!(
                    "Account {} is ineligible: {}", account, reason
                ))
            }
            TransactionError::TransactionNotFound(msg) => Status::not_found(msg),
            TransactionError::InvalidCurrency(msg) => Status::invalid_argument(msg),
            TransactionError::Encryption(msg) => Status::internal(msg),
            TransactionError::Authentication(msg) => Status::unauthenticated(msg),
            TransactionError::InvalidAmount(msg) => Status::invalid_argument(msg),
            TransactionError::Internal(msg) => Status::internal(msg),
        }
    }
}
