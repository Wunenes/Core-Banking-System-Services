use chrono::Utc;
use tonic::{Request, Response, Status};
use num_traits::FromPrimitive;
use log::warn;

pub mod account {
    tonic::include_proto!("account");
}

use account::account_service_server::AccountService;
use account::{
    AccountResponse, AccountsListResponse, CreateAccountRequest, CreditRequest, CreditResponse,
    DebitRequest, DebitResponse, DeleteRequest, DeleteResponse, FreezeActionRequest,
    FreezeActionResponse, GetAccountRequest, GetAccountsByUserIdRequest,
};

use crate::{
    auth::jwt_validator::{JwtValidator, Claims},
    errors::AccountError,
    models::{AccountType, CurrencyType},
    services::account_service::AccountServiceTrait as ServiceTrait,
};

#[derive(Debug)]
pub struct GrpcAccountService<T: ServiceTrait + Send + Sync + 'static> {
    service: T,
    jwt_validator: JwtValidator,
}

impl<T: ServiceTrait + Send + Sync + 'static> GrpcAccountService<T> {
    pub fn new(service: T, jwt_validator: JwtValidator) -> Self {
        Self { 
            service,
            jwt_validator,
        }
    }

    fn map_error(error: AccountError) -> Status {
        match error {
            AccountError::NotFound(msg) => Status::not_found(msg),
            AccountError::InsufficientFunds {
                current_balance,
                requested_amount,
            } => Status::failed_precondition(format!(
                "Insufficient funds: current balance {}, requested {}",
                current_balance, requested_amount
            )),
            AccountError::IneligibleAccount {
                account_number,
                operation,
            } => Status::failed_precondition(format!(
                "Account {} is not eligible for operation {}",
                account_number, operation
            )),
            AccountError::InvalidAmount(msg) => Status::invalid_argument(msg),
            AccountError::InvalidCurrency(msg) => Status::invalid_argument(msg),
            AccountError::Internal(msg) => Status::internal(msg),
            AccountError::DatabaseError(msg) => Status::internal(msg.to_string()),
        }
    }

    fn account_to_response(account: crate::models::Account) -> AccountResponse {
        AccountResponse {
            account_type: account.account_type as i32,
            account_status: account.account_status as i32,
            current_balance: account.current_balance.to_string(),
            available_balance: account.available_balance.to_string(),
            currency_type: account.currency_type as i32,
            account_number: account.account_number,
            interest_rate: account.interest_rate.to_string(),
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
impl<T: ServiceTrait + Send + Sync + 'static> AccountService for GrpcAccountService<T> {
    async fn create_account(
        &self,
        request: Request<CreateAccountRequest>,
    ) -> Result<Response<AccountResponse>, Status> {
        // Validate JWT token first
        self.validate_request(&request, "CreateAccount")?;
        
        let req = request.into_inner();
        let account_type = AccountType::from_i32(req.account_type)
            .ok_or_else(|| Status::invalid_argument("Invalid account type"))?;
        let currency_type = CurrencyType::from_i32(req.currency_type)
            .ok_or_else(|| Status::invalid_argument("Invalid currency type"))?;

        let user_id = uuid::Uuid::parse_str(&req.user_id)
            .map_err(|_| Status::invalid_argument("Invalid user_id format"))?;
        let account = self
            .service
            .create_account(
                user_id,
                account_type,
                currency_type,
                req.current_balance,
                req.interest_rate,
            )
            .await
            .map_err(Self::map_error)?;

        Ok(Response::new(Self::account_to_response(account)))
    }

    async fn get_account_details(
        &self,
        request: Request<GetAccountRequest>,
    ) -> Result<Response<AccountResponse>, Status> {
        // Validate JWT token first
        self.validate_request(&request, "GetAccountDetails")?;
        
        let req = request.into_inner();
        let account = self
            .service
            .get_account_details(&req.account_number)
            .await
            .map_err(Self::map_error)?;

        Ok(Response::new(Self::account_to_response(account)))
    }

    async fn credit_account(
        &self,
        request: Request<CreditRequest>,
    ) -> Result<Response<CreditResponse>, Status> {
        // Validate JWT token first
        self.validate_request(&request, "CreditAccount")?;
        
        let req = request.into_inner();
        let currency_type = CurrencyType::from_i32(req.currency_type)
            .ok_or_else(|| Status::invalid_argument("Invalid currency type"))?;

        let account = self
            .service
            .credit_account(&req.account_number, &req.amount, currency_type)
            .await
            .map_err(Self::map_error)?;

        Ok(Response::new(CreditResponse {
            account_number: account.account_number,
            amount: req.amount,
            currency_type: req.currency_type,
            new_balance: account.current_balance.to_string(),
        }))
    }
 
    async fn debit_account(
        &self,
        request: Request<DebitRequest>,
    ) -> Result<Response<DebitResponse>, Status> {
        // Validate JWT token first
        self.validate_request(&request, "DebitAccount")?;
        
        let req = request.into_inner();
        let currency_type = CurrencyType::from_i32(req.currency_type)
            .ok_or_else(|| Status::invalid_argument("Invalid currency type"))?;

        let account = self
            .service
            .debit_account(&req.account_number, &req.amount, currency_type)
            .await
            .map_err(Self::map_error)?;

        Ok(Response::new(DebitResponse {
            account_number: account.account_number,
            amount: req.amount,
            currency_type: req.currency_type,
            new_balance: account.current_balance.to_string(),
        }))
    }

    async fn freeze_action(
        &self,
        request: Request<FreezeActionRequest>,
    ) -> Result<Response<FreezeActionResponse>, Status> {
        // Validate JWT token first
        self.validate_request(&request, "FreezeAction")?;
        
        let req = request.into_inner();
        let account = self
            .service
            .freeze_action(&req.action, &req.account_number, &req.reason)
            .await
            .map_err(Self::map_error)?;

        Ok(Response::new(FreezeActionResponse {
            action: req.action,
            account_number: account.account_number,
            reason: req.reason,
            timestamp: Utc::now().to_rfc3339(),
        }))
    }

    async fn delete_account(
        &self,
        request: Request<DeleteRequest>,
    ) -> Result<Response<DeleteResponse>, Status> {
        // Validate JWT token first
        self.validate_request(&request, "DeleteAccount")?;
        
        let req = request.into_inner();
        let account = self
            .service
            .delete_account(&req.account_number, &req.receiving_account_number)
            .await
            .map_err(Self::map_error)?;

        Ok(Response::new(DeleteResponse {
            account_number: account.account_number,
            timestamp: Utc::now().to_rfc3339(),
            credit_response: None,
            debit_response: None,
        }))
    }

    async fn get_account_details_by_user_id(
        &self,
        request: Request<GetAccountsByUserIdRequest>,
    ) -> Result<Response<AccountsListResponse>, Status> {
        // Validate JWT token first
        self.validate_request(&request, "GetAccountDetailsByUserId")?;
        
        let user_id = uuid::Uuid::parse_str(&request.into_inner().user_id)
            .map_err(|_| Status::invalid_argument("Invalid user_id format"))?;
        let accounts = self
            .service
            .get_accounts_by_user_id(user_id)
            .await
            .map_err(Self::map_error)?;

        let account_responses = accounts
            .into_iter()
            .map(Self::account_to_response)
            .collect();

        Ok(Response::new(AccountsListResponse {
            accounts: account_responses,
        }))
    }
}