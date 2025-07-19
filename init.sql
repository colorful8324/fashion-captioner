CREATE DATABASE IF NOT EXISTS image_captioning;
USE image_captioning;

-- Table: image_caption
CREATE TABLE image_caption (
    record_id INT PRIMARY KEY AUTO_INCREMENT,
    image_name VARCHAR(255),
    image_url TEXT,
    caption_generated TEXT,
    created_at DATETIME
);

-- Table: revised_caption
CREATE TABLE revised_caption (
    revised_id INT PRIMARY KEY AUTO_INCREMENT,
    record_id INT,
    user_revised_caption TEXT,
    FOREIGN KEY (record_id) REFERENCES image_caption(record_id)
);

-- Table: search_history
CREATE TABLE search_history (
    search_id INT PRIMARY KEY AUTO_INCREMENT,
    record_id INT,
    search_query TEXT,
    FOREIGN KEY (record_id) REFERENCES image_caption(record_id)
);

-- Table: response_use_record
CREATE TABLE response_use_record (
    response_id INT,
    record_id INT,
    PRIMARY KEY (response_id, record_id),
    FOREIGN KEY (record_id) REFERENCES image_caption(record_id)
);

-- Table: response_use_search
CREATE TABLE response_use_search (
    response_id INT,
    search_id INT,
    PRIMARY KEY (response_id, search_id),
    FOREIGN KEY (search_id) REFERENCES search_history(search_id)
);

-- Table: llm_response
CREATE TABLE llm_response (
    response_id INT PRIMARY KEY AUTO_INCREMENT,
    user_query TEXT,
    response LONGTEXT,
    created_at DATETIME
);

-- Table: rate_response
CREATE TABLE rate_response (
    rate_response_id INT PRIMARY KEY AUTO_INCREMENT,
    response_id INT,
    user_rate INT,
    FOREIGN KEY (response_id) REFERENCES llm_response(response_id)
);

-- Table: rate_caption
CREATE TABLE rate_caption (
    rate_id INT PRIMARY KEY AUTO_INCREMENT,
    record_id INT,
    user_rate INT,
    FOREIGN KEY (record_id) REFERENCES image_caption(record_id)
);

-- Table: rate_search
CREATE TABLE rate_search (
    rate_search_id INT PRIMARY KEY AUTO_INCREMENT,
    search_id INT,
    user_rate INT,
    FOREIGN KEY (search_id) REFERENCES search_history(search_id)
);