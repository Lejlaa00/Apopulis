export async function authFetch(url, options = {}) {
    const token = localStorage.getItem("token");

    const headers = {
        ...options.headers,
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
    };

    const config = {
        ...options,
        headers,
    };

    return fetch(url, config);
}
