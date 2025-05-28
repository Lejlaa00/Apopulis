import { useEffect, useState } from 'react';
import { Line, Pie, Bar, Scatter } from 'react-chartjs-2';
import { Chart as ChartJS } from 'chart.js/auto';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faChartLine, faChartPie, faChartBar, faChartColumn } from '@fortawesome/free-solid-svg-icons';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5001/api';

const Graphs = () => {
  const [chartType, setChartType] = useState('popularity-trend');
  const [chartData, setChartData] = useState({ labels: [], datasets: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);



  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetch(`${API_URL}/stats/${chartType}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({})
        });
        
        if (!response.ok) {
          throw new Error('Failed to fetch data');
        }
        
        const data = await response.json();
        console.log(`Data for ${chartType}:`, data);
        let datasets = [];

        // Check if data has the expected properties
        if (!data.labels || data.labels.length === 0) {
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
              borderColor: '#36A2EB',
              backgroundColor: 'rgba(54, 162, 235, 0.2)',
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
                'rgba(54, 162, 235, 0.8)',   // Primary blue
                'rgba(54, 162, 235, 0.6)',   // Lighter blue
                'rgba(54, 162, 235, 0.4)',   // Even lighter blue
                'rgba(54, 162, 235, 0.2)',   // Very light blue
                'rgba(54, 162, 235, 1.0)'    // Full blue
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
              backgroundColor: 'rgba(54, 162, 235, 0.6)',
              borderColor: 'rgba(54, 162, 235, 1)',
              borderWidth: 2,
              barThickness: 40,
              borderRadius: 4,
              hoverBackgroundColor: 'rgba(54, 162, 235, 0.8)',
              hoverBorderColor: 'rgba(54, 162, 235, 1)'
            }];
            break;

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
  }, [chartType]);

  const ChartComponent = {
    'popularity-trend': Line,
    'category-distribution': Pie,
    'engagement-by-source': Bar,

  }[chartType] || Line;

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: chartType === 'category-distribution',
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
        cornerRadius: 4
      }
    },
    scales: chartType !== 'category-distribution' ? {
      x: {
        grid: {
          color: 'rgba(255, 255, 255, 0.1)'
        },
        ticks: {
          color: 'white',
          font: {
            size: 11
          }
        }
      },
      y: {
        grid: {
          color: 'rgba(255, 255, 255, 0.1)'
        },
        ticks: {
          color: 'white',
          font: {
            size: 11
          }
        }
      }
    } : undefined
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
      <select 
        value={chartType} 
        onChange={(e) => setChartType(e.target.value)}
        className="chart-selector"
      >
        {[
          { value: 'popularity-trend', label: 'Popularity Trend' },
          { value: 'category-distribution', label: 'Category Distribution' },
          { value: 'engagement-by-source', label: 'Engagement by Source' },
        ].map(option => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>

      <div className="chart-container">
        {loading ? (
          <div className="loading-spinner">Loading...</div>
        ) : error ? (
          <div className="error-message">{error}</div>
        ) : (
          <ChartComponent data={chartData} options={chartOptions} />
        )}
      </div>
    </div>
  );
};

export default Graphs;