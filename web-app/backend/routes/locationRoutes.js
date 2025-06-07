const express = require('express');
const router = express.Router();
const { 
    getLocations, 
    getLocationById, 
    createLocation, 
    updateLocation, 
    deleteLocation 
} = require('../controllers/locationController');

// Get all locations
router.get('/', getLocations);

// Get a single location
router.get('/:id', getLocationById);

// Create a new location
router.post('/', createLocation);

// Update a location
router.put('/:id', updateLocation);

// Delete a location
router.delete('/:id', deleteLocation);

module.exports = router;
