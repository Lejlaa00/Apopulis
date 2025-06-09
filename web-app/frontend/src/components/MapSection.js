import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, CircleMarker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import '../css/mapSection.css';


const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5001/api';

// Slovenia bounds
const SLOVENIA_BOUNDS = [
  [45.4231, 13.3754], // Southwest corner
  [46.8766, 16.6106]  // Northeast corner
];
const DEFAULT_ZOOM = 8;

// Map style options
const mapOptions = {
  minZoom: 7,
  maxZoom: 12,
  maxBounds: SLOVENIA_BOUNDS,
  maxBoundsViscosity: 1.0
};

export default function MapSection() {
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchLocations = async () => {
      try {
        setLoading(true);
        const response = await fetch(`${API_URL}/news/location-stats`);
        if (!response.ok) throw new Error('Failed to fetch location data');
        const data = await response.json();
        setLocations(data);
        setError(null);
      } catch (err) {
        console.error('Error fetching location data:', err);
        setError('Failed to load location data');
      } finally {
        setLoading(false);
      }
    };

    fetchLocations();
  }, []);

  if (loading) {
    return (
      <div className="map-container">
        <div className="map-loading">Loading map data...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="map-container">
        <div className="map-error">{error}</div>
      </div>
    );
  }

  return (    <div className="map-container">
      <MapContainer 
        bounds={SLOVENIA_BOUNDS}
        zoom={DEFAULT_ZOOM}
        {...mapOptions}
      >
        <TileLayer
          url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
        />
        {locations.map((location) => (
          <CircleMarker
            key={location._id}
            center={[location.latitude, location.longitude]}radius={Math.min(20, Math.max(5, location.newsCount * 2))}
            fillColor="#8a2be2"
            color="white"
            weight={1}
            opacity={0.8}
            fillOpacity={0.6}
          >
            <Popup className="map-popup">
              <strong>{location.name}</strong>
              <br />
              {location.newsCount} news items in the last 24h
            </Popup>
          </CircleMarker>
        ))}
      </MapContainer>
    </div>
  );
}
