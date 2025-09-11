import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Home from './pages/Home';
import StockDetail from './pages/StockDetail';
import OrderForm from './pages/OrderForm';
import OrderSuccess from './pages/OrderSuccess';
import OrderError from './pages/OrderError';
import Orders from './pages/Orders';
import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';

function App() {
  return (
      <Router>
        <div className="App">
          <Navbar />
          <div className="container mt-4">
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/home" element={<Home />} />
              <Route path="/stocks/:symbol" element={<StockDetail />} />
              <Route path="/orders/new" element={<OrderForm />} />
              <Route path="/orders/success" element={<OrderSuccess />} />
              <Route path="/orders/error" element={<OrderError />} />
              <Route path="/orders" element={<Orders />} />
            </Routes>
          </div>
        </div>
      </Router>
  );
}

export default App;
