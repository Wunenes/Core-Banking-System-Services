use serde::{Deserialize, Serialize};
use sqlx::types::BigDecimal;
use strum_macros::{Display};
use num_derive::FromPrimitive; 
use std::str::FromStr;
use crate::utils::account_number_generator::AccountNumberGenerator;

#[derive(Clone, Debug, Serialize, Deserialize, sqlx::Type, FromPrimitive, PartialEq)]
#[sqlx(rename_all = "SCREAMING_SNAKE_CASE")]
#[sqlx(type_name = "account_type")]
pub enum AccountType {
    Checking,
    Savings,
    Internal,
}

#[derive(Debug, Serialize, Clone, Display, Deserialize, sqlx::Type, FromPrimitive, PartialEq)]
#[sqlx(rename_all = "SCREAMING_SNAKE_CASE")]
#[sqlx(type_name = "account_status")]
pub enum AccountStatus {
    Active,
    Inactive,
    Frozen,
    Closed,
    Dormant,
}

#[derive(Debug, Serialize, Deserialize, sqlx::Type, Display, Clone, Copy, PartialEq, Eq, FromPrimitive)]
#[sqlx(rename_all = "SCREAMING_SNAKE_CASE")]
#[sqlx(type_name = "currency_type")]
pub enum CurrencyType {
    Kes,
    Usd,
    Eur,
    Gbp,
}

impl ToString for AccountType {
    fn to_string(&self) -> String {
        match self {
            AccountType::Checking => "CHECKING",
            AccountType::Savings => "SAVINGS",
            AccountType::Internal => "INTERNAL",
        }.to_string()
    }
}

impl FromStr for AccountType {
    type Err = String;
    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s.to_uppercase().as_str() {
            "CHECKING" => Ok(AccountType::Checking),
            "SAVINGS" => Ok(AccountType::Savings),
            "INTERNAL" => Ok(AccountType::Internal),
            _ => Err(format!("Invalid account type: {}", s))
        }
    }
}
 
#[derive(Debug, sqlx::FromRow, Clone)]
pub struct Account {
    pub account_number: String,
    pub user_id: uuid::Uuid,
    pub account_type: AccountType,
    pub account_status: AccountStatus,
    pub currency_type: CurrencyType,
    pub current_balance: BigDecimal,
    pub available_balance: BigDecimal,
    pub interest_rate: BigDecimal,
    pub created_at: chrono::DateTime<chrono::Utc>,
    pub updated_at: chrono::DateTime<chrono::Utc>,
}

impl Account {
    pub fn new(
        user_id: uuid::Uuid,
        account_type: AccountType,
        currency_type: CurrencyType,
        current_balance: BigDecimal,
        interest_rate: BigDecimal,
    ) -> Self {
        let now = chrono::Utc::now();
        Self {
            account_number: AccountNumberGenerator::generate(&account_type),
            user_id,
            account_type,
            account_status: AccountStatus::Active,
            currency_type,
            available_balance: current_balance.clone(),
            current_balance,
            interest_rate,
            created_at: now,
            updated_at: now,
        }
    }

    
    pub fn can_debit(&self, amount: &BigDecimal) -> bool {
        self.account_status == AccountStatus::Active && self.available_balance >= *amount
    }

    pub fn can_credit(&self) -> bool {
        self.account_status == AccountStatus::Active
    }

}
