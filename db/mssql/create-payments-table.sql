-- filepath: db/mssql/create-payments-table.sql
-- Create payments table compatible with com.quickmove.entity.Payment

IF OBJECT_ID('dbo.payments', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.payments (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        ride_id BIGINT NULL,
        user_id NVARCHAR(128) NULL,
        driver_id NVARCHAR(128) NULL,
        vehicle_type NVARCHAR(64) NULL,
        amount DECIMAL(18,2) NULL,
        method VARCHAR(16) NULL,
        status VARCHAR(32) NULL,
        card_number VARCHAR(64) NULL,
        cvc VARCHAR(16) NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL
    );
    PRINT 'Created table dbo.payments';
END
ELSE
BEGIN
    PRINT 'Table dbo.payments already exists';
END

