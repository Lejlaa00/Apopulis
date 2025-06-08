import { useState } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { UserContext } from "./userContext";
import Layout from "./components/Layout";
import Login from "./components/Login";
import Register from "./components/Register";
import Home from "./components/Home";
import NewsDetail from './components/NewsDetail';
import Profile from './components/Profile';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import 'leaflet/dist/leaflet.css';


function App() {
  const [user, setUser] = useState(localStorage.user ? JSON.parse(localStorage.user) : null);

  const setUserContext = (userInfo) => {
    localStorage.setItem("user", JSON.stringify(userInfo));
    setUser(userInfo);
  };

  return (
    <BrowserRouter>
      <UserContext.Provider value={{ user, setUserContext }}>
        <Layout>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/news/:id" element={<NewsDetail />} />
            <Route path="/profile" element={<Profile />} />
          </Routes>
        </Layout>

        <ToastContainer
          position="top-right"
          autoClose={3000}
          hideProgressBar={false}
          newestOnTop={false}
          closeOnClick
          pauseOnFocusLoss
          draggable
          pauseOnHover
          theme="dark"
        />
      </UserContext.Provider>
    </BrowserRouter>
  );
}

export default App;
