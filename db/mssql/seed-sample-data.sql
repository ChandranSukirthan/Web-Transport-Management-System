/*
 QuickMove sample dataset for SQL Server (TransportDB)
 - Inserts sample Drivers, Rides, and Tracking Sessions
 - Uses safe upserts (IF NOT EXISTS) to avoid duplicates on re-run
 - Assumes Hibernate created tables with names: drivers, rides, tracking_sessions
*/

SET NOCOUNT ON;
SET XACT_ABORT ON;

-- Create database if missing, then use it
IF DB_ID('TransportDB') IS NULL
BEGIN
    PRINT 'Creating database [TransportDB]...';
    CREATE DATABASE TransportDB;
END
GO

USE TransportDB;
GO

BEGIN TRAN;

-- Seed Drivers
INSERT INTO drivers (name, userId, vehicleType, numberPlate, status)
SELECT v.name, v.userId, v.vehicleType, v.numberPlate, v.status
FROM (VALUES
    ('Alex Johnson', 'd_alex',  'car',  'GJ-01-AB-1234', 'AVAILABLE'),
    ('Priya Singh',  'd_priya', 'auto', 'GJ-02-XY-9876', 'BUSY'),
    ('Rohan Mehta',  'd_rohan', 'bike', 'GJ-03-QW-4321', 'AVAILABLE'),
    ('Sara Khan',    'd_sara',  'car',  'GJ-04-LM-1122', 'OFFLINE'),
    ('John Doe',     'd_john',  'bike', 'GJ-05-ZZ-7788', 'AVAILABLE')
) AS v(name, userId, vehicleType, numberPlate, status)
WHERE NOT EXISTS (
    SELECT 1 FROM drivers d WHERE d.userId = v.userId
);

-- Seed Rides
-- Note: driverId column in rides is a string (stores driver's userId), not a FK
INSERT INTO rides (userId, vehicleType, pickupLocation, dropoffLocation, fare, distance, duration, status, driverId)
SELECT v.userId, v.vehicleType, v.pickupLocation, v.dropoffLocation, v.fare, v.distance, v.duration, v.status, v.driverId
FROM (VALUES
    ('u_rider1', 'car',  'Navrangpura',  'Maninagar',   240.50, 12.3, 28.5, 'COMPLETED', 'd_priya'),
    ('u_rider2', 'bike', 'CG Road',      'Gandhinagar', 120.00, 22.0, 45.0, 'BOOKED',    'd_rohan'),
    ('u_rider3', 'auto', 'Vastrapur Lake','Iscon Mall',  80.00,  5.0, 15.0, 'PENDING',    NULL),
    ('u_rider4', 'car',  'Airport',      'Bopal',         0.00,  0.0,  0.0, 'CANCELLED', 'd_sara'),
    ('u_rider5', 'bike', 'Sabarmati',    'Law Garden',    60.00,  6.5, 18.0, 'COMPLETED', 'd_alex'),
    ('u_rider6', 'auto', 'Paldi',        'Naranpura',     95.00,  9.2, 25.0, 'BOOKED',   'd_john')
) AS v(userId, vehicleType, pickupLocation, dropoffLocation, fare, distance, duration, status, driverId)
WHERE NOT EXISTS (
    SELECT 1 FROM rides r
    WHERE r.userId = v.userId
      AND r.pickupLocation = v.pickupLocation
      AND r.dropoffLocation = v.dropoffLocation
      AND r.status = v.status
);

-- Capture Driver IDs by userId for FK inserts into tracking_sessions
DECLARE @d_alex  BIGINT = (SELECT id FROM drivers WHERE userId = 'd_alex');
DECLARE @d_priya BIGINT = (SELECT id FROM drivers WHERE userId = 'd_priya');
DECLARE @d_rohan BIGINT = (SELECT id FROM drivers WHERE userId = 'd_rohan');
DECLARE @d_sara  BIGINT = (SELECT id FROM drivers WHERE userId = 'd_sara');
DECLARE @d_john  BIGINT = (SELECT id FROM drivers WHERE userId = 'd_john');

-- Capture Ride IDs by rider and status
DECLARE @r_u1_completed BIGINT = (SELECT TOP 1 id FROM rides WHERE userId = 'u_rider1' AND status = 'COMPLETED' ORDER BY id DESC);
DECLARE @r_u2_booked    BIGINT = (SELECT TOP 1 id FROM rides WHERE userId = 'u_rider2' AND status = 'BOOKED'    ORDER BY id DESC);
DECLARE @r_u3_pending   BIGINT = (SELECT TOP 1 id FROM rides WHERE userId = 'u_rider3' AND status = 'PENDING'   ORDER BY id DESC);
DECLARE @r_u5_completed BIGINT = (SELECT TOP 1 id FROM rides WHERE userId = 'u_rider5' AND status = 'COMPLETED' ORDER BY id DESC);

-- Seed Tracking Sessions (FKs: ride_id -> rides.id, driver_id -> drivers.id)
-- Completed session for rider1 & Priya
IF @r_u1_completed IS NOT NULL AND @d_priya IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM tracking_sessions WHERE ride_id = @r_u1_completed AND driver_id = @d_priya
)
BEGIN
    INSERT INTO tracking_sessions (ride_id, driver_id, rider_user_id, start_time, end_time, status, created_by, driver_accepted, rider_accepted)
    VALUES (@r_u1_completed, @d_priya, 'u_rider1', DATEADD(MINUTE, -30, SYSDATETIME()), SYSDATETIME(), 'ENDED', 'system', 1, 1);
END

-- Active session for rider2 & Rohan
IF @r_u2_booked IS NOT NULL AND @d_rohan IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM tracking_sessions WHERE ride_id = @r_u2_booked AND driver_id = @d_rohan
)
BEGIN
    INSERT INTO tracking_sessions (ride_id, driver_id, rider_user_id, start_time, end_time, status, created_by, driver_accepted, rider_accepted)
    VALUES (@r_u2_booked, @d_rohan, 'u_rider2', DATEADD(MINUTE, -5, SYSDATETIME()), NULL, 'ACTIVE', 'rider', 0, 1);
END

-- Pending ride has no session yet; create a requested session waiting for driver acceptance
IF @r_u3_pending IS NOT NULL AND @d_john IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM tracking_sessions WHERE ride_id = @r_u3_pending AND driver_id = @d_john
)
BEGIN
    INSERT INTO tracking_sessions (ride_id, driver_id, rider_user_id, start_time, end_time, status, created_by, driver_accepted, rider_accepted)
    VALUES (@r_u3_pending, @d_john, 'u_rider3', SYSDATETIME(), NULL, 'ACTIVE', 'rider', 0, 1);
END

-- Completed session for rider5 & Alex
IF @r_u5_completed IS NOT NULL AND @d_alex IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM tracking_sessions WHERE ride_id = @r_u5_completed AND driver_id = @d_alex
)
BEGIN
    INSERT INTO tracking_sessions (ride_id, driver_id, rider_user_id, start_time, end_time, status, created_by, driver_accepted, rider_accepted)
    VALUES (@r_u5_completed, @d_alex, 'u_rider5', DATEADD(MINUTE, -20, SYSDATETIME()), SYSDATETIME(), 'ENDED', 'system', 1, 1);
END

COMMIT TRAN;

PRINT 'Sample data seed complete.';

