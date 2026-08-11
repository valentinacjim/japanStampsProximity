import json
import math

# ===========================
# Configuración
# ===========================

INPUT_FILE = r"C:/Users/Murphy Ronnie/StudioProjects/japanStampsProximity/app/src/main/res/raw/mapa.json"
OUTPUT_FILE = "ekistamps_filtrado.json"

# Categorías que quieres eliminar
CATEGORIES_TO_REMOVE = {
    "Estacion de Carretera",
    "Carretera",
    "Estacion de Tren No Permanente",
    "Punto Turistico No Permanente",
    
    
    
}

with open(INPUT_FILE, "r", encoding="utf-8") as f:
    stamps = json.load(f)

filtered = [
    stamp
    for stamp in stamps
    if stamp.get("categoria") not in CATEGORIES_TO_REMOVE
]

with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    json.dump(filtered, f, ensure_ascii=False, indent=2)

print(f"Originales: {len(stamps)}")
print(f"Conservados: {len(filtered)}")
print(f"Eliminados: {len(stamps) - len(filtered)}")