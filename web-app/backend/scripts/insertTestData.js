require('dotenv').config();
const mongoose = require('mongoose');
const Province = require('../models/provinceModel');
const Location = require('../models/locationModel');
const NewsItem = require('../models/newsItemModel');
const Category = require('../models/categoryModel');
const Source = require('../models/sourceModel');

// Slovenian provinces with codes
const provincesData = [
    { name: 'Pomurska', code: 'SI-001' },
    { name: 'Podravska', code: 'SI-002' },
    { name: 'Koroška', code: 'SI-003' },
    { name: 'Savinjska', code: 'SI-004' },
    { name: 'Zasavska', code: 'SI-005' },
    { name: 'Posavska', code: 'SI-006' },
    { name: 'Jugovzhodna Slovenija', code: 'SI-007' },
    { name: 'Osrednjeslovenska', code: 'SI-008' },
    { name: 'Gorenjska', code: 'SI-009' },
    { name: 'Primorsko-notranjska', code: 'SI-010' },
    { name: 'Goriška', code: 'SI-011' },
    { name: 'Obalno-kraška', code: 'SI-012' }
];

// Sample locations with approximate coordinates (latitude, longitude)
const locationsData = [
    // Pomurska
    { name: 'Murska Sobota', latitude: 46.6611, longitude: 16.1667, provinceCode: 'SI-001' },
    { name: 'Lendava', latitude: 46.5506, longitude: 16.4506, provinceCode: 'SI-001' },
    
    // Podravska
    { name: 'Maribor', latitude: 46.5547, longitude: 15.6467, provinceCode: 'SI-002' },
    { name: 'Ptuj', latitude: 46.4203, longitude: 15.8697, provinceCode: 'SI-002' },
    
    // Koroška
    { name: 'Slovenj Gradec', latitude: 46.5106, longitude: 15.0794, provinceCode: 'SI-003' },
    { name: 'Dravograd', latitude: 46.5889, longitude: 15.0208, provinceCode: 'SI-003' },
    
    // Savinjska
    { name: 'Celje', latitude: 46.2361, longitude: 15.2675, provinceCode: 'SI-004' },
    { name: 'Velenje', latitude: 46.3594, longitude: 15.1114, provinceCode: 'SI-004' },
    
    // Zasavska
    { name: 'Trbovlje', latitude: 46.1539, longitude: 15.0511, provinceCode: 'SI-005' },
    { name: 'Zagorje ob Savi', latitude: 46.1311, longitude: 14.9967, provinceCode: 'SI-005' },
    
    // Posavska
    { name: 'Krško', latitude: 45.9589, longitude: 15.4919, provinceCode: 'SI-006' },
    { name: 'Brežice', latitude: 45.9039, longitude: 15.5911, provinceCode: 'SI-006' },
    
    // Jugovzhodna Slovenija
    { name: 'Novo Mesto', latitude: 45.8014, longitude: 15.1694, provinceCode: 'SI-007' },
    { name: 'Črnomelj', latitude: 45.5711, longitude: 15.1917, provinceCode: 'SI-007' },
    
    // Osrednjeslovenska
    { name: 'Ljubljana', latitude: 46.0569, longitude: 14.5058, provinceCode: 'SI-008' },
    { name: 'Domžale', latitude: 46.1378, longitude: 14.5953, provinceCode: 'SI-008' },
    
    // Gorenjska
    { name: 'Kranj', latitude: 46.2389, longitude: 14.3556, provinceCode: 'SI-009' },
    { name: 'Jesenice', latitude: 46.4333, longitude: 14.0500, provinceCode: 'SI-009' },
    
    // Primorsko-notranjska
    { name: 'Postojna', latitude: 45.7714, longitude: 14.2133, provinceCode: 'SI-010' },
    { name: 'Ilirska Bistrica', latitude: 45.5678, longitude: 14.2481, provinceCode: 'SI-010' },
    
    // Goriška
    { name: 'Nova Gorica', latitude: 45.9564, longitude: 13.6483, provinceCode: 'SI-011' },
    { name: 'Tolmin', latitude: 46.1833, longitude: 13.7333, provinceCode: 'SI-011' },
    
    // Obalno-kraška
    { name: 'Koper', latitude: 45.5469, longitude: 13.7306, provinceCode: 'SI-012' },
    { name: 'Piran', latitude: 45.5281, longitude: 13.5681, provinceCode: 'SI-012' }
];

const categoriesData = [
    { name: 'Splošno' },
    { name: 'Biznis' },
    { name: 'Gospodarstvo' },
    { name: 'Kultura' },
    { name: 'Lifestyle' },
    { name: 'Politika' },
    { name: 'Tehnologija' },
    { name: 'Vreme' }
];

const sourcesData = [
    { name: '24ur', url: 'https://www.24ur.com' },
    { name: 'RTV SLO', url: 'https://www.rtvslo.si' },
    { name: 'Delo', url: 'https://www.delo.si' },
    { name: 'Dnevnik', url: 'https://www.dnevnik.si' },
    { name: 'Večer', url: 'https://www.vecer.com' }
];

