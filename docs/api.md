# API Reference

Base URL: `/api`. All request and response bodies are JSON.

Errors use a single shape:

```json
{
  "timestamp": "2026-10-13T08:00:00Z",
  "status": 422,
  "error": "Unprocessable Content",
  "message": "Une commande doit contenir au moins une ligne",
  "path": "/api/orders",
  "details": []
}
```

| Status | Meaning |
|--------|---------|
| 400 | Body validation failed (`details` lists the field errors) |
| 404 | Resource not found |
| 409 | Uniqueness conflict (client email, product reference) |
| 422 | Business rule violated |

## Clients

### `POST /api/clients` → 201

```json
{ "firstName": "Ada", "lastName": "Lovelace", "email": "ada@example.com" }
```

```json
{ "id": 1, "firstName": "Ada", "lastName": "Lovelace", "email": "ada@example.com" }
```

`409` if the email already exists.

### `GET /api/clients` · `GET /api/clients/{id}`

Returns a client or a list of clients. `404` if the id is unknown.

## Products

### `POST /api/products` → 201

```json
{ "reference": "REF-1", "name": "Keyboard", "price": 49.90 }
```

```json
{ "id": 1, "reference": "REF-1", "name": "Keyboard", "price": 49.90, "active": true }
```

`409` if the reference already exists.

### `PATCH /api/products/{id}/status`

```json
{ "active": false }
```

Activates or deactivates the product.

### `GET /api/products` · `GET /api/products/{id}`

Returns a product or a list of products.

## Orders

### `POST /api/orders` → 201

Creates a `DRAFT` order with at least one line.

```json
{
  "clientId": 1,
  "lines": [
    { "productId": 1, "quantity": 2 },
    { "productId": 2, "quantity": 1 }
  ]
}
```

```json
{
  "id": 10,
  "clientId": 1,
  "clientName": "Ada Lovelace",
  "status": "DRAFT",
  "lines": [
    { "id": 100, "productId": 1, "productReference": "REF-1", "quantity": 2, "unitPrice": 49.90, "lineTotal": 99.80 }
  ],
  "total": 99.80
}
```

- `400` if the body fails validation (no line, or a quantity is not strictly positive).
- `404` if the client or a product is unknown.
- `422` if a product is inactive.

### `POST /api/orders/{id}/lines`

Adds a line to a `DRAFT` order and recomputes the total.

```json
{ "productId": 3, "quantity": 5 }
```

`400` if the quantity is not strictly positive; `422` if the order is not `DRAFT` or if
the product is inactive.

### `POST /api/orders/{id}/confirm`

`DRAFT → CONFIRMED`. `422` for any other source status.

### `POST /api/orders/{id}/cancel`

`DRAFT → CANCELLED` or `CONFIRMED → CANCELLED`. `422` if the order is already
`CANCELLED`.

### `GET /api/orders` · `GET /api/orders/{id}`

Returns an order or a list of orders.
