import os
import shutil

src_dir = "d:/weatherapp/app/src/main/res/drawable"
dst_dir = "d:/weatherapp/app/src/main/res/raw"

if not os.path.exists(dst_dir):
    os.makedirs(dst_dir)

for filename in os.listdir(src_dir):
    src_path = os.path.join(src_dir, filename)
    if os.path.isfile(src_path):
        try:
            with open(src_path, 'rb') as f:
                header = f.read(100)
                # Check for SVG signature (xml tag or svg tag)
                if b'<svg' in header or b'<?xml' in header and b'<svg' in f.read(500):
                    # It is an SVG
                    new_name = os.path.splitext(filename)[0] + ".svg"
                    dst_path = os.path.join(dst_dir, new_name)
                    print(f"Moving SVG: {filename} -> {new_name}")
                    f.close() # Close before moving
                    shutil.move(src_path, dst_path)
        except Exception as e:
            print(f"Error processing {filename}: {e}")
