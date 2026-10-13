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

- Les commandes ne sont créées qu'en statut `DRAFT`. Via l'API REST, les lignes ne sont pas modifiables après création ; seul le panier de l'interface web (voir plus bas) modifie les lignes d'une commande `DRAFT`.
- L'unicité de la référence produit n'est pas exigée par les règles métier : elle n'est pas imposée techniquement.
- La suppression d'un client ou d'un produit référencé par une commande existante est bloquée par la contrainte de clé étrangère en base (HTTP 409), aucune règle métier ne précisant ce cas.

## Lancer l'application

```bash
docker compose up -d        # démarre PostgreSQL
./mvnw spring-boot:run
```

Configuration par défaut (`src/main/resources/application.properties`) : base `iaspringlab` sur `localhost:5432`, utilisateur/mot de passe `iaspringlab`/`iaspringlab`.

## Tester l'API via Swagger UI

Une fois l'application démarrée (étape ci-dessus), ouvrir :

```
http://localhost:8080/swagger-ui.html
```

Chaque endpoint y est documenté avec son schéma de requête/réponse et un bouton **"Try it out"** pour l'exécuter directement depuis le navigateur (créer un client, un produit, une commande, changer un statut, etc.), sans outil externe (Postman/curl). La spécification OpenAPI brute est disponible sur `http://localhost:8080/v3/api-docs`.

## Interface web

Une fois l'application démarrée, ouvrir `http://localhost:8080/` (pages Thymeleaf, authentification Spring Security par formulaire).

| Compte | Mot de passe | Rôle | Accès |
|--------|--------------|------|-------|
| `admin` | `admin` | ADMIN | `/admin/products`, `/admin/categories` : créer, modifier, supprimer produits et catégories |
| `user`  | `user`  | USER  | `/shop` : catalogue, panier, commande, historique des commandes |

Les deux comptes sont créés au démarrage s'ils n'existent pas (table `app_users`, mots de passe hachés en BCrypt). Le compte `user` est rattaché au client `user@ia-spring-lab.local`, créé lui aussi au démarrage.

Fonctionnement :

- **Catégories** : un produit peut avoir zéro, une ou plusieurs catégories. Supprimer une catégorie conserve les produits, qui en sont simplement détachés.
- **Panier** : c'est la commande `DRAFT` du client, enregistrée en base au fil des ajouts (on la retrouve donc après une déconnexion). Ajouter un produit déjà présent augmente sa quantité ; retirer la dernière ligne supprime le panier (une commande contient au moins une ligne). Un produit inactif ne peut pas être ajouté.
- **Commander** : le bouton « Commander » fait passer le panier de `DRAFT` à `CONFIRMED`.
- **Mes commandes** : liste des commandes passées (hors panier en cours), de la plus récente à la plus ancienne, avec leur détail.
- L'API REST `/api/**` et Swagger UI restent accessibles sans authentification, comme avant.

## Tests

```bash
./mvnw test
```

- Tests unitaires (`*ServiceTest`, `OrderStatusTest`) : Mockito + AssertJ, sans contexte Spring.
- Tests de contrôleur (`*ControllerTest`) : `@WebMvcTest` + MockMvc, service mocké.
- Tests d'intégration (`*IntegrationTest`, `IaSpringLabApplicationTests`) : `@SpringBootTest` / `@DataJpaTest` + Testcontainers (PostgreSQL réel). **Nécessitent un démon Docker actif.**