// Sample news titles and summaries by category
const newsTemplates = {
    'Splošno': [
        { title: 'Pomembne novice v regiji', summary: 'Pregled najnovejših dogodkov in razvojnih projektov v regiji.' },
        { title: 'Lokalni dogodki tega tedna', summary: 'Povzetek kulturnih in družabnih dogodkov v lokalni skupnosti.' }
    ],
    'Biznis': [
        { title: 'Nova podjetja v regiji', summary: 'Predstavitev novih poslovnih priložnosti in podjetij v lokalni skupnosti.' },
        { title: 'Gospodarski razvoj', summary: 'Analiza gospodarskega razvoja in naložb v regiji.' }
    ],
    'Gospodarstvo': [
        { title: 'Ekonomske napovedi', summary: 'Pregled gospodarskih trendov in napovedi za prihodnost.' },
        { title: 'Trg dela', summary: 'Aktualne informacije o zaposlitvenih možnostih v regiji.' }
    ],
    'Kultura': [
        { title: 'Kulturni dogodki', summary: 'Pregled kulturnih prireditev in razstav v regiji.' },
        { title: 'Umetniška ustvarjalnost', summary: 'Predstavitev lokalnih umetnikov in njihovih del.' }
    ],
    'Politika': [
        { title: 'Lokalna politika', summary: 'Aktualne politične odločitve in spremembe v lokalni skupnosti.' },
        { title: 'Zakonodajne spremembe', summary: 'Pomembne spremembe zakonodaje, ki vplivajo na regijo.' }
    ],
    'Tehnologija': [
        { title: 'Tehnološke inovacije', summary: 'Nove tehnologije in inovacije v lokalni industriji.' },
        { title: 'Digitalna transformacija', summary: 'Napredek digitalizacije v regiji in njeni učinki.' }
    ]
};

async function insertTestData() {
    try {
        console.log('Connecting to MongoDB...');
        await mongoose.connect(process.env.MONGO_URI);
        console.log('Connected to MongoDB');

        // Clear existing data
        console.log('\nClearing existing test data...');
        await NewsItem.deleteMany({});
        await Location.deleteMany({});
        await Province.deleteMany({});
        
        // Check if categories and sources exist, if not create them
        let categories = await Category.find();
        if (categories.length === 0) {
            console.log('Creating categories...');
            categories = await Category.insertMany(categoriesData);
            console.log(`Created ${categories.length} categories`);
        } else {
            console.log(`Found ${categories.length} existing categories`);
        }

        let sources = await Source.find();
        if (sources.length === 0) {
            console.log('Creating sources...');
            sources = await Source.insertMany(sourcesData);
            console.log(`Created ${sources.length} sources`);
        } else {
            console.log(`Found ${sources.length} existing sources`);
        }

        // Insert provinces
        console.log('\nInserting provinces...');
        const provinces = await Province.insertMany(provincesData);
        console.log(`Inserted ${provinces.length} provinces`);

        // Insert locations
        console.log('\nInserting locations...');
        const locationsWithProvinceIds = await Promise.all(locationsData.map(async (loc) => {
            const province = provinces.find(p => p.code === loc.provinceCode);
            return {
                name: loc.name,
                latitude: loc.latitude,
                longitude: loc.longitude,
                province: province._id
            };
        }));
        const locations = await Location.insertMany(locationsWithProvinceIds);
        console.log(`Inserted ${locations.length} locations`);

        // Insert news items (5-15 per location with random dates in last 24 hours)
        console.log('\nInserting news items...');
        const newsItems = [];
        const now = new Date();
        const oneDayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);

        for (const location of locations) {
            const numNews = Math.floor(Math.random() * 11) + 5; // 5-15 news items
            
            for (let i = 0; i < numNews; i++) {
                // Pick random category
                const category = categories[Math.floor(Math.random() * categories.length)];
                const source = sources[Math.floor(Math.random() * sources.length)];
                
                // Get templates for this category
                const templates = newsTemplates[category.name] || newsTemplates['Splošno'];
                const template = templates[Math.floor(Math.random() * templates.length)];
                
                // Random date within last 24 hours
                const randomTime = oneDayAgo.getTime() + Math.random() * 24 * 60 * 60 * 1000;
                const publishedAt = new Date(randomTime);

                newsItems.push({
                    title: `${template.title} - ${location.name}`,
                    summary: template.summary,
                    content: `${template.summary} Več informacij o dogodkih v ${location.name} in okolici.`,
                    author: 'Test Author',
                    publishedAt: publishedAt,
                    sourceId: source._id,
                    locationId: location._id,
                    categoryId: category._id,
                    url: `${source.url}/news/${Date.now()}`,
                    imageUrl: 'http://localhost:5001/images/default-image.jpg',
                    views: Math.floor(Math.random() * 1000),
                    likes: Math.floor(Math.random() * 100),
                    dislikes: Math.floor(Math.random() * 20),
                    commentsCount: Math.floor(Math.random() * 50)
                });
            }
        }

        const insertedNews = await NewsItem.insertMany(newsItems);
        console.log(`Inserted ${insertedNews.length} news items`);

        // Print summary
        console.log('\n=== Test Data Summary ===');
        console.log(`Provinces: ${provinces.length}`);
        console.log(`Locations: ${locations.length}`);
        console.log(`News Items: ${insertedNews.length}`);
        
        console.log('\n=== News per Province ===');
        for (const province of provinces) {
            const provinceLocs = locations.filter(l => l.province.toString() === province._id.toString());
            const provinceLocIds = provinceLocs.map(l => l._id);
            const count = insertedNews.filter(n => provinceLocIds.some(id => id.toString() === n.locationId.toString())).length;
            console.log(`${province.name}: ${count} news items`);
        }

        console.log('\nTest data inserted successfully!');
        process.exit(0);
    } catch (error) {
        console.error('Error inserting test data:', error);
        process.exit(1);
    }
}

insertTestData();

