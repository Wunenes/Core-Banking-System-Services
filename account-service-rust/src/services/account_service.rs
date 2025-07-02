use crate::{
    errors::{AccountError, Result},
    models::{Account, AccountStatus, AccountType, CurrencyType}
};

use sqlx::{Pool, Postgres};
use std::str::FromStr;
use chrono::Utc;
use sqlx::types::BigDecimal;
use num_traits::Zero;

pub struct AccountService {
    pool: Pool<Postgres>,
}

#[async_trait::async_trait]
pub trait AccountServiceTrait {
    async fn create_account(
        &self,
        user_id: uuid::Uuid,
        account_type: AccountType,
        currency_type: CurrencyType,
        current_balance: String,
        interest_rate: String,
    ) -> Result<Account>;

    async fn get_account_details(&self, account_number: &str) -> Result<Account>;

    async fn credit_account(
        &self,
        account_number: &str,
        amount: &str,
        currency_type: CurrencyType,
    ) -> Result<Account>;

    async fn debit_account(
        &self,
        account_number: &str,
        amount: &str,
        currency_type: CurrencyType,
    ) -> Result<Account>;

    async fn freeze_action(
        &self,
        action: &str,
        account_number: &str,
        reason: &str,
    ) -> Result<Account>;

    async fn delete_account(
        &self,
        account_number: &str,
        receiving_account_number: &str,
    ) -> Result<Account>;

    async fn get_accounts_by_user_id(&self, user_id: uuid::Uuid) -> Result<Vec<Account>>;
}

