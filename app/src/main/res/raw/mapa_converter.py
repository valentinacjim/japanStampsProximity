import csv
import json
import re


def parse_point(wkt):
    match = re.search(r"POINT\s*\(([-0-9.]+)\s+([-0-9.]+)\)", wkt)
    if not match:
        return None

    lon = float(match.group(1))
    lat = float(match.group(2))

    return {
        "latitud": lat,
        "longitud": lon
    }


def clean_value(v):
    if v is None:
        return None
    v = v.strip()
    return v if v != "" else None


def transform_row(row):
    point = parse_point(row[0])

    return {
        "id": float(row[1]) if row[1] else None,
        "nombre_jp": clean_value(row[2]),
        "nombre_en": clean_value(row[3]),
        "direccion": clean_value(row[4]),
        "url": clean_value(row[5]),
        "tiene_sello": clean_value(row[8]),
        "coordenadas": point,
        "lat_csv": clean_value(row[9]),
        "lon_csv": clean_value(row[10])
    }


def csv_to_list(input_csv, categoria):
    result = []

    with open(input_csv, "r", encoding="utf-8") as f:
        reader = csv.reader(f)

        for row in reader:
            if len(row) < 11:
                continue

            row_data = transform_row(row)
            row_data["categoria"] = categoria
            result.append(row_data)

    return result


if __name__ == "__main__":
    data = []

    data += csv_to_list("estacionCarreteraStamp.csv", "Estacion de Carretera")
    data += csv_to_list("carreteraStamp.csv", "Carretera")
    data += csv_to_list("EstacionTrenNoPermanente.csv", "Estacion de Tren No Permanente")
    data += csv_to_list("EstacionTrenPermanente.csv", "Estacion de Tren Permanente")
    data += csv_to_list("PuntosTuristicosNoPermanentes.csv", "Punto Turistico No Permanente")
    data += csv_to_list("PuntosTuristicosPermanentes.csv", "Punto Turistico Permanente")

    with open("mapa.json", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print("¡Conversión completada!")