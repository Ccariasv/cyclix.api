ALTER TABLE wallet_transactions
    DROP CHECK chk_wallet_transactions_type;

ALTER TABLE wallet_transactions
    ADD CONSTRAINT chk_wallet_transactions_type
        CHECK (type IN ('TOP_UP', 'TRIP_CHARGE', 'SUBSCRIPTION_PURCHASE', 'REFUND', 'ADJUSTMENT'));
