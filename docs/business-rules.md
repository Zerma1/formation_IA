# Business Rules

## Customer

A customer must have:

- first name
- last name
- unique email

## Product

A product has:

- reference
- name
- price
- active status

Inactive products cannot be added to new orders.

## Order

An order:

- belongs to exactly one customer
- contains at least one line
- has a strictly positive quantity for each line
- stores the product price at the time of ordering

## Total

The order total is:

sum(quantity × unitPrice)

## Status

Allowed transitions:

DRAFT -> CONFIRMED
DRAFT -> CANCELLED

CONFIRMED -> CANCELLED

No other transition is allowed.
