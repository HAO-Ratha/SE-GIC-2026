# Lab 03 - Task 1: Schema Validation Experiments

## 1. Renaming `sku` to `code` in `V1__init.sql`
Error message recorded when Hibernate validates the schema:
org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing column [sku] in table [product]

## 2. Changing Sequence Increment to 1 (`INCREMENT BY 1`)
Error message recorded when Hibernate checks sequence parameters against `allocationSize = 50`:
org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: sequence [product_seq] increment size mismatch; found [1], expected [50]