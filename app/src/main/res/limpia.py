import xml.etree.ElementTree as ET
from collections import defaultdict

KML_NS = {'kml': 'http://www.opengis.net/kml/2.2'}
ET.register_namespace('', KML_NS['kml'])


def limpiar_kml(input_path, output_path):
    tree = ET.parse(input_path)
    root = tree.getroot()

    # 1. Detectar estilos usados
    estilos_usados = set()
    for elem in root.findall(".//kml:styleUrl", KML_NS):
        if elem.text:
            estilos_usados.add(elem.text.replace("#", ""))

    # 2. Eliminar estilos no usados
    for style in root.findall(".//kml:Style", KML_NS):
        style_id = style.attrib.get("id")
        if style_id and style_id not in estilos_usados:
            parent = root.find(".//kml:Document", KML_NS)
            parent.remove(style)

    for stylemap in root.findall(".//kml:StyleMap", KML_NS):
        style_id = stylemap.attrib.get("id")
        if style_id and style_id not in estilos_usados:
            parent = root.find(".//kml:Document", KML_NS)
            parent.remove(stylemap)

    # 3. Eliminar placemarks sin geometría
    for placemark in root.findall(".//kml:Placemark", KML_NS):
        tiene_geometria = any([
            placemark.find(".//kml:Point", KML_NS),
            placemark.find(".//kml:LineString", KML_NS),
            placemark.find(".//kml:Polygon", KML_NS)
        ])
        if not tiene_geometria:
            parent = root.find(".//kml:Document", KML_NS)
            parent.remove(placemark)

    # 4. Eliminar descripciones vacías
    for desc in root.findall(".//kml:description", KML_NS):
        if not desc.text or not desc.text.strip():
            parent = desc.getparent() if hasattr(desc, "getparent") else None
            if parent is not None:
                parent.remove(desc)

    # 5. Eliminar coordenadas duplicadas consecutivas
    for coords in root.findall(".//kml:coordinates", KML_NS):
        if coords.text:
            puntos = coords.text.strip().split()
            nuevos = []
            for p in puntos:
                if not nuevos or nuevos[-1] != p:
                    nuevos.append(p)
            coords.text = " ".join(nuevos)

    # 6. Eliminar folders vacíos
    for folder in root.findall(".//kml:Folder", KML_NS):
        if len(folder) == 0:
            parent = root.find(".//kml:Document", KML_NS)
            parent.remove(folder)

    tree.write(output_path, encoding="utf-8", xml_declaration=True)
    print(f"KML limpio guardado en: {output_path}")


if __name__ == "__main__":
    limpiar_kml("input.kml", "output_limpio.kml")
