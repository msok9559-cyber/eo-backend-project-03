-- plans (PlanEntity 기준: plan_name, token_limit, ai_use, price)
INSERT INTO plans (
    plan_name, token_limit, ai_use, price,
    daily_chat_limit, image_upload_limit, file_upload_limit, file_size_limit,
    created_at, updated_at
)
VALUES ('NORMAL', 10000, 1, 0, 20, 5, 3, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO plans (
    plan_name, token_limit, ai_use, price,
    daily_chat_limit, image_upload_limit, file_upload_limit, file_size_limit,
    created_at, updated_at
)
VALUES ('PRO', 100000, 1, 9900, 100, 20, 10, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO plans (
    plan_name, token_limit, ai_use, price,
    daily_chat_limit, image_upload_limit, file_upload_limit, file_size_limit,
    created_at, updated_at
)
VALUES ('MAX', 500000, 1, 29900, -1, 50, 30, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- plan_models
INSERT INTO plan_models (plan_id, model_name) VALUES (1, 'alan-4.0');
INSERT INTO plan_models (plan_id, model_name) VALUES (2, 'alan-4.0');
INSERT INTO plan_models (plan_id, model_name) VALUES (2, 'alan-4.1');
INSERT INTO plan_models (plan_id, model_name) VALUES (3, 'alan-4.0');
INSERT INTO plan_models (plan_id, model_name) VALUES (3, 'alan-4.1');
INSERT INTO plan_models (plan_id, model_name) VALUES (3, 'alan-4-turbo');

-- users 테스트 계정 (비밀번호: test1234)
INSERT INTO users (plan_id, userid, username, password, email, used_token, active, locked, created_at, updated_at)
VALUES (1, 'testuser', '테스트유저', '$2a$10$HXPeKfi7mPp2c06omWGQXuXjD1wI9GSbaT9H//bV2mQ2AtL48cbFC', 'test@test.com', 0, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- admin
-- 비밀번호: admin1234
DELETE FROM admin WHERE admin_id = 'admin';

INSERT INTO admin (admin_id, admin_name, password, created_at)
VALUES ('admin', '관리자', '$2a$10$gotJuh1CdusjYeArK8tUd.QwBg7oBx3cVZs87QKV0v9yT4EsgeJTW', CURRENT_TIMESTAMP);
