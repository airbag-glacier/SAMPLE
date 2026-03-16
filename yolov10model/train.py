from ultralytics import YOLOv10
import torch

print(f"CUDA Available: {torch.cuda.is_available()}")
print(f"Using Device: {torch.cuda.get_device_name(0)}")

model = YOLOv10("yolov10n.pt") 

if __name__ == '__main__':
    model.train(
        data="C:yYOURPATH\yolov10model\YOLOv10-70-20-10 split\data.yaml", # Update to your local path (Sample/yolov10Model/)
        epochs=100,
        imgsz=640,
        batch=16,       # Adjust based on your VRAM (8, 16, or 32)
        device=0,
        optimizer='auto',
        project='DeTechStroke',
        name='v1_training'
    )