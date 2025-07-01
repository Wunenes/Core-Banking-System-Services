use crate::models::AccountType;
use rand::rngs::StdRng;
use rand::{Rng, SeedableRng};

pub struct AccountNumberGenerator;

impl AccountNumberGenerator {
    pub fn generate(account_type: &AccountType) -> String {
        let account_code = match account_type {
            AccountType::Savings => "02",
            AccountType::Checking => "04",
            AccountType::Internal => "05",
        };

        // Create a cryptographically secure RNG
        let mut rng = StdRng::from_entropy();
        
        // Build base number (without checksum)
        let random_part = format!("{:04}", rng.gen_range(0..100000));
        let base_number = format!("00{}{}", account_code, random_part);

        // Add Luhn checksum
        format!("{}{}", base_number, Self::calculate_luhn_checksum(&base_number))
    }

    fn calculate_luhn_checksum(base: &str) -> u8 {
        let mut sum = 0;
        let mut alternate = false;

        for c in base.chars().rev() {
            let mut digit = c.to_digit(10).unwrap() as i32;
            
            if alternate {
                digit *= 2;
                if digit > 9 {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }

        ((10 - (sum % 10)) % 10) as u8
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_account_number_generation() {
        let account_number = AccountNumberGenerator::generate(&AccountType::Savings);
        assert_eq!(account_number.len(), 9); // 8 digits + 1 checksum
        assert!(account_number.starts_with("00")); // Branch code
        assert!(account_number[2..4].eq("02")); // Savings account code
    }

    #[test]
    fn test_luhn_checksum() {
        let base = "12345678";
        let checksum = AccountNumberGenerator::calculate_luhn_checksum(base);
        assert!(checksum <= 9);
    }
}