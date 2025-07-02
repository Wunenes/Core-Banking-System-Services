use jsonwebtoken::{decode, decode_header, DecodingKey, Validation, Algorithm};
use serde::{Deserialize, Serialize};
use reqwest;
use std::fmt;
use std::collections::HashMap;
use log::warn;
use base64::engine::Engine;

#[derive(Debug, Serialize, Deserialize)]
pub struct Claims {
    pub exp: usize,
    #[serde(rename = "client_id")]
    pub client_id: String,
    #[serde(default)]
    pub scope: Vec<String>,  // Changed from String to Vec<String>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub iat: Option<usize>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub iss: Option<String>,
}

#[derive(Deserialize)]
struct Jwk {
    n: String,
    e: String,
    kid: String,
}

#[derive(Deserialize)]
struct JwkSet {
    keys: Vec<Jwk>,
}

#[derive(Clone)]
pub struct JwtValidator {
    jwks_url: String,
    // Map to store multiple keys by their kid
    public_keys: HashMap<String, DecodingKey>,
}

impl fmt::Debug for JwtValidator {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("JwtValidator")
            .field("jwks_url", &self.jwks_url)
            .field("public_keys", &format!("[{} keys]", self.public_keys.len()))
            .finish()
    }
}

impl JwtValidator {
    pub fn new(auth_server_url: String) -> Self {
        Self {
            jwks_url: format!("{}/.well-known/jwks.json", auth_server_url),
            public_keys: HashMap::new(),
        }
    }

    pub async fn fetch_public_key(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        let jwks: JwkSet = reqwest::Client::new()
            .get(&self.jwks_url)
            .send()
            .await?
            .json()
            .await?;

        // Store all keys in the HashMap
        for key in jwks.keys {
            let decoding_key = DecodingKey::from_rsa_components(&key.n, &key.e)?;
            self.public_keys.insert(key.kid, decoding_key);
        }

        Ok(())
    }

    pub fn validate_token(&self, token: &str) -> Result<Claims, jsonwebtoken::errors::Error> {
        
        let header = match decode_header(token) {
            Ok(h) => h,
            Err(e) => {
                warn!("Failed to decode header: {}", e);
                return Err(e);
            }
        };

        let kid = header.kid.ok_or_else(|| {
            warn!("No 'kid' found in token header");
            jsonwebtoken::errors::Error::from(jsonwebtoken::errors::ErrorKind::InvalidToken)
        })?;
        

        let key = self.public_keys.get(&kid).ok_or_else(|| {
            warn!("No matching key found for kid: {}", kid);
            jsonwebtoken::errors::Error::from(jsonwebtoken::errors::ErrorKind::InvalidToken)
        })?;

        let mut validation = Validation::new(Algorithm::RS256);
        validation.validate_exp = true;
        validation.set_required_spec_claims(&["exp", "client_id"]);
        validation.leeway = 60;
        validation.set_audience(&["client-id"]);
        
        match decode::<Claims>(token, key, &validation) {
            Ok(token_data) => {
                Ok(token_data.claims)
            },
            Err(e) => {
                warn!("Token validation failed. Error: {}", e);
                // Use decode_header to print token parts for debugging
                if let Ok(header) = decode_header(token) {
                    warn!("Token header: {:?}", header);
                    // Try to decode the payload part without verification
                    if let Some(payload) = token.split('.').nth(1) {
                        if let Ok(decoded) = base64::engine::general_purpose::URL_SAFE_NO_PAD.decode(payload) {
                            if let Ok(payload_str) = String::from_utf8(decoded) {
                                warn!("Expected audiences: {:?}", validation.aud);
                                warn!("Raw payload: {}", payload_str);
                            }
                        }
                    }
                }
                Err(e)
            }
        }
    }

    pub fn has_required_scope(&self, scopes: &[String], method_name: &str) -> bool {
    let required_scope = if method_name.contains("InternalTransfer") || method_name.contains("ProcessDeposit") {
        "account:transaction"
    } else if method_name.contains("GetTransactionByReference")
        || method_name.contains("GetTransactionsByAccountId")
        || method_name.contains("GetTransactionByFromAccount")
        || method_name.contains("GetTransactionByToAccount")
    {
        "account:read"
    } else if method_name.contains("GetTransactionByTransactionTime") {
        "account:admin"
    } else {
        return false;
    };

    scopes.iter().any(|scope| scope == required_scope)
}

}