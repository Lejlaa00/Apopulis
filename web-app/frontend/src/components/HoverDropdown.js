import React, { useState, useRef, useEffect } from 'react';

function HoverDropdown({ options, selected, onChange }) {
  const [open, setOpen] = useState(false);
  const dropdownRef = useRef(null);
  const hoverTimeout = useRef(null);

  // Otvori dropdown odmah na hover
  const handleMouseEnter = () => {
    clearTimeout(hoverTimeout.current);
    setOpen(true);
  };

  // Zakašnjelo zatvaranje dropdowna na mouse leave
  const handleMouseLeave = () => {
    hoverTimeout.current = setTimeout(() => {
      setOpen(false);
    }, 150);
  };

  // Zatvori dropdown ako se klikne izvan
  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setOpen(false);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      clearTimeout(hoverTimeout.current);
    };
  }, []);

  return (
    <div
      className="hover-dropdown"
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      ref={dropdownRef}
      tabIndex={0}
    >
      <div className="hover-dropdown-selected">
        {options.find(opt => opt.value === selected)?.label || selected}
      </div>
      {open && (
        <ul className="hover-dropdown-list">
          {options.map(opt => (
            <li
              key={opt.value}
              className="hover-dropdown-item"
              onClick={() => onChange(opt.value)}
              onKeyDown={e => {
                if (e.key === 'Enter' || e.key === ' ') {
                  onChange(opt.value);
                }
              }}
              tabIndex={0}
            >
              {opt.label}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default HoverDropdown;
