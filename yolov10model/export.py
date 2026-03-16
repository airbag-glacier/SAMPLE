from ultralytics import YOLOv10

model = YOLOv10("runs/detect/v1_training/weights/best.pt")

model.export(format="tflite", int8=True)

print("Export Complete! Check the 'weights' folder for the .tflite file.")