#[async_trait::async_trait]
impl AccountServiceTrait for AccountService {
    async fn create_account(
        &self,
        user_id: uuid::Uuid,
        account_type: AccountType,
        currency_type: CurrencyType,
        current_balance: String,
        interest_rate: String,
    ) -> Result<Account> {
        let current_balance = BigDecimal::from_str(&current_balance)
            .map_err(|e| AccountError::InvalidAmount(e.to_string()))?;
        let interest_rate = BigDecimal::from_str(&interest_rate)
            .map_err(|e| AccountError::InvalidAmount(e.to_string()))?;

        let account = Account::new(
            user_id,
            account_type,
            currency_type,
            current_balance,
            interest_rate,
        );

        sqlx::query(
            r#"
            INSERT INTO accounts2 (
                account_number, user_id, account_type, account_status,
                currency_type, current_balance, available_balance,
                interest_rate, created_at, updated_at
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
            "#,
        )
        .bind(&account.account_number)
        .bind(&account.user_id)
        .bind(account.account_type.clone())
        .bind(account.account_status.clone())
        .bind(account.currency_type)
        .bind(&account.current_balance)
        .bind(&account.available_balance)
        .bind(&account.interest_rate)
        .bind(account.created_at)
        .bind(account.updated_at)
        .execute(&self.pool)
        .await?;

        Ok(account)
    }

    async fn get_account_details(&self, account_number: &str) -> Result<Account> {
        let account = sqlx::query_as::<_, Account>(
            r#"
            SELECT account_number, user_id, account_type, account_status,
               currency_type, current_balance, available_balance,
               interest_rate, created_at, updated_at
            FROM accounts2 WHERE account_number = $1
            "#,
        )
        .bind(account_number)
        .fetch_optional(&self.pool)
        .await?
        .ok_or_else(|| AccountError::NotFound(format!("Account not found: {:?}", account_number)))?;
    
        println!("Retrieved account: {:?}", account);
        Ok(account)
    }

    async fn credit_account(
        &self,
        account_number: &str,
        amount: &str,
        currency_type: CurrencyType,
    ) -> Result<Account> {
        let amount = BigDecimal::from_str(amount)
            .map_err(|e| AccountError::InvalidAmount(e.to_string()))?;

        let mut account = self.get_account_details(account_number).await?;

        if !account.can_credit() {
            return Err(AccountError::IneligibleAccount {
                account_number: account_number.to_string(),
                operation: "credit".to_string(),
            });
        }

        if account.currency_type != currency_type {
            return Err(AccountError::InvalidCurrency(format!(
                "Account currency {:?} does not match transaction currency {:?}",
                account.currency_type, currency_type
            )));
        }

        account.current_balance += amount.clone();
        account.available_balance += amount;
        account.updated_at = Utc::now();

        sqlx::query(
            r#"
            UPDATE accounts2
            SET current_balance = $1, available_balance = $2, updated_at = $3
            WHERE account_number = $4
            "#,
        )
        .bind(&account.current_balance)
        .bind(&account.available_balance)
        .bind(account.updated_at)
        .bind(&account.account_number)
        .execute(&self.pool)
        .await?;

        Ok(account)
    }

    async fn debit_account(
        &self,
        account_number: &str,
        amount: &str,
        currency_type: CurrencyType,
    ) -> Result<Account> {
        let amount = BigDecimal::from_str(amount)
            .map_err(|e| AccountError::InvalidAmount(e.to_string()))?;

        let mut account = self.get_account_details(account_number).await?;

        if !account.can_debit(&amount) {
            return Err(AccountError::InsufficientFunds {
                current_balance: account.current_balance.to_string(),
                requested_amount: amount.to_string(),
            });
        }

        if account.currency_type != currency_type {
            return Err(AccountError::InvalidCurrency(format!(
                "Account currency {:?} does not match transaction currency {:?}",
                account.currency_type, currency_type
            )));
        }

        account.current_balance -= amount.clone();
        account.available_balance -= amount;
        account.updated_at = Utc::now();

        sqlx::query(
            r#"
            UPDATE accounts2
            SET current_balance = $1, available_balance = $2, updated_at = $3
            WHERE account_number = $4
            "#,
        )
        .bind(&account.current_balance)
        .bind(&account.available_balance)
        .bind(account.updated_at)
        .bind(&account.account_number)
        .execute(&self.pool)
        .await?;

        Ok(account)
    }

    async fn freeze_action(
        &self,
        action: &str,
        account_number: &str,
        _reason: &str,  // Added to match trait definition
    ) -> Result<Account> {
        let mut account = self.get_account_details(account_number).await?;

        let new_status = match action.to_lowercase().as_str() {
            "freeze" => AccountStatus::Frozen,
            "unfreeze" => AccountStatus::Active,
            _ => return Err(AccountError::Internal("Invalid freeze action".to_string())),
        };

        account.account_status = new_status.clone();
        account.updated_at = Utc::now();

        sqlx::query(
            r#"
            UPDATE accounts2
            SET account_status = $1, updated_at = $2
            WHERE account_number = $3
            "#,
        )
        .bind(new_status)
        .bind(account.updated_at)
        .bind(&account.account_number)
        .execute(&self.pool)
        .await?;

        Ok(account)
    }

    async fn delete_account(
        &self,
        account_number: &str,
        receiving_account_number: &str,
    ) -> Result<Account> {
        let mut account = self.get_account_details(account_number).await?;

        if !account.current_balance.is_zero() {
            self.credit_account(
                receiving_account_number,
                &account.current_balance.to_string(),
                account.currency_type,
            )
            .await?;
        }

        account.account_status = AccountStatus::Closed;
        account.updated_at = Utc::now();

        sqlx::query(
            r#"
            UPDATE accounts2
            SET account_status = $1, updated_at = $2
            WHERE account_number = $3
            "#,
        )
        .bind(account.account_status.clone())
        .bind(account.updated_at)
        .bind(&account.account_number)
        .execute(&self.pool)
        .await?;

        Ok(account)
    }

    async fn get_accounts_by_user_id(&self, user_id: uuid::Uuid) -> Result<Vec<Account>> {
        let accounts = sqlx::query_as::<_, Account>(
            r#"
            SELECT account_number, user_id, account_type, account_status,
                   currency_type, current_balance, available_balance,
                   interest_rate, created_at, updated_at
            FROM accounts2 WHERE user_id = $1
            "#,
        )
        .bind(user_id)
        .fetch_all(&self.pool)
        .await?;

        Ok(accounts)
    }
}

impl AccountService {
    pub fn new(pool: Pool<Postgres>) -> Self {
        Self { pool }
    }
}