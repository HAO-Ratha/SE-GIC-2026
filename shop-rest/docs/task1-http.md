# Task 1: HTTP transcript

Run each command with the server running and paste the full output (headers and body) under it.

## 1. List all products

```
curl.exe -s http://localhost:8080/api/products |
    ConvertFrom-Json |
    ConvertTo-Json -Depth 10
```

```
{
    "value":  [
                  {
                      "id":  1,
                      "name":  "Teclado",
                      "price":  24.90,
                      "sku":  "TP-01",
                      "stock":  12,
                      "version":  1
                  },
                  {
                      "id":  2,
                      "name":  "Mouse",
                      "price":  9.90,
                      "sku":  "MG-02",
                      "stock":  40,
                      "version":  1
                  },
                  {
                      "id":  3,
                      "name":  "T-shirt",
                      "price":  39.90,
                      "sku":  "TS-03",
                      "stock":  8,
                      "version":  1
                  }
              ],
    "Count":  3
}
```

## 2. One product

```
curl.exe -s localhost:8080/api/products/MG-02
    ConvertFrom-Json |
    ConvertTo-Json -Depth 10
```

```
{
    "id":  2,
    "name":  "Mouse",
    "price":  9.90,
    "sku":  "MG-02",
    "stock":  40,
    "version":  1
}
```

## 3. Unknown id (explicit 404)

```
curl.exe -s localhost:8080/api/products/99|
    ConvertFrom-Json |
    ConvertTo-Json -Depth 10
```

```
nothing 
```

## 4. Not a number (no route matches, 404)

```
curl.exe -s localhost:8080/api/products/abc|
    ConvertFrom-Json |
    ConvertTo-Json -Depth 10
```

```
nothing.
```