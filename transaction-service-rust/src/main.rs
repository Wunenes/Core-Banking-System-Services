use dotenv::dotenv;
use sqlx::postgres::PgPoolOptions;
use tonic::transport::Server;
use std::env;

mod auth;
mod errors;
mod models;
mod services;
mod grpc;

use crate::auth::token_cache::TokenCache;
use std::sync::Arc;
use crate::proto::transaction::transaction_service_server::TransactionServiceServer;
use crate::proto::account::account_service_client::AccountServiceClient;
use services::TransactionService;
use auth::jwt_validator::JwtValidator;
use grpc::GrpcTransactionService;
 
pub mod proto {
    pub mod transaction {
        tonic::include_proto!("transaction");
    }
    pub mod account {
        tonic::include_proto!("account");
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    dotenv().ok();
    unsafe {
        env::set_var("RUST_LOG", "info");
    }
    env_logger::init();

    let database_url = std::env::var("DATABASE_URL").expect("DATABASE_URL must be set");
    let auth_server_url = std::env::var("AUTH_URL").expect("AUTH_URL must be set");
    let _port = std::env::var("GRPC_PORT").expect("GRPC_PORT must be set")
        .parse::<u16>()
        .expect("GRPC_PORT must be a valid port number");
    let host = std::env::var("HOST").expect("GRPC_HOST must be set");
    let addr = format!("{}:{}", host, _port).parse()?;
    let account_service_url = std::env::var("ACCOUNT_SERVICE_URL").expect("ACCOUNT_SERVICE_URL must be set");

    // Initialize JWT validator
    let mut jwt_validator = JwtValidator::new(auth_server_url);
    jwt_validator.fetch_public_key().await?;

    // Create connection pool
    let pool = PgPoolOptions::new()
        .max_connections(5)
        .connect(&database_url)
        .await?;

    println!("Connected to database at {}", database_url);

    // Initialize account service client
    let account_client = AccountServiceClient::connect(account_service_url.clone()).await?;
    println!("Connected to account service at {}", account_service_url);

    // Initialize transaction service implementation
    let token_cache = Arc::new(TokenCache::new());
    let transaction_service = TransactionService::new(pool, account_client.clone(), token_cache);

    // Initialize gRPC service implementation
    let grpc_service = GrpcTransactionService::new(
        transaction_service,
        jwt_validator,
        account_client,
    );

    println!("Transaction service initialized");
    println!("Starting gRPC server on {}", addr);

    Server::builder()
        .add_service(TransactionServiceServer::new(grpc_service))
        .serve(addr)
        .await?;

    Ok(())
}
