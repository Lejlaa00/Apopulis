import React from "react";
import "../css/about.css"; 

const AboutUs = () => {
  return (
    <div className="page-content">
      <h1>About Us</h1>
      <p>
        Apopulis is a smart Slovenian news aggregator developed by a passionate team of students. 
        We built this platform to make news more accessible, personalized, and filtered in real time.
      </p>
      <p>
        Every 20 minutes, our backend scrapes and categorizes news using natural language processing 
        and saves it to our database for you to browse, sort, and read in one clean interface.
      </p>
    </div>
  );
};

export default AboutUs;
