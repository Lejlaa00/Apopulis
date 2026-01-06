"""
FastAPI service for Real vs Fake image classification using EfficientNet-B0.
This service loads a pre-trained PyTorch model and exposes a /predict endpoint.
"""

from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
import torch
import torch.nn as nn
from torchvision import transforms, models
from torchvision.models import efficientnet_b0
import io
import uvicorn

# Initialize FastAPI app
app = FastAPI(title="Apopulis ML Service", description="Real vs Fake Image Detection")

# CORS configuration for localhost testing
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:5001"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Device configuration
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
print(f"Using device: {device}")

# Model path
MODEL_PATH = "models/UvRVRV_efficientnet_b0_bigdata_finetuned_v2.pth"

# Load model
model = None

def load_model():
    """Load the EfficientNet-B0 model with custom classifier."""
    global model
    
    try:
        # Create model architecture (same as training)
        model = efficientnet_b0(weights=None)  # No pretrained weights
        
        # Replace classifier for 2 classes (fake, real)
        in_features = model.classifier[1].in_features
        model.classifier[1] = nn.Linear(in_features, 2)
        
        # Load trained weights
        model.load_state_dict(torch.load(MODEL_PATH, map_location=device))
        model = model.to(device)
        model.eval()
        
        print("Model loaded successfully!")
        return True
    except Exception as e:
        print(f"Error loading model: {e}")
        return False

# Image preprocessing transforms (same as validation transforms in training)
transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(
        mean=[0.485, 0.456, 0.406],
        std=[0.229, 0.224, 0.225]
    )
])

# Class names mapping (from training)
CLASS_NAMES = {0: "fake", 1: "real"}

@app.on_event("startup")
async def startup_event():
    """Load model on startup."""
    success = load_model()
    if not success:
        print("WARNING: Model failed to load. Check MODEL_PATH and model file.")

@app.get("/")
async def root():
    """Health check endpoint."""
    return {
        "service": "Apopulis ML Service",
        "status": "running",
        "model_loaded": model is not None,
        "device": str(device)
    }

@app.get("/health")
async def health():
    """Health check endpoint with model status."""
    return {
        "status": "healthy" if model is not None else "model not loaded",
        "device": str(device)
    }

@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    """
    Predict if an uploaded image is real or fake.
    
    Args:
        file: Image file (JPEG, PNG, etc.)
        
    Returns:
        JSON with prediction, confidence scores, and class probabilities
    """
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    
    # Validate file type
    if not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="File must be an image")
    
    try:
        # Read and open image
        image_bytes = await file.read()
        image = Image.open(io.BytesIO(image_bytes))
        
        # Convert to RGB if necessary (handle RGBA, grayscale, etc.)
        if image.mode != "RGB":
            image = image.convert("RGB")
        
        # Apply preprocessing transforms
        image_tensor = transform(image).unsqueeze(0)  # Add batch dimension
        image_tensor = image_tensor.to(device)
        
        # Inference
        with torch.no_grad():
            outputs = model(image_tensor)
            probabilities = torch.nn.functional.softmax(outputs, dim=1)
            confidence, predicted_class = torch.max(probabilities, 1)
        
        # Extract results
        predicted_idx = predicted_class.item()
        predicted_label = CLASS_NAMES[predicted_idx]
        confidence_score = confidence.item()
        
        # Get individual class probabilities
        fake_prob = probabilities[0][0].item()
        real_prob = probabilities[0][1].item()
        
        return {
            "prediction": predicted_label,
            "confidence": round(confidence_score, 4),
            "is_fake": predicted_label == "fake",
            "probabilities": {
                "fake": round(fake_prob, 4),
                "real": round(real_prob, 4)
            }
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prediction error: {str(e)}")

if __name__ == "__main__":
    # Run the server
    uvicorn.run(app, host="127.0.0.1", port=8000)

