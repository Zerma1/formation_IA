# AI Spring Lab

API REST de gestion de commandes, construite avec Java 21 et Spring Boot.

## Stack technique

- Java 21, Maven
- Spring Web, Spring Data JPA, Bean Validation
- PostgreSQL (exécution), Testcontainers (tests d'intégration)
- JUnit 5, Mockito, AssertJ, Spring Boot Test

## Architecture

Flux imposé : `Controller -> Service -> Repository` (voir `AGENTS.md`).

Organisation par domaine métier :

```
com.example.iaspringlab
├── customer   (Customer, CustomerRepository, CustomerService, CustomerController, DTOs)
├── product    (Product, ProductRepository, ProductService, ProductController, DTOs)
├── order      (Order, OrderLine, OrderStatus, OrderRepository, OrderService, OrderController, DTOs)
└── exception  (exceptions métier + GlobalExceptionHandler)
```

Les entités JPA ne sont jamais exposées directement : chaque endpoint échange des DTO (records).

## Règles métier implémentées

Référence unique : `docs/business-rules.md`.

- **Client** : prénom, nom, email unique (contrainte DB + vérification applicative).
- **Produit** : référence, nom, prix, statut actif/inactif. Un produit inactif ne peut pas être ajouté à une nouvelle commande (`InactiveProductException` → HTTP 422).
- **Commande** : appartient à exactement un client, contient au moins une ligne (`@NotEmpty`), quantité strictement positive par ligne (`@Positive`), le prix du produit est figé ("snapshot") sur la ligne au moment de la création.
- **Calcul** : total de ligne = quantité × prix unitaire ; total de commande = somme des lignes (`Order.total()`).
- **Statuts** : transitions autorisées `DRAFT -> CONFIRMED`, `DRAFT -> CANCELLED`, `CONFIRMED -> CANCELLED`. Toute autre transition lève `InvalidOrderStatusTransitionException` (HTTP 422), encapsulée dans `OrderStatus.canTransitionTo`.

## Endpoints

| Méthode | URL                          | Description                                  |
|---------|------------------------------|-----------------------------------------------|
| POST    | `/api/customers`             | Créer un client                               |
| GET     | `/api/customers`             | Lister les clients                            |
| GET     | `/api/customers/{id}`        | Récupérer un client                           |
| PUT     | `/api/customers/{id}`        | Modifier un client                            |
| DELETE  | `/api/customers/{id}`        | Supprimer un client                           |
| POST    | `/api/products`              | Créer un produit                              |
| GET     | `/api/products`              | Lister les produits                           |
| GET     | `/api/products/{id}`         | Récupérer un produit                          |
| PUT     | `/api/products/{id}`         | Modifier un produit (y compris actif/inactif) |
| DELETE  | `/api/products/{id}`         | Supprimer un produit                          |
| POST    | `/api/orders`                | Créer une commande (DRAFT)                    |
| GET     | `/api/orders`                | Lister les commandes (`?customerId=` optionnel) |
| GET     | `/api/orders/{id}`           | Récupérer une commande                        |
| PATCH   | `/api/orders/{id}/status`    | Changer le statut (`{"status": "CONFIRMED"}`) |

Codes d'erreur : `400` (validation Bean Validation), `404` (ressource absente), `409` (email dupliqué / conflit d'intégrité), `422` (règle métier violée : produit inactif, transition de statut invalide).

## Hypothèses (non couvertes explicitement par `business-rules.md`)

- Les commandes ne sont créées qu'en statut `DRAFT` ; aucune modification des lignes après création n'est prévue par les règles métier, elle n'est donc pas implémentée.
- L'unicité de la référence produit n'est pas exigée par les règles métier : elle n'est pas imposée techniquement.
- La suppression d'un client ou d'un produit référencé par une commande existante est bloquée par la contrainte de clé étrangère en base (HTTP 409), aucune règle métier ne précisant ce cas.

## Lancer l'application

```bash
docker compose up -d        # démarre PostgreSQL
./mvnw spring-boot:run
```

Configuration par défaut (`src/main/resources/application.properties`) : base `iaspringlab` sur `localhost:5432`, utilisateur/mot de passe `iaspringlab`/`iaspringlab`.

## Tests

```bash
./mvnw test
```

- Tests unitaires (`*ServiceTest`, `OrderStatusTest`) : Mockito + AssertJ, sans contexte Spring.
- Tests de contrôleur (`*ControllerTest`) : `@WebMvcTest` + MockMvc, service mocké.
- Tests d'intégration (`*IntegrationTest`, `IaSpringLabApplicationTests`) : `@SpringBootTest` / `@DataJpaTest` + Testcontainers (PostgreSQL réel). **Nécessitent un démon Docker actif.**
