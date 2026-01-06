# Apopulis ML Service

FastAPI service for detecting fake vs real images using a fine-tuned EfficientNet-B0 model.

## Setup

### 1. Create Python Virtual Environment

```bash
cd web-app/ml-service
python -m venv venv
```

### 2. Activate Virtual Environment

**Windows (PowerShell):**
```powershell
.\venv\Scripts\Activate.ps1
```

**Windows (Command Prompt):**
```cmd
venv\Scripts\activate.bat
```

**Mac/Linux:**
```bash
source venv/bin/activate
```

### 3. Install Dependencies

```bash
pip install -r requirements.txt
```

## Running the Service

```bash
python app.py
```

The service will start on `http://127.0.0.1:8000`

## API Endpoints

### GET `/`
Health check endpoint that returns service information.

**Response:**
```json
{
  "service": "Apopulis ML Service",
  "status": "running",
  "model_loaded": true,
  "device": "cpu"
}
```

### POST `/predict`
Upload an image and get a prediction whether it's real or fake.

**Request:**
- Method: POST
- Content-Type: multipart/form-data
- Body: Image file with key `file`

**Response:**
```json
{
  "prediction": "real",
  "confidence": 0.9234,
  "is_fake": false,
  "probabilities": {
    "fake": 0.0766,
    "real": 0.9234
  }
}
```

## Model Information

- **Architecture:** EfficientNet-B0
- **Classes:** 2 (fake, real)
- **Input Size:** 224x224 RGB images
- **Preprocessing:** Resize to 224x224, normalize with ImageNet means/stds

## Troubleshooting

### Model Not Loading
- Ensure the model file exists at `models/UvRVRV_efficientnet_b0_bigdata_finetuned_v2.pth`
- Check that you have enough memory to load the model

### CUDA Errors
- If you get CUDA errors but don't have a GPU, the service will automatically fall back to CPU
- CPU inference will be slower but still functional

### Port Already in Use
- If port 8000 is already in use, edit `app.py` and change the port number in the last line

