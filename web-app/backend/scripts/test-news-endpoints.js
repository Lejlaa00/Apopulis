const axios = require('axios');

const API = 'http://localhost:5000/api';

async function main() {
  try {
    // 1. Get all news
    console.log('Fetching all news...');
    const allNewsRes = await axios.get(`${API}/news`);
    const newsList = allNewsRes.data.news || allNewsRes.data;
    console.log(`Found ${newsList.length} news items.`);

    if (newsList.length === 0) {
      console.log('No news items found. Please seed your database first.');
      return;
    }

    // 2. Get a single news item by ID
    const newsId = newsList[0]._id;
    console.log(`Fetching news item with ID: ${newsId}`);
    const singleNewsRes = await axios.get(`${API}/news/${newsId}`);
    console.log('Single news item:', singleNewsRes.data);

    // 3. Update a news item
    console.log(`Updating news item with ID: ${newsId}`);
    const updatedNewsRes = await axios.put(`${API}/news/${newsId}`, {
      title: 'Updated News Title',
      summary: 'Updated summary',
      content: 'Updated content',
      publishedAt: new Date(),
      sourceId: singleNewsRes.data.sourceId._id || singleNewsRes.data.sourceId,
      locationId: singleNewsRes.data.locationId._id || singleNewsRes.data.locationId,
      categoryId: singleNewsRes.data.categoryId._id || singleNewsRes.data.categoryId,
      url: 'https://example.com/updated'
    });
    console.log('Updated news item:', updatedNewsRes.data);

    // 4. Delete a news item
    console.log(`Deleting news item with ID: ${newsId}`);
    const deleteRes = await axios.delete(`${API}/news/${newsId}`);
    console.log('Delete response:', deleteRes.data);

    // 5. Confirm deletion
    try {
      await axios.get(`${API}/news/${newsId}`);
      console.log('Error: News item still exists after deletion!');
    } catch (err) {
      if (err.response && err.response.status === 404) {
        console.log('Confirmed: News item was deleted.');
      } else {
        throw err;
      }
    }

    console.log('All news endpoint tests completed successfully!');
  } catch (err) {
    if (err.response) {
      console.error('Error:', err.response.data);
    } else {
      console.error('Error:', err.message);
    }
  }
}

main();
