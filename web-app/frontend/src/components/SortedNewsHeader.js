import React, { useState } from 'react';
import HoverDropdown from './HoverDropdown'; 
import '../css/sortedNewsHeader.css';

export default function SortedNewsHeader({ categories, onFilterChange, onCategoryChange, onSearch, isAuthenticated }) {
  const [filter, setFilter] = useState('trending');
  const [category, setCategory] = useState('all');
  const [searchText, setSearchText] = useState('');

  const handleFilterChange = (val) => {
    setFilter(val);
    onFilterChange(val);
  };

  const handleCategoryChange = (val) => {
    setCategory(val);
    onCategoryChange(val);
    onFilterChange('category');
  };

  const handleSearchChange = (e) => {
    const val = e.target.value;
    setSearchText(val);
    onSearch(val);
  };

  const filterOptions = [
    { value: 'latest', label: 'Latest' },
    { value: 'trending', label: 'Trending' },
    ...(isAuthenticated ? [{ value: 'bookmark', label: 'Bookmark' }] : [])
  ];

  const categoryOptions = [
  { value: 'all', label: 'All Categories' },
  ...categories.map(cat => ({ value: cat.name, label: cat.name }))
];


return (
  <div className="sorted-news-header">
    <div className="header-box">
      <div className="dropdown-group">
        <HoverDropdown
          options={filterOptions}
          selected={filter}
          onChange={handleFilterChange}
        />
        <HoverDropdown
          options={categoryOptions}
          selected={category}
          onChange={handleCategoryChange}
        />
      </div>

      <input
        type="text"
        placeholder="Search news..."
        value={searchText}
        onChange={handleSearchChange}
        className="search-input"
      />
    </div>
  </div>
);
}
