from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
import random

app = Flask(__name__)
# app.config['SQLALCHEMY_DATABASE_URI'] = 'mysql+pymysql://root:1234@localhost:3306/image_captioning'
app.config['SQLALCHEMY_DATABASE_URI'] = 'mysql+pymysql://root:1234@db:3306/image_captioning'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False  # Disable modification tracking for performance
db = SQLAlchemy(app)

class Image(db.Model):
    __tablename__ = 'image_caption'
    record_id = db.Column(db.Integer, primary_key=True)
    image_name = db.Column(db.String(255))
    image_url = db.Column(db.Text)
    caption_generated = db.Column(db.Text)
    created_at = db.Column(db.DateTime)

@app.route('/images/advise', methods=['POST'])
def generate_advise_from_images():
    data = request.get_json()

    if not data or 'captions' not in data or 'question' not in data:
        return jsonify({"error": "Missing captions or question"}), 400

    captions = data['captions']  # expect list of strings
    question = data['question']  # expect string

    # Fake answer logic (replace with real model logic)
    fake_answer = f"This is a fake answer for: {question}, based on {len(captions)} captions"

    return jsonify({
        "answer": fake_answer
    })

@app.route('/query/advise', methods=['POST'])
def generate_advise_from_query():
    data = request.get_json()

    if 'question' not in data:
        return jsonify({"error": "Missing 'question'"}), 400

    question = data['question']

    # Fake answer generation (replace this with model inference)
    fake_answer = f"This is a fake answer for: {question}"

    # Get 1 to 3 random images from the database
    all_images = Image.query.all()
    selected_images = random.sample(all_images, min(len(all_images), random.randint(1, 3)))

    image_results = [
        {
            "record_id": img.record_id,
            "image_url": img.image_url
        }
        for img in selected_images
    ]

    return jsonify({
        "answer": fake_answer,
        "images": image_results
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
