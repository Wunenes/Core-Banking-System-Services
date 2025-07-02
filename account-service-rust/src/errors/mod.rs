use thiserror::Error;

#[derive(Error, Debug)]
pub enum AccountError {
    #[error("Account not found: {0}")]
    NotFound(String),

    #[error("Insufficient funds: current balance {current_balance}, requested {requested_amount}")]
    InsufficientFunds {
        current_balance: String,
        requested_amount: String,
    },

    #[error("Ineligible account: {account_number} for operation {operation}")]
    IneligibleAccount {
        account_number: String,
        operation: String,
    },

    #[error("Database error: {0}")]
    DatabaseError(#[from] sqlx::Error),

    #[error("Invalid amount: {0}")]
    InvalidAmount(String),

    #[error("Invalid currency conversion: {0}")]
    InvalidCurrency(String),

    #[error("Internal error: {0}")]
    Internal(String),
}

pub type Result<T> = std::result::Result<T, AccountError>;
