import { useEffect, useState } from 'react';
import { Line, Pie, Bar, PolarArea, Doughnut, Radar } from 'react-chartjs-2';
//import { Chart as ChartJS } from 'chart.js/auto';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faChartLine, faChartPie, faChartBar, faChartColumn } from '@fortawesome/free-solid-svg-icons';
import {
  Chart as ChartJS,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  RadialLinearScale,
  BarElement,
  ArcElement,
  Tooltip,
  Legend,
  Filler
} from 'chart.js';

ChartJS.register(
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,        
  RadialLinearScale,   
  BarElement,
  ArcElement,
  Tooltip,
  Legend,
  Filler
);

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5001/api';

const Graphs = () => {
  const [chartType, setChartType] = useState('popularity-trend');
  const [chartData, setChartData] = useState({ labels: [], datasets: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const chartTypesToCycle = [
    'popularity-trend',
    'category-distribution',
    'engagement-by-source',
    'news-by-location',
    ...(localStorage.getItem('token') ? [
      'user-interest-compass',
      'user-interest-compass-radar'
    ] : [])
  ];
  const [showChart, setShowChart] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      setError(null);
      try {
        let endpoint;
        if (chartType === 'user-interest-compass') {
          endpoint = `${API_URL}/stats/user-interest-compass-pie`;
        } else if (chartType === 'user-interest-compass-radar') {
          endpoint = `${API_URL}/stats/user-interest-compass-radar`;
        } else {
          endpoint = `${API_URL}/stats/${chartType}`;
        }

        const response = await fetch(endpoint, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(chartType.startsWith('user-interest-compass') && {
              Authorization: `Bearer ${localStorage.getItem('token')}`
          
            })
          },
          body: JSON.stringify({})
        });
        
        if (!response.ok) {
          throw new Error('Failed to fetch data');
        }
        
        const data = await response.json();
        console.log(`Data for ${chartType}:`, data);
        let datasets = [];

        // Checking if data has the expected properties
        if (
          (
            chartType !== 'user-interest-compass' &&
            chartType !== 'user-interest-compass-radar' &&
            (!data.labels || data.labels.length === 0)
          )||
          (chartType === 'user-interest-compass' && (!data.pieData || !data.pieData.labels || data.pieData.labels.length === 0))
        ) {
          console.warn(`No labels found for ${chartType}`);
          throw new Error(`No data available for ${chartType}`);
        }

        switch (chartType) {
          case 'popularity-trend':
            if (!data.popularityScores) {
              console.warn('No popularity scores found');
              throw new Error('No popularity data available');
            }
            datasets = [{
              label: 'Average Popularity Score',
              data: data.popularityScores,
              //borderColor: '#36A2EB',
              //backgroundColor: 'rgba(54, 162, 235, 0.2)',
              borderColor: 'rgba(211, 188, 232, 0.6)', // pale purple
              backgroundColor: 'rgba(113, 73, 156, 0.6)',//accent purple - main website color
              fill: true,
              tension: 0.1
            }];
            break;
          case 'category-distribution':
            if (!data.counts || data.counts.length === 0) {
              console.warn('No category counts found');
              throw new Error('No category data available');
            }
            datasets = [{
              data: data.counts,
                backgroundColor: [
                  /* 'rgba(54, 162, 235, 0.8)',   // Primary blue
               'rgba(54, 162, 235, 0.6)',   // Lighter blue
               'rgba(54, 162, 235, 0.4)',   // Even lighter blue
               'rgba(54, 162, 235, 0.2)',   // Very light blue
               'rgba(54, 162, 235, 1.0)'    // Full blue*/
                  'rgba(113, 73, 156, 0.6)',   // #71499c - main website color
                  'rgba(130, 85, 170, 0.6)',   // #8255aa
                  'rgba(147, 97, 184, 0.6)',   // #9361b8
                  'rgba(164, 109, 198, 0.6)',  // #a46dc6
                  'rgba(181, 121, 212, 0.6)',  // #b579d4
                  'rgba(198, 133, 226, 0.6)',  // #c685e2
                  'rgba(215, 145, 240, 0.6)',  // #d791f0
                  'rgba(156, 109, 179, 0.6)',  // #9c6db3
                  'rgba(91, 59, 121, 0.6)',    // #5b3b79
                  'rgba(170, 136, 200, 0.6)',  // #aa88c8
                  'rgba(191, 160, 218, 0.6)',  // #bfa0da
                  'rgba(211, 188, 232, 0.6)'   // #d3bce8
                ],
              borderColor: 'white',
              borderWidth: 1,
              hoverOffset: 4
            }];
            break;
          case 'engagement-by-source':
            if (!data.engagement || data.engagement.length === 0) {
              console.warn('No engagement data found');
              throw new Error('No engagement data available');
            }
            datasets = [{
              label: 'Total Engagement',
              data: data.engagement,
              backgroundColor: 'rgba(113, 73, 156, 0.6)',       
              borderColor: 'rgba(156, 109, 179, 0.6)',          
              hoverBackgroundColor: 'rgba(160, 119, 197, 0.6)',  
              hoverBorderColor: 'rgba(211, 188, 232, 0.6)',  
              //backgroundColor: 'rgba(54, 162, 235, 0.6)',
              //borderColor: 'rgba(54, 162, 235, 1)',
              //hoverBackgroundColor: 'rgba(54, 162, 235, 0.8)',
              //hoverBorderColor: 'rgba(54, 162, 235, 1)'
              borderWidth: 2,
              barThickness: 40,
              borderRadius: 4
            }];
            break;

          case 'news-by-location':
            if (!data.labels || !data.counts || data.labels.length === 0) {
              throw new Error('No location data available');
            }

            datasets = [{
              label: 'Number of News Items',
              data: data.counts,
              backgroundColor: data.labels.map((_, i) =>
                `hsl(270, 80%, ${60 + i * 3}%)` 
              ),
              borderColor: 'white',
              borderWidth: 1,
              barThickness: 40,
              borderRadius: 4
            }];

            setChartData({ labels: data.labels, datasets });
              return;

          case 'user-interest-compass':
            if (!data.pieData || !data.pieData.labels || !data.pieData.counts) {
              throw new Error('Invalid pie data');
            }

            const pieLabels = data.pieData.labels;
            const pieCounts = data.pieData.counts;

            setChartData({
              labels: pieLabels,
              datasets: [{
                label: 'User Interests',
                data: pieCounts,
                backgroundColor: [
                  'rgba(255, 140, 180, 0.6)',
                  'rgba(100, 200, 255, 0.6)',
                  'rgba(180, 150, 255, 0.6)', 
                  'rgba(120, 180, 200, 0.6)',
                  'rgba(255, 220, 100, 0.6)',
                  'rgba(201, 203, 207, 0.6)',

                  'rgba(255, 99, 132, 0.6)',     
                  'rgba(255, 159, 64, 0.6)',     
                  'rgba(255, 206, 86, 0.6)',     
                  'rgba(75, 192, 192, 0.6)',     
                  'rgba(54, 162, 235, 0.6)',     
                  'rgba(153, 102, 255, 0.6)',    

                     
                ],
                borderColor: 'white',
                borderWidth: 1,
                hoverOffset: 6
              }]
            });

            return;     


          case 'user-interest-compass-radar':
            if (!data.radarData || data.radarData.length === 0) {
              console.warn('No radar data found');
              throw new Error('No radar data available');
            }

            setChartData({
              labels: data.radarData.map(item => item.category),
              datasets: [{
                label: 'User Interest',
                data: data.radarData.map(item => item.views),
                backgroundColor: 'rgba(113, 73, 156, 0.2)',      
                borderColor: 'rgba(142, 95, 191, 1)',             
                pointBackgroundColor: 'rgba(160, 119, 197, 1)',   
                pointBorderColor: '#fff',
                pointHoverBackgroundColor: '#fff',
                pointHoverBorderColor: 'rgba(160, 119, 197, 1)'  
              }]
            });
            return;
          default:
            throw new Error('Unknown graph type');
        }

        setChartData({ labels: data.labels, datasets });
      } catch (err) {
        console.error('Error fetching chart data:', err);
        setError(err.message || 'Failed to load chart data');
        setChartData({ labels: [], datasets: [] });
      } finally {
        setLoading(false);
      }
    };

    fetchData();

    const interval = setInterval(() => {
      setShowChart(false); 
      setTimeout(() => {
        setChartType(prev => {
          const currentIndex = chartTypesToCycle.indexOf(prev);
          const nextIndex = (currentIndex + 1) % chartTypesToCycle.length;
          return chartTypesToCycle[nextIndex];
        });
        setShowChart(true); 
      }, 500);
    }, 5000);

    return () => clearInterval(interval);
  }, [chartType]);

  const ChartComponent = {
    'popularity-trend': Line,
    'category-distribution': Pie,
    'engagement-by-source': Bar,
    'news-by-location': Doughnut,
    'user-interest-compass': Pie,
    'user-interest-compass-radar': Radar

  }[chartType] || Line;

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: chartType === 'category-distribution' || chartType === 'user-interest-compass',
        position: 'bottom',
        labels: {
          color: 'white',
          padding: 10,
          font: {
            size: 12
          }
        }
      },
      title: {
        display: true,
        text: chartType.split('-').map(word => 
          word.charAt(0).toUpperCase() + word.slice(1)
        ).join(' '),
        color: 'white',
        font: {
          size: 16,
          weight: 'bold'
        },
        padding: {
          top: 10,
          bottom: 20
        }
      },
      tooltip: {
        backgroundColor: 'rgba(0, 0, 0, 0.7)',
        titleColor: 'white',
        bodyColor: 'white',
        padding: 10,
        cornerRadius: 4,
        callbacks: {
          label: function (context) {
            const label = context.label || '';
            const value = context.raw !== undefined ? context.raw : context.parsed;
            return `${label}: ${value}`;
          }
        }
      }
    },
    scales: ['category-distribution', 'user-interest-compass'].includes(chartType)
    ? undefined
    : {
        x: { /* ... */ },
        y: { /* ... */ }
      }
  };

  const getIcon = (type) => {
    const icons = {
      'popularity-trend': faChartLine,
      'category-distribution': faChartPie,
      'engagement-by-source': faChartBar,

    };
    return icons[type] || faChartLine;
  };

  return (
    
    <div className="graphs-content">
      <div className="chart-container">
        {loading ? (
          <div className="loading-spinner"> </div>
        ) : error ? (
          <div className="error-message">{error}</div>
        ) : (
         <div className="chart-container">
          <h3 className={`graph-title ${showChart ? 'fade-in' : ''}`}>
            {{
              'popularity-trend': 'News Popularity Over Time',
              'category-distribution': 'News by Category',
              'engagement-by-source': 'Engagement by Source',
              'user-interest-compass': 'Your Interests Breakdown',
              'user-interest-compass-radar': 'Your Interest Radar',
              'news-by-location': 'News Count by Location',
            }[chartType]}
          </h3>
          <div className={`fade-chart ${showChart ? '' : 'hidden'}`}>
            <ChartComponent data={chartData} options={chartOptions} />
          </div>
        </div>
        )}
      </div>
    </div>
  );
};

export default Graphs;