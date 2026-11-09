# Product REST API Session Log

## 1. Create a Product (201 Created)
```bash
curl -i -X POST localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"USB-C cable","price":7.90,"stock":25}'
```
```http
HTTP/1.1 201 Created
Location: /api/products/1
Content-Type: application/json

{"id":1,"name":"USB-C cable","price":7.90,"stock":25}
```

## 2. Invalid Payload Validation (400 Bad Request)
```bash
curl -i -X POST localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"","price":0,"stock":-5}'
```
```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"about:blank","title":"Bad Request","status":400,"detail":"Request validation failed","instance":"/api/products","errors":[{"field":"name","message":"Name cannot be empty"},{"field":"price","message":"Price must be at least 0.01"},{"field":"stock","message":"Stock cannot be negative"}]}
```

## 3. Insufficient Stock Conflict (409 Conflict)
```bash
curl -i -X PATCH localhost:8080/api/products/1/stock \
  -H 'Content-Type: application/json' \
  -d '{"delta":-30}'
```
```http
HTTP/1.1 409 Conflict
Content-Type: application/problem+json

{"type":"about:blank","title":"Conflict","status":409,"detail":"Product 1 has 25 units, cannot remove 30","instance":"/api/products/1/stock"}
```

## 4. Product Not Found (404 Not Found)
```bash
curl -i localhost:8080/api/products/999
```
```http
HTTP/1.1 404 Not Found
Content-Type: application/problem+json

{"type":"about:blank","title":"Not Found","status":404,"detail":"Product 999 not found","instance":"/api/products/999"}
```

## 5. Delete Product (204 No Content)
```bash
curl -i -X DELETE localhost:8080/api/products/1
```
```http
HTTP/1.1 204 No Content
```