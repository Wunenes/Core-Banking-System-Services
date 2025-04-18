# Account Microservice

## Overview
The Account Microservice is a core component of our banking system that manages all account-related operations. It provides RESTful APIs for account creation, management, and transaction processing.

## Features
- Account creation and management
- Account status management (Active, Inactive, Frozen, Closed, Dormant)
- Multi-currency support (KES, USD, EUR, GBP)
- Balance management (Current and Available balance)
- Transaction processing (Credit and Debit operations)
- Account freezing/unfreezing capabilities
- Account deletion with balance transfer

## Technical Stack
- Java
- Spring Boot
- Spring Data JPA
- Hibernate
- PostgresSQL
- Maven

## API Endpoints

### Account Management
- `POST /api/account/create` - Create a new account
- `GET /api/account?accountNumber={accountNumber}` - Get account details
- `DELETE /api/account/delete` - Delete an account

### Transaction Operations
- `POST /api/account/credit` - Credit an account
- `POST /api/account/debit` - Debit an account

### Account Status Management
- `POST /api/account/freeze_actions` - Freeze or unfreeze an account

## Account Types
- Checking Account
- Savings Account
- Loan Account
- Foreign Account

## Account Statuses
- ACTIVE
- INACTIVE
- FROZEN
- CLOSED
- DORMANT

## Business Rules
1. **Account Creation**
    - Checking accounts are created with INACTIVE status
    - Other account types are created with ACTIVE status
    - Unique account numbers are generated automatically

2. **Account Activation**
    - Inactive accounts are automatically activated when credited with more than KES 200.00

3. **Transaction Rules**
    - Currency conversion is applied when transaction currency differs from account currency at the pairs' exchange rate
    - Debit operations require sufficient available balance
    - Frozen, Closed, or Dormant accounts cannot process transactions
    - Inactive accounts cannot process debit operations

4. **Account Deletion**
    - Frozen accounts cannot be deleted
    - Accounts with positive balance require a receiving account for balance transfer
    - Account status is set to CLOSED upon deletion

## Error Handling
The service handles various business exceptions:
- `AccountNotFoundException`
- `IneligibleAccountException`
- `InsufficientFundsException`

## Data Model
### Account Entity
- Unique identifier (ID)
- User ID (UUID)
- Account Number (unique)
- Current Balance
- Available Balance
- Account Type
- Account Status
- Currency Type
- Creation Timestamp
- Update Timestamp
- Interest Rate (only in Saving's and Loan accounts)

## Security Considerations
- Input validation using Jakarta Validation
- Currency type validation
- Amount validation (non-negative, decimal precision)
- Account status validation for operations

## Getting Started
1. Clone the repository
2. Configure database properties in `application.properties`
3. Run the application using Maven:
   ```bash
   mvn spring-boot:run
   ```

## Dependencies
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Lombok
- Jakarta Validation
- Hibernate

## Contributing
Please read CONTRIBUTING.md for details on our code of conduct and the process for submitting pull requests.

## License
This project is licensed under the MIT License - see the LICENSE.md file for details