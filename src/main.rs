use auth::jwt_validator::JwtValidator;
use dotenv::dotenv;
use sqlx::postgres::PgPoolOptions;
use tonic::transport::Server;
use std::env;

mod errors;
mod grpc;
mod models;
mod services;
mod utils;
mod auth;

use grpc::{account::account_service_server::AccountServiceServer, GrpcAccountService};
use services::AccountService;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    dotenv().ok();
    env::set_var("RUST_LOG", "info");
    env_logger::init();

    let database_url = std::env::var("DATABASE_URL").expect("DATABASE_URL must be set");
    let auth_server_url = std::env::var("AUTH_URL").unwrap_or_else(|_| "http://localhost:9000".to_string());
    let port = std::env::var("GRPC_PORT").unwrap_or_else(|_| "9092".to_string());
    let addr = format!("0.0.0.0:{}", port).parse()?;

    // Initialize JWT validator
    let mut jwt_validator = JwtValidator::new(auth_server_url);
    jwt_validator.fetch_public_key().await?;

    // Create connection pool
    let pool = PgPoolOptions::new()
        .max_connections(5)
        .connect(&database_url)
        .await?;

    // Create services
    let account_service = AccountService::new(pool);
    let grpc_service = GrpcAccountService::new(account_service, jwt_validator);

    println!("Starting gRPC server on {}", addr);

    Server::builder()
        .add_service(AccountServiceServer::new(grpc_service))
        .serve(addr)
        .await?;

    Ok(())
}
