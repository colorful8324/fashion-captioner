import json
import os
import random
import time
import logging
from typing import List
from duckduckgo_search.exceptions import RatelimitException

from fastapi import FastAPI, UploadFile, File, Form
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from google import genai
import requests
from sqlalchemy import create_engine, Column, Integer, String, Text, DateTime
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from sqlalchemy.exc import OperationalError
from duckduckgo_search import DDGS

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = FastAPI()

# Database setup
SQLALCHEMY_DATABASE_URL = "mysql+pymysql://root:1234@db:3306/image_captioning"
# SQLALCHEMY_DATABASE_URL = "mysql+pymysql://root:1234@localhost:3306/image_captioning"
engine = create_engine(SQLALCHEMY_DATABASE_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

class Image(Base):
    __tablename__ = 'image_caption'
    record_id = Column(Integer, primary_key=True)
    image_name = Column(String(255))
    image_url = Column(Text)
    caption_generated = Column(Text)
    created_at = Column(DateTime)

# Retry database connection
max_retries = 5
retry_delay = 5  # seconds

for attempt in range(max_retries):
    try:
        Base.metadata.create_all(bind=engine)
        print("Successfully connected to database")
        break
    except OperationalError as e:
        if attempt < max_retries - 1:
            print(f"Database connection attempt {attempt + 1} failed. Retrying in {retry_delay} seconds...")
            time.sleep(retry_delay)
        else:
            print(f"Warning: Could not connect to database after {max_retries} attempts: {e}")

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def truncate_response(text: str, max_length: int = 1000) -> str:
    """Truncate response text to fit database column size"""
    if len(text) <= max_length:
        return text
    return text[:max_length-3] + "..."

class ImageAdviceRequest(BaseModel):
    captions: List[str]
    question: str

class QueryRequest(BaseModel):
    question: str

class FashionAdvice(BaseModel):
    clothes: list[str]
    how_to_fit_it: str

@app.post("/images/advise")
async def _get_image_advice(request: ImageAdviceRequest):
    logger.info(f"Received request for /images/advise with question: {request.question}")
    logger.info(f"Number of captions received: {len(request.captions)}")
    logger.info(f"First caption: {request.captions[0]}")

    prompt = f"""
    You are a fashion assistant. Your task is to give users advices based on the description of the clothes they give and
    their question. Answer in Vietnamese.

    Description of clothes: {request.captions[0]}

    User's question: {request.question}
    """
    logger.info("Generated prompt for LLM")

    try:
        llm = genai.Client(api_key="AIzaSyC0SFy8iUZycmWXbUtTA6IEgof2O1PS5jc")
        llm_response = llm.models.generate_content(
            model="gemini-2.0-flash",
            contents=prompt
        )
        logger.info("Successfully received response from LLM")
        logger.info(f"Response length: {len(llm_response.text)}")

        return JSONResponse({
            "answer": llm_response.text
        })
    except Exception as e:
        logger.error(f"Error in /images/advise: {str(e)}", exc_info=True)
        return JSONResponse(
            status_code=500,
            content={"error": f"Error generating advice: {str(e)}"}
        )

@app.post("/query/advise")
async def _get_query_advice(request: QueryRequest):
    logger.info(f"Received request for /query/advise with question: {request.question}")
    
    try:
        prompt = f"""
            You are a fashion assistant. Your task is to give users advice based on their question.
            The clothes field is a list of description of the clothes without any comment of yours. For example:
            "white t-shirt with printed graphics."
            The how_to_fit_it field should be in Vietnamese and the clothes field should be in English.
            The clothes list should contain at maximum 4 items.
            User's question: {request.question}
            """
        logger.info("Generated prompt for LLM")
        
        llm = genai.Client(api_key="AIzaSyC0SFy8iUZycmWXbUtTA6IEgof2O1PS5jc")
        llm_response = llm.models.generate_content(
            model="gemini-2.0-flash",
            contents=prompt,
            config={
                "response_mime_type": "application/json",
                "response_schema": FashionAdvice
            }
        )
        logger.info("Successfully received response from LLM")

        advice: FashionAdvice = llm_response.parsed
        logger.info(f"Parsed advice with {len(advice.clothes)} clothing items")

        # Initialize empty list for image URLs
        image_urls = []
        
        # Try to get images with retry logic
        for clothing_item in advice.clothes:
            max_retries = 3
            retry_delay = 2  # seconds
            
            for attempt in range(max_retries):
                try:
                    logger.info(f"Searching for image: {clothing_item}")
                    ddg = DDGS()
                    results = ddg.images(keywords=clothing_item)
                    if results and len(results) > 0:
                        image_urls.append(results[0]["image"])
                        logger.info(f"Found image URL for: {clothing_item}")
                        break
                except RatelimitException as e:
                    logger.warning(f"Rate limit hit for {clothing_item}, attempt {attempt + 1}/{max_retries}")
                    if attempt < max_retries - 1:
                        time.sleep(retry_delay)
                    else:
                        logger.error(f"Failed to get image for {clothing_item} after {max_retries} attempts")
                        # Add a placeholder or default image URL
                        image_urls.append("https://via.placeholder.com/300x400?text=Image+Not+Available")
                except Exception as e:
                    logger.error(f"Error getting image for {clothing_item}: {str(e)}")
                    image_urls.append("https://via.placeholder.com/300x400?text=Image+Not+Available")
                    break

        logger.info(f"Successfully processed request with {len(image_urls)} images")
        return JSONResponse({
            "image_urls": image_urls,
            "answer": advice.how_to_fit_it
        })
        
    except Exception as e:
        logger.error(f"Error in /query/advise: {str(e)}", exc_info=True)
        return JSONResponse(
            status_code=500,
            content={"error": f"Error generating advice: {str(e)}"}
        )