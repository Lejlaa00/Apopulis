import { useState } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { UserContext } from "./userContext";
import Header from "./components/Header";
import Login from "./components/Login";
import Register from "./components/Register";
import Home from "./components/Home";

function App() {
  const [user, setUser] = useState(localStorage.user ? JSON.parse(localStorage.user) : null);

  const updateUserData = (userInfo) => {
    localStorage.setItem("user", JSON.stringify(userInfo));
    setUser(userInfo);
  }

  return (
    <BrowserRouter>
      <UserContext.Provider value={{ user: user, setUserContext: updateUserData }}>
        <div className="App">
          <Header />
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
          </Routes>
        </div>
      </UserContext.Provider>
    </BrowserRouter>
  );
}

export default App;
