import { Link, useNavigate } from "react-router-dom";
import { useContext } from "react";
import { UserContext } from "../userContext";

function Header() {
    const { user, setUserContext } = useContext(UserContext);
    const navigate = useNavigate();

    const handleLogout = (e) => {
        e.preventDefault();
        localStorage.removeItem("user");
        localStorage.removeItem("token");
        setUserContext(null);
        navigate("/"); 
    };

    return (
        <header>
            <h1>Apopulis</h1>
            <nav>
                {user ? (
                    <> 
                    <Link to="/" onClick={handleLogout}>Logout</Link>
                    </>
                ) : (
                    <>
                            <Link to="/login">Login</Link> | <Link to="/register">Register</Link> | <Link to="/">Homepage</Link>
                    </>
                )}
            </nav>
        </header>
    );
}

export default Header;
