import os

def decode_mojibake(name):
    try:
        # Convert the string to bytes assuming it came from the filesystem as EUC-KR
        # Then decode it as EUC-KR to get the correct Korean string
        # Since the OS might have already garbled it, we try to fix it.
        # This is the "classic" RO filename decoding trick.
        # Often the names are EUC-KR interpreted as CP1252.
        return name.encode('cp1252').decode('euc-kr')
    except:
        return name

def rename_files(root_dir):
    for root, dirs, files in os.walk(root_dir, topdown=False):
        for name in files:
            new_name = decode_mojibake(name)
            if new_name != name:
                try:
                    os.rename(os.path.join(root, name), os.path.join(root, new_name))
                    print(f"Renamed: {name} -> {new_name}")
                except Exception as e:
                    print(f"Error renaming {name}: {e}")
        
        for name in dirs:
            new_name = decode_mojibake(name)
            if new_name != name:
                try:
                    os.rename(os.path.join(root, name), os.path.join(root, new_name))
                    print(f"Renamed: {name} -> {new_name}")
                except Exception as e:
                    print(f"Error renaming {name}: {e}")

if __name__ == "__main__":
    # Change this path to your current 'data' folder
    data_path = r"E:\OneDrive\Desktop\GRF\2012\data"
    rename_files(data_path)
