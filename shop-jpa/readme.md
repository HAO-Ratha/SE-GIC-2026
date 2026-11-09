# Lab 03 - Task 1: Schema Validation Experiments

## 1. Renaming `sku` to `code` in `V1__init.sql`
Error message recorded when Hibernate validates the schema:
org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [sku] in table [product]

## 2. Changing Sequence Increment to 1 (`INCREMENT BY 1`)
Error message recorded when Hibernate checks sequence parameters against `allocationSize = 50`:
org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: sequence [product_seq] increment size mismatch; found [1], expected [50]

# Lab 3 – Task 3: Repositories, Queries, Projections and Paging

Project: `edu.itc.se:shop-jpa` (Spring Boot, Spring Data JPA)

## What I built

- Spring Data repositories for Customer, Product and Order
- Derived queries, JPQL queries (with join fetch), and one native query
- Projections: an interface projection and a record projection
- A search with Specifications (optional filters)
- A data seeder for the `dev` profile
- Three REST endpoints with paging and sorting
- Tests with `@DataJpaTest`

## Project structure

```
src/main/java/edu/itc/se/shop_jpa/
├── domain/        entities (from Lab 1 and 2)
├── repository/    CustomerRepository, ProductRepository,
│                  OrderRepository, ProductSpecs
├── service/       ProductService, CustomerOrderService
├── seed/          DataSeeder
└── web/           OrderController, ProductController, CustomerController
    └── dto/       OrderSummary, ProductView, OrderDto, OrderLineDto

src/test/java/edu/itc/se/shop_jpa/repository/
└── OrderRepositoryTest
```

## Queries

| Type | Method |
|------|--------|
| Derived | `findByCustomerEmailAndStatusOrderByPlacedAtDesc`, `countByStatus`, `findFirstByCustomerIdOrderByPlacedAtDesc` |
| Derived | `existsBySku`, `findTop3ByCategoryOrderByPriceAmountDesc`, `findByTagsName` |
| JPQL | `findWithLinesByCustomerEmail` (uses `join fetch`) |
| JPQL | `countLinesByStatus` (uses `count`) |
| Native | `findLowStock(int min)` |

## Projections

- `OrderSummary` is an interface projection with a nested `CustomerName`. It selects only `id`, `status`, `placed_at` and the customer `name`.
- `ProductView` is a record projection. `findByCategory` uses a `@Query` constructor expression, because the record field `amount` does not match a direct property of `Product` (the entity has `price.amount`).

## Specifications

`ProductSpecs.filter(q, category, maxPrice, inStock)` uses `Specification.allOf`. Only the filters that are not `null` are applied.

## Endpoints

| Endpoint | Returns |
|----------|---------|
| `GET /api/orders?status=PAID&page=0&size=5&sort=placedAt,desc` | `Page<OrderSummary>` |
| `GET /api/products?q=&category=&maxPrice=&inStock=&page=&size=` | `Page<ProductView>` |
| `GET /api/customers/{email}/orders` | `List<OrderDto>` (with lines) |

No controller returns an entity. The customer endpoint runs inside a `@Transactional(readOnly = true)` service method.

## How to run

Start the app with the `dev` profile (this runs the seeder):

```powershell
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Run the tests:

```powershell
./mvnw -q test -Dtest=OrderRepositoryTest
```

The HTTP calls and their responses are in `api-session.md`.

## Tests (OrderRepositoryTest)

1. Derived query returns the newest order first
2. Fetch join: `Hibernate.isInitialized(order.getLines())` is `true` after the query, and `false` after a plain `findById` + `clear`
3. Native query `findLowStock`
4. Interface projection with nested customer name
5. `countByStatus` and `countLinesByStatus`

## Typo exercise: boot error

I changed a method name to `findByCustomerEmali...` and started the app. Spring Data reads every repository method at startup, so the app failed to boot.

Error:

```
PASTE YOUR REAL BOOT ERROR HERE
```

It looks something like this:

```
Caused by: org.springframework.data.core.PropertyReferenceException:
No property 'emali' found for type 'Customer'; Did you mean 'email'?
```

**What I learned:** derived query names are checked at startup, not when the method is called. A wrong property name stops the app right away. I fixed the typo after this test.

## What I learned

- Derived queries save a lot of code, but long names get hard to read. Use `@Query` then.
- `join fetch` loads the lines in one query, so there is no N+1 problem.
- Interface projections select only the columns they need. I checked this in the SQL log.
- Specifications let me combine optional filters without writing many query methods.