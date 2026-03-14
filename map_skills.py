import os, json

skills_dir = 'd:/Mods/RagnarMMO/src/main/resources/data/ragnarmmo/skills'

tree = {}
for f in os.listdir(skills_dir):
    if f.endswith('.json'):
        with open(os.path.join(skills_dir, f), 'r', encoding='utf-8') as file:
            data = json.load(file)
            
            jobs = data.get('jobs', [])
            job = jobs[0] if isinstance(jobs, list) and len(jobs) > 0 else "NOVICE"
            
            if job not in tree:
                tree[job] = []
            
            ui = data.get('ui', {})
            x = ui.get('grid_x', 0)
            y = ui.get('grid_y', 0)
            
            tree[job].append({
                'id': data.get('id', '').replace('ragnarmmo:', ''),
                'x': x,
                'y': y
            })

for job, skills in tree.items():
    print(f"\n======== {job} ========")
    grid = [['       ' for _ in range(8)] for _ in range(12)]
    for s in skills:
        x, y = s['x'], s['y']
        if 0 <= x < 8 and 0 <= y < 12:
            grid[y][x] = s['id'][:7]
            
    for y in range(12):
        row = " | ".join(f"{c:7s}" for c in grid[y])
        if any(c.strip() for c in grid[y]):
            print(f"Y={y:02d}: {row}")
