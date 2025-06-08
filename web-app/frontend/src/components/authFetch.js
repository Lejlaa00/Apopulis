import AuthService from '../services/authService';

export async function authFetch(url, options = {}) {
    const token = AuthService.getToken();
    
    // Public routes that don't require auth but can use it if available
    const publicRoutes = [
        '/api/news$', // Main news endpoint
        '/api/news\\?.*$', // News with query parameters
        '/api/news/trending$', // Trending news
        '/api/news/[0-9a-fA-F]{24}$', // Individual news items by MongoDB ID
        '/api/news/[0-9a-fA-F]{24}\\?.*$', // Individual news items with query params
        '/api/stats/', // Stats endpoints
        '/api/categories$', // Categories list
        '/api/locations$', // Locations list
        '/api/comments/news/.+$', // Comments for a news item (reading)
        '/api/votes/news/.+$' // Vote counts for a news item (reading)
    ];

    // Check if the URL matches any public route patterns
    const isPublicRoute = publicRoutes.some(pattern => {
        const regex = new RegExp(pattern);
        return regex.test(url);
    });

    const headers = {
        ...options.headers,
        'Content-Type': 'application/json',
    };

    // For protected routes, throw if no token
    if (!isPublicRoute && !token) {
        throw new Error('Authentication required');
    }

    // Always send token if we have it, regardless of route
    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    const config = {
        ...options,
        headers,
    };

    const response = await fetch(url, config);    // Handle various error responses
    if (!response.ok) {
        // Handle 401 Unauthorized
        if (response.status === 401) {
            AuthService.logout();
            window.location.href = '/login';
            throw new Error('Session expired. Please log in again.');
        }
        
        // Try to get error message from response
        try {
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const errorData = await response.json();
                throw new Error(errorData.msg || errorData.message || 'Request failed');
            } else {
                throw new Error(`Request failed with status ${response.status}`);
            }
        } catch (e) {
            throw new Error(e.message || `Request failed with status ${response.status}`);
        }
    }

    return response;
}
