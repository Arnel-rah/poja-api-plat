
CREATE TABLE IF NOT EXISTS prompt_app_request (
                                                  id VARCHAR(255) PRIMARY KEY,
                                                  org_id VARCHAR(255) NOT NULL,
                                                  user_id VARCHAR(255) NOT NULL,
                                                  prompt TEXT NOT NULL,
                                                  status VARCHAR(20) NOT NULL,
                                                  application_id VARCHAR(255),
                                                  application_name VARCHAR(255),
                                                  error_message TEXT,
                                                  created_at TIMESTAMP DEFAULT NOW(),
                                                  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_prompt_app_request_org_id ON prompt_app_request(org_id);
CREATE INDEX idx_prompt_app_request_status ON prompt_app_request(status);
CREATE INDEX idx_prompt_app_request_created_at ON prompt_app_request(created_at);