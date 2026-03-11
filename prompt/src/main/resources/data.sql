INSERT INTO plans (plan_name, price, token_limit, ai_use, created_at, updated_at)
SELECT 'NORMAL', 0, 10000, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM plans WHERE plan_name = 'NORMAL');

INSERT INTO plans (plan_name, price, token_limit, ai_use, created_at, updated_at)
SELECT 'PRO', 9900, 50000, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM plans WHERE plan_name = 'PRO');

INSERT INTO plans (plan_name, price, token_limit, ai_use, created_at, updated_at)
SELECT 'MAX', 19900, 150000, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM plans WHERE plan_name = 'MAX');