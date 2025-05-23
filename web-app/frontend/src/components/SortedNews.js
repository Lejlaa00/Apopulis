import React, { useState, useEffect } from 'react';
import SortedNewsHeader from './SortedNewsHeader';
import '../css/sortedNews.css';

export default function SortedNews() {
  const [categories, setCategories] = useState([]);
  const [filter, setFilter] = useState('trending');
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    async function fetchCategories() {
      const res = await fetch('/api/categories');
      const data = await res.json();
      setCategories(data.categories || []);
    }
    fetchCategories();
  }, []);

  // Ovdje dodaj logiku za filtriranje i pretraživanje

  return (
    <div className="sorted-news">
      <SortedNewsHeader
        categories={categories}
        onFilterChange={setFilter}
        onCategoryChange={setSelectedCategory}
        onSearch={setSearchTerm}
      />
      {/* Ovdje ide prikaz filtriranih i pretraženih vijesti */}
    </div>
  );
}
