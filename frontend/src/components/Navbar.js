import React from 'react';
import { Link } from 'react-router-dom';

function Navbar() {
    return (
        <nav className="navbar navbar-expand-lg navbar-dark bg-success">
            <div className="container">
                <Link className="navbar-brand" to="/">Green Trading</Link>
                <button className="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
                    <span className="navbar-toggler-icon"></span>
                </button>
                <div className="collapse navbar-collapse" id="navbarNav">
                    <ul className="navbar-nav">
                        <li className="nav-item">
                            <Link className="nav-link" to="/">Dashboard</Link>
                        </li>
                        <li className="nav-item">
                            <Link className="nav-link" to="/orders">Orders</Link>
                        </li>
                        <li className="nav-item">
                            <Link className="nav-link" to="/portfolio">Portfolio</Link>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>
    );
}

export default Navbar;