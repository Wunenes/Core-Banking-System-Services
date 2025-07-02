use crate::auth::{
    token_cache::TokenCache,
};
use crate::errors::TransactionError;
use crate::proto::transaction::{
    TransactionRequest, TransactionResponse, TransactionReferenceRequest, 
    AccountRequest, AccountTransactionsRequest, TransactionTimeRequest,
    TransactionsListResponse
};
use crate::proto::account::*;
use crate::models::transaction::*;
use sqlx::{PgPool, Postgres, Transaction as SqlxTransaction};
use tonic::{Request, Response, Status};
use std::sync::Arc;
use rust_decimal::Decimal;
use tokio::sync::Mutex;
// Import AccountServiceClient from the generated gRPC client module (adjust the path as needed)
use crate::proto::account::account_service_client::AccountServiceClient;

// Import Transaction struct (adjust the path if it's in a different module)
use crate::models::transaction::Transaction;
 
pub struct TransactionService {
    pool: PgPool,
    account_client: Arc<Mutex<AccountServiceClient<tonic::transport::Channel>>>,
    token_cache: Arc<TokenCache>,
}

#[async_trait::async_trait]
pub trait TransactionServiceTrait {
    async fn internal_transfer(
        &self,
        request: Request<TransactionRequest>
    ) -> Result<Response<TransactionResponse>, Status>;

    async fn process_deposit(
        &self,
        request: Request<TransactionRequest>
    ) -> Result<Response<TransactionResponse>, Status>;

    async fn get_transaction_by_reference(
        &self,
        request: Request<TransactionReferenceRequest>
    ) -> Result<Response<TransactionResponse>, Status>;

    async fn get_transactions_by_from_account(
        &self,
        request: Request<AccountRequest>
    ) -> Result<Response<TransactionsListResponse>, Status>;

    async fn get_transactions_by_to_account(
        &self,
        request: Request<AccountRequest>
    ) -> Result<Response<TransactionsListResponse>, Status>;

    async fn get_transactions_by_account_id(
        &self,
        request: Request<AccountTransactionsRequest>
    ) -> Result<Response<TransactionsListResponse>, Status>;

    async fn get_transaction_by_transaction_time(
        &self,
        request: Request<TransactionTimeRequest>
    ) -> Result<Response<TransactionsListResponse>, Status>;
}

impl TransactionService {
    pub fn new(
        pool: PgPool,
        account_client: AccountServiceClient<tonic::transport::Channel>,
        token_cache: Arc<TokenCache>,
    ) -> Self {
        Self {
            pool,
            account_client: Arc::new(Mutex::new(account_client)),
            token_cache,
        }
    }

    async fn begin_transaction(&self) -> Result<SqlxTransaction<'_, Postgres>, TransactionError> {
        self.pool
            .begin()
            .await
            .map_err(TransactionError::Database)
    }

    // async fn validate_accounts(
    // &self,
    // from_account: &str,
    // to_account: &str,
    // ) -> Result<(), TransactionError> {
    //     let mut account_client = self.account_client.lock().await;

    //     // Obtain token
    //     let token = self.token_cache.get_token().await
    //         .map_err(|e| Status::internal(format!("Token fetch failed: {}", e)));
    //     let metadata = self.create_auth_metadata(token.unwrap().as_str());

    //     // Validate from account
    //     let mut from_request = Request::new(GetAccountRequest {
    //         account_number: from_account.to_string(),
    //     });
    //     *from_request.metadata_mut() = metadata.clone();

    //     let from_account_details = account_client
    //         .get_account_details(from_request)
    //         .await
    //         .map_err(|e| TransactionError::AccountNotFound(e.to_string()))?
    //         .into_inner();

    //     if from_account_details.account_status != AccountStatus::Active as i32 {
    //         return Err(TransactionError::IneligibleAccount {
    //             account: from_account.to_string(),
    //             reason: "Account is not active".to_string(),
    //         });
    //     }

    //     // Validate to account
    //     let mut to_request = Request::new(GetAccountRequest {
    //         account_number: to_account.to_string(),
    //     });
    //     *to_request.metadata_mut() = metadata;

    //     account_client
    //         .get_account_details(to_request)
    //         .await
    //         .map_err(|e| TransactionError::AccountNotFound(e.to_string()))?;

    //     Ok(())
    // }


    fn create_auth_metadata(&self, token: &str) -> tonic::metadata::MetadataMap {
        let mut metadata = tonic::metadata::MetadataMap::new();
        metadata.insert(
            "authorization", 
            format!("Bearer {}", token)
                .parse()
                .unwrap_or_else(|_| tonic::metadata::MetadataValue::from_static(""))
        );
        metadata
    }
}

