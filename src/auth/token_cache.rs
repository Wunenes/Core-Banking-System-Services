use std::sync::Arc;
use std::time::{Duration, Instant};
use tokio::sync::RwLock;
use crate::auth::token_service::TokenObtainService;

#[derive(Clone)]
pub struct CachedToken {
    pub token: String,
    pub expires_at: Instant,
}

pub struct TokenCache {
    inner: Arc<RwLock<Option<CachedToken>>>,
    token_service: TokenObtainService,
}

impl TokenCache {
    pub fn new() -> Self {
        Self {
            inner: Arc::new(RwLock::new(None)),
            token_service: TokenObtainService::new(),
        }
    }

    pub async fn get_token(&self) -> Result<String, String> {
        {
            let read_guard = self.inner.read().await;
            if let Some(cached) = &*read_guard {
                if cached.expires_at > Instant::now() + Duration::from_secs(30) {
                    return Ok(cached.token.clone());
                }
            }
        }

        // Fetch new token
        let new_token = self.token_service.obtain_token().await
            .map_err(|e| format!("Failed to get token: {}", e))?;

        let mut write_guard = self.inner.write().await;
        *write_guard = Some(CachedToken {
            token: new_token.clone(),
            expires_at: Instant::now() + Duration::from_secs(300),
        });

        Ok(new_token)
    }
}
