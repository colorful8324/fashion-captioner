from typing import List
from transformers import AutoProcessor, BlipForConditionalGeneration
import torch
from fastapi import FastAPI, File, UploadFile
from PIL import Image
import io

# load the model and processor
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
processor = AutoProcessor.from_pretrained('kzap201/fashion_BLIP', revision='v1.0')  # change to version='v1.0' for the latest version
model = BlipForConditionalGeneration.from_pretrained('kzap201/fashion_BLIP', revision='v1.0')
model.to(device)
model.eval()

app = FastAPI()

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
                captions = processor.batch_decode(outputs, skip_special_tokens=True)
                all_captions.append({
                    "filename": file.filename,
                    "caption": captions
                })

        except Exception as e:
            all_captions.append({
                "filename": file.filename,
                "error": str(e)
            })

    return {"results": all_captions}

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=9000)