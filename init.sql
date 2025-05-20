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

---- Insert into image_caption
--INSERT INTO image_caption (image_name, image_url, caption_generated, created_at)
--VALUES
--('jacket.jpg', 'http://example.com/jacket.jpg', 'A stylish denim jacket on a mannequin.', NOW()),
--('dress.png', 'http://example.com/dress.png', 'A red evening dress displayed on a hanger.', NOW());
--
---- Insert into revised_caption
--INSERT INTO revised_caption (record_id, user_revised_caption)
--VALUES
--(1, 'Modern denim jacket with silver buttons.'),
--(2, 'Elegant red gown, perfect for formal occasions.');
--
---- Insert into search_history
--INSERT INTO search_history (record_id, search_query)
--VALUES
--(1, 'denim jacket fashion 2025'),
--(2, 'red evening dress styles');
--
---- Insert into llm_response
--INSERT INTO llm_response (user_query, response, created_at)
--VALUES
--('Describe this denim jacket', 'This is a trendy denim jacket featuring a clean design and button-up front.', NOW()),
--('What kind of dress is this?', 'It’s a floor-length red evening dress with a sleek silhouette.', NOW());
--
---- Insert into response_use_record
--INSERT INTO response_use_record (response_id, record_id)
--VALUES
--(1, 1),
--(2, 2);
--
---- Insert into response_use_search
--INSERT INTO response_use_search (response_id, search_id)
--VALUES
--(1, 1),
--(2, 2);
--
---- Insert into rate_response
--INSERT INTO rate_response (response_id, user_rate)
--VALUES
--(1, 5),
--(2, 4);
--
---- Insert into rate_caption
--INSERT INTO rate_caption (record_id, user_rate)
--VALUES
--(1, 4),
--(2, 5);
--
---- Insert into rate_search
--INSERT INTO rate_search (search_id, user_rate)
--VALUES
--(1, 5),
--(2, 4);
