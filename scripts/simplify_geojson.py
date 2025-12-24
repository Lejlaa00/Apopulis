#!/usr/bin/env python3
"""
GeoJSON Simplification Script for Mobile Android Apps

This script simplifies complex GeoJSON geometries to reduce coordinate count
while preserving overall shape. Designed for mobile performance optimization.

Usage:
    python3 simplify_geojson.py input.geojson output.geojson [--tolerance 0.001]

Requirements:
    pip install shapely geojson

Recommended tolerance values:
    - 0.0005 (0.05%) - Minimal simplification, high precision
    - 0.001 (0.1%)   - Balanced (recommended for mobile)
    - 0.002 (0.2%)   - More aggressive simplification
    - 0.005 (0.5%)   - Maximum simplification for very large files
"""

import json
import sys
import argparse
from typing import Dict, List, Any

try:
    from shapely.geometry import shape, mapping
    from shapely.ops import unary_union
except ImportError:
    print("ERROR: shapely is not installed.")
    print("Install it with: pip install shapely geojson")
    sys.exit(1)


def simplify_geometry(geometry: Dict[str, Any], tolerance: float) -> Dict[str, Any]:
    """
    Simplifies a GeoJSON geometry using Douglas-Peucker algorithm.

    Args:
        geometry: GeoJSON geometry object
        tolerance: Simplification tolerance (in degrees)

    Returns:
        Simplified GeoJSON geometry
    """
    try:
        shapely_geom = shape(geometry)

        # Simplify the geometry
        simplified = shapely_geom.simplify(tolerance, preserve_topology=True)

        # Convert back to GeoJSON
        return mapping(simplified)
    except Exception as e:
        print(f"Warning: Failed to simplify geometry: {e}")
        return geometry


def simplify_feature(feature: Dict[str, Any], tolerance: float) -> Dict[str, Any]:
    """
    Simplifies a GeoJSON feature's geometry.

    Args:
        feature: GeoJSON feature object
        tolerance: Simplification tolerance

    Returns:
        Simplified GeoJSON feature
    """
    if 'geometry' in feature and feature['geometry']:
        feature['geometry'] = simplify_geometry(feature['geometry'], tolerance)
    return feature


def simplify_geojson(input_file: str, output_file: str, tolerance: float = 0.001):
    """
    Simplifies a GeoJSON file and writes the result.

    Args:
        input_file: Path to input GeoJSON file
        output_file: Path to output GeoJSON file
        tolerance: Simplification tolerance (default: 0.001 = 0.1%)
    """
    print(f"Loading GeoJSON from: {input_file}")

    try:
        with open(input_file, 'r', encoding='utf-8') as f:
            geojson_data = json.load(f)
    except FileNotFoundError:
        print(f"ERROR: File not found: {input_file}")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"ERROR: Invalid JSON: {e}")
        sys.exit(1)

    if geojson_data.get('type') != 'FeatureCollection':
        print("ERROR: Expected FeatureCollection type")
        sys.exit(1)

    features = geojson_data.get('features', [])
    print(f"Found {len(features)} features")

    # Count initial coordinates
    initial_coords = count_coordinates(geojson_data)
    print(f"Initial coordinate count: {initial_coords:,}")

    # Simplify each feature
    print(f"Simplifying geometries with tolerance: {tolerance}...")
    simplified_features = []

    for i, feature in enumerate(features):
        if (i + 1) % 10 == 0:
            print(f"  Processing feature {i + 1}/{len(features)}...")

        # Keep only essential properties (SR_ID and name)
        simplified_feature = {
            'type': 'Feature',
            'geometry': feature.get('geometry'),
            'properties': {
                'SR_ID': feature.get('properties', {}).get('SR_ID'),
                'SR_UIME': feature.get('properties', {}).get('SR_UIME', '')
            }
        }

        # Simplify geometry
        simplified_feature = simplify_feature(simplified_feature, tolerance)
        simplified_features.append(simplified_feature)

    # Create output GeoJSON
    output_data = {
        'type': 'FeatureCollection',
        'features': simplified_features
    }

    # Count final coordinates
    final_coords = count_coordinates(output_data)
    reduction = ((initial_coords - final_coords) / initial_coords * 100) if initial_coords > 0 else 0

    print(f"\nSimplification complete!")
    print(f"Final coordinate count: {final_coords:,}")
    print(f"Reduction: {reduction:.1f}%")
    print(f"File size reduction: ~{reduction:.1f}%")

    # Write output
    print(f"\nWriting simplified GeoJSON to: {output_file}")
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(output_data, f, separators=(',', ':'))  # Compact JSON

    # Calculate file sizes
    import os
    input_size = os.path.getsize(input_file) / (1024 * 1024)  # MB
    output_size = os.path.getsize(output_file) / (1024 * 1024)  # MB
    size_reduction = ((input_size - output_size) / input_size * 100) if input_size > 0 else 0

    print(f"\nFile sizes:")
    print(f"  Input:  {input_size:.2f} MB")
    print(f"  Output: {output_size:.2f} MB")
    print(f"  Size reduction: {size_reduction:.1f}%")


def count_coordinates(geojson_data: Dict[str, Any]) -> int:
    """Counts total coordinates in a GeoJSON structure."""
    count = 0

    def count_in_geometry(geom):
        nonlocal count
        if geom is None:
            return

        geom_type = geom.get('type')
        if geom_type == 'Point':
            count += 1
        elif geom_type == 'LineString':
            count += len(geom.get('coordinates', []))
        elif geom_type == 'Polygon':
            for ring in geom.get('coordinates', []):
                count += len(ring)
        elif geom_type == 'MultiPolygon':
            for polygon in geom.get('coordinates', []):
                for ring in polygon:
                    count += len(ring)

    if geojson_data.get('type') == 'FeatureCollection':
        for feature in geojson_data.get('features', []):
            count_in_geometry(feature.get('geometry'))
    elif geojson_data.get('type') == 'Feature':
        count_in_geometry(geojson_data.get('geometry'))

    return count


def main():
    parser = argparse.ArgumentParser(
        description='Simplify GeoJSON geometries for mobile performance',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Basic usage (default tolerance 0.001)
  python3 simplify_geojson.py input.geojson output.geojson

  # More aggressive simplification
  python3 simplify_geojson.py input.geojson output.geojson --tolerance 0.002

  # Minimal simplification (high precision)
  python3 simplify_geojson.py input.geojson output.geojson --tolerance 0.0005
        """
    )

    parser.add_argument('input', help='Input GeoJSON file path')
    parser.add_argument('output', help='Output GeoJSON file path')
    parser.add_argument(
        '--tolerance',
        type=float,
        default=0.001,
        help='Simplification tolerance in degrees (default: 0.001 = 0.1%%)'
    )

    args = parser.parse_args()

    print("=" * 60)
    print("GeoJSON Simplification Tool for Mobile Apps")
    print("=" * 60)
    print()

    simplify_geojson(args.input, args.output, args.tolerance)

    print("\n" + "=" * 60)
    print("Done! Replace your original file with the simplified version.")
    print("=" * 60)


if __name__ == '__main__':
    main()

