from ultralytics import YOLOv10
import cv2
import numpy as np

# Load the trained model
model = YOLOv10("runs/detect/v1_training/weights/best.pt")

# Run on a test image or live webcam (source=0)
results = model.predict(source="test_face.jpg", conf=0.5)

for r in results:
    boxes = r.boxes.xyxy.cpu().numpy()  # [x1, y1, x2, y2]
    cls = r.boxes.cls.cpu().numpy()      # Class IDs
    
    # Dictionaries to store coordinates
    coords = {}
    
    for box, class_id in zip(boxes, cls):
        name = model.names[int(class_id)]
        # Get the center-point Y coordinate
        center_y = (box[1] + box[3]) / 2
        coords[name] = center_y

    # ASYMMETRY CALCULATION Logic
    if "left_lip" in coords and "right_lip" in coords:
        diff = abs(coords["left_lip"] - coords["right_lip"])
        print(f"Lip Asymmetry (Y-axis diff): {diff:.2f} pixels")
        
        # Threshold (You will determine this in your thesis experiments)
        if diff > 15.0: 
            print("ALERT: Potential Facial Droop Detected!")