### 1. GET all products
GET http://localhost:8080/api/products

### 2. Search products
GET http://localhost:8080/api/products?q=laptop

### 3. GET product by ID
GET http://localhost:8080/api/products/1

### 4. GET nonexistent product
GET http://localhost:8080/api/products/999

### 5. POST create product
POST http://localhost:8080/api/products
Content-Type: application/json

{
  "name": "Tablet",
  "price": 299.99,
  "stock": 10
}

### 6. POST invalid product
POST http://localhost:8080/api/products
Content-Type: application/json

{
  "name": "",
  "price": null,
  "stock": 10
}

### 7. PATCH update stock
PATCH http://localhost:8080/api/products/1/stock
Content-Type: application/json

{
  "delta": -2
}

### 8. PATCH insufficient stock
PATCH http://localhost:8080/api/products/1/stock
Content-Type: application/json

{
  "delta": -999
}

### 9. DELETE existing product
DELETE http://localhost:8080/api/products/1

### 10. DELETE nonexistent product
DELETE http://localhost:8080/api/products/999