#[async_trait::async_trait]
impl TransactionServiceTrait for TransactionService {
    async fn internal_transfer(
        &self,
        request: Request<TransactionRequest>
    ) -> Result<Response<TransactionResponse>, Status> {
        let req = request.into_inner();

        // Validate accounts and amount
        

        // Begin database transaction
        let tx = self.begin_transaction()
            .await
            .map_err(|e| Status::internal(format!("Failed to begin transaction: {:?}", e)))?;

        // Create transaction record
        let mut transaction = Transaction::new(
            req.from_account.clone(),
            req.to_account.clone(),
            req.amount.parse().map_err(|_| Status::invalid_argument("Invalid amount format"))?,
            req.currency_type.clone(),
            TransactionType::Internal,
            Some(req.description.clone()),
            Some(req.metadata.clone()),
        );
 
        // Save transaction
        sqlx::query(
            r#"
            INSERT INTO transactions (
                transaction_reference, from_account, to_account, 
                amount, currency_type, transaction_type, 
                description, fee_currency, fee_amount, status, transaction_time
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
            "#,
        )
        .bind(&transaction.transaction_reference)
        .bind(&transaction.from_account)
        .bind(&transaction.to_account)
        .bind(transaction.amount.clone())
        .bind(&transaction.currency_type)
        .bind(transaction.transaction_type.clone())
        .bind(&transaction.description)
        .bind(&transaction.fee_currency)
        .bind(transaction.fee_amount.clone())
        .bind(transaction.status.clone())
        .bind(transaction.transaction_time)
        .execute(&self.pool)
        .await
        .map_err(|e| Status::internal(format!("Database error: {:?}", e)))?;

        // Process the actual transfer through account service
        {
            let mut account_client = self.account_client.lock().await;

            let token = self.token_cache.get_token().await
                .map_err(|e| Status::internal(format!("Token fetch failed: {}", e)))?;
            let metadata = self.create_auth_metadata(&token);
            
            // Debit from source account
            let debit_request = DebitRequest {
                account_number: req.from_account.clone(),
                amount: req.amount.clone(),
                currency_type: CurrencyType::from_str_name(&req.currency_type)
                    .ok_or_else(|| TransactionError::InvalidAmount("Invalid currency type".to_string()))? as i32,
            };

            let mut request = Request::new(debit_request);
            *request.metadata_mut() = metadata.clone();

            account_client
                .debit_account(request)
                .await
                .map_err(|e| TransactionError::AccountNotFound(e.to_string()))?;

            // Credit to destination account
            let mut credit_request = Request::new(CreditRequest {
                account_number: req.to_account.clone(),
                amount: req.amount.clone(),
                currency_type: CurrencyType::from_str_name(&req.currency_type)
                    .ok_or_else(|| TransactionError::InvalidAmount("Invalid currency type".to_string()))? as i32,
            });
            *credit_request.metadata_mut() = metadata.clone();
            account_client.credit_account(credit_request)
                .await
                .map_err(|e| TransactionError::AccountNotFound(e.to_string()))?;
        }
        // Update transaction status to completed
        transaction.status = TransactionStatus::Completed;

        sqlx::query(
            r#"
            UPDATE transactions 
            SET status = $1
            WHERE transaction_reference = $2
            "#
        )
        .bind(&transaction.status)
        .bind(&transaction.transaction_reference)
        .execute(&self.pool)
        .await
        .map_err(|e| Status::internal(format!("Failed to update transaction status: {:?}", e)))?;
        // Commit the transaction
        tx.commit().await.map_err(TransactionError::Database)?;

        // Convert Transaction to TransactionResponse
        let response = transaction.to_response();

        Ok(Response::new(response))
    }

    async fn process_deposit(
    &self,
    request: Request<TransactionRequest>
    ) -> Result<Response<TransactionResponse>, Status> {
        let req = request.into_inner();

        let amount = req.amount.parse::<Decimal>()
            .map_err(|_| Status::invalid_argument("Invalid deposit amount"))?;

        if amount <= Decimal::ZERO {
            return Err(Status::invalid_argument("Deposit amount must be positive"));
        }

        let transaction = Transaction::new(
            "".to_string(),  // no from_account for deposit
            req.to_account.clone(),
            req.amount.parse().map_err(|_| Status::invalid_argument("Invalid amount format"))?,
            req.currency_type.clone(),
            TransactionType::Deposit,
            Some(req.description.clone()),
            Some(req.metadata.clone()),
        );

        let mut tx = self.begin_transaction()
            .await
            .map_err(|e| Status::internal(format!("DB begin error: {:?}", e)))?;

        sqlx::query(
            r#"
            INSERT INTO transactions (
                transaction_reference, from_account, to_account, 
                amount, currency_type, transaction_type, 
                description, fee_currency, fee_amount, status, transaction_time
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
            "#,
        )
        .bind(&transaction.transaction_reference)
        .bind(&transaction.from_account)
        .bind(&transaction.to_account)
        .bind(transaction.amount.clone())
        .bind(&transaction.currency_type)
        .bind(transaction.transaction_type.clone())
        .bind(&transaction.description)
        .bind(&transaction.fee_currency)
        .bind(transaction.fee_amount.clone())
        .bind(transaction.status.clone())
        .bind(transaction.transaction_time)
        .execute(&self.pool)
        .await
        .map_err(|e| Status::internal(format!("Insert failed: {:?}", e)))?;

        let token = self.token_cache.get_token().await
                .map_err(|e| Status::internal(format!("Token fetch failed: {}", e)))?;
        let metadata = self.create_auth_metadata(&token);

        let mut account_client = self.account_client.lock().await;
        let mut credit_req = Request::new(CreditRequest {
            account_number: req.to_account.clone(),
            amount: req.amount.clone(),
            currency_type: CurrencyType::from_str_name(&req.currency_type)
                .ok_or_else(|| Status::invalid_argument("Invalid currency type"))? as i32,
        });
        *credit_req.metadata_mut() = metadata;

        account_client
            .credit_account(credit_req)
            .await
            .map_err(|e| {
                // Set status based on failure type
                Status::internal(format!("Credit account failed: {:?}", e))
            })?
            .into_inner();

        // Update transaction
        sqlx::query(
            r#"
            UPDATE transactions 
            SET status = $1
            WHERE transaction_reference = $2
            RETURNING 
                id,
                transaction_reference,
                from_account,
                to_account,
                amount,
                currency_type,
                transaction_type,
                description,
                fee_currency,
                fee_amount,
                status,
                transaction_time,
                metadata
            "#
        )
        .bind(TransactionStatus::Completed)
        .bind(&transaction.transaction_reference)
        .fetch_one(&mut *tx)
        .await
        .map_err(|e| Status::internal(format!("Update failed: {:?}", e)))?;

        tx.commit().await.map_err(|e| Status::internal(format!("Commit failed: {:?}", e)))?;

        // Build response
        let response = transaction.to_response();

        Ok(Response::new(response))
    }

    async fn get_transaction_by_reference(
    &self,
    request: Request<TransactionReferenceRequest>
    ) -> Result<Response<TransactionResponse>, Status> {
        let reference = request.into_inner().transaction_reference;

        let row = sqlx::query_as::<_, Transaction>(
            "SELECT * FROM transactions WHERE transaction_reference = $1"
        )
        .bind(&reference)
        .fetch_optional(&self.pool)
        .await
        .map_err(|e| Status::internal(format!("DB error: {:?}", e)))?;

        match row {
            Some(transaction) => {
                let response = transaction.to_response();
                Ok(Response::new(response))
            },
            None => Err(Status::not_found("Transaction not found")),
        }
    }

    async fn get_transactions_by_from_account(
    &self,
    request: Request<AccountRequest>
    ) -> Result<Response<TransactionsListResponse>, Status> {
        let account_number = request.into_inner().account_id;

        let transactions = sqlx::query_as::<_, Transaction>(
            "SELECT * FROM transactions WHERE from_account = $1"
        )
        .bind(&account_number)
        .fetch_all(&self.pool)
        .await
        .map_err(|e| Status::internal(format!("DB error: {:?}", e)))?;

        let list = transactions.clone()
            .into_iter()
            .map(|t| t.to_response())
            .collect();

        Ok(Response::new(TransactionsListResponse {
            transactions: list,
            total_count: transactions.len() as i32,
            page: 1,
            size: transactions.len() as i32,
        }))
    }

    async fn get_transactions_by_to_account(
    &self,
    request: Request<AccountRequest>
    ) -> Result<Response<TransactionsListResponse>, Status> {
        let account_number = request.into_inner().account_id;

        let transactions = sqlx::query_as::<_, Transaction>(
            "SELECT * FROM transactions WHERE to_account = $1"
        )
        .bind(&account_number)
        .fetch_all(&self.pool)
        .await
        .map_err(|e| Status::internal(format!("DB error: {:?}", e)))?;

        let list = transactions.clone()
            .into_iter()
            .map(|t| t.to_response())
            .collect();

        Ok(Response::new(TransactionsListResponse {
            transactions: list,
            total_count: transactions.len() as i32,
            page: 1,
            size: transactions.len() as i32,
        }))
    }

    async fn get_transactions_by_account_id(
    &self,
    request: Request<AccountTransactionsRequest>
    ) -> Result<Response<TransactionsListResponse>, Status> {
        let account_id = &request.into_inner().account_id;

        let mut tx = self.pool.acquire().await.map_err(|e| {
            Status::internal(format!("Failed to acquire DB connection: {:?}", e))
        })?;

        let from_query = sqlx::query_as::<_, Transaction>(
            r#"
            SELECT * FROM transactions
            WHERE from_account = $1
            "#
        )
        .bind(account_id)
        .fetch_all(&mut *tx)
        .await
        .map_err(|e| Status::internal(format!("DB query failed (from_account): {:?}", e)))?;

        let to_query = sqlx::query_as::<_, Transaction>(
            r#"
            SELECT * FROM transactions
            WHERE to_account = $1
            "#
        )
        .bind(account_id)
        .fetch_all(&mut *tx)
        .await
        .map_err(|e| Status::internal(format!("DB query failed (to_account): {:?}", e)))?;

        let mut all_transactions = Vec::new();
        all_transactions.extend(from_query);
        all_transactions.extend(to_query);

        // Optional: sort by transaction_time descending
        all_transactions.sort_by(|a, b| b.transaction_time.cmp(&a.transaction_time));

        let total_count = all_transactions.len() as i32;

        let response = TransactionsListResponse {
            transactions: all_transactions
                .into_iter()
                .map(Transaction::to_response)
                .collect(),
            total_count,
            page: 1,
            size: total_count,
        };

        Ok(Response::new(response))
    }

    async fn get_transaction_by_transaction_time(
    &self,
    request: Request<TransactionTimeRequest>
    ) -> Result<Response<TransactionsListResponse>, Status> {
        let time = request.into_inner().transaction_time;

        let parsed_time = chrono::DateTime::parse_from_rfc3339(&time)
            .map_err(|_| Status::invalid_argument("Invalid timestamp format"))?
            .with_timezone(&chrono::Utc);

        let transactions = sqlx::query_as::<_, Transaction>(
            "SELECT * FROM transactions WHERE DATE(transaction_time) = DATE($1)"
        )
        .bind(parsed_time)
        .fetch_all(&self.pool)
        .await
        .map_err(|e| Status::internal(format!("DB error: {:?}", e)))?;

        let list = transactions.clone()
            .into_iter()
            .map(|t| t.to_response())
            .collect();

        Ok(Response::new(TransactionsListResponse {
            transactions: list,
            total_count: transactions.len() as i32,
            page: 1,
            size: transactions.len() as i32,
        }))
    }

}