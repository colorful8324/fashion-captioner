from transformers import AutoProcessor, BlipForConditionalGeneration
import torch
from fastapi import FastAPI, File, UploadFile
from PIL import Image
import io
import requests
from typing import List

API_KEY = 'AIzaSyALJPAV59m1nBL8RyJ0dpYSyfwHnt9qqCw'
TRANSLATE_URL = 'https://translation.googleapis.com/language/translate/v2'

def translate_text(text, target_language='vi', source_language='en'):
    response = requests.post(
        TRANSLATE_URL,
        params={
            'key': API_KEY,
            'q': text,
            'source': source_language,
            'target': target_language,
            'format': 'text'
        }
    )
    result = response.json()
    return result['data']['translations'][0]['translatedText']

device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
processor = AutoProcessor.from_pretrained('kzap201/fashion_BLIP', revision='v1.0')
model = BlipForConditionalGeneration.from_pretrained('kzap201/fashion_BLIP', revision='v1.0')
model.to(device)
model.eval()

app = FastAPI()

@app.post("/generate")
async def generate_caption(file: UploadFile = File(...), max_tokens: int = 100):
    try:
        contents = await file.read()
        image = Image.open(io.BytesIO(contents))
        
        inputs = processor(images=image, return_tensors="pt").to(device)
        
        with torch.no_grad():
            outputs = model.generate(
                **inputs, 
                max_length=max_tokens,
                min_length=10,
                num_beams=5,
                num_return_sequences=1,
                temperature=1.0,
                top_k=50,
                top_p=0.9,
                repetition_penalty=1.2,
                length_penalty=1.0,
                no_repeat_ngram_size=2
            )
            captions = processor.batch_decode(outputs, skip_special_tokens=True)
        
        return {"caption": captions}
        
    except Exception as e:
        return {"error": str(e)}
    
@app.post("/gen-cap")
async def generate_captions(files: List[UploadFile] = File(...), max_tokens: int = 100):
    all_captions = []

    for file in files:
        try:
            contents = await file.read()
            image = Image.open(io.BytesIO(contents))
            inputs = processor(images=image, return_tensors="pt").to(device)

            with torch.no_grad():
                outputs = model.generate(
                    **inputs, 
                    max_length=max_tokens,
                    min_length=10,
                    num_beams=6,
                    num_return_sequences=1,
                    temperature=1.0,
                    top_k=50,
                    top_p=0.9,
                    repetition_penalty=1.2,
                    length_penalty=1.0,
                    no_repeat_ngram_size=2
                )
                english_captions = processor.batch_decode(outputs, skip_special_tokens=True)
                
                vietnamese_captions = []
                for caption in english_captions:
                    vietnamese_caption = translate_text(caption)
                    vietnamese_captions.append(vietnamese_caption)
                
                all_captions.append({
                    "filename": file.filename,
                    "caption": vietnamese_captions[0] if vietnamese_captions else ""
                })

        except Exception as e:
            all_captions.append({
                "filename": file.filename,
                "error": str(e)
            })

    return {"results": all_captions}