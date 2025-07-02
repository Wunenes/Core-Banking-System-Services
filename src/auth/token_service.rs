use reqwest::Client;
use serde::{Deserialize, Serialize};
use std::env;
use base64::Engine;

#[derive(Debug, Serialize, Deserialize)]
struct TokenResponse {
    access_token: String,
    token_type: String,
    expires_in: u64,
    scope: String,
}

#[derive(Clone)]
pub struct TokenObtainService {
    client_id: String,
    client_secret: String,
    token_url: String,
    client: Client,
}

impl TokenObtainService {
    pub fn new() -> Self {
        let client_id = env::var("CLIENT_ID").expect("CLIENT_ID must be set");
        let client_secret = env::var("CLIENT_SECRET").expect("CLIENT_SECRET must be set");
        let token_url = env::var("AUTH_URL")
            .unwrap_or_else(|_| "http://localhost:9000".to_string())
            + "/oauth2/token";

        Self {
            client_id,
            client_secret,
            token_url,
            client: Client::new(),
        }
    }

    pub async fn obtain_token(&self) -> Result<String, Box<dyn std::error::Error>> {
        let auth = base64::engine::general_purpose::STANDARD.encode(format!("{}:{}", self.client_id, self.client_secret));

        let params = [
            ("grant_type", "client_credentials"),
            ("scope", "account:write account:read account:transaction"),
        ];

        let response = self.client
            .post(&self.token_url)
            .header("Authorization", format!("Basic {}", auth))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .form(&params)
            .send()
            .await?;

        if response.status().is_success() {
            let token_response: TokenResponse = response.json().await?;
            Ok(token_response.access_token)
        } else {
            Err(format!("Failed to obtain token: {}", response.status()).into())
        }
    }
}