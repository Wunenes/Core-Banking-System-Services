use tonic::{Request, Response, Status};
use log::warn;

use crate::auth::jwt_validator::{Claims, JwtValidator};
use crate::proto::transaction::{
    transaction_service_server::TransactionService,
    TransactionRequest, TransactionResponse, TransactionReferenceRequest,
    AccountRequest, AccountTransactionsRequest, TransactionTimeRequest,
    TransactionsListResponse
};
use crate::proto::account::account_service_client::AccountServiceClient;
use crate::services::transaction_service::TransactionServiceTrait;

#[derive(Debug)]
pub struct GrpcTransactionService<T: TransactionServiceTrait + Send + Sync + 'static> {
    service: T,
    jwt_validator: JwtValidator,
}

impl<T: TransactionServiceTrait + Send + Sync + 'static> GrpcTransactionService<T> {
    pub fn new(
        service: T,
        jwt_validator: JwtValidator,
        _account_client: AccountServiceClient<tonic::transport::Channel>,
    ) -> Self {
        Self {
            service,
            jwt_validator,
        }
    }

    fn validate_request<R>(&self, request: &Request<R>, method_name: &str) -> Result<Claims, Status> {
        let metadata = request.metadata();
        let auth_header = metadata
            .get("authorization")
            .ok_or_else(|| Status::unauthenticated("No authorization header"))?;

        let auth_str = auth_header
            .to_str()
            .map_err(|_| Status::unauthenticated("Invalid authorization header"))?;

        if !auth_str.starts_with("Bearer ") {
            warn!("Invalid authorization header format: {}", auth_str);
            return Err(Status::unauthenticated("Invalid token format"));
        }

        let token = &auth_str["Bearer ".len()..];
        let claims = self.jwt_validator
            .validate_token(token)
            .map_err(|e| {
                warn!("Token validation failed: {:?}", e);
                Status::unauthenticated("Invalid token")
            })?;

        // Check scope
        if !self.jwt_validator.has_required_scope(&claims.scope, method_name) {
            warn!(
                "User {} attempted to access {} without sufficient privileges. Available scopes: {:?}", 
                claims.client_id, 
                method_name,
                claims.scope
            );
            return Err(Status::permission_denied("Insufficient privileges"));
        }

        Ok(claims)
    }
    
}

#[tonic::async_trait]
impl<T: TransactionServiceTrait + Send + Sync + 'static> TransactionService for GrpcTransactionService<T> {
    async fn internal_transfer(
        &self,
        request: Request<TransactionRequest>
    ) -> Result<Response<TransactionResponse>, Status> {
        self.validate_request(&request, "InternalTransfer")?;
        let req = request;
        let response = self
            .service.internal_transfer(req)
            .await?;

        Ok(response)

    }

    async fn process_deposit(
        &self,
        request: Request<TransactionRequest>
    ) -> Result<Response<TransactionResponse>, Status> {
        self.validate_request(&request, "ProcessDeposit")?;
        let req = request;
        let response = self
            .service.process_deposit(req)
            .await?;

        Ok(response)
    }

    async fn get_transaction_by_reference(
        &self,
        request: Request<TransactionReferenceRequest>
    ) -> Result<Response<TransactionResponse>, Status> {
        self.validate_request(&request, "GetTransactionByReference")?;
        let req = tonic::Request::new(request.into_inner());
        let response = self
            .service.get_transaction_by_reference(req)
            .await?;

        Ok(response)
    }

    async fn get_transactions_by_from_account(
        &self,
        request: Request<AccountRequest>
    ) -> Result<Response<TransactionsListResponse>, Status> {
        self.validate_request(&request, "GetTransactionsByFromAccount")?;
        let response = self
            .service.get_transactions_by_from_account(request)
            .await?;

        Ok(response)
    }

    async fn get_transactions_by_to_account(
        &self,
        request: Request<AccountRequest>
    ) -> Result<Response<TransactionsListResponse>, Status> {
        self.validate_request(&request, "GetTransactionsByToAccount")?;
        let response = self
            .service.get_transactions_by_to_account(request)
            .await?;

        Ok(response)
    }

    async fn get_transactions_by_account_id(
        &self,
        request: Request<AccountTransactionsRequest>
    ) -> Result<Response<TransactionsListResponse>, Status> {
        self.validate_request(&request, "GetTransactionsByAccountId")?;
        let req = request;
        let response = self
            .service.get_transactions_by_account_id(req)
            .await?;

        Ok(response)
    }

    async fn get_transaction_by_transaction_time(
        &self,
        request: Request<TransactionTimeRequest>
    ) -> Result<Response<TransactionsListResponse>, Status> {
        self.validate_request(&request, "GetTransactionByTransactionTime")?;
        let req = request;
        let response = self
            .service.get_transaction_by_transaction_time(req)
            .await?;

        Ok(response)
    }
}