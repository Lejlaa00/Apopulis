const getApiUrl = () => {
    // Use environment variable if set, otherwise fall back to defaults
    return process.env.REACT_APP_API_URL || 
           (process.env.NODE_ENV === 'development' ? 
           'http://localhost:5001/api' : 
           'http://backend:5001/api');
};

export const config = {
    apiUrl: getApiUrl()
